import test from "node:test";
import assert from "node:assert/strict";
import * as path from "node:path";
import { scanWorkspaceForBladePacks } from "../services/importScan/detector.js";

const fixturesRoot = path.join(process.cwd(), "src", "__tests__", "fixtures", "import-scan");

test("detects discord-js from package dependency", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "discord"));
  assert.equal(result.recommendedPackId, "discord-js");
});

test("detects telegraf", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "telegraf"));
  assert.equal(result.recommendedPackId, "telegraf");
});

test("detects slack-bolt", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "slack"));
  assert.equal(result.recommendedPackId, "slack-bolt");
});

test("detects generic-python", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "python"));
  assert.equal(result.recommendedPackId, "generic-python");
});

test("detects n8n workflow", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "n8n"));
  assert.equal(result.recommendedPackId, "n8n-workflow");
});

test("unknown folder returns unknown", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "unknown"));
  assert.equal(result.recommendedPackId, "unknown");
});

test("dependency prefix does not trigger exact package detector", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "prefix"));
  assert.equal(result.recommendedPackId, "unknown");
});
