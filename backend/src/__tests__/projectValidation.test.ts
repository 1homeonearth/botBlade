import test from "node:test";
import assert from "node:assert/strict";
import type { BotProject } from "../models/project.js";
import { validateProject } from "../services/projectValidation.js";

function draft(overrides: Partial<BotProject> = {}): BotProject {
  const now = new Date().toISOString();
  return {
    id: "project_test",
    name: "Squire Junior",
    slug: "squire-junior",
    description: "A modular Discord utility bot.",
    templateId: "template_blank_discord_ts",
    language: "typescript",
    runtime: "node22",
    discord: {
      applicationId: null,
      clientId: null,
      defaultGuildId: null,
      tokenSecretRef: null,
      commandRegistration: "guild",
    },
    permissions: { intents: [], botPermissions: [] },
    commands: [],
    events: [],
    deployment: { targetId: null, lastDeploymentId: null },
    archivedAt: null,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

test("draft projects are valid with warnings", () => {
  const result = validateProject(draft());
  assert.equal(result.valid, true);
  assert.equal(result.errors.length, 0);
  assert.ok(result.warnings.some((warning) => warning.code === "MISSING_DISCORD_TOKEN"));
  assert.ok(result.warnings.some((warning) => warning.code === "MISSING_DISCORD_APPLICATION_ID"));
  assert.ok(result.warnings.some((warning) => warning.code === "MISSING_DISCORD_GUILD_ID"));
  assert.ok(result.warnings.some((warning) => warning.code === "NO_COMMANDS_DEFINED"));
});

test("global command registration warns for missing application ID but not guild ID", () => {
  const result = validateProject(draft({ discord: { ...draft().discord, commandRegistration: "global" } }));
  assert.equal(result.valid, true);
  assert.ok(result.warnings.some((warning) => warning.code === "MISSING_DISCORD_APPLICATION_ID"));
  assert.equal(result.warnings.some((warning) => warning.code === "MISSING_DISCORD_GUILD_ID"), false);
  assert.ok(result.warnings.some((warning) => warning.code === "GLOBAL_COMMAND_PROPAGATION"));
});

test("invalid command names return errors", () => {
  const result = validateProject(draft({
    commands: [{ name: "Bad Name", description: "bad", handler: "handleBad" }],
  }));
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((error) => error.code === "INVALID_COMMAND_NAME"));
});

test("duplicate command names return errors", () => {
  const result = validateProject(draft({
    commands: [
      { name: "ping", description: "Ping", handler: "handlePing" },
      { name: "ping", description: "Ping again", handler: "handlePingAgain" },
    ],
  }));
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((error) => error.code === "DUPLICATE_COMMAND_NAME"));
});
