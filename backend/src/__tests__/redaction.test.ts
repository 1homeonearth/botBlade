// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import test from "node:test";  // line 7: executes this statement as part of this file's behavior
import assert from "node:assert/strict";  // line 8: executes this statement as part of this file's behavior
import { redactSecrets, registerSecretValue, unregisterSecretValue } from "../services/redaction.js";  // line 9: executes this statement as part of this file's behavior

test("redactSecrets removes registered secret values", () => {  // line 11: executes this statement as part of this file's behavior
  const secret = "registered-super-secret-value";  // line 12: executes this statement as part of this file's behavior
  registerSecretValue(secret);  // line 13: executes this statement as part of this file's behavior
  assert.equal(redactSecrets(`token=${secret}`), "token=[REDACTED_SECRET]");  // line 14: executes this statement as part of this file's behavior
  unregisterSecretValue(secret);  // line 15: executes this statement as part of this file's behavior
});  // line 16: executes this statement as part of this file's behavior

test("redactSecrets removes common token patterns", () => {  // line 18: executes this statement as part of this file's behavior
  const discordToken = "ABCDEFGHIJKLMNOPQRSTUVW.XYZabc.superSecretDiscordTokenPart";  // line 19: executes this statement as part of this file's behavior
  assert.equal(redactSecrets(`token ${discordToken}`).includes(discordToken), false);  // line 20: executes this statement as part of this file's behavior
});  // line 21: executes this statement as part of this file's behavior

import { redactMetadata } from "../services/auditService.js";  // line 23: executes this statement as part of this file's behavior

test("audit metadata redaction removes stored secrets", () => {  // line 25: executes this statement as part of this file's behavior
  const secret = "audit-metadata-super-secret";  // line 26: executes this statement as part of this file's behavior
  registerSecretValue(secret);  // line 27: executes this statement as part of this file's behavior
  const metadata = redactMetadata({ safe: "ok", nested: { token: secret } });  // line 28: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(metadata).includes(secret), false);  // line 29: executes this statement as part of this file's behavior
  unregisterSecretValue(secret);  // line 30: executes this statement as part of this file's behavior
});  // line 31: executes this statement as part of this file's behavior
