import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, mkdirSync, writeFileSync, existsSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import { purgeAndroidAssetCaches, removeAndroidBuild } from "../src/android-cache.js";

function fixture() {
  const dir = mkdtempSync(path.join(os.tmpdir(), "android-cache-"));
  for (const sub of [
    "outputs/apk/debug", "intermediates/assets/debug", "intermediates/compressed_assets/debug",
    "intermediates/incremental/mergeDebugAssets", "intermediates/incremental/mergeDebugResources",
    "intermediates/dex/debug", "intermediates/merged_native_libs",
  ]) {
    mkdirSync(path.join(dir, sub), { recursive: true });
    writeFileSync(path.join(dir, sub, "x.bin"), "x");
  }
  return dir;
}

test("old APKs and the asset merge caches go, the dex and resource caches stay", () => {
  const dir = fixture();
  const removed = purgeAndroidAssetCaches(dir);
  assert.ok(removed.length >= 4);
  assert.equal(existsSync(path.join(dir, "outputs")), false);
  assert.equal(existsSync(path.join(dir, "intermediates", "assets")), false);
  assert.equal(existsSync(path.join(dir, "intermediates", "compressed_assets")), false);
  assert.equal(existsSync(path.join(dir, "intermediates", "incremental", "mergeDebugAssets")), false);
  assert.equal(existsSync(path.join(dir, "intermediates", "incremental", "mergeDebugResources")), true);
  assert.equal(existsSync(path.join(dir, "intermediates", "dex")), true);
});

test("a missing build directory is not an error", () => {
  assert.deepEqual(purgeAndroidAssetCaches(path.join(os.tmpdir(), "does-not-exist-" + Date.now())), []);
  assert.equal(removeAndroidBuild(path.join(os.tmpdir(), "does-not-exist-" + Date.now())), false);
});

test("the clean action removes the whole module build directory", () => {
  const dir = fixture();
  assert.equal(removeAndroidBuild(dir), true);
  assert.equal(existsSync(dir), false);
});
