// Small shared helpers for the Pokemon Builder.
import { createHash } from "node:crypto";
import { inflateSync } from "node:zlib";

const UTF8 = new TextDecoder("utf-8", { fatal: true });
const LATIN1 = new TextDecoder("latin1");

// Ruby Marshal carries no charset. RGSS projects are written in a local code
// page, but UTF-8 is by far the most common today, so prefer it and fall back
// to latin1 when the bytes are not valid UTF-8.
export function decodeRubyString(buffer) {
  try {
    return UTF8.decode(buffer);
  } catch (error) {
    return LATIN1.decode(buffer);
  }
}

// Script sources inside Scripts.rxdata are zlib deflated streams. Returns a
// strict result so the scanner can report sections it cannot recover instead
// of silently returning garbage:
//   { ok: true,  mode: "zlib"|"plain"|"empty", source: "<ruby source>" }
//   { ok: false, mode: "...",                  error: "<reason>" }
export function inflateRubySource(payload) {
  const bytes = Buffer.isBuffer(payload)
    ? payload
    : Buffer.from(payload === null || payload === undefined ? "" : payload, "latin1");
  if (bytes.length === 0) return { ok: true, mode: "empty", source: "" };
  try {
    return { ok: true, mode: "zlib", source: decodeRubyString(inflateSync(bytes)) };
  } catch (error) {
    // Some tools store sections uncompressed; accept that when the bytes look
    // like text instead of binary noise.
    const text = decodeRubyString(bytes);
    if (text.includes("\0")) {
      return { ok: false, mode: "unknown", error: "payload is neither zlib nor readable text" };
    }
    return { ok: true, mode: "plain", source: text };
  }
}

// Backwards compatible helper: never throws, falls back to the raw bytes.
export function inflateRubyString(text) {
  const result = inflateRubySource(text);
  return result.ok ? result.source : decodeRubyString(Buffer.from(text || "", "latin1"));
}

export function sha1(text) {
  return createHash("sha1").update(text, "utf8").digest("hex").slice(0, 16).toUpperCase();
}

export function countLines(text) {
  if (text.length === 0) return 0;
  const lines = text.split(/\r?\n/);
  if (lines.length > 1 && lines[lines.length - 1] === "") lines.pop();
  return lines.length;
}

export function truncate(text, max) {
  if (text.length <= max) return text;
  return text.slice(0, max) + "...";
}

export function slug(text) {
  return text
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60) || "pattern";
}
