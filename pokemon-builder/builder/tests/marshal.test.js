// Unit tests for the Ruby Marshal reader (Phase 3).
// Fixtures are produced by marshal-writer.js so every encoding branch
// (short negatives, object links, ivar wrapping, ...) is covered.

import test from "node:test";
import assert from "node:assert/strict";

import { marshalLoad, MarshalError, hashEntries } from "../src/marshal.js";
import {
  array, bool, dump, float, hash, int, ivar, link, nilValue, object, str, sym, uclass,
} from "./marshal-writer.js";

test("reads the marshal version header", () => {
  assert.throws(() => marshalLoad(Buffer.from([9, 8, 0])), MarshalError);
  assert.throws(() => marshalLoad(Buffer.from([4, 9, 0])), /unsupported marshal version/);
});

test("reads nil, booleans and fixnums", () => {
  const data = marshalLoad(dump(array(nilValue(), bool(true), bool(false), int(0))));
  assert.deepEqual(data, [null, true, false, 0]);
});

test("reads the short positive fixnum range", () => {
  const data = marshalLoad(dump(array(int(1), int(5), int(122), int(123), int(255), int(65535))));
  assert.deepEqual(data, [1, 5, 122, 123, 255, 65535]);
});

test("reads short negative fixnums (the RGSS packing used by 128..251)", () => {
  // Byte 0x83 (131) packs Fixnum -120; a naive reader treats it as a
  // multi-byte length and desynchronises the whole stream.
  const data = marshalLoad(dump(array(int(-1), int(-4), int(-5), int(-120), int(-123))));
  assert.deepEqual(data, [-1, -4, -5, -120, -123]);
});

test("reads multi-byte negative fixnums", () => {
  const data = marshalLoad(dump(array(int(-124), int(-1000), int(-70000), int(-2147483648))));
  assert.deepEqual(data, [-124, -1000, -70000, -2147483648]);
});

const utf8 = (text) => Buffer.from(text, "utf8").toString("latin1");

test("keeps strings byte preserving and decodes UTF-8 by request", () => {
  const data = marshalLoad(dump(array(str("hello"), str(utf8("中文テスト")))));
  assert.equal(data[0], "hello");
  // byte-preserving: latin1 view of the UTF-8 bytes
  assert.equal(Buffer.from(data[1], "latin1").toString("utf8"), "中文テスト");
});

test("reads symbols and symbol links", () => {
  const data = marshalLoad(dump(hash([sym("a"), int(1)], [sym("b"), sym("a")])));
  const pairs = hashEntries(data);
  assert.equal(pairs[0][0], "a");
  assert.equal(pairs[1][1], "a"); // written as a ';' symlink
});

test("reads nested arrays, hashes and object links", () => {
  const shared = array(int(1), int(2));
  const data = marshalLoad(dump(array(shared, link(shared))));
  assert.deepEqual(data[0], [1, 2]);
  assert.equal(data[1], data[0]); // same reference => link resolved
});

test("reads objects with instance variables in declaration order", () => {
  const data = marshalLoad(dump(object("RPG::EventCommand", {
    "@code": int(355),
    "@indent": int(0),
    "@parameters": array(str("pbTrainerBattle(:TYPE")),
  })));
  assert.equal(data.__class__, "RPG::EventCommand");
  assert.equal(data["@code"], 355);
  assert.equal(data["@indent"], 0);
  assert.deepEqual(data["@parameters"], ["pbTrainerBattle(:TYPE"]);
});

test("reads hashes with non string keys", () => {
  const data = marshalLoad(dump(hash([int(7), str("seven")], [nilValue(), int(0)])));
  const pairs = hashEntries(data);
  assert.deepEqual(pairs[0], [7, "seven"]);
  assert.deepEqual(pairs[1], [null, 0]);
});

test("reads floats", () => {
  const data = marshalLoad(dump(array(float(1.5), float(-2.25), float(62.207176384839649))));
  assert.equal(data[0], 1.5);
  assert.equal(data[1], -2.25);
  assert.ok(Math.abs(data[2] - 62.207176384839649) < 1e-9);
});

test("reads TYPE_IVAR wrapped values and TYPE_UCLASS values", () => {
  // Pokemon Essentials writes PBAnimations / PBAnimation as Array subclasses
  // with extra instance variables.
  const anim = ivar(uclass("PBAnimation", array(int(1))), { "@name": str("Recover"), "@hue": int(0) });
  const data = marshalLoad(dump(anim));
  assert.equal(data.__class__, "PBAnimation");
  assert.ok(Array.isArray(data));
  assert.equal(data.length, 1);
  assert.equal(data[0], 1);
  assert.equal(data["@name"], "Recover");
  assert.equal(data["@hue"], 0);
});

test("wraps instance variables attached to primitive values instead of dropping them", () => {
  const data = marshalLoad(dump(ivar(str("text"), { "@encoding": sym("utf8") })));
  assert.equal(data.__ivar_wrapped__, true);
  assert.equal(data.__value__, "text");
  assert.equal(data["@encoding"], "utf8");
});

test("rejects unsupported node types with the stream offset", () => {
  const buffer = Buffer.from([4, 8, 0x58, 0x58]); // version + unknown node 'X'
  assert.throws(() => marshalLoad(buffer), /unsupported marshal node "X" at offset 2/);
});

test("rejects truncated streams", () => {
  const buffer = Buffer.from([4, 8, 0x5b, 0x0a, 0x69]); // '[' long(5) then EOF
  assert.throws(() => marshalLoad(buffer), MarshalError);
});

test("rejects empty buffers", () => {
  assert.throws(() => marshalLoad(Buffer.alloc(0)), MarshalError);
});
