// P3: the resource cipher must match core/.../data/ResourceCrypto.java byte for byte.
import test from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, readFileSync, writeFileSync, statSync, utimesSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import {
  decryptBuffer, encryptBuffer, encryptFileInPlace, encryptTree, isEncryptedFile, HEADER_SIZE,
} from "../src/resource-crypto.js";

// The same vector is in ResourceCryptoTest.java.
const VECTOR = "504b52450100000001020304050607089995c27102bea08ba863223940d427b2a1";

test("the encrypted bytes of the shared test vector", () => {
  const encrypted = encryptBuffer(Buffer.from("hello 世界 PKRE"), Buffer.from([1, 2, 3, 4, 5, 6, 7, 8]));
  assert.equal(encrypted.toString("hex"), VECTOR);
  assert.equal(decryptBuffer(encrypted).toString(), "hello 世界 PKRE");
});

test("a plain buffer passes through decryptBuffer", () => {
  assert.equal(decryptBuffer(Buffer.from("plain text, long enough")).toString(), "plain text, long enough");
});

test("encryptFileInPlace keeps the mtime, adds the header once and encryptTree counts new files only", () => {
  const dir = mkdtempSync(path.join(os.tmpdir(), "pkre-"));
  const file = path.join(dir, "a.json");
  writeFileSync(file, '{"a":1}');
  const when = new Date("2020-01-02T03:04:05Z");
  utimesSync(file, when, when);
  assert.equal(encryptFileInPlace(file), true);
  assert.equal(statSync(file).size, 7 + HEADER_SIZE);
  assert.equal(statSync(file).mtimeMs, when.getTime());
  assert.equal(isEncryptedFile(file), true);
  assert.equal(encryptFileInPlace(file), false);
  assert.equal(decryptBuffer(readFileSync(file)).toString(), '{"a":1}');
  writeFileSync(path.join(dir, "b.png"), Buffer.from([1, 2, 3]));
  assert.equal(encryptTree(dir), 1);
});
