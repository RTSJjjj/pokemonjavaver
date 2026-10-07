// Battle move / common animations export (PkmnAnimations.rxdata + move2anim.dat),
// the data behind the runtime's PBAnimationPlayerX port.

import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, readdirSync, readFileSync, writeFileSync } from "node:fs";
import path from "node:path";

import { convertProject } from "../../tools/data-converter/index.js";
import { buildBattleAnimations } from "../../tools/data-converter/battle-animations.js";
import { array, dump, int, ivar, nilValue, object, str, uclass, userdef } from "./marshal-writer.js";
import { makeProject, scratch } from "./fixtures.js";

function readJson(file) {
  return JSON.parse(readFileSync(file, "utf8"));
}

// One 27 slot AnimFrame cel (PokeBattle_AnimationPlayer:4-30).
function cel({ pattern, x, y, zoom = 100, angle = 0, mirror = 0, opacity = 255, blend = 0,
  priority = 1, focus = 4, color = [0, 0, 0, 0], tone = [0, 0, 0, 0] }) {
  const slots = new Array(27).fill(null).map(() => nilValue());
  const set = (index, value) => { slots[index] = int(value); };
  set(0, x); set(1, y); set(2, zoom); set(3, angle); set(4, mirror); set(5, blend); set(6, 1);
  set(7, pattern); set(8, opacity); set(11, zoom);
  color.forEach((value, i) => set(12 + i, value));
  tone.forEach((value, i) => set(16 + i, value));
  set(20, 0); set(25, priority); set(26, focus);
  return array(...slots);
}

const flashColor = userdef("Color", Buffer.alloc(32));

function timing(fields) {
  const ivars = {
    "@frame": int(fields.frame), "@timingType": int(fields.type ?? 0), "@name": str(fields.name ?? ""),
    "@volume": int(fields.volume ?? 80), "@pitch": int(fields.pitch ?? 100),
    "@bgX": fields.bgX === undefined ? nilValue() : int(fields.bgX),
    "@bgY": fields.bgY === undefined ? nilValue() : int(fields.bgY),
    "@opacity": fields.opacity === undefined ? nilValue() : int(fields.opacity),
    "@colorRed": nilValue(), "@colorGreen": nilValue(), "@colorBlue": nilValue(),
    "@colorAlpha": nilValue(), "@duration": int(fields.duration ?? 5),
    "@flashScope": int(0), "@flashColor": flashColor, "@flashDuration": int(5),
  };
  return object("PBAnimTiming", ivars);
}

// PBAnimation is an Array subclass with ivars: Marshal writes I + C + [] + ivars.
function pbAnimation({ id, name, graphic, hue = 0, position = 4, frames, timings }) {
  return ivar(uclass("PBAnimation", array()), {
    "@id": int(id), "@name": str(name), "@graphic": str(graphic), "@hue": int(hue),
    "@position": int(position), "@scope": int(0),
    "@array": array(...frames.map((cels) => array(...cels))),
    "@timing": array(...timings),
  });
}

function writeAnimationData(project) {
  const first = pbAnimation({
    id: -1, name: "Common:StatUp", graphic: "PRAS- Stats.png", position: 2,
    frames: [
      [cel({ pattern: -1, x: 128, y: 224, focus: 2 }), cel({ pattern: -2, x: 384, y: 96, focus: 1 }),
        cel({ pattern: 6, x: 128, y: 267, opacity: 195, focus: 2, blend: 1, tone: [10, 20, 30, 40] })],
      [cel({ pattern: -1, x: 128, y: 224, focus: 2 })],
    ],
    timings: [timing({ frame: 0, name: "PRSFX- Stat Up.wav", volume: 100 }),
      timing({ frame: 1, type: 1, name: "PRAS- Fire BG.png", bgX: 0, bgY: 0, opacity: 255 })],
  });
  const second = pbAnimation({
    id: -1, name: "Move:TACKLE", graphic: "", position: 3,
    frames: [[cel({ pattern: -1, x: 128, y: 224, focus: 2 })]], timings: [],
  });
  writeFileSync(path.join(project, "Data", "PkmnAnimations.rxdata"),
    dump(ivar(uclass("PBAnimations", array()), { "@selected": int(0), "@array": array(first, second) })));
  writeFileSync(path.join(project, "Data", "move2anim.dat"),
    dump(array(array(nilValue(), nilValue(), int(1)), array(nilValue(), int(1), nilValue()))));
}

test("buildBattleAnimations returns null without PkmnAnimations.rxdata", (t) => {
  const dir = scratch("pb-battleanim-", t);
  const project = makeProject(dir, "FakeProject");
  assert.equal(buildBattleAnimations(project), null);
});

test("PkmnAnimations + move2anim become an index and one file per animation", (t) => {
  const dir = scratch("pb-battleanim-", t);
  const project = makeProject(dir, "FakeProject");
  writeAnimationData(project);
  const built = buildBattleAnimations(project);
  assert.equal(built.index.total, 2);
  assert.deepEqual(built.index.moveToAnim, [null, null, 1]);
  assert.deepEqual(built.index.oppMoveToAnim, [null, 1, null]);
  assert.deepEqual(built.index.animations.map((entry) => entry.name), ["Common:StatUp", "Move:TACKLE"]);

  const statUp = built.animations[0].payload;
  assert.equal(statUp.id, 0, "the array position is the animation id (animations[anim])");
  assert.equal(statUp.graphic, "PRAS- Stats.png");
  assert.equal(statUp.position, 2);
  assert.equal(statUp.frames.length, 2);
  // [pattern,x,y,zoomX,zoomY,angle,mirror,opacity,blend,priority,focus,visible]
  assert.deepEqual(statUp.frames[0][0], [-1, 128, 224, 100, 100, 0, 0, 255, 0, 1, 2, 1]);
  assert.deepEqual(statUp.frames[0][2],
    [6, 128, 267, 100, 100, 0, 0, 195, 1, 1, 2, 1, 0, 0, 0, 0, 10, 20, 30, 40],
    "colour and tone ride along only when one of them is not 0");
  assert.equal(statUp.timings[0].name, "PRSFX- Stat Up.wav");
  assert.equal(statUp.timings[0].volume, 100);
  assert.equal(statUp.timings[1].type, 1);
  assert.equal(statUp.timings[1].bgX, 0);
  assert.equal(statUp.timings[1].colorRed, null, "nil stays null (:577 `if i.bgX!=nil` style tests)");
  assert.equal(statUp.timings[1].duration, 5);
});

test("convertProject writes battle-animations/ and reuses the unit on the next build", (t) => {
  const dir = scratch("pb-battleanim-", t);
  const project = makeProject(dir, "FakeProject");
  writeAnimationData(project);
  const generatedDir = path.join(dir, "generated");
  const cacheDir = path.join(dir, "build");
  const options = { generatedDir, cacheDir };

  const first = convertProject(project, options);
  assert.equal(first.ok, true, JSON.stringify(first.errors));
  const out = path.join(generatedDir, "battle-animations");
  assert.deepEqual(readdirSync(out).sort(), ["anim-0.json", "anim-1.json", "index.json"]);
  const index = readJson(path.join(out, "index.json"));
  assert.deepEqual(index.animations[1], { id: 1, name: "Move:TACKLE", file: "anim-1.json" });
  assert.equal(readJson(path.join(out, "anim-0.json")).kind, "battleAnimation");
  assert.ok(first.incremental.rebuilt.includes("battle-animations"));

  const second = convertProject(project, options);
  assert.equal(second.ok, true, JSON.stringify(second.errors));
  assert.ok(second.incremental.skipped.includes("battle-animations"), "unchanged inputs reuse the unit");

  // Editing move2anim.dat invalidates the unit.
  writeFileSync(path.join(project, "Data", "move2anim.dat"),
    dump(array(array(nilValue(), int(1), int(1)), array())));
  const third = convertProject(project, options);
  assert.ok(third.incremental.rebuilt.includes("battle-animations"));
  assert.deepEqual(readJson(path.join(out, "index.json")).moveToAnim, [null, 1, 1]);
});

test("a project without PkmnAnimations.rxdata writes no battle-animations files", (t) => {
  const dir = scratch("pb-battleanim-", t);
  const project = makeProject(dir, "FakeProject");
  const generatedDir = path.join(dir, "generated");
  const result = convertProject(project, { generatedDir });
  assert.equal(result.ok, true, JSON.stringify(result.errors));
  assert.equal(existsSync(path.join(generatedDir, "battle-animations", "index.json")), false);
});
