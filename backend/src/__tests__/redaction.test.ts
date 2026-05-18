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
