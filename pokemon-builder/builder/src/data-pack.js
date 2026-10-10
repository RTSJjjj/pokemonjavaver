// L3: the Android runtime data pack.
//
// The data the runtime reads on a device (generated/ + Graphics/ + Fonts/) is
// staged as ONE zip asset for the APK; the launcher unpacks it on first launch
// (core DataPackUnpacker). This module writes the zip itself with a small
// synchronous ZIP writer so the Builder stays dependency-free and fast enough
// for ~26k files / several hundred megabytes; the payload is mostly already
// compressed (ogg/png), so entries deflate at level 1 and fall back to "store"
// when deflating does not help.

import { closeSync, copyFileSync, mkdirSync, openSync, readFileSync, readdirSync, statSync, writeFileSync, writeSync } from "node:fs";
import { createHash } from "node:crypto";
import path from "node:path";
import { deflateRawSync } from "node:zlib";
import { HEADER_SIZE, encryptBuffer, shouldEncrypt } from "./resource-crypto.js";

const CRC_TABLE = (() => {
  const table = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) {
      c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    }
    table[n] = c;
  }
  return table;
})();

function crc32(buffer) {
  let crc = 0xffffffff;
  for (let i = 0; i < buffer.length; i++) {
    crc = CRC_TABLE[(crc ^ buffer[i]) & 0xff] ^ (crc >>> 8);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function dosDateTime(mtimeMs) {
  const date = new Date(mtimeMs);
  const year = Math.min(2107, Math.max(1980, date.getFullYear()));
  return {
    time: ((date.getHours() << 11) | (date.getMinutes() << 5) | Math.floor(date.getSeconds() / 2)) & 0xffff,
    date: (((year - 1980) << 9) | ((date.getMonth() + 1) << 5) | date.getDate()) & 0xffff,
  };
}

function collectFiles(root, prefix) {
  const files = [];
  const walk = (dir, parts) => {
    let items;
    try {
      items = readdirSync(dir, { withFileTypes: true }).sort((a, b) => (a.name < b.name ? -1 : 1));
    } catch (error) {
      return; // a missing source root contributes nothing (reported by its caller)
    }
    for (const item of items) {
      const diskPath = path.join(dir, item.name);
      if (item.isDirectory()) {
        walk(diskPath, parts.concat(item.name));
      } else if (item.isFile()) {
        files.push({
          diskPath,
          entryName: parts.concat(item.name).join("/"),
          mtimeMs: statSync(diskPath).mtimeMs,
        });
      }
    }
  };
  walk(root, [prefix.replace(/\/+$/, "")]);
  return files;
}

function writeAll(fd, buffer) {
  let offset = 0;
  while (offset < buffer.length) {
    offset += writeSync(fd, buffer, offset, buffer.length - offset);
  }
}

/**
 * Writes a ZIP with one entry per file below each source root.
 *
 * @param {{root: string, prefix: string}[]} sources directory + in-zip prefix
 * @param {string} zipPath target zip file
 * @param {{encrypt?: boolean}} [options] encrypt: every entry is stored encrypted (P3, resource-crypto.js)
 * @returns {{zipPath: string, files: number, bytes: number, version: string}}
 *          version is a content hash usable as the unpack marker
 */
export function createDataPack(sources, zipPath, options = {}) {
  const entries = [];
  for (const source of sources) {
    entries.push(...collectFiles(source.root, source.prefix));
  }
  entries.sort((a, b) => (a.entryName < b.entryName ? -1 : a.entryName > b.entryName ? 1 : 0));

  mkdirSync(path.dirname(zipPath), { recursive: true });
  const fd = openSync(zipPath, "w");
  const hash = createHash("sha256");
  const central = [];
  let offset = 0;
  let bytes = 0;
  try {
    for (const entry of entries) {
      const plain = readFileSync(entry.diskPath);
      hash.update(entry.entryName).update("\u0000").update(plain); // the version follows the content, not the nonce
      const data = options.encrypt && shouldEncrypt(entry.entryName) ? encryptBuffer(plain) : plain;
      let method = 8;
      let payload = deflateRawSync(data, { level: 1 });
      if (payload.length >= data.length) {
        method = 0;
        payload = data;
      }
      const name = Buffer.from(entry.entryName, "utf8");
      const { time, date } = dosDateTime(entry.mtimeMs);
      const crc = crc32(data);
      const header = Buffer.alloc(30);
      header.writeUInt32LE(0x04034b50, 0);
      header.writeUInt16LE(20, 4); // version needed
      header.writeUInt16LE(0x0800, 6); // UTF-8 names
      header.writeUInt16LE(method, 8);
      header.writeUInt16LE(time, 10);
      header.writeUInt16LE(date, 12);
      header.writeUInt32LE(crc, 14);
      header.writeUInt32LE(payload.length, 18);
      header.writeUInt32LE(data.length, 22);
      header.writeUInt16LE(name.length, 26);
      header.writeUInt16LE(0, 28);
      writeAll(fd, header);
      writeAll(fd, name);
      writeAll(fd, payload);
      central.push({ name, method, time, date, crc, compressed: payload.length, uncompressed: data.length, offset });
      offset += header.length + name.length + payload.length;
      bytes += data.length;
    }

    if (entries.length > 0xffff || bytes >= 0xffffffff) {
      throw new Error("data pack exceeds the classic ZIP limits (files=" + entries.length + ", bytes=" + bytes + ")");
    }

    const centralStart = offset;
    let centralBytes = 0;
    for (const entry of central) {
      const header = Buffer.alloc(46);
      header.writeUInt32LE(0x02014b50, 0);
      header.writeUInt16LE(20, 4); // version made by
      header.writeUInt16LE(20, 6); // version needed
      header.writeUInt16LE(0x0800, 8);
      header.writeUInt16LE(entry.method, 10);
      header.writeUInt16LE(entry.time, 12);
      header.writeUInt16LE(entry.date, 14);
      header.writeUInt32LE(entry.crc, 16);
      header.writeUInt32LE(entry.compressed, 20);
      header.writeUInt32LE(entry.uncompressed, 24);
      header.writeUInt16LE(entry.name.length, 28);
      header.writeUInt16LE(0, 30); // extra length
      header.writeUInt16LE(0, 32); // comment length
      header.writeUInt16LE(0, 34); // disk number
      header.writeUInt16LE(0, 36); // internal attributes
      header.writeUInt32LE(0, 38); // external attributes
      header.writeUInt32LE(entry.offset, 42);
      writeAll(fd, header);
      writeAll(fd, entry.name);
      centralBytes += header.length + entry.name.length;
    }

    const eocd = Buffer.alloc(22);
    eocd.writeUInt32LE(0x06054b50, 0);
    eocd.writeUInt16LE(0, 4);
    eocd.writeUInt16LE(0, 6);
    eocd.writeUInt16LE(central.length, 8);
    eocd.writeUInt16LE(central.length, 10);
    eocd.writeUInt32LE(centralBytes, 12);
    eocd.writeUInt32LE(centralStart, 16);
    eocd.writeUInt16LE(0, 20);
    writeAll(fd, eocd);
    offset += eocd.length;
  } finally {
    closeSync(fd);
  }

  const version = hash.digest("hex").slice(0, 16) + "-" + entries.length;
  return { zipPath, files: entries.length, bytes, version };
}

/**
 * L4 desktop helper: copies the graphical assets the runtime reads into the
 * packaged runtime data root (Graphics/ + Fonts/) so the distribution works
 * without the source project.
 */
export function copyRuntimeAssets(projectPath, runtimeDataRoot, logger) {
  let files = 0;
  let bytes = 0;
  for (const folder of ["Graphics", "Fonts"]) {
    const source = path.join(projectPath, folder);
    const target = path.join(runtimeDataRoot, folder);
    const result = copyTree(source, target, logger);
    files += result.files;
    bytes += result.bytes;
  }
  return { files, bytes };
}

function copyTree(source, target, logger) {
  let files = 0;
  let bytes = 0;
  let entries;
  try {
    entries = readdirSync(source, { withFileTypes: true });
  } catch (error) {
    if (logger) logger.warn("cannot read " + source + ": " + error.message);
    return { files, bytes };
  }
  for (const entry of entries) {
    const from = path.join(source, entry.name);
    const to = path.join(target, entry.name);
    if (entry.isDirectory()) {
      const nested = copyTree(from, to, logger);
      files += nested.files;
      bytes += nested.bytes;
    } else if (entry.isFile()) {
      let stat;
      let existing = null;
      try {
        stat = statSync(from);
        existing = statSync(to);
      } catch (error) {
        existing = null;
      }
      // P3: an encrypted copy is HEADER_SIZE bytes longer and keeps the source's mtime
      if (existing && (existing.size === stat.size || existing.size === stat.size + HEADER_SIZE)
          && existing.mtimeMs >= stat.mtimeMs) {
        continue; // up to date
      }
      mkdirSync(path.dirname(to), { recursive: true });
      copyFileSync(from, to);
      files++;
      bytes += stat.size;
    }
  }
  return { files, bytes };
}
