// Transcribes Boss_Battles (Scripts.rxdata section 319, exported to plugin-src/ruby/319_Boss_Battles.rb) into
// runtime/core/src/main/java/pokemon/runtime/event/BossBattleData.java.
//
// Usage: node tools/boss-battles/generate.mjs [319_Boss_Battles.rb] [BossBattleData.java]
//
// Every `def battleXxx` in the section has the same shape:
//   [if $game_switches[197] / pbMessage(_INTL(..)) / return true / end]        :lazy-dog skip
//   [$game_switches[196] = true]
//   count = $Trainer.ablePokemonCount ; size = <ternary on count>
//   setBattleRule(sprintf("%dv1",size)) ; setBattleRule("canlose") ; setBattleRule("noexp")
//   pkmn = pbGenPkmn(:SPECIES, level) ; <pkmn.* statements, in order>
//   decision = pbWildBattleCore(pkmn)
//   [$game_switches[196] = false]
//   return decision==N
// The parser accepts exactly those statements and throws on anything else, so nothing is silently dropped.
// `battleBoss(species,level,rank)` is not a def of that shape: it calls changeEVandNature / pbRandomIV, which are
// not part of the exported sections, so it is reported and left out.
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));

/**
 * Transcribes the text of section 319 into the Java source of BossBattleData.
 * Throws an Error listing every def that does not have the shared shape.
 * Returns { java, entries, unsupported, defCount }.
 */
export function generateBossBattleData(source) {
const lines = source.split(/\r?\n/);

const defs = [];
let current = null;
lines.forEach((raw, index) => {
  const lineNo = index + 1;
  const def = /^def (\w+)\s*(\(.*\))?\s*$/.exec(raw);
  if (def) { current = { name: def[1], line: lineNo, body: [] }; defs.push(current); return; }
  if (current === null) return;
  if (/^end\s*(#.*)?$/.test(raw)) { current.endLine = lineNo; current = null; return; }
  const text = raw.replace(/\s+#.*$/, "").trim();   // the Chinese trailing comments
  if (text === "" || text.startsWith("#")) return;
  current.body.push({ line: lineNo, text });
});

const unsupported = [];
const entries = [];

function sizeTable(expr, fn) {
  // `(count > 2) ? 3 : (count > 1) ? 2 : 1` and `(count >= 1) ? 1 : 1`: evaluated for 0..6 able Pokemon.
  const table = [];
  for (let count = 0; count <= 6; count++) {
    let rest = expr.trim();
    let value = null;
    while (value === null) {
      const m = /^\(count\s*(>=|>)\s*(\d+)\)\s*\?\s*(\d+)\s*:\s*(.*)$/.exec(rest);
      if (!m) { const last = /^(\d+)$/.exec(rest); if (!last) throw new Error(`${fn}: size expression ${expr}`); value = Number(last[1]); break; }
      const holds = m[1] === ">" ? count > Number(m[2]) : count >= Number(m[2]);
      if (holds) value = Number(m[3]); else rest = m[4];
    }
    table.push(value);
  }
  return table;
}

const failures = [];
for (const def of defs) {
  try {
  const body = def.body.slice();
  const take = (re, what) => {
    const s = body.shift();
    const m = s && re.exec(s.text);
    if (!m) throw new Error(`${def.name} (line ${s ? s.line : def.endLine}): expected ${what}, got ${s ? s.text : "end of def"}`);
    return m;
  };
  if (def.name === "battleBoss") { unsupported.push(def.name); continue; }
  const entry = { name: def.name, line: def.line, lazyDog: false, switch196: false, ops: [] };
  if (body[0] && /^if \$game_switches\[197\]$/.test(body[0].text)) {   // :6-9
    body.shift();
    take(/^pbMessage\(_INTL\("懒狗模式跳过BOSS战并默认胜利。"\)\)$/, "the lazy-dog message");
    take(/^return true$/, "return true");
    take(/^end$/, "end");
    entry.lazyDog = true;
  }
  if (body[0] && /^\$game_switches\[196\] = true$/.test(body[0].text)) { body.shift(); entry.switch196 = true; }
  const literalSize = body[0] && /^setBattleRule\("(\d)v1"\)$/.exec(body[0].text);
  if (literalSize) {   // a fixed size, no ablePokemonCount (e.g. 3148 setBattleRule("1v1"))
    body.shift();
    entry.sizes = [0, 1, 2, 3, 4, 5, 6].map(() => Number(literalSize[1]));
  } else {
    take(/^count = \$Trainer\.ablePokemonCount$/, "count");
    entry.sizes = sizeTable(take(/^size = (.*)$/, "size")[1], def.name);
    take(/^setBattleRule\(sprintf\("%dv1",\s*size\)\)$/, "the %dv1 rule");
  }
  take(/^setBattleRule\("canlose"\)$/, "canlose");
  take(/^setBattleRule\("noexp"\)$/, "noexp");
  const gen = take(/^pkmn = pbGenPkmn\(:(\w+),\s*(\d+)\)$/, "pbGenPkmn");
  entry.species = gen[1]; entry.level = Number(gen[2]);
  for (;;) {
    const s = body[0];
    if (!s || /^decision = pbWildBattleCore\(pkmn\)$/.test(s.text)) break;
    body.shift();
    let m;
    if ((m = /^pkmn\.form = (\d+)$/.exec(s.text))) entry.ops.push(["form", Number(m[1])]);
    else if ((m = /^pkmn\.(iv|ev) = \[([\d,\s]+)\]$/.exec(s.text))) entry.ops.push([m[1], m[2].split(",").map((n) => Number(n.trim()))]);
    else if ((m = /^pkmn\.battleRank = (\d+)$/.exec(s.text))) entry.ops.push(["battleRank", Number(m[1])]);
    else if ((m = /^pkmn\.(setItem|setNature|pbLearnMove)\(:(\w+)\)$/.exec(s.text))) entry.ops.push([m[1], m[2]]);
    else if ((m = /^pkmn\.setAbility\((\d+)\)$/.exec(s.text))) entry.ops.push(["setAbility", Number(m[1])]);
    else if ((m = /^pkmn\.name="([^"]*)"$/.exec(s.text))) entry.ops.push(["name", m[1]]);
    else if ((m = /^pkmn\.(makeNotShiny|makeShiny|makeSuperShiny|makeMale|calcStats)$/.exec(s.text))) entry.ops.push([m[1]]);
    else if ((m = /^pkmn\.totalhp = pkmn\.totalhp \* (\d+)$/.exec(s.text))) entry.ops.push(["totalhpTimes", Number(m[1])]);
    else if (/^pkmn\.hp = pkmn\.totalhp$/.test(s.text)) entry.ops.push(["hpToTotal"]);
    else throw new Error(`${def.name} (line ${s.line}): unknown statement ${s.text}`);
  }
  take(/^decision = pbWildBattleCore\(pkmn\)$/, "pbWildBattleCore");
  if (body[0] && /^\$game_switches\[196\] = false$/.test(body[0].text)) {
    body.shift();
    if (!entry.switch196) throw new Error(`${def.name}: switch 196 reset without set`);
  } else if (entry.switch196) throw new Error(`${def.name}: switch 196 set but never reset`);
  entry.decision = Number(take(/^return decision\s*==\s*(\d+)$/, "return decision==N")[1]);
  if (body.length) throw new Error(`${def.name} (line ${body[0].line}): trailing statement ${body[0].text}`);
  entries.push(entry);
  } catch (error) { failures.push(error.message); }
}
if (failures.length) throw new Error(failures.join("\n"));

const lit = (v) => Array.isArray(v) ? `new int[] { ${v.join(", ")} }`
  : typeof v === "number" ? String(v) : JSON.stringify(v);
let java = `package pokemon.runtime.event;

import java.util.HashMap;
import java.util.Map;

/**
 * GENERATED by tools/boss-battles/generate.mjs from Boss_Battles (section 319, plugin-src/ruby/319_Boss_Battles.rb) -
 * do not edit by hand. Each entry is one \`def battleXxx\` transcribed statement by statement; the comment on an entry
 * is the section line of its \`def\`. The shape every def shares (319:5-31 / :33-57 ...):
 * <pre>
 * [lazy-dog skip: if $game_switches[197] ... return true]
 * [$game_switches[196] = true]
 * size = f($Trainer.ablePokemonCount)             -> sizes[count]
 * setBattleRule(sprintf("%dv1",size)); setBattleRule("canlose"); setBattleRule("noexp")
 * pkmn = pbGenPkmn(species, level); the pkmn.* statements in order  -> ops
 * decision = pbWildBattleCore(pkmn)
 * [$game_switches[196] = false]
 * return decision==N                                -> decision
 * </pre>
 * Not transcribed: \`battleBoss(species,level,rank)\` (319:5-31) calls changeEVandNature and pbRandomIV, which are in
 * none of the exported sections.
 */
final class BossBattleData {

    /** One \`pkmn.*\` statement: {@code name} with its literal arguments. */
    static final class Op {
        final String name;
        final Object arg;

        Op(String name, Object arg) {
            this.name = name;
            this.arg = arg;
        }
    }

    static final class Entry {
        final String name;
        final int sourceLine;
        /** \`if $game_switches[197]\` returns true after "懒狗模式跳过BOSS战并默认胜利。" (319:6-9). */
        final boolean lazyDogSkip;
        /** \`$game_switches[196] = true\` before and \`= false\` after the battle. */
        final boolean uncatchable;
        /** \`size\` for 0..6 able Pokemon. */
        final int[] sizes;
        final String species;
        final int level;
        final Op[] ops;
        /** \`return decision==N\`. */
        final int decision;

        Entry(String name, int sourceLine, boolean lazyDogSkip, boolean uncatchable, int[] sizes, String species,
              int level, int decision, Op... ops) {
            this.name = name;
            this.sourceLine = sourceLine;
            this.lazyDogSkip = lazyDogSkip;
            this.uncatchable = uncatchable;
            this.sizes = sizes;
            this.species = species;
            this.level = level;
            this.decision = decision;
            this.ops = ops;
        }
    }

    private static final Map<String, Entry> BY_NAME = new HashMap<>();

    static Entry find(String name) {
        return BY_NAME.get(name);
    }

    private static Op op(String name) {
        return new Op(name, null);
    }

    private static Op op(String name, Object arg) {
        return new Op(name, arg);
    }

    private static void add(Entry entry) {
        BY_NAME.put(entry.name, entry);
    }

    static {
`;
for (const e of entries) {
  const ops = e.ops.map(([n, a]) => a === undefined ? `op(${lit(n)})` : `op(${lit(n)}, ${lit(a)})`).join(",\n                ");
  java += `        // 319:${e.line}
        add(new Entry(${lit(e.name)}, ${e.line}, ${e.lazyDog}, ${e.switch196}, ${lit(e.sizes)}, ${lit(e.species)}, ${e.level}, ${e.decision},
                ${ops}));
`;
}
java += `    }

    private BossBattleData() {
    }
}
`;
return { java, entries, unsupported, defCount: defs.length };
}

if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const input = process.argv[2] ?? path.join(here, "../../plugin-src/ruby/319_Boss_Battles.rb");
  const output = process.argv[3] ?? path.join(here, "../../runtime/core/src/main/java/pokemon/runtime/event/BossBattleData.java");
  try {
    const { java, entries, unsupported } = generateBossBattleData(readFileSync(input, "utf8"));
    writeFileSync(output, java, "utf8");
    console.log(`${entries.length} entries written to ${output}; not transcribed: ${unsupported.join(", ") || "none"}`);
  } catch (error) {
    console.error(error.message);
    process.exit(1);
  }
}
