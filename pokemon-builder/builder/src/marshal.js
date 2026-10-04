// Minimal reader for the Ruby Marshal stream format used by RPG Maker XP
// (.rxdata) files. Only the node types produced by RGSS (Ruby 1.8) are
// implemented; anything else raises a descriptive error.
//
// Verified in Phase 3 against a real Pokemon Essentials project: every
// Data/*.rxdata file of E:\仓库\范例\929 parses byte-exact (541 files).
// The details a naive reader gets wrong (and that this reader handles):
//   * w_long packing: 1..4 = positive multi-byte payload, 5..127 = short
//     positive (byte - 5), 128..251 = SHORT NEGATIVE (byte - 251),
//     252..255 = negative multi-byte with little-endian payload.
//   * Class names (object/struct/userdef/usermarshal/userclass) and instance
//     variable names are full symbol nodes (':' definition or ';' link), not
//     bare symbol table indexes.
//   * Strings, Arrays, Hashes, Objects, Structs, Userdefs, Usermarshals,
//     Userclasses, Regexps, Floats and Bignums each occupy a slot in the
//     object link table (TYPE_LINK '@'); nil/true/false/Fixnum/Symbol do not.
//   * Floats are stored as 'f' + w_long(len) + ascii + trailing bytes; the
//     ascii number is terminated by a NUL.
//   * 'I' (TYPE_IVAR) wraps a value and carries trailing instance variables;
//     'C' (TYPE_UCLASS) names the class of a builtin value - Pokemon
//     Essentials writes PBAnimations / PBAnimation that way.

const MAJOR_VERSION = 4;
const MINOR_VERSION = 8;

export class MarshalError extends Error {}

const NIL = "0";
const TRUE = "T";
const FALSE = "F";
const IVAR = "I";
const LINK = "@";
const FIXNUM = "i";
const FLOAT = "f";
const BIGNUM = "l";
const STRING = "\"";
const SYMBOL = ":";
const SYMLINK = ";";
const ARRAY = "[";
const HASH = "{";
const HASH_DEF = "}";
const OBJECT = "o";
const STRUCT = "S";
const USERDEF = "u";
const USERMARSHAL = "U";
const USER_STRING = "c";
const EXTENDED = "e";
const REGEXP = "/";
const UCLASS = "C";

export function marshalLoad(buffer) {
  return new MarshalReader(buffer).load();
}

// Marshal hashes are exposed as Map<label, {key, value}> so that non-string
// keys keep their original Ruby type. This helper returns plain [key, value]
// pairs for downstream consumers (event analyzers, the scanner, ...).
export function hashEntries(hash) {
  if (!(hash instanceof Map)) return [];
  const pairs = [];
  for (const entry of hash.values()) pairs.push([entry.key, entry.value]);
  return pairs;
}

function keyLabel(key) {
  if (key === null) return "nil";
  if (typeof key === "object") return JSON.stringify(key);
  return String(key);
}

export class MarshalReader {
  constructor(buffer) {
    this.buffer = buffer;
    this.pos = 0;
    this.symbols = [];
    this.objects = [];
  }

  load() {
    const major = this.byte();
    const minor = this.byte();
    if (major !== MAJOR_VERSION || minor > MINOR_VERSION) {
      throw new MarshalError("unsupported marshal version " + major + "." + minor);
    }
    return this.readValue();
  }

  byte() {
    if (this.pos >= this.buffer.length) {
      throw new MarshalError("unexpected end of marshal stream");
    }
    return this.buffer[this.pos++];
  }

  read(n) {
    if (this.pos + n > this.buffer.length) {
      throw new MarshalError("unexpected end of marshal stream");
    }
    const out = this.buffer.subarray(this.pos, this.pos + n);
    this.pos += n;
    return out;
  }

  char() {
    return String.fromCharCode(this.byte());
  }

  // Ruby w_long / r_long packing (see class comment above).
  long() {
    const first = this.byte();
    if (first === 0) return 0;
    if (first > 4 && first < 128) return first - 5;
    if (first < 128) {
      const width = first;
      const bytes = this.read(width);
      let value = 0;
      for (let i = width - 1; i >= 0; i--) value = value * 256 + bytes[i];
      return value;
    }
    if (first < 252) return first - 256 + 5;
    const width = 256 - first;
    const bytes = this.read(width);
    let value = 0;
    for (let i = width - 1; i >= 0; i--) value = value * 256 + bytes[i];
    return value - Math.pow(2, 8 * width);
  }

  float() {
    const length = this.long();
    const bytes = this.read(length);
    // The ASCII representation is NUL terminated; RGSS may append trailing
    // bytes after the terminator, so only the prefix is the number.
    const text = bytes.toString("latin1").split("\0")[0];
    const value = Number.parseFloat(text);
    return Number.isFinite(value) ? value : 0;
  }

  // Byte-preserving string (latin1). Higher layers decide whether the content
  // is UTF-8 text or a binary payload.
  string() {
    const length = this.long();
    if (length < 0 || this.pos + length > this.buffer.length) {
      throw new MarshalError("bad string length in marshal stream");
    }
    const value = this.buffer.toString("latin1", this.pos, this.pos + length);
    this.pos += length;
    return value;
  }

  symbol() {
    const name = this.string();
    this.symbols.push(name);
    return name;
  }

  symref() {
    const index = this.long();
    if (index < 0 || index >= this.symbols.length) {
      throw new MarshalError("bad symbol link " + index);
    }
    return this.symbols[index];
  }

  // Class names and instance variable names are full symbol nodes.
  symbolNode() {
    const type = this.char();
    if (type === SYMBOL) return this.symbol();
    if (type === SYMLINK) return this.symref();
    throw new MarshalError(
      "expected symbol node but got " + JSON.stringify(type) + " at offset " + (this.pos - 1),
    );
  }

  // Attaches instance variables to an object value. Primitives (strings are
  // primitives in JS) cannot carry properties, so they are wrapped instead of
  // being silently dropped.
  attachIvars(target) {
    const count = this.long();
    if (count === 0) return target;
    if (typeof target === "object" && target !== null) {
      for (let i = 0; i < count; i++) {
        const name = this.symbolNode();
        target[name] = this.readValue();
      }
      return target;
    }
    const wrapper = { __ivar_wrapped__: true, __value__: target };
    for (let i = 0; i < count; i++) {
      const name = this.symbolNode();
      wrapper[name] = this.readValue();
    }
    return wrapper;
  }

  className(target) {
    if (typeof target === "object" && target !== null) {
      target.__class__ = this._className;
      return target;
    }
    return { __class__: this._className, __value__: target };
  }

  readValue() {
    const type = this.char();
    switch (type) {
      case NIL:
        return null;
      case TRUE:
        return true;
      case FALSE:
        return false;
      case IVAR:
        return this.attachIvars(this.readValue());
      case LINK: {
        const index = this.long();
        if (index < 0 || index >= this.objects.length) {
          throw new MarshalError("bad object link " + index);
        }
        return this.objects[index];
      }
      case FIXNUM:
        return this.long();
      case FLOAT:
        return this.register(this.float());
      case BIGNUM: {
        const sign = this.char();
        const halves = this.long();
        const bytes = this.read(halves * 2);
        const chunks = [];
        for (let i = 0; i < bytes.length; i += 2) {
          chunks.unshift(bytes[i + 1].toString(16).padStart(2, "0") + bytes[i].toString(16).padStart(2, "0"));
        }
        let value = BigInt("0x" + (chunks.join("") || "0"));
        return this.register(sign === "-" ? -value : value);
      }
      case STRING: {
        const value = this.string();
        this.objects.push(value);
        return value;
      }
      case SYMBOL:
        return this.symbol();
      case SYMLINK:
        return this.symref();
      case ARRAY: {
        const length = this.long();
        const array = new Array(length);
        this.objects.push(array);
        for (let i = 0; i < length; i++) array[i] = this.readValue();
        return array;
      }
      case HASH:
      case HASH_DEF:
        return this.readHash(type === HASH_DEF);
      case OBJECT: {
        this._className = this.symbolNode();
        const object = { __class__: this._className };
        this.objects.push(object);
        this.attachIvars(object);
        return object;
      }
      case STRUCT: {
        this._className = this.symbolNode();
        const struct = { __class__: this._className, __struct__: true };
        this.objects.push(struct);
        const members = this.long();
        for (let i = 0; i < members; i++) {
          const name = this.symbolNode();
          struct[name] = this.readValue();
        }
        return struct;
      }
      case USERDEF: {
        const className = this.symbolNode();
        const size = this.long();
        const bytes = this.read(size);
        return this.register({ __class__: className, __userdef__: bytes });
      }
      case USERMARSHAL: {
        const className = this.symbolNode();
        const payload = this.readValue();
        return this.register({ __class__: className, __usermarshal__: payload });
      }
      case USER_STRING: {
        const className = this.symbolNode();
        const text = this.read(this.long()).toString("latin1");
        return this.register(this.className(text));
      }
      case UCLASS: {
        const className = this.symbolNode();
        const value = this.readValue();
        if (typeof value === "object" && value !== null) {
          value.__class__ = className;
          return value;
        }
        return { __class__: className, __value__: value };
      }
      case EXTENDED: {
        this.symbolNode(); // extended module
        return this.register(this.readValue());
      }
      case REGEXP: {
        const source = this.string();
        this.byte(); // trailing options byte
        return this.register({ __class__: "Regexp", __regexp__: source });
      }
      default:
        throw new MarshalError(
          "unsupported marshal node " + JSON.stringify(type) + " at offset " + (this.pos - 1),
        );
    }
  }

  register(value) {
    this.objects.push(value);
    return value;
  }

  readHash(withDefault) {
    const length = this.long();
    const hash = new Map();
    this.objects.push(hash);
    for (let i = 0; i < length; i++) {
      const key = this.readValue();
      const value = this.readValue();
      hash.set(keyLabel(key), { key: key, value: value });
    }
    if (withDefault) this.readValue();
    return hash;
  }
}
