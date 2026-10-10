// Reads the icon a Windows .exe carries (RMXP's Game.exe) and converts it to RGBA / PNG, so the Android build can use it as the
// launcher icon. Pure Node: a small PE resource reader, an ICO DIB decoder and a PNG writer.

import { readFileSync } from "node:fs";
import { deflateSync } from "node:zlib";

const RT_ICON = 3;
const RT_GROUP_ICON = 14;

function sectionOffset(sections, rva) {
  for (const s of sections) {
    if (rva >= s.rva && rva < s.rva + Math.max(s.virtualSize, s.rawSize)) {
      return rva - s.rva + s.rawPointer;
    }
  }
  return -1;
}

/** Walks the resource directory and returns {type: {id: Buffer}} for RT_ICON and RT_GROUP_ICON. */
export function readExeResources(buffer) {
  if (buffer.readUInt16LE(0) !== 0x5a4d) throw new Error("not an exe (MZ)");
  const peOffset = buffer.readUInt32LE(0x3c);
  if (buffer.readUInt32LE(peOffset) !== 0x00004550) throw new Error("not a PE file");
  const sectionCount = buffer.readUInt16LE(peOffset + 6);
  const optionalSize = buffer.readUInt16LE(peOffset + 20);
  const optional = peOffset + 24;
  const magic = buffer.readUInt16LE(optional);
  const dirBase = optional + (magic === 0x20b ? 112 : 96);
  const resourceRva = buffer.readUInt32LE(dirBase + 2 * 8);
  if (!resourceRva) return {};
  const sections = [];
  let at = optional + optionalSize;
  for (let i = 0; i < sectionCount; i++, at += 40) {
    sections.push({
      virtualSize: buffer.readUInt32LE(at + 8),
      rva: buffer.readUInt32LE(at + 12),
      rawSize: buffer.readUInt32LE(at + 16),
      rawPointer: buffer.readUInt32LE(at + 20),
    });
  }
  const base = sectionOffset(sections, resourceRva);
  if (base < 0) return {};

  const readDir = (offset) => {
    const named = buffer.readUInt16LE(offset + 12);
    const ids = buffer.readUInt16LE(offset + 14);
    const entries = [];
    for (let i = 0; i < named + ids; i++) {
      const at2 = offset + 16 + i * 8;
      const name = buffer.readUInt32LE(at2);
      const target = buffer.readUInt32LE(at2 + 4);
      entries.push({
        id: name & 0x80000000 ? null : name,
        isDir: (target & 0x80000000) !== 0,
        offset: base + (target & 0x7fffffff),
      });
    }
    return entries;
  };

  const out = {};
  for (const typeEntry of readDir(base)) {
    if (typeEntry.id !== RT_ICON && typeEntry.id !== RT_GROUP_ICON) continue;
    out[typeEntry.id] = {};
    for (const nameEntry of readDir(typeEntry.offset)) {
      if (!nameEntry.isDir) continue;
      for (const langEntry of readDir(nameEntry.offset)) {
        const dataEntry = langEntry.offset;                   // IMAGE_RESOURCE_DATA_ENTRY
        const dataRva = buffer.readUInt32LE(dataEntry);
        const size = buffer.readUInt32LE(dataEntry + 4);
        const file = sectionOffset(sections, dataRva);
        if (file >= 0) {
          out[typeEntry.id][nameEntry.id] = buffer.subarray(file, file + size);
          break;
        }
      }
    }
  }
  return out;
}

/** The icon images of the exe's first icon group: [{width, height, bits, data}] (data = the RT_ICON payload). */
export function exeIcons(buffer) {
  const resources = readExeResources(buffer);
  const groups = resources[RT_GROUP_ICON] || {};
  const icons = resources[RT_ICON] || {};
  const groupIds = Object.keys(groups).sort((a, b) => Number(a) - Number(b));
  if (groupIds.length === 0) return [];
  const group = groups[groupIds[0]];
  const count = group.readUInt16LE(4);
  const images = [];
  for (let i = 0; i < count; i++) {
    const at = 6 + i * 14;
    const width = group[at] || 256;
    const height = group[at + 1] || 256;
    const bits = group.readUInt16LE(at + 6);
    const id = group.readUInt16LE(at + 12);
    if (icons[id]) images.push({ width, height, bits, data: icons[id] });
  }
  return images;
}

/** Decodes one icon image (PNG payload or DIB with AND mask) to {width, height, rgba}. */
export function decodeIcon(image) {
  const d = image.data;
  if (d.length > 8 && d.readUInt32BE(0) === 0x89504e47) {
    return { png: d, width: image.width, height: image.height };
  }
  const headerSize = d.readUInt32LE(0);
  const width = d.readInt32LE(4);
  const heightWithMask = d.readInt32LE(8);
  const height = Math.abs(heightWithMask) / 2;
  const bpp = d.readUInt16LE(14);
  const colorsUsed = d.readUInt32LE(32) || (bpp <= 8 ? 1 << bpp : 0);
  let pos = headerSize;
  const palette = [];
  if (bpp <= 8) {
    for (let i = 0; i < colorsUsed; i++, pos += 4) palette.push([d[pos + 2], d[pos + 1], d[pos], 255]);
  }
  const rowBytes = (Math.floor((width * bpp + 31) / 32)) * 4;
  const maskRow = (Math.floor((width + 31) / 32)) * 4;
  const maskStart = pos + rowBytes * height;
  const rgba = Buffer.alloc(width * height * 4);
  let anyAlpha = false;
  for (let y = 0; y < height; y++) {
    const src = pos + (height - 1 - y) * rowBytes;           // bottom-up
    for (let x = 0; x < width; x++) {
      let r; let g; let b; let a = 255;
      if (bpp === 32) {
        const p = src + x * 4;
        b = d[p]; g = d[p + 1]; r = d[p + 2]; a = d[p + 3];
        if (a !== 0) anyAlpha = true;
      } else if (bpp === 24) {
        const p = src + x * 3;
        b = d[p]; g = d[p + 1]; r = d[p + 2];
      } else if (bpp === 8) {
        [r, g, b] = palette[d[src + x]] || [0, 0, 0];
      } else if (bpp === 4) {
        const v = (d[src + (x >> 1)] >> (x & 1 ? 0 : 4)) & 15;
        [r, g, b] = palette[v] || [0, 0, 0];
      } else if (bpp === 1) {
        const v = (d[src + (x >> 3)] >> (7 - (x & 7))) & 1;
        [r, g, b] = palette[v] || [0, 0, 0];
      } else {
        throw new Error("unsupported icon bit depth " + bpp);
      }
      const o = (y * width + x) * 4;
      rgba[o] = r; rgba[o + 1] = g; rgba[o + 2] = b; rgba[o + 3] = a;
    }
  }
  // the AND mask makes pixels transparent (all 32-bit icons with a real alpha channel ignore it)
  if (!(bpp === 32 && anyAlpha)) {
    for (let y = 0; y < height; y++) {
      const src = maskStart + (height - 1 - y) * maskRow;
      for (let x = 0; x < width; x++) {
        if ((d[src + (x >> 3)] >> (7 - (x & 7))) & 1) rgba[(y * width + x) * 4 + 3] = 0;
        else if (bpp === 32) rgba[(y * width + x) * 4 + 3] = 255;
      }
    }
  }
  return { width, height, rgba };
}

const CRC_TABLE = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c >>> 0;
  }
  return table;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const out = Buffer.alloc(12 + data.length);
  out.writeUInt32BE(data.length, 0);
  out.write(type, 4, "latin1");
  data.copy(out, 8);
  out.writeUInt32BE(crc32(out.subarray(4, 8 + data.length)), 8 + data.length);
  return out;
}

/** An RGBA image as a PNG. */
export function encodePng(width, height, rgba) {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width, 0);
  header.writeUInt32BE(height, 4);
  header[8] = 8; header[9] = 6;
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y++) {
    raw[y * (width * 4 + 1)] = 0;
    rgba.copy(raw, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4);
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk("IHDR", header), chunk("IDAT", deflateSync(raw)), chunk("IEND", Buffer.alloc(0)),
  ]);
}

/** Nearest-neighbour resize (the icons are pixel art). */
export function resizeNearest(src, srcW, srcH, size) {
  const out = Buffer.alloc(size * size * 4);
  for (let y = 0; y < size; y++) {
    const sy = Math.min(srcH - 1, Math.floor((y * srcH) / size));
    for (let x = 0; x < size; x++) {
      const sx = Math.min(srcW - 1, Math.floor((x * srcW) / size));
      src.copy(out, (y * size + x) * 4, (sy * srcW + sx) * 4, (sy * srcW + sx) * 4 + 4);
    }
  }
  return out;
}

/** The largest icon of an exe as {width, height, rgba}, or null when it has none. */
export function largestExeIcon(exePath) {
  const images = exeIcons(readFileSync(exePath)).filter((i) => i.data.readUInt32BE(0) !== 0x89504e47 || true);
  if (images.length === 0) return null;
  images.sort((a, b) => b.width * b.height - a.width * a.height || b.bits - a.bits);
  for (const image of images) {
    try {
      const decoded = decodeIcon(image);
      if (decoded.rgba) return decoded;
    } catch (error) {
      // try the next size
    }
  }
  return null;
}
