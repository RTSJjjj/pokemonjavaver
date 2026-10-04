// Test-only writer for Ruby 1.8 / RGSS Marshal streams.
//
// The builder itself only READS marshal data; this helper exists so the unit
// tests can build small, deterministic .rxdata fixtures (including edge cases
// such as negative Fixnums, object links and zlib deflated script payloads)
// without having to check binary blobs into the repo.

import { deflateSync } from "node:zlib";

class MarshalWriter {
  constructor() {
    this.out = [];
    this.symbols = new Map();
    this.objects = new Map();
  }

  byte(value) {
    this.out.push(value & 0xff);
    return this;
  }

  raw(bytes) {
    for (const value of bytes) this.out.push(value & 0xff);
    return this;
  }

  long(value) {
    if (value === 0) return this.byte(0);
    if (value > 0 && value < 123) return this.byte(value + 5);
    if (value < 0 && value > -124) return this.byte((value - 5) & 0xff);
    const bytes = [];
    let rest = value;
    if (rest > 0) {
      while (rest > 0 && bytes.length < 4) {
        bytes.push(rest & 0xff);
        rest = Math.floor(rest / 256);
      }
      this.byte(bytes.length);
    } else {
      while (rest !== -1 && bytes.length < 4) {
        bytes.push(rest & 0xff);
        rest = Math.floor(rest / 256);
      }
      this.byte((256 - bytes.length) & 0xff);
    }
    return this.raw(bytes);
  }

  text(value) {
    const bytes = Buffer.from(value, "latin1");
    this.long(bytes.length);
    return this.raw(bytes);
  }

  symbol(name) {
    if (this.symbols.has(name)) {
      this.byte(";".charCodeAt(0));
      return this.long(this.symbols.get(name));
    }
    this.byte(":".charCodeAt(0));
    this.symbols.set(name, this.symbols.size);
    return this.text(name);
  }

  register(value) {
    this.objects.set(value, this.objects.size);
  }

  link(value) {
    this.byte("@".charCodeAt(0));
    return this.long(this.objects.get(value));
  }

  value(node) {
    switch (typeof node === "object" && node !== null ? node.__type__ : typeof node) {
      case "nil":
        return this.byte("0".charCodeAt(0));
      case "bool":
        return this.byte(node.__value__ ? "T".charCodeAt(0) : "F".charCodeAt(0));
      case "int":
        return this.byte("i".charCodeAt(0)).long(node.__value__);
      case "float": {
        this.byte("f".charCodeAt(0));
        const text = node.__value__ + "\0";
        return this.long(text.length).raw(Buffer.from(text, "latin1"));
      }
      case "string":
        return this.byte('"'.charCodeAt(0)).text(node.__value__);
      case "symbol":
        return this.symbol(node.__value__);
      case "array": {
        this.byte("[".charCodeAt(0));
        this.long(node.__value__.length);
        this.register(node);
        for (const item of node.__value__) this.value(item);
        return this;
      }
      case "hash": {
        this.byte("{".charCodeAt(0));
        this.long(node.__value__.length);
        this.register(node);
        for (const [key, item] of node.__value__) {
          this.value(key);
          this.value(item);
        }
        return this;
      }
      case "object": {
        this.byte("o".charCodeAt(0));
        this.symbol(node.__class__);
        this.long(Object.keys(node.__ivars__).length);
        this.register(node);
        for (const [name, item] of Object.entries(node.__ivars__)) {
          this.symbol(name);
          this.value(item);
        }
        return this;
      }
      case "ivar": {
        this.byte("I".charCodeAt(0));
        this.value(node.__value__);
        this.long(Object.keys(node.__ivars__).length);
        for (const [name, item] of Object.entries(node.__ivars__)) {
          this.symbol(name);
          this.value(item);
        }
        return this;
      }
      case "uclass": {
        this.byte("C".charCodeAt(0));
        this.symbol(node.__class__);
        return this.value(node.__value__);
      }
      case "userdef": {
        // 'u' + class symbol + the raw _dump output as a marshaled String.
        this.byte("u".charCodeAt(0));
        this.symbol(node.__class__);
        this.long(node.__bytes__.length);
        return this.raw(node.__bytes__);
      }
      case "link":
        return this.link(node.__value__);
      default:
        throw new Error("unsupported fixture node: " + JSON.stringify(node));
    }
  }

  finish(value) {
    this.byte(4);
    this.byte(8);
    this.value(value);
    return Buffer.from(this.out);
  }
}

// Fixture helpers.
export const nilValue = () => ({ __type__: "nil" });
export const bool = (value) => ({ __type__: "bool", __value__: Boolean(value) });
export const int = (value) => ({ __type__: "int", __value__: value });
export const float = (value) => ({ __type__: "float", __value__: value });
export const str = (value) => ({ __type__: "string", __value__: value });
export const sym = (value) => ({ __type__: "symbol", __value__: value });
export const array = (...items) => ({ __type__: "array", __value__: items });
export const hash = (...pairs) => ({ __type__: "hash", __value__: pairs });
export const object = (className, ivars) => ({ __type__: "object", __class__: className, __ivars__: ivars });
export const ivar = (value, ivars) => ({ __type__: "ivar", __value__: value, __ivars__: ivars });
export const uclass = (className, value) => ({ __type__: "uclass", __class__: className, __value__: value });
// Raw user-defined Marshal object ('u'), used for RPG::Table payloads.
export const userdef = (className, bytes) => ({ __type__: "userdef", __class__: className, __bytes__: bytes });
export const link = (target) => ({ __type__: "link", __value__: target });

export function dump(value) {
  return new MarshalWriter().finish(value);
}

// A compressed script section body as stored inside Scripts.rxdata.
export function deflateText(text) {
  return deflateSync(Buffer.from(text, "utf8")).toString("latin1");
}
