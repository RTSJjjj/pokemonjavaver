// Battle move / common animations: Data/PkmnAnimations.rxdata (PBAnimations)
// and Data/move2anim.dat, exported for the runtime's PBAnimationPlayerX port.
//
// Source of truth is the plugin's PokeBattle_AnimationPlayer section:
//   * PBAnimations (:356-412) / PBAnimation (:419-498) / PBAnimTiming (:241-349)
//     are Array subclasses / plain objects, so Marshal stores them as
//     'C' (user class) nodes with the Ruby ivars attached (@array, @timing,
//     @name, @graphic, @hue, @position, @id, ...).
//   * A cel is the 27 slot Array of AnimFrame (:4-30); nil cels stay null.
//   * Data/move2anim.dat is Marshal [moveToAnim, oppMoveToAnim], each indexed
//     by move id (Data_Cache:265-270, Scene_Animations:427-438).
//
// Layout written below generated/battle-animations/:
//   index.json         { moveToAnim, oppMoveToAnim, animations: [{id,name,file}] }
//   anim-<id>.json     one animation (loaded lazily by the runtime)
//
// Cel encoding (positional; the runtime reads it back in the same order):
//   [pattern, x, y, zoomX, zoomY, angle, mirror, opacity, blend, priority,
//    focus, visible]                       (12 numbers, always)
//   + [colorR, colorG, colorB, colorA, toneR, toneG, toneB, toneGray]
//                                          (only when one of them is not 0)

import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { marshalLoad } from "../../builder/src/marshal.js";

export const BATTLE_ANIMATIONS_DIR = "battle-animations";
export const BATTLE_ANIMATIONS_FORMAT = "pokemon-builder/battle-animations/1";

// AnimFrame indexes (PokeBattle_AnimationPlayer:4-30).
const F = {
  X: 0, Y: 1, ZOOMX: 2, ANGLE: 3, MIRROR: 4, BLENDTYPE: 5, VISIBLE: 6, PATTERN: 7,
  OPACITY: 8, ZOOMY: 11, COLORRED: 12, COLORGREEN: 13, COLORBLUE: 14, COLORALPHA: 15,
  TONERED: 16, TONEGREEN: 17, TONEBLUE: 18, TONEGRAY: 19, PRIORITY: 25, FOCUS: 26,
};

// Marshal reads strings as latin1; strings with instance variables (the
// encoding flag) arrive wrapped. Plugin text is UTF-8.
function text(value) {
  let raw = value;
  if (raw && typeof raw === "object" && "__value__" in raw) raw = raw.__value__;
  if (typeof raw !== "string") return "";
  return Buffer.from(raw, "latin1").toString("utf8");
}

function num(value, fallback = 0) {
  return typeof value === "number" && Number.isFinite(value) ? value : fallback;
}

// nil stays null (PBAnimTiming leaves bgX/opacity/colour* nil on purpose).
function optNum(value) {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function celIr(cel) {
  if (!Array.isArray(cel)) return null;
  const out = [
    num(cel[F.PATTERN]), num(cel[F.X]), num(cel[F.Y]),
    num(cel[F.ZOOMX], 100), num(cel[F.ZOOMY], 100), num(cel[F.ANGLE]),
    num(cel[F.MIRROR]), num(cel[F.OPACITY], 255), num(cel[F.BLENDTYPE]),
    num(cel[F.PRIORITY], 1), num(cel[F.FOCUS], 4), num(cel[F.VISIBLE], 1),
  ];
  const tail = [
    num(cel[F.COLORRED]), num(cel[F.COLORGREEN]), num(cel[F.COLORBLUE]), num(cel[F.COLORALPHA]),
    num(cel[F.TONERED]), num(cel[F.TONEGREEN]), num(cel[F.TONEBLUE]), num(cel[F.TONEGRAY]),
  ];
  if (tail.some((value) => value !== 0)) out.push(...tail);
  return out;
}

function frameIr(frame) {
  const cels = (Array.isArray(frame) ? frame : []).map(celIr);
  // `next if !cel` (:834): trailing nil cels draw nothing.
  while (cels.length > 0 && cels[cels.length - 1] === null) cels.pop();
  return cels;
}

// PBAnimTiming (:241-276). flashScope/flashColor/flashDuration are only read
// by code that is commented out in playTiming (:512-515), so they are not kept.
function timingIr(timing) {
  return {
    frame: num(timing["@frame"]),
    type: num(timing["@timingType"]),        // timingType (:278-280)
    name: text(timing["@name"]),
    volume: num(timing["@volume"], 80),
    pitch: num(timing["@pitch"], 100),
    bgX: optNum(timing["@bgX"]),
    bgY: optNum(timing["@bgY"]),
    opacity: optNum(timing["@opacity"]),
    colorRed: optNum(timing["@colorRed"]),
    colorGreen: optNum(timing["@colorGreen"]),
    colorBlue: optNum(timing["@colorBlue"]),
    colorAlpha: optNum(timing["@colorAlpha"]),
    duration: num(timing["@duration"], 5),   // duration (:282-284)
  };
}

function animationIr(animation, index) {
  return {
    format: BATTLE_ANIMATIONS_FORMAT,
    kind: "battleAnimation",
    id: index,
    name: text(animation["@name"]),
    graphic: text(animation["@graphic"]),
    hue: num(animation["@hue"]),
    position: num(animation["@position"], 4),
    frames: (animation["@array"] || []).map(frameIr),
    timings: (animation["@timing"] || []).map(timingIr),
  };
}

function idTable(table) {
  return (Array.isArray(table) ? table : []).map((value) => (typeof value === "number" ? value : null));
}

/**
 * Parses both files. Returns null when the project has no PkmnAnimations
 * (a plain Essentials project without the plugin's animation editor data).
 *
 * @returns {{ index: object, animations: Array<{file: string, payload: object}> } | null}
 */
export function buildBattleAnimations(projectPath) {
  const animationsFile = path.join(projectPath, "Data", "PkmnAnimations.rxdata");
  if (!existsSync(animationsFile)) return null;
  const root = marshalLoad(readFileSync(animationsFile));
  const list = root && Array.isArray(root["@array"]) ? root["@array"] : [];
  const animations = [];
  const entries = [];
  list.forEach((animation, index) => {
    // pbCommonAnimation skips `!a || a.is_a?(String)` entries (:536).
    if (!animation || typeof animation !== "object" || !Array.isArray(animation["@array"])) return;
    const payload = animationIr(animation, index);
    const file = `${BATTLE_ANIMATIONS_DIR}/anim-${index}.json`;
    animations.push({ file, payload });
    entries.push({ id: index, name: payload.name, file: `anim-${index}.json` });
  });

  let moveToAnim = [];
  let oppMoveToAnim = [];
  const move2animFile = path.join(projectPath, "Data", "move2anim.dat");
  if (existsSync(move2animFile)) {
    const tables = marshalLoad(readFileSync(move2animFile));
    if (Array.isArray(tables)) {
      moveToAnim = idTable(tables[0]);
      oppMoveToAnim = idTable(tables[1]);
    }
  }
  return {
    index: {
      format: BATTLE_ANIMATIONS_FORMAT,
      kind: "battleAnimationIndex",
      total: entries.length,
      moveToAnim,
      oppMoveToAnim,
      animations: entries,
    },
    animations,
  };
}
