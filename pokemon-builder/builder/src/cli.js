#!/usr/bin/env node
// Builder CLI entry point. builder.bat calls this file; it can also be run
// directly:  node builder/src/cli.js audit "D:\\Game\\PokemonProject"

import path from "node:path";
import { fileURLToPath } from "node:url";
import { runCli } from "./commands.js";

const builderRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const exitCode = runCli(builderRoot, process.argv.slice(2));
process.exit(exitCode);