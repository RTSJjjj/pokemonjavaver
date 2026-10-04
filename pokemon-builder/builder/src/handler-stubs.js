// Handler stub generator (project3 section 26).
//
// Every JAVA_HANDLER_REQUIRED script block gets a small Java class whose name
// carries the block id and whose javadoc embeds the original Ruby, so the Java
// author has a compileable starting point instead of a runtime surprise.

/** "map70/event4/page1/cmd2" -> "Script_map70_event4_page1_cmd2_Handler". */
export function stubClassName(id) {
  const safe = String(id || "unknown").replace(/[^A-Za-z0-9]+/g, "_").replace(/^_+|_+$/g, "");
  const name = safe.length === 0 ? "unknown" : safe;
  return `Script_${name}_Handler`;
}

/** Ruby inside a javadoc comment: close the comment safely. */
export function commentSafe(ruby) {
  return String(ruby || "").replace(/\*\//g, "*\\/");
}

/** Java source of one stub. */
export function stubSource(block) {
  const className = stubClassName(block.id);
  const source = block.source || {};
  const ruby = commentSafe(block.rubySource || block.reason || "");
  return `package pokemon.runtime.script.generated;

/**
 * Generated handler stub (project3 section 26) for ${block.id}.
 *
 * <p>Source: map=${source.mapId} event=${source.eventId} page=${source.page}
 * command=${source.commandIndex} (${block.category}). API: ${(block.apis || []).join(", ")}.</p>
 *
 * <p>Original Ruby:</p>
 * <pre>
 * ${ruby.trim()}
 * </pre>
 */
public final class ${className} {

    private ${className}() {
    }
}
`;
}

/** One stub per problem block, at most {@code limit} (0 = all). */
export function generateStubs(problems, { limit = 20 } = {}) {
  const blocks = (problems && problems.javaHandlerRequired) || [];
  const selected = limit > 0 ? blocks.slice(0, limit) : blocks;
  return selected.map((block) => ({
    id: block.id,
    fileName: `${stubClassName(block.id)}.java`,
    source: stubSource(block),
  }));
}
