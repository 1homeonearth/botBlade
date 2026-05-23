// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import test from "node:test";  // line 7: executes this statement as part of this file's behavior
import assert from "node:assert/strict";  // line 8: executes this statement as part of this file's behavior
import * as path from "node:path";  // line 9: executes this statement as part of this file's behavior
import { scanWorkspaceForBladePacks } from "../services/importScan/detector.js";  // line 10: executes this statement as part of this file's behavior

const fixturesRoot = path.join(process.cwd(), "src", "__tests__", "fixtures", "import-scan");  // line 12: executes this statement as part of this file's behavior

test("detects discord-js from package dependency", async () => {  // line 14: executes this statement as part of this file's behavior
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "discord"));  // line 15: executes this statement as part of this file's behavior
  assert.equal(result.recommendedPackId, "discord-js");  // line 16: executes this statement as part of this file's behavior
});  // line 17: executes this statement as part of this file's behavior

test("detects telegraf", async () => {  // line 19: executes this statement as part of this file's behavior
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "telegraf"));  // line 20: executes this statement as part of this file's behavior
  assert.equal(result.recommendedPackId, "telegraf");  // line 21: executes this statement as part of this file's behavior
});  // line 22: executes this statement as part of this file's behavior

test("detects slack-bolt", async () => {  // line 24: executes this statement as part of this file's behavior
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "slack"));  // line 25: executes this statement as part of this file's behavior
  assert.equal(result.recommendedPackId, "slack-bolt");  // line 26: executes this statement as part of this file's behavior
});  // line 27: executes this statement as part of this file's behavior

test("detects generic-python", async () => {  // line 29: executes this statement as part of this file's behavior
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "python"));  // line 30: executes this statement as part of this file's behavior
  assert.equal(result.recommendedPackId, "generic-python");  // line 31: executes this statement as part of this file's behavior
});  // line 32: executes this statement as part of this file's behavior

test("detects n8n workflow", async () => {  // line 34: executes this statement as part of this file's behavior
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "n8n"));  // line 35: executes this statement as part of this file's behavior
  assert.equal(result.recommendedPackId, "n8n-workflow");  // line 36: executes this statement as part of this file's behavior
});  // line 37: executes this statement as part of this file's behavior

test("unknown folder returns unknown", async () => {  // line 39: executes this statement as part of this file's behavior
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "unknown"));  // line 40: executes this statement as part of this file's behavior
  assert.equal(result.recommendedPackId, "unknown");  // line 41: executes this statement as part of this file's behavior
});  // line 42: executes this statement as part of this file's behavior

test("dependency prefix does not trigger exact package detector", async () => {  // line 44: executes this statement as part of this file's behavior
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "prefix"));  // line 45: executes this statement as part of this file's behavior
  assert.equal(result.recommendedPackId, "unknown");  // line 46: executes this statement as part of this file's behavior
});  // line 47: executes this statement as part of this file's behavior

test("missing workspace path returns unknown instead of throwing", async () => {  // line 49: executes this statement as part of this file's behavior
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "does-not-exist"));  // line 50: executes this statement as part of this file's behavior
  assert.equal(result.recommendedPackId, "unknown");  // line 51: executes this statement as part of this file's behavior
  assert.equal(result.matches.length, 0);  // line 52: executes this statement as part of this file's behavior
});  // line 53: executes this statement as part of this file's behavior
