// Audio audit reports: build/reports/audio-audit.json (machine readable, the
// input for later translation work) and docs/audio-audit.md (human readable).
// Mirrors the data audit report writer: JSON first, Markdown is a rendering.

import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";

import {
  AUDIO_CATEGORIES,
  formatBytes,
  formatDuration,
} from "./index.js";

function codecCounts(scan) {
  const counts = {};
  for (const file of scan.files) {
    const key = file.container ? file.container + " / " + file.codec : "unrecognized";
    counts[key] = (counts[key] || 0) + 1;
  }
  return counts;
}

function problemTables(problems) {
  const tables = {
    unreadable: [],
    "zero-length": [],
    "invalid-header": [],
    "unsupported-codec": [],
    "extension-mismatch": [],
  };
  for (const problem of problems) {
    if (!tables[problem.kind]) tables[problem.kind] = [];
    tables[problem.kind].push(problem);
  }
  return tables;
}

export function buildAudioAuditJson(projectPath, scan, options, builderVersion) {
  const files = scan.files.map((file) => ({
    category: file.category,
    path: file.relativePath,
    name: file.name,
    extension: file.ext,
    size: file.size,
    sha256: file.sha256,
    container: file.container,
    codec: file.codec,
    sampleRate: file.sampleRate,
    channels: file.channels,
    bitrate: file.bitrate,
    duration: file.duration === null || file.duration === undefined ? null : Math.round(file.duration * 1000) / 1000,
    durationApproximate: file.durationApproximate,
    matchedExtension: file.matchedExtension,
    unsupported: file.unsupported,
    problem: file.problem,
  }));
  return {
    generatedAt: new Date().toISOString(),
    builderVersion,
    project: projectPath,
    audioRoot: scan.audioRoot,
    options,
    totals: scan.totals,
    codecs: codecCounts(scan),
    duplicates: scan.duplicates,
    problems: scan.problems,
    files,
  };
}

function markdownTable(rows, header) {
  if (rows.length === 0) return "";
  const lines = ["| " + header.join(" | ") + " |", "| " + header.map(() => "---").join(" | ") + " |"];
  for (const row of rows) lines.push("| " + row.join(" | ") + " |");
  return lines.join("\n");
}

export function renderAudioAuditMarkdown(projectPath, scan, options, builderVersion) {
  const lines = [];
  lines.push("# audio-audit");
  lines.push("");
  lines.push("> 由 `builder audio-audit` 自动生成（音频阶段 A1），请勿手工编辑。");
  lines.push("");
  lines.push("- 工程路径: `" + projectPath + "`");
  lines.push("- 生成时间: " + new Date().toISOString().replace("T", " ").slice(0, 19) + " UTC");
  lines.push("- Builder 版本: " + builderVersion);
  lines.push("- 质量预设: " + options.preset + "（BGM q" + options.bgmQuality + " / BGS q" + options.bgsQuality + " / " + options.sampleRate + " Hz）");
  lines.push("- SE 阈值: " + formatBytes(options.seThresholdBytes) + "（convertSE=" + (options.convertSE ? "on" : "off") + "）");
  lines.push("- 分析静音: " + (options.analyzeSilence ? "on" : "off") + "，自动裁静音: " + (options.trimSilence ? "on" : "off") + "，响度归一化: " + (options.normalize ? "on" : "off"));
  lines.push("");
  lines.push("## 总览");
  lines.push("");
  lines.push("- 音频文件总数: **" + scan.totals.files + "**");
  lines.push("- 原始总体积: **" + formatBytes(scan.totals.bytes) + "**");
  lines.push("- 完全重复组: " + scan.duplicates.length);
  lines.push("- 问题资源: " + scan.problems.length);
  lines.push("");
  lines.push("## 按目录");
  lines.push("");
  const categoryRows = AUDIO_CATEGORIES.filter((category) => (scan.totals.byCategory[category] || {}).files > 0).map(
    (category) => {
      const totals = scan.totals.byCategory[category];
      return [category, String(totals.files), formatBytes(totals.bytes)];
    },
  );
  for (const [category, totals] of Object.entries(scan.totals.byCategory)) {
    if (AUDIO_CATEGORIES.includes(category) || totals.files === 0) continue;
    categoryRows.push([category + "（附加）", String(totals.files), formatBytes(totals.bytes)]);
  }
  lines.push(markdownTable(categoryRows, ["目录", "文件数", "体积"]));
  lines.push("");
  lines.push("## 编解码分布（按文件内容判定）");
  lines.push("");
  const codecRows = Object.entries(codecCounts(scan))
    .sort((a, b) => b[1] - a[1])
    .map(([codec, count]) => [codec, String(count)]);
  lines.push(markdownTable(codecRows, ["容器 / 编解码", "文件数"]));
  lines.push("");
  lines.push("## 最大 20 个音频资源");
  lines.push("");
  const largest = scan.files
    .slice()
    .sort((a, b) => b.size - a.size)
    .slice(0, 20)
    .map((file) => [
      "`" + file.relativePath + "`",
      formatBytes(file.size),
      file.codec || "-",
      file.sampleRate ? file.sampleRate + " Hz" : "-",
      file.channels ? file.channels + " ch" : "-",
      formatDuration(file.duration),
    ]);
  if (largest.length > 0) {
    lines.push(markdownTable(largest, ["文件", "体积", "编解码", "采样率", "声道", "时长"]));
  } else {
    lines.push("（无音频文件）");
  }
  lines.push("");
  lines.push("## 完全重复（SHA-256 相同，仅精确去重）");
  lines.push("");
  if (scan.duplicates.length > 0) {
    const duplicateRows = scan.duplicates
      .slice(0, 50)
      .map((group) => [group.files.map((file) => "`" + file + "`").join("<br>"), String(group.count), formatBytes(group.bytes * (group.count - 1))]);
    lines.push(markdownTable(duplicateRows, ["重复文件", "副本数", "可省体积"]));
    if (scan.duplicates.length > 50) {
      lines.push("");
      lines.push("（仅显示前 50 组，共 " + scan.duplicates.length + " 组；完整见 `build/reports/audio-audit.json`）");
    }
  } else {
    lines.push("（未发现完全重复）");
  }
  lines.push("");
  lines.push("## 异常资源");
  lines.push("");
  const tables = problemTables(scan.problems);
  const problemSections = [
    ["unreadable", "无法读取"],
    ["zero-length", "零长度文件"],
    ["invalid-header", "无效文件头"],
    ["unsupported-codec", "不支持的编解码"],
    ["extension-mismatch", "扩展名与内容不符"],
  ];
  let anyProblem = false;
  for (const [kind, title] of problemSections) {
    if (!tables[kind] || tables[kind].length === 0) continue;
    anyProblem = true;
    lines.push("### " + title + "（" + tables[kind].length + "）");
    lines.push("");
    lines.push(markdownTable(tables[kind].map((p) => ["`" + p.file + "`", p.reason]), ["文件", "原因"]));
    lines.push("");
  }
  if (!anyProblem) lines.push("（未发现异常资源）");
  lines.push("");
  lines.push("## 说明");
  lines.push("");
  lines.push("- 本报告只做只读分析；原始 `Audio/` 不被修改。");
  lines.push("- 压缩与去重由 `builder build-audio` 执行，结果输出到 `generated/audio/`。");
  lines.push("- 静音 / 循环 / Missing 引用分析在音频阶段 A5 落地后追加。");
  lines.push("");
  return lines.join("\n");
}

export function writeAudioAuditReports(ctx, projectPath, scan, options, builderVersion) {
  const jsonPath = path.join(ctx.paths.build, "reports", "audio-audit.json");
  const markdownPath = path.join(ctx.paths.docs || path.join(ctx.builderRoot, "docs"), "audio-audit.md");
  try {
    mkdirSync(path.dirname(jsonPath), { recursive: true });
    writeFileSync(jsonPath, JSON.stringify(buildAudioAuditJson(projectPath, scan, options, builderVersion), null, 2), "utf8");
    ctx.logger.output(jsonPath);
    mkdirSync(path.dirname(markdownPath), { recursive: true });
    writeFileSync(markdownPath, renderAudioAuditMarkdown(projectPath, scan, options, builderVersion), "utf8");
    ctx.logger.output(markdownPath);
  } catch (error) {
    ctx.logger.error("cannot write audio audit reports: " + error.message);
    return null;
  }
  return { jsonPath, markdownPath };
}
