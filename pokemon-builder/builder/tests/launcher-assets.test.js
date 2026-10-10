// The Android launcher icon / name come from the game project (Icon192.png, Game.ico or Game.exe).
import test from "node:test";
import assert from "node:assert/strict";
import { existsSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import { encodePng } from "../src/exe-icon.js";
import { DENSITIES, decodePng, findProjectIcon, scaleRgba, writeLauncherAssets } from "../src/launcher-assets.js";

function solid(width, height, [r, g, b, a]) {
  const rgba = Buffer.alloc(width * height * 4);
  for (let i = 0; i < width * height; i++) {
    rgba[i * 4] = r; rgba[i * 4 + 1] = g; rgba[i * 4 + 2] = b; rgba[i * 4 + 3] = a;
  }
  return rgba;
}

test("a PNG written by encodePng decodes back to the same pixels", () => {
  const rgba = solid(4, 3, [10, 20, 30, 200]);
  const decoded = decodePng(encodePng(4, 3, rgba));
  assert.equal(decoded.width, 4);
  assert.equal(decoded.height, 3);
  assert.deepEqual([...decoded.rgba], [...rgba]);
});

test("scaleRgba averages when it shrinks and repeats pixels when it grows", () => {
  const src = Buffer.concat([solid(1, 1, [0, 0, 0, 255]), solid(1, 1, [200, 200, 200, 255]), solid(1, 1, [0, 0, 0, 255]), solid(1, 1, [200, 200, 200, 255])]);
  const down = scaleRgba(src, 2, 2, 1);
  assert.equal(down[0], 100);
  const up = scaleRgba(down, 1, 1, 4);
  assert.equal(up.length, 4 * 4 * 4);
  assert.equal(up[0], 100);
});

test("Icon192.png and the application name are staged for every launcher density", () => {
  const project = mkdtempSync(path.join(os.tmpdir(), "pb-icon-"));
  const android = mkdtempSync(path.join(os.tmpdir(), "pb-android-"));
  writeFileSync(path.join(project, "Icon192.png"), encodePng(8, 8, solid(8, 8, [255, 0, 0, 255])));
  const result = writeLauncherAssets(project, android, "昕纪元 & <影辞>");
  assert.equal(result.icon, "Icon192.png");
  for (const density of DENSITIES) {
    const file = path.join(result.dir, density.dir, "ic_launcher.png");
    assert.ok(existsSync(file), density.dir);
    const png = decodePng(readFileSync(file));
    assert.equal(png.width, density.size);
    assert.equal(png.rgba[0], 255);
  }
  const xml = readFileSync(path.join(result.dir, "values", "launcher_strings.xml"), "utf8");
  assert.ok(xml.includes("昕纪元 &amp; &lt;影辞&gt;"));
});

test("a project without any icon still gets its name and no icon files", () => {
  const project = mkdtempSync(path.join(os.tmpdir(), "pb-noicon-"));
  const android = mkdtempSync(path.join(os.tmpdir(), "pb-android2-"));
  assert.equal(findProjectIcon(project), null);
  const result = writeLauncherAssets(project, android, "Name");
  assert.equal(result.icon, null);
  assert.ok(existsSync(path.join(result.dir, "values", "launcher_strings.xml")));
  assert.equal(existsSync(path.join(result.dir, "mipmap-mdpi")), false);
});
