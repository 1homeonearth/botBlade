import test from "node:test";
import assert from "node:assert/strict";
import * as path from "node:path";
import fs from "node:fs/promises";
import os from "node:os";
import { scanWorkspaceForBladePacks } from "../services/importScan/detector.js";
import { writeBotbladeMetadata } from "../services/importScan/botbladeMetadata.js";

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

test("missing workspace path returns unknown instead of throwing", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "does-not-exist"));
  assert.equal(result.recommendedPackId, "unknown");
  assert.equal(result.matches.length, 0);
});

test("botblade metadata persists only secret metadata without raw values", async () => {
  const workspace = path.join(os.tmpdir(), `botblade-import-scan-${Date.now()}-${Math.random().toString(16).slice(2)}`);
  const sentinelTokenValue = "DO_NOT_PERSIST_TOKEN_VALUE";
  const sentinelClientIdValue = "DO_NOT_PERSIST_CLIENT_ID_VALUE";

  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(path.join(workspace, "package.json"), JSON.stringify({ name: "discord-bot", dependencies: { "discord.js": "^14.0.0" } }), "utf8");
    await fs.writeFile(path.join(workspace, ".env.example"), `DISCORD_TOKEN=${sentinelTokenValue}\nCLIENT_ID=${sentinelClientIdValue}`, "utf8");

    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection, { kind: "git", url: "https://github.com/example/repo" });
    const metadataText = await fs.readFile(metadataPath, "utf8");
    const metadata = JSON.parse(metadataText) as { secrets: Array<{ name: string; configured: boolean }> };

    assert.ok(metadata.secrets.length > 0);
    assert.equal(metadata.secrets.every((secret) => secret.configured === false), true);
    assert.equal(metadataText.includes(sentinelTokenValue), false);
    assert.equal(metadataText.includes(sentinelClientIdValue), false);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});
