import test from "node:test";
import assert from "node:assert/strict";
import { redactSecrets, registerSecretValue, unregisterSecretValue } from "../services/redaction.js";

test("redactSecrets removes registered secret values", () => {
  const secret = "registered-super-secret-value";
  registerSecretValue(secret);
  assert.equal(redactSecrets(`token=${secret}`), "token=[REDACTED_SECRET]");
  unregisterSecretValue(secret);
});

test("redactSecrets removes common token patterns", () => {
  const discordToken = "ABCDEFGHIJKLMNOPQRSTUVW.XYZabc.superSecretDiscordTokenPart";
  assert.equal(redactSecrets(`token ${discordToken}`).includes(discordToken), false);
});

import { redactMetadata } from "../services/auditService.js";

test("audit metadata redaction removes stored secrets", () => {
  const secret = "audit-metadata-super-secret";
  registerSecretValue(secret);
  const metadata = redactMetadata({ safe: "ok", nested: { token: secret } });
  assert.equal(JSON.stringify(metadata).includes(secret), false);
  unregisterSecretValue(secret);
});


test("redactSecrets handles long non-token strings quickly", () => {
  const input = "x".repeat(1024 * 1024);
  assert.equal(redactSecrets(input), input);
});
