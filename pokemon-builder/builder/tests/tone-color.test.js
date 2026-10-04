// Builder test: RPG::Tone / RPG::Color _dump payloads become readable IR.

import test from "node:test";
import assert from "node:assert/strict";
import { decodeRgssColorLike } from "../../tools/data-converter/index.js";

function payload(values) {
  const buffer = Buffer.alloc(values.length * 8);
  values.forEach((value, i) => buffer.writeDoubleLE(value, i * 8));
  return buffer;
}

test("a Tone payload decodes into red/green/blue/gray", () => {
  const tone = decodeRgssColorLike({
    __class__: "RPG::Tone",
    __userdef__: payload([-255, -255, -255, 0]),
  });
  assert.deepEqual(tone, { class: "RPG::Tone", red: -255, green: -255, blue: -255, gray: 0 });
});

test("a Color payload decodes into red/green/blue/alpha", () => {
  const color = decodeRgssColorLike({
    __class__: "RPG::Color",
    __userdef__: payload([255, 128, 0, 255]),
  });
  assert.deepEqual(color, { class: "RPG::Color", red: 255, green: 128, blue: 0, alpha: 255 });
});

test("the plain Tone / Color spellings the Marshal file uses are accepted too", () => {
  assert.deepEqual(
    decodeRgssColorLike({ __class__: "Tone", __userdef__: payload([10, 20, 30, 0]) }),
    { class: "Tone", red: 10, green: 20, blue: 30, gray: 0 });
  assert.deepEqual(
    decodeRgssColorLike({ __class__: "Color", __userdef__: payload([1, 2, 3, 4]) }),
    { class: "Color", red: 1, green: 2, blue: 3, alpha: 4 });
});

test("implausible payloads stay undecoded instead of inventing numbers", () => {
  assert.equal(decodeRgssColorLike({ __class__: "RPG::Tone", __userdef__: Buffer.alloc(8) }), null);
  const garbage = payload([1e9, 0, 0, 0]);
  assert.equal(decodeRgssColorLike({ __class__: "RPG::Tone", __userdef__: garbage }), null);
  assert.equal(decodeRgssColorLike({ __class__: "RPG::Other", __userdef__: payload([0, 0, 0, 0]) }), null);
});
