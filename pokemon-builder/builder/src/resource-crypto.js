// P3: resource encryption (mirror of core/.../data/ResourceCrypto.java).
//
// File format: "PKRE" 01 00 00 00 | nonce(8) | AES-128-CTR ciphertext (counter block = nonce + 8 zero bytes).
// The key is rebuilt from three fragments; this stops ordinary unpacking tools, not a determined person.

import { createCipheriv, createHash, randomBytes } from "node:crypto";
import {
  closeSync, openSync, readFileSync, readSync, readdirSync, renameSync, statSync, utimesSync, writeFileSync,
} from "node:fs";
import path from "node:path";

export const HEADER_SIZE = 16;
const MAGIC = Buffer.from([0x50, 0x4b, 0x52, 0x45, 1, 0, 0, 0]);
const A = Buffer.from([
  0xd3, 0xc3, 0xf6, 0xfc, 0x22, 0x4c, 0x47, 0x96, 0x9e, 0x2a, 0x12, 0x50, 0x15, 0xa2, 0x80, 0xdb,
  0x7d, 0x11, 0x24, 0x10, 0x96, 0xd9, 0xb6, 0x42, 0xef, 0xd9, 0x67, 0xde, 0x3c, 0xde, 0x5a, 0x48]);
const B = Buffer.from([
  0x39, 0xfb, 0xe5, 0xa6, 0x48, 0x23, 0x66, 0x07, 0x22, 0xc4, 0x67, 0x5b, 0xe9, 0x27, 0xe5, 0x14,
  0xba, 0x1c, 0xf0, 0xaf, 0xa8, 0x52, 0x9e, 0xae, 0x5c, 0x21, 0x65, 0x3e, 0x5f, 0x35, 0xf0, 0xa9]);
const C = Buffer.from([
  0x85, 0xf3, 0xf5, 0x98, 0xf1, 0x2e, 0xd0, 0xce, 0x31, 0x3b, 0x4d, 0x79, 0x64, 0xb9, 0x05, 0xb6,
  0x47, 0x81, 0x68, 0xe4, 0xe8, 0xa0, 0x3b, 0x62, 0x42, 0xa6, 0x08, 0x4e, 0x2c, 0xf2, 0x20, 0x37]);

export function resourceKey() {
  const mix = Buffer.alloc(32);
  for (let i = 0; i < 32; i++) {
    mix[i] = A[i] ^ B[(i + 3) % 32] ^ C[31 - i];
  }
  return createHash("sha256").update(mix).digest().subarray(0, 16);
}

/** True when the file already starts with the encrypted-file header. */
export function isEncryptedFile(file) {
  let fd;
  try {
    fd = openSync(file, "r");
    const head = Buffer.alloc(HEADER_SIZE);
    const got = readSync(fd, head, 0, HEADER_SIZE, 0);
    return got === HEADER_SIZE && head.subarray(0, MAGIC.length).equals(MAGIC);
  } catch (error) {
    return false;
  } finally {
    if (fd !== undefined) closeSync(fd);
  }
}

/** Encrypts a buffer: header + ciphertext. */
export function encryptBuffer(data, nonce = randomBytes(8)) {
  const iv = Buffer.concat([nonce, Buffer.alloc(8)]);
  const cipher = createCipheriv("aes-128-ctr", resourceKey(), iv);
  return Buffer.concat([MAGIC, nonce, cipher.update(data), cipher.final()]);
}

/** Decrypts a buffer made by {@link encryptBuffer}; a buffer without the header is returned as it is. */
export function decryptBuffer(data) {
  if (data.length < HEADER_SIZE || !data.subarray(0, MAGIC.length).equals(MAGIC)) return data;
  const iv = Buffer.concat([data.subarray(8, 16), Buffer.alloc(8)]);
  const cipher = createCipheriv("aes-128-ctr", resourceKey(), iv);
  return Buffer.concat([cipher.update(data.subarray(HEADER_SIZE)), cipher.final()]);
}

/**
 * Encrypts a file in place (mtime kept, so the "up to date" copy check still works).
 * Returns false when it was already encrypted.
 */
export function encryptFileInPlace(file) {
  if (isEncryptedFile(file)) return false;
  const stat = statSync(file);
  const encrypted = encryptBuffer(readFileSync(file));
  const temp = file + ".enc-tmp";
  writeFileSync(temp, encrypted);
  utimesSync(temp, stat.atime, stat.mtime);
  renameSync(temp, file);
  return true;
}

/** Encrypts every file below a directory; returns how many were converted. */
export function encryptTree(root) {
  let count = 0;
  const walk = (dir) => {
    let entries;
    try {
      entries = readdirSync(dir, { withFileTypes: true });
    } catch (error) {
      return; // a missing directory has nothing to encrypt
    }
    for (const entry of entries) {
      const full = path.join(dir, entry.name);
      if (entry.isDirectory()) walk(full);
      else if (entry.isFile() && encryptFileInPlace(full)) count++;
    }
  };
  walk(root);
  return count;
}
