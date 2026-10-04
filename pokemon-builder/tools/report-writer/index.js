// Audit report writer (Phase 6).
//
// Turns the results of the Phase 3 scanner, the Phase 4 event analyzer and
// the Phase 5 script analyzer into the deliverables of the audit:
//
//   docs/source-audit.md         project inventory
//   docs/script-event-audit.md   totals + Essentials APIs + complex Ruby detail
//   docs/api-usage.md            Pokemon Essentials API usage
//   docs/plugin-usage.md         third party plugin usage
//   docs/unsupported-scripts.md  everything that could NOT be interpreted
//   build/reports/script-audit.json   machine readable data for the translator
//
// Markdown is a human report only; the JSON file is the program input, so it
// carries map / event / page / type / method / rubySource for every block.

import { writeFileSync, mkdirSync } from "node:fs";
import path from "node:path";

const CONTROL_FLOW = /\b(?:if|unless|while|until|for|case|begin|do)\b/;

function escapeCell(value) {
  return String(value === null || value === undefined ? "" : value)
    .replace(/\|/g, "\\|")
    .replace(/\r?\n/g, " ");
}

function table(headers, rows) {
  if (rows.length === 0) return "_（无）_\n";
  const lines = [];
  lines.push("| " + headers.map(escapeCell).join(" | ") + " |");
  lines.push("| " + headers.map(() => "---").join(" | ") + " |");
  for (const row of rows) lines.push("| " + row.map(escapeCell).join(" | ") + " |");
  return lines.join("\n") + "\n";
}

function codeBlock(source) {
  return ["```ruby", String(source).replace(/\s+$/, ""), "```", ""].join("\n");
}

function heading(text) {
  return "\n## " + text + "\n\n";
}

function when(date) {
  return date.toISOString().replace("T", " ").slice(0, 19) + " UTC";
}

function kib(bytes) {
  if (bytes === null || bytes === undefined) return "?";
  if (bytes < 1024) return bytes + " B";
  return (bytes / 1024).toFixed(1) + " KiB";
}

// A block is "complex" when it needs a human to port it: control flow, a long
// script or several merged chunks. Complexity is independent of the API
// category, because `if x then pbSet(1,1) end` is both.
export function isComplexBlock(block) {
  return CONTROL_FLOW.test(block.rubySource) || block.lines >= 3 || block.continuations >= 2;
}

export function sourceAuditMarkdown({ project, scan, scripts, version }) {
  const parts = [];
  parts.push("# source-audit");
  parts.push("");
  parts.push("> 由 `builder.bat audit` 自动生成(阶段 6),请勿手工编辑。");
  parts.push("");
  parts.push("- 工程路径: `" + project + "`");
  parts.push("- 生成时间: " + when(new Date()));
  parts.push("- Builder 版本: " + version);
  parts.push("");
  parts.push(heading("RPG Maker 数据文件"));
  const rows = scan.data.files.map((file) => [
    file.name,
    file.kind,
    file.ok ? "OK" : "FAILED",
    kib(file.bytes),
    file.error || "",
  ]);
  if (rows.length > 60) {
    // The per file list stays readable; scan.json carries every entry anyway.
    parts.push(table(["文件", "类型", "解析", "大小", "错误"], rows.slice(0, 60)));
    parts.push("");
    parts.push("_另有 " + (rows.length - 60) + " 个文件解析成功,明细见 `build/reports/scan.json`。_");
  } else {
    parts.push(table(["文件", "类型", "解析", "大小", "错误"], rows));
  }
  parts.push("");
  parts.push(
    "合计: **" + scan.data.parsedCount + "/" + scan.data.rxdataCount + "** 个 `.rxdata` 解析成功,失败 " +
      scan.data.failedCount + " 个。",
  );
  parts.push(heading("按类型统计"));
  parts.push(
    table(
      ["类型", "文件数"],
      Object.entries(scan.data.kindCounts).map(([kind, count]) => [kind, count]),
    ),
  );
  parts.push(heading("编译后的 PBS 数据(Data 下非 .rxdata 文件)"));
  parts.push(table(["文件", "大小"], scan.data.otherFiles.map((file) => [file.name, kib(file.bytes)])));
  parts.push(heading("Scripts.rxdata"));
  parts.push(
    "- 段数: " + scan.scripts.totalSections + " 段,可解压 " + scan.scripts.readableSections + " 段,共 " +
      scan.scripts.totalLines + " 行 Ruby",
  );
  parts.push("- 解压后大小: " + kib(scan.scripts.inflatedBytes));
  parts.push(
    "- 核心(Essentials)段: " + scripts.sections.coreSections.length + " 段;插件 / 注入段: " +
      scripts.sections.pluginSections.length + " 段(插件区自第 " +
      (scripts.sections.coreCutoff === null ? "?" : scripts.sections.coreCutoff) + " 段开始)",
  );
  parts.push("- 被事件脚本引用的符号: " + scripts.summary.uniqueApis + " 个");
  if (scan.scripts.problems.length > 0) {
    parts.push("");
    parts.push("### 无法解压的脚本段(必须处理)");
    parts.push(
      table(["段号", "名称", "原因"], scan.scripts.problems.map((p) => [p.index, p.name, p.reason])),
    );
  } else {
    parts.push("");
    parts.push("无法解压的脚本段: 无。");
  }
  parts.push(heading("扫描警告与错误"));
  const problems = [];
  for (const warning of scan.warnings) problems.push(["warning", warning]);
  for (const error of scan.errors) problems.push(["error", error.file + ": " + error.error]);
  parts.push(table(["级别", "内容"], problems));
  parts.push(heading("结论"));
  parts.push(
    scan.ok ? "工程数据完整,可以被构建器继续处理。" : "工程数据存在问题(见上),必须先修复。",
  );
  parts.push("");
  return parts.join("\n");
}

export function scriptEventAuditMarkdown({ project, events, scripts, version }) {
  const parts = [];
  const complex = events.scriptBlocks.filter(isComplexBlock);
  parts.push("# script-event-audit");
  parts.push("");
  parts.push("> 由 `builder.bat audit` 自动生成(阶段 6),请勿手工编辑。");
  parts.push("");
  parts.push("- 工程路径: `" + project + "`");
  parts.push("- 生成时间: " + when(new Date()));
  parts.push("- Builder 版本: " + version);
  parts.push("");
  parts.push(heading("总量"));
  parts.push(
    table(
      ["指标", "数量"],
      [
        ["地图文件", events.summary.mapFiles],
        ["已分析地图", events.summary.mapsAnalyzed],
        ["含事件脚本的地图", events.summary.mapsWithScripts],
        ["事件", events.summary.events],
        ["事件页(page)", events.summary.pages],
        ["事件命令", events.summary.commands],
        ["CommonEvent", events.summary.commonEvents],
        ["Script block(355/655 合并后)", events.summary.scriptBlocks],
        ["其中来自 CommonEvent", events.summary.commonEventScriptBlocks],
        ["合并的续行命令", events.summary.continuationCommands],
        ["Ruby 行数", events.summary.scriptLines],
      ],
    ),
  );
  parts.push(heading("脚本分类"));
  parts.push(
    table(
      ["分类", "block 数", "占比"],
      Object.entries(scripts.summary.byCategory)
        .sort((a, b) => b[1] - a[1])
        .map(([category, count]) => [
          category,
          count,
          ((count / events.summary.scriptBlocks) * 100).toFixed(1) + "%",
        ]),
    ),
  );
  parts.push(heading("Essentials API 使用表"));
  const essentials = scripts.apis.filter((api) => api.category === "ESSENTIALS_API");
  parts.push(
    table(
      ["API", "Count", "Blocks", "Maps", "Unique patterns", "参数形态"],
      essentials.map((api) => [
        "`" + api.name + "`",
        api.occurrences,
        api.blocks,
        api.maps,
        api.uniquePatterns,
        api.patterns.map((pattern) => pattern.pattern + " x" + pattern.count).join("<br>"),
      ]),
    ),
  );
  parts.push(
    "共 **" + essentials.length + "** 个 Essentials API、**" +
      essentials.reduce((sum, api) => sum + api.occurrences, 0) + "** 次调用、**" +
      essentials.reduce((sum, api) => sum + api.uniquePatterns, 0) +
      "** 种参数形态。迁移工作量按 unique pattern 计,不按出现次数计。",
  );
  parts.push(heading("Complex Ruby 明细(需人工移植)"));
  parts.push("判定标准:含 if / unless / while / until / for / case / begin / do、≥3 行或 ≥2 次续行合并。");
  parts.push("");
  parts.push("共 **" + complex.length + "** 个 block。");
  parts.push("");
  let currentFile = null;
  for (const block of complex) {
    if (block.file !== currentFile) {
      currentFile = block.file;
      const map = events.maps.find((entry) => entry.file === currentFile);
      parts.push("\n### " + currentFile + (map && map.mapName ? " . " + map.mapName : "") + "\n");
    }
    const where =
      (block.source === "commonEvent" ? "CommonEvent " + block.eventId : "Map " + block.mapId) +
      " / Event " + block.eventId +
      (block.eventName ? "(" + block.eventName + ")" : "") +
      " / Page " + block.page +
      " / Cmd " + block.commandIndex;
    parts.push("- **" + where + "** - " + block.lines + " 行 / " + block.characters + " 字符");
    parts.push(codeBlock(block.rubySource));
  }
  parts.push("");
  return parts.join("\n");
}export function apiUsageMarkdown({ project, scripts, version }) {
  const parts = [];
  parts.push("# api-usage");
  parts.push("");
  parts.push("> 由 `builder.bat audit` 自动生成(阶段 6),请勿手工编辑。");
  parts.push("");
  parts.push("- 工程路径: `" + project + "`");
  parts.push("- 生成时间: " + when(new Date()));
  parts.push("- Builder 版本: " + version);
  parts.push("");
  const essentials = scripts.apis.filter((api) => api.category === "ESSENTIALS_API");
  const single = essentials.filter((api) => api.uniquePatterns === 1).length;
  parts.push(heading("总览"));
  parts.push(
    table(
      ["指标", "数值"],
      [
        ["Essentials API(被事件使用)", essentials.length],
        ["调用次数(occurrences)", essentials.reduce((sum, api) => sum + api.occurrences, 0)],
        ["参数形态总数(unique patterns)", essentials.reduce((sum, api) => sum + api.uniquePatterns, 0)],
        ["单一形态的 API", single],
        ["多形态的 API(需重点翻译)", essentials.length - single],
        ["Unresolved 标识符", scripts.summary.unresolvedIdentifiers],
      ],
    ),
  );
  parts.push(heading("全部 Essentials API"));
  parts.push(
    table(
      ["API", "Count", "Blocks", "Maps", "Unique patterns", "参数形态", "样例位置"],
      essentials.map((api) => [
        "`" + api.name + "`",
        api.occurrences,
        api.blocks,
        api.maps,
        api.uniquePatterns,
        api.patterns.map((pattern) => pattern.pattern + " x" + pattern.count).join("<br>"),
        api.sample ? api.sample.file + " event " + api.sample.eventId + " page " + api.sample.page : "",
      ]),
    ),
  );
  parts.push(heading("需重点翻译的 API(多种参数形态)"));
  const multi = essentials.filter((api) => api.uniquePatterns > 1);
  if (multi.length === 0) {
    parts.push("_每个 API 只有一种参数形态。_");
  } else {
    parts.push(
      table(
        ["API", "Unique patterns", "形态明细"],
        multi
          .sort((a, b) => b.uniquePatterns - a.uniquePatterns)
          .map((api) => [
            "`" + api.name + "`",
            api.uniquePatterns,
            api.patterns.map((pattern) => pattern.count + "x " + pattern.pattern).join("<br>"),
          ]),
      ),
    );
  }
  parts.push(heading("RPG Maker / Ruby 运行时引用"));
  const runtime = scripts.apis.filter((api) => api.category === "RMXP_GLOBAL" || api.category === "RUBY");
  if (runtime.length === 0) {
    parts.push("_事件脚本没有直接引用运行时类。_");
  } else {
    parts.push(
      table(
        ["引用", "分类", "Count", "Blocks"],
        runtime.map((api) => ["`" + api.name + "`", api.category, api.occurrences, api.blocks]),
      ),
    );
  }
  parts.push("");
  return parts.join("\n");
}

export function pluginUsageMarkdown({ project, scripts, version }) {
  const parts = [];
  parts.push("# plugin-usage");
  parts.push("");
  parts.push("> 由 `builder.bat audit` 自动生成(阶段 6),请勿手工编辑。");
  parts.push("");
  parts.push("- 工程路径: `" + project + "`");
  parts.push("- 生成时间: " + when(new Date()));
  parts.push("- Builder 版本: " + version);
  parts.push("");
  parts.push(
    "插件 / 注入区段判定:第 " +
      (scripts.sections.coreCutoff === null ? "?" : scripts.sections.coreCutoff) +
      " 段起出现插件标记(版本号 / `插件` 字样 / 中文分隔线),该段及其后全部视为第三方脚本;" +
      "核心区内的中文命名段同样算注入脚本,不算 Essentials 核心。",
  );
  parts.push(heading("插件段清单"));
  const used = new Set(scripts.apis.map((api) => api.name));
  parts.push(
    table(
      ["段号", "名称", "行数", "被事件调用的符号数"],
      scripts.sectionDetails
        .filter((section) => scripts.sections.pluginSections.includes(section.index))
        .map((section) => [
          section.index,
          section.name,
          section.lines,
          section.symbols.filter((symbol) => used.has(symbol)).length,
        ]),
    ),
  );
  parts.push(heading("插件 API 使用情况"));
  const pluginRefs = scripts.apis.filter((api) => api.category === "PLUGIN_API" && api.occurrences === 0);
  const plugins = scripts.apis.filter((api) => api.category === "PLUGIN_API" && api.occurrences > 0);
  if (plugins.length === 0) {
    parts.push("_事件脚本没有调用任何第三方插件 API。_");
  } else {
    parts.push(
      table(
        ["API", "Count", "Blocks", "Unique patterns", "参数形态", "定义段"],
        plugins.map((api) => [
          "`" + api.name + "`",
          api.occurrences,
          api.blocks,
          api.uniquePatterns,
          api.patterns.map((pattern) => pattern.pattern + " x" + pattern.count).join("<br>"),
          api.definedIn.map((index) => "#" + index).join(", "),
        ]),
      ),
    );
    parts.push(
      "共 **" + plugins.length + "** 个第三方 API 被事件脚本调用,合计 " +
        plugins.reduce((sum, api) => sum + api.occurrences, 0) + " 次。",
    );
  }
  parts.push(heading("仅被引用的常量(无调用参数)"));
  if (pluginRefs.length === 0) {
    parts.push("_无。_");
  } else {
    parts.push(
      table(
        ["常量 / 全局", "Blocks", "定义段"],
        pluginRefs.map((api) => [
          "`" + api.name + "`",
          api.blocks,
          api.definedIn.map((index) => "#" + index).join(", "),
        ]),
      ),
    );
    parts.push("这些名字只作为常量 / 全局被读取(没有调用参数),例如插件重新打开的 `Tone`。");
  }

  parts.push(heading("事件脚本定义的全局(工程状态)"));
  const eventGlobals = scripts.eventGlobals || [];
  if (eventGlobals.length === 0) {
    parts.push("_无。_");
  } else {
    parts.push(
      table(
        ["全局", "赋值次数", "首次出现"],
        eventGlobals.map((global) => [
          "`" + global.name + "`",
          global.assignments,
          global.sample ? global.sample.file + " event " + global.sample.eventId : "",
        ]),
      ),
    );
    parts.push("这些全局不在 `Scripts.rxdata`,而是在事件脚本里赋值;Java 运行时必须自带同名状态。");
  }
  parts.push("");
  return parts.join("\n");
}

export function unsupportedScriptsMarkdown({ project, scan, events, scripts, version }) {
  const parts = [];
  parts.push("# unsupported-scripts");
  parts.push("");
  parts.push("> 由 `builder.bat audit` 自动生成(阶段 6),请勿手工编辑。");
  parts.push("");
  parts.push("- 工程路径: `" + project + "`");
  parts.push("- 生成时间: " + when(new Date()));
  parts.push("- Builder 版本: " + version);
  parts.push("");
  parts.push("本表列出所有**无法被自动识别**的内容;硬性规则是不允许静默忽略,每一条都必须带位置。");
  parts.push(heading("未解析标识符"));
  if (scripts.unresolvedIdentifiers.length === 0) {
    parts.push("无。所有事件脚本标识符都能在 `Scripts.rxdata`、Ruby / RMXP 运行时或事件全局中找到。");
  } else {
    parts.push(
      table(
        ["标识符", "出现次数", "首次出现位置"],
        scripts.unresolvedIdentifiers.map((identifier) => [
          "`" + identifier.name + "`",
          identifier.occurrences,
          identifier.sample
            ? identifier.sample.file + " event " + identifier.sample.eventId + " page " + identifier.sample.page
            : "",
        ]),
      ),
    );
  }
  parts.push(heading("无法解压的 Scripts.rxdata 段"));
  if (scan.scripts.problems.length === 0) {
    parts.push("无。");
  } else {
    parts.push(
      table(
        ["段号", "名称", "原因"],
        scan.scripts.problems.map((problem) => [problem.index, problem.name, problem.reason]),
      ),
    );
  }
  parts.push(heading("无法解析的事件数据"));
  const mapFailures = events.maps.filter((map) => !map.ok);
  if (mapFailures.length === 0 && events.problems.length === 0) {
    parts.push("无。`MapXXX.rxdata` / `CommonEvents.rxdata` 全部可解析,355 / 655 全部可合并。");
  } else {
    parts.push(
      table(
        ["文件", "问题"],
        [
          ...mapFailures.map((map) => [map.file, map.error]),
          ...events.problems.map((problem) => [problem.file, problem.location + ": " + problem.reason]),
        ],
      ),
    );
  }
  parts.push(heading("无法读取的 .rxdata"));
  if (scan.data.failedCount === 0) {
    parts.push("无。");
  } else {
    parts.push(
      table(["文件", "错误"], scan.data.files.filter((file) => !file.ok).map((file) => [file.name, file.error])),
    );
  }
  parts.push(heading("解释器实例状态(移植时需实现的字段)"));
  const stateBlocks = events.scriptBlocks.filter((block) => block.rubySource.includes("@"));
  if (stateBlocks.length === 0) {
    parts.push("无。");
  } else {
    const names = new Map();
    for (const block of stateBlocks) {
      for (const match of block.rubySource.matchAll(/@([A-Za-z_]\w*)/g)) {
        names.set(match[1], (names.get(match[1]) || 0) + 1);
      }
    }
    parts.push(
      table(
        ["实例变量", "出现 block 数"],
        Array.from(names.entries())
          .sort((a, b) => b[1] - a[1])
          .map(([name, count]) => ["`@" + name + "`", count]),
      ),
    );
    parts.push("这些是事件解释器(Game_Interpreter)的实例变量,不是 API 调用;Java 运行时需要对应字段。");
  }
  parts.push("");
  return parts.join("\n");
}
export function buildScriptAuditJson({ project, version, scan, events, scripts }) {
  const classification = new Map(scripts.classification.map((record) => [record.id, record]));
  const records = events.scriptBlocks.map((block) => {
    const verdict = classification.get(block.id) || {};
    const calls = verdict.calls || [];
    const method = calls.length > 0 ? calls[0].name : null;
    return {
      id: block.id,
      source: block.source,
      file: block.file,
      map: block.mapId,
      mapName: block.mapName,
      event: block.eventId,
      eventName: block.eventName,
      page: block.page,
      commandIndex: block.commandIndex,
      indent: block.indent,
      trigger: block.trigger,
      switchId: block.switchId,
      type: verdict.category === undefined ? null : verdict.category,
      method,
      argumentsShape: calls.length > 0 ? calls[0].shape : null,
      continuations: block.continuations,
      lines: block.lines,
      characters: block.characters,
      complex: isComplexBlock(block),
      interpreterState: verdict.interpreterState || null,
      eventGlobals: verdict.eventGlobals || null,
      unresolved: verdict.unresolved && verdict.unresolved.length > 0 ? verdict.unresolved : null,
      rubySource: block.rubySource,
    };
  });
  return {
    generatedAt: new Date().toISOString(),
    builderVersion: version,
    project,
    totals: {
      mapFiles: events.summary.mapFiles,
      mapsAnalyzed: events.summary.mapsAnalyzed,
      events: events.summary.events,
      pages: events.summary.pages,
      commands: events.summary.commands,
      commonEvents: events.summary.commonEvents,
      scriptBlocks: events.summary.scriptBlocks,
      continuationCommands: events.summary.continuationCommands,
      scriptLines: events.summary.scriptLines,
      byCategory: scripts.summary.byCategory,
      uniqueApis: scripts.summary.uniqueApis,
      uniquePatterns: scripts.summary.uniquePatterns,
      unresolvedIdentifiers: scripts.summary.unresolvedIdentifiers,
      eventGlobals: scripts.summary.eventGlobals,
      rxdataParsed: scan.data.parsedCount,
      rxdataTotal: scan.data.rxdataCount,
      scriptSections: scan.scripts.totalSections,
      scriptLinesInScripts: scan.scripts.totalLines,
      coreSections: scripts.sections.coreSections.length,
      pluginSections: scripts.sections.pluginSections.length,
    },
    sections: {
      coreCutoff: scripts.sections.coreCutoff,
      core: scripts.sections.coreSections,
      plugins: scripts.sections.pluginSections,
    },
    maps: events.maps.map((map) => ({
      mapId: map.mapId,
      mapName: map.mapName,
      file: map.file,
      events: map.events,
      pages: map.pages,
      scriptBlocks: map.scriptBlocks,
      ok: map.ok,
      error: map.error,
    })),
    apis: scripts.apis.map((api) => ({
      name: api.name,
      kind: api.kind,
      category: api.category,
      occurrences: api.occurrences,
      blocks: api.blocks,
      maps: api.maps,
      uniquePatterns: api.uniquePatterns,
      patterns: api.patterns,
      sample: api.sample,
    })),
    plugins: scripts.apis.filter((api) => api.category === "PLUGIN_API").map((api) => api.name),
    unresolvedIdentifiers: scripts.unresolvedIdentifiers,
    eventGlobals: scripts.eventGlobals,
    problems: {
      scan: scan.errors,
      scriptSections: scan.scripts.problems,
      events: events.errors,
      eventCommands: events.problems,
    },
    scripts: records,
  };
}

// Writes every Phase 6 deliverable and returns what was written.
export function writeAuditReports({ project, version, scan, events, scripts, docsDir, reportsDir }) {
  const written = [];
  const write = (file, content) => {
    mkdirSync(path.dirname(file), { recursive: true });
    writeFileSync(file, content, "utf8");
    written.push(file);
  };

  write(path.join(docsDir, "source-audit.md"), sourceAuditMarkdown({ project, scan, scripts, version }));
  write(path.join(docsDir, "script-event-audit.md"), scriptEventAuditMarkdown({ project, events, scripts, version }));
  write(path.join(docsDir, "api-usage.md"), apiUsageMarkdown({ project, scripts, version }));
  write(path.join(docsDir, "plugin-usage.md"), pluginUsageMarkdown({ project, scripts, version }));
  write(path.join(docsDir, "unsupported-scripts.md"), unsupportedScriptsMarkdown({ project, scan, events, scripts, version }));

  const jsonTarget = path.join(reportsDir, "script-audit.json");
  mkdirSync(path.dirname(jsonTarget), { recursive: true });
  writeFileSync(
    jsonTarget,
    JSON.stringify(buildScriptAuditJson({ project, version, scan, events, scripts }), null, 2),
    "utf8",
  );
  written.push(jsonTarget);

  return { written, json: jsonTarget };
}
