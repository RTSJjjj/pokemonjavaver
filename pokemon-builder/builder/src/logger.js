// Build logger: mirrors every message to the console and appends it to
// logs/<timestamp>.log while keeping logs/latest.log as the current run.
// Errors are never swallowed: they go to the log, to stderr and to the
// exit code.

import { appendFileSync, mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";

function pad2(value) {
  return String(value).padStart(2, "0");
}

export function timestamp(date = new Date()) {
  return (
    date.getFullYear() +
    "-" +
    pad2(date.getMonth() + 1) +
    "-" +
    pad2(date.getDate()) +
    "_" +
    pad2(date.getHours()) +
    pad2(date.getMinutes()) +
    pad2(date.getSeconds())
  );
}

export class Logger {
  constructor(logsDir, meta = {}) {
    this.logsDir = logsDir;
    this.meta = meta;
    this.startedAt = Date.now();
    this.buffer = [];
    // The log directory itself can be unwritable (a file in its place, a read
    // only volume). That must not crash the run: messages still reach the
    // console and the exit code still fails.
    let logsReady = true;
    try {
      mkdirSync(logsDir, { recursive: true });
    } catch (error) {
      logsReady = false;
      console.error("ERROR: cannot create log directory: " + logsDir + ": " + error.message);
    }
    this.logFile = logsReady ? path.join(logsDir, timestamp() + ".log") : null;
    this.latestFile = logsReady ? path.join(logsDir, "latest.log") : null;
    this._append("=== Pokemon Builder " + (meta.version || "") + " ===");
    this._append("Command: " + (meta.command || "(none)"));
    this._append("Arguments: " + (meta.argv && meta.argv.length ? meta.argv.join(" ") : "(none)"));
    this._append("Project: " + (meta.project || "(not set)"));
    this._append("Started: " + new Date().toISOString());
  }

  _append(line) {
    this.buffer.push(line);
    if (!this.logFile) return;
    try {
      appendFileSync(this.logFile, line + "\n", "utf8");
      writeFileSync(this.latestFile, this.buffer.join("\n") + "\n", "utf8");
    } catch (error) {
      console.error("ERROR: cannot write log file: " + error.message);
    }
  }

  info(message) {
    this._append("[INFO] " + message);
    console.log(message);
  }

  step(message) {
    this._append("[STEP] " + message);
    console.log(message);
  }

  warn(message) {
    this._append("[WARN] " + message);
    console.log("WARNING: " + message);
  }

  error(message) {
    this._append("[ERROR] " + message);
    console.error("ERROR: " + message);
  }
  // Every artifact a run produced, in order. Recorded in the log only: the
  // console stays readable while latest.log keeps the full manifest.
  output(file) {
    this._append("[OUTPUT] " + file);
  }

  finish(exitCode) {
    this._append(
      "Finished: " +
        new Date().toISOString() +
        " exit=" +
        exitCode +
        " duration=" +
        (Date.now() - this.startedAt) +
        "ms"
    );
  }
}