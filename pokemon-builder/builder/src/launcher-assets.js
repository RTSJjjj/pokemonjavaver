// The Android launcher icon and application name, taken from the game project.
//
// Icon: Icon192.png in the project root when it exists (the same artwork as Game.exe's icon at its native size), else
// Game.ico, else the icon inside Game.exe. It is scaled to the five launcher densities and written, with the application
// name, to runtime/android/build/launcher-info/res - the directory runtime/android/build.gradle uses when it exists.

import { cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import path from "node:path";
import { inflateSync } from "node:zlib";
import { decodeIcon, encodePng, largestExeIcon } from "./exe-icon.js";

export const DENSITIES = [
  { dir: "mipmap-mdpi", size: 48 },
  { dir: "mipmap-hdpi", size: 72 },
  { dir: "mipmap-xhdpi", size: 96 },
  { dir: "mipmap-xxhdpi", size: 144 },
  { dir: "mipmap-xxxhdpi", size: 192 },
];

/** Decodes a PNG (8-bit grey / RGB / palette / grey+alpha / RGBA, no interlacing) to {width, height, rgba}. */
export function decodePng(buffer) {
  if (buffer.readUInt32BE(0) !== 0x89504e47) throw new Error("not a PNG");
  let pos = 8;
  let width = 0;
  let height = 0;
  let depth = 0;
  let colorType = 0;
  let interlace = 0;
  let palette = null;
  let transparency = null;
  const data = [];
  while (pos < buffer.length) {
    const length = buffer.readUInt32BE(pos);
    const type = buffer.toString("latin1", pos + 4, pos + 8);
    const body = buffer.subarray(pos + 8, pos + 8 + length);
    if (type === "IHDR") {
      width = body.readUInt32BE(0);
      height = body.readUInt32BE(4);
      depth = body[8];
      colorType = body[9];
      interlace = body[12];
    } else if (type === "PLTE") {
      palette = body;
    } else if (type === "tRNS") {
      transparency = body;
    } else if (type === "IDAT") {
      data.push(body);
    } else if (type === "IEND") {
      break;
    }
    pos += 12 + length;
  }
  if (depth !== 8 || interlace !== 0) throw new Error("unsupported PNG (depth " + depth + ", interlace " + interlace + ")");
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
  if (!channels) throw new Error("unsupported PNG colour type " + colorType);
  const raw = inflateSync(Buffer.concat(data));
  const stride = width * channels;
  const pixels = Buffer.alloc(stride * height);
  for (let y = 0; y < height; y++) {
    const filter = raw[y * (stride + 1)];
    const line = y * (stride + 1) + 1;
    for (let x = 0; x < stride; x++) {
      const left = x >= channels ? pixels[y * stride + x - channels] : 0;
      const up = y > 0 ? pixels[(y - 1) * stride + x] : 0;
      const upLeft = y > 0 && x >= channels ? pixels[(y - 1) * stride + x - channels] : 0;
      let value = raw[line + x];
      if (filter === 1) value += left;
      else if (filter === 2) value += up;
      else if (filter === 3) value += (left + up) >> 1;
      else if (filter === 4) {
        const p = left + up - upLeft;
        const pa = Math.abs(p - left);
        const pb = Math.abs(p - up);
        const pc = Math.abs(p - upLeft);
        value += pa <= pb && pa <= pc ? left : pb <= pc ? up : upLeft;
      }
      pixels[y * stride + x] = value & 0xff;
    }
  }
  const rgba = Buffer.alloc(width * height * 4);
  for (let i = 0; i < width * height; i++) {
    let r; let g; let b; let a = 255;
    if (colorType === 6) {
      r = pixels[i * 4]; g = pixels[i * 4 + 1]; b = pixels[i * 4 + 2]; a = pixels[i * 4 + 3];
    } else if (colorType === 2) {
      r = pixels[i * 3]; g = pixels[i * 3 + 1]; b = pixels[i * 3 + 2];
    } else if (colorType === 0) {
      r = g = b = pixels[i];
    } else if (colorType === 4) {
      r = g = b = pixels[i * 2]; a = pixels[i * 2 + 1];
    } else {
      const index = pixels[i];
      r = palette[index * 3]; g = palette[index * 3 + 1]; b = palette[index * 3 + 2];
      if (transparency && index < transparency.length) a = transparency[index];
    }
    rgba[i * 4] = r; rgba[i * 4 + 1] = g; rgba[i * 4 + 2] = b; rgba[i * 4 + 3] = a;
  }
  return { width, height, rgba };
}

/** Scales a square-ish RGBA image: nearest when it grows (pixel art), an area average when it shrinks. */
export function scaleRgba(src, srcW, srcH, size) {
  const out = Buffer.alloc(size * size * 4);
  if (size >= srcW) {
    for (let y = 0; y < size; y++) {
      const sy = Math.min(srcH - 1, Math.floor((y * srcH) / size));
      for (let x = 0; x < size; x++) {
        const sx = Math.min(srcW - 1, Math.floor((x * srcW) / size));
        src.copy(out, (y * size + x) * 4, (sy * srcW + sx) * 4, (sy * srcW + sx) * 4 + 4);
      }
    }
    return out;
  }
  for (let y = 0; y < size; y++) {
    const y0 = (y * srcH) / size;
    const y1 = ((y + 1) * srcH) / size;
    for (let x = 0; x < size; x++) {
      const x0 = (x * srcW) / size;
      const x1 = ((x + 1) * srcW) / size;
      let r = 0; let g = 0; let b = 0; let a = 0; let weight = 0;
      for (let sy = Math.floor(y0); sy < Math.ceil(y1); sy++) {
        const wy = Math.min(sy + 1, y1) - Math.max(sy, y0);
        for (let sx = Math.floor(x0); sx < Math.ceil(x1); sx++) {
          const w = wy * (Math.min(sx + 1, x1) - Math.max(sx, x0));
          const o = (sy * srcW + sx) * 4;
          const alpha = src[o + 3] * w;                     // colours weighted by alpha
          r += src[o] * alpha; g += src[o + 1] * alpha; b += src[o + 2] * alpha;
          a += alpha;
          weight += w;
        }
      }
      const o = (y * size + x) * 4;
      if (a > 0) {
        out[o] = Math.round(r / a); out[o + 1] = Math.round(g / a); out[o + 2] = Math.round(b / a);
      }
      out[o + 3] = Math.round(a / weight);
    }
  }
  return out;
}

/** Finds the game's icon: {width, height, rgba, source} or null. */
export function findProjectIcon(projectPath) {
  const png = path.join(projectPath, "Icon192.png");
  if (existsSync(png)) {
    try {
      return { ...decodePng(readFileSync(png)), source: "Icon192.png" };
    } catch (error) {
      // fall through to the exe
    }
  }
  for (const name of ["Game.ico"]) {
    const file = path.join(projectPath, name);
    if (!existsSync(file)) continue;
    try {
      const buffer = readFileSync(file);
      const count = buffer.readUInt16LE(4);
      let best = null;
      for (let i = 0; i < count; i++) {
        const at = 6 + i * 16;
        const w = buffer[at] || 256;
        const size = buffer.readUInt32LE(at + 8);
        const offset = buffer.readUInt32LE(at + 12);
        if (!best || w > best.width) best = { width: w, height: buffer[at + 1] || 256, data: buffer.subarray(offset, offset + size) };
      }
      if (best) {
        const decoded = decodeIcon(best);
        if (decoded.rgba) return { ...decoded, source: name };
        if (decoded.png) return { ...decodePng(decoded.png), source: name };
      }
    } catch (error) {
      // next candidate
    }
  }
  const exe = path.join(projectPath, "Game.exe");
  if (existsSync(exe)) {
    const icon = largestExeIcon(exe);
    if (icon) return { ...icon, source: "Game.exe" };
  }
  return null;
}

function xmlEscape(text) {
  return String(text).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "\\'");
}

/**
 * Writes runtime/android/build/launcher-info/res/{mipmap-*, values/launcher_strings.xml}.
 *
 * @returns {{icon: string|null, appName: string, dir: string}}
 */
export function writeLauncherAssets(projectPath, androidDir, appName) {
  const dir = path.join(androidDir, "build", "launcher-info", "res");
  rmSync(path.join(androidDir, "build", "launcher-info"), { recursive: true, force: true });
  mkdirSync(path.join(dir, "values"), { recursive: true });
  const defaults = path.join(androidDir, "src", "main", "launcher-default");
  if (existsSync(defaults)) {
    cpSync(defaults, dir, { recursive: true });                 // the default icon / name first; the project's replace them
  }
  const icon = findProjectIcon(projectPath);
  if (icon) {
    for (const density of DENSITIES) {
      const target = path.join(dir, density.dir);
      mkdirSync(target, { recursive: true });
      const scaled = scaleRgba(icon.rgba, icon.width, icon.height, density.size);
      writeFileSync(path.join(target, "ic_launcher.png"), encodePng(density.size, density.size, scaled));
    }
  }
  writeFileSync(
    path.join(dir, "values", "launcher_strings.xml"),
    '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">' + xmlEscape(appName) + "</string>\n</resources>\n",
    "utf8",
  );
  return { icon: icon ? icon.source : null, appName, dir };
}
