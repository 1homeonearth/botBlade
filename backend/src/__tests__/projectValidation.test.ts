// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import test from "node:test";  // line 7: executes this statement as part of this file's behavior
import assert from "node:assert/strict";  // line 8: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 9: executes this statement as part of this file's behavior
import { validateProject } from "../services/projectValidation.js";  // line 10: executes this statement as part of this file's behavior

function draft(overrides: Partial<BotProject> = {}): BotProject {  // line 12: executes this statement as part of this file's behavior
  const now = new Date().toISOString();  // line 13: executes this statement as part of this file's behavior
  return {  // line 14: executes this statement as part of this file's behavior
    id: "project_test",  // line 15: executes this statement as part of this file's behavior
    name: "Squire Junior",  // line 16: executes this statement as part of this file's behavior
    slug: "squire-junior",  // line 17: executes this statement as part of this file's behavior
    description: "A modular Discord utility bot.",  // line 18: executes this statement as part of this file's behavior
    templateId: "template_blank_discord_ts",  // line 19: executes this statement as part of this file's behavior
    language: "typescript",  // line 20: executes this statement as part of this file's behavior
    runtime: "node22",  // line 21: executes this statement as part of this file's behavior
    discord: {  // line 22: executes this statement as part of this file's behavior
      applicationId: null,  // line 23: executes this statement as part of this file's behavior
      clientId: null,  // line 24: executes this statement as part of this file's behavior
      defaultGuildId: null,  // line 25: executes this statement as part of this file's behavior
      tokenSecretRef: null,  // line 26: executes this statement as part of this file's behavior
      commandRegistration: "guild",  // line 27: executes this statement as part of this file's behavior
    },  // line 28: executes this statement as part of this file's behavior
    permissions: { intents: [], botPermissions: [] },  // line 29: executes this statement as part of this file's behavior
    commands: [],  // line 30: executes this statement as part of this file's behavior
    events: [],  // line 31: executes this statement as part of this file's behavior
    deployment: { targetId: null, lastDeploymentId: null },  // line 32: executes this statement as part of this file's behavior
    archivedAt: null,  // line 33: executes this statement as part of this file's behavior
    createdAt: now,  // line 34: executes this statement as part of this file's behavior
    updatedAt: now,  // line 35: executes this statement as part of this file's behavior
    ...overrides,  // line 36: executes this statement as part of this file's behavior
  };  // line 37: executes this statement as part of this file's behavior
}  // line 38: executes this statement as part of this file's behavior

test("draft projects are valid with warnings", () => {  // line 40: executes this statement as part of this file's behavior
  const result = validateProject(draft());  // line 41: executes this statement as part of this file's behavior
  assert.equal(result.valid, true);  // line 42: executes this statement as part of this file's behavior
  assert.equal(result.errors.length, 0);  // line 43: executes this statement as part of this file's behavior
  assert.ok(result.warnings.some((warning) => warning.code === "MISSING_DISCORD_TOKEN"));  // line 44: executes this statement as part of this file's behavior
  assert.ok(result.warnings.some((warning) => warning.code === "NO_COMMANDS_DEFINED"));  // line 45: executes this statement as part of this file's behavior
});  // line 46: executes this statement as part of this file's behavior

test("invalid command names return errors", () => {  // line 48: executes this statement as part of this file's behavior
  const result = validateProject(draft({  // line 49: executes this statement as part of this file's behavior
    commands: [{ name: "Bad Name", description: "bad", handler: "handleBad" }],  // line 50: executes this statement as part of this file's behavior
  }));  // line 51: executes this statement as part of this file's behavior
  assert.equal(result.valid, false);  // line 52: executes this statement as part of this file's behavior
  assert.ok(result.errors.some((error) => error.code === "INVALID_COMMAND_NAME"));  // line 53: executes this statement as part of this file's behavior
});  // line 54: executes this statement as part of this file's behavior

test("duplicate command names return errors", () => {  // line 56: executes this statement as part of this file's behavior
  const result = validateProject(draft({  // line 57: executes this statement as part of this file's behavior
    commands: [  // line 58: executes this statement as part of this file's behavior
      { name: "ping", description: "Ping", handler: "handlePing" },  // line 59: executes this statement as part of this file's behavior
      { name: "ping", description: "Ping again", handler: "handlePingAgain" },  // line 60: executes this statement as part of this file's behavior
    ],  // line 61: executes this statement as part of this file's behavior
  }));  // line 62: executes this statement as part of this file's behavior
  assert.equal(result.valid, false);  // line 63: executes this statement as part of this file's behavior
  assert.ok(result.errors.some((error) => error.code === "DUPLICATE_COMMAND_NAME"));  // line 64: executes this statement as part of this file's behavior
});  // line 65: executes this statement as part of this file's behavior
