// test-builder.bat wrapper tests (Phase 12).
//
// test-builder.bat runs the whole node:test suite, so these tests must never
// launch the real suite from inside the suite (that would recurse forever).
// A scratch builder therefore gets a tiny controlled pair of test files, which
// still exercises the real wrapper through cmd.exe: its existence checks, its
// node --test invocation and its exit-code propagation. The full suite against
// the real builder is run manually with the same wrapper.

import test from "node:test";
import assert from "node:assert/strict";
import { mkdirSync, writeFileSync, rmSync } from "node:fs";
import path from "node:path";

import {
  copyBuilder,
  makeProject,
  runTestBuilderBat,
  outputOf,
  snapshotProject,
  scratch,
} from "./fixtures.js";

const PASSING_TESTS = {
  "tiny-pass.test.js": [
    'import test from "node:test";',
    'test("tiny pass alpha", () => {});',
    'test("tiny pass beta", () => {});',
    "",
  ].join("\n"),
};

const FAILING_TESTS = {
  "tiny-pass.test.js": PASSING_TESTS["tiny-pass.test.js"],
  "tiny-fail.test.js": [
    'import test from "node:test";',
    'import assert from "node:assert/strict";',
    'test("tiny failing test", () => { assert.equal(1, 2); });',
    "",
  ].join("\n"),
};

// Replaces the copied real suite with the given fixture files so the wrapper
// under test has deterministic work to do and no recursion can happen.
function replaceSuite(root, files) {
  const testsDir = path.join(root, "builder", "tests");
  rmSync(testsDir, { recursive: true, force: true });
  mkdirSync(testsDir, { recursive: true });
  for (const [name, source] of Object.entries(files)) {
    writeFileSync(path.join(testsDir, name), source, "utf8");
  }
  return testsDir;
}

test("test-builder.bat runs the suite and reports TEST SUCCESS", (t) => {
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  replaceSuite(root, PASSING_TESTS);

  const result = runTestBuilderBat(root);
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  assert.match(out, /\[1\/1\] Running Builder test suite/);
  assert.match(out, /tiny pass alpha/);
  assert.match(out, /TEST SUCCESS/);
  assert.ok(!/TEST FAILED/.test(out), out);
});

test("test-builder.bat reports failing tests with a non-zero exit code", (t) => {
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  replaceSuite(root, FAILING_TESTS);

  const result = runTestBuilderBat(root);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /tiny failing test/);
  assert.match(out, /TEST FAILED/);
  assert.ok(!/TEST SUCCESS/.test(out), out);
});

test("test-builder.bat passes node:test options through to the runner", (t) => {
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  replaceSuite(root, PASSING_TESTS);

  const result = runTestBuilderBat(root, "--test-name-pattern=tiny pass beta");
  const out = outputOf(result);
  assert.equal(result.status, 0, out);
  assert.match(out, /tiny pass beta/);
  assert.ok(!/tiny pass alpha/.test(out), out);
  assert.match(out, /TEST SUCCESS/);
});

test("test-builder.bat stops when the builder installation is incomplete", (t) => {
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  replaceSuite(root, PASSING_TESTS);
  rmSync(path.join(root, "builder-config.json"), { force: true });

  const result = runTestBuilderBat(root);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /\[ERROR\] builder-config\.json not found/);
  // The wrapper stopped before running the suite: no suite output at all.
  assert.ok(!/Running Builder test suite/.test(out), out);
  assert.ok(!/tiny pass alpha/.test(out), out);
});

test("test-builder.bat stops when the tests directory is missing", (t) => {
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  rmSync(path.join(root, "builder", "tests"), { recursive: true, force: true });

  const result = runTestBuilderBat(root);
  const out = outputOf(result);
  assert.equal(result.status, 1, out);
  assert.match(out, /\[ERROR\] Builder tests directory not found/);
  assert.ok(!/Running Builder test suite/.test(out), out);
});

test("test-builder.bat never reads or writes the source project", (t) => {
  const dir = scratch("pb-scratch-", t);
  const root = copyBuilder();
  t.after(() => rmSync(root, { recursive: true, force: true }));
  const project = makeProject(dir, "Read Only Project");
  const before = snapshotProject(project);
  replaceSuite(root, PASSING_TESTS);

  assert.equal(runTestBuilderBat(root).status, 0, outputOf(runTestBuilderBat(root)));
  assert.deepEqual(snapshotProject(project), before);
});