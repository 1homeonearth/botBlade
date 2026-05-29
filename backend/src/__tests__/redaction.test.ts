import test from "node:test";
import assert from "node:assert/strict";
import { AuditService, redactMetadata } from "../services/auditService.js";
import { redactSecrets, registerSecretValue, unregisterSecretValue } from "../services/redaction.js";
import { ScriptProfileService } from "../services/scriptProfiles/scriptProfileService.js";

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

test("audit metadata redaction removes stored secrets", () => {
  const secret = "audit-metadata-super-secret";
  registerSecretValue(secret);
  const metadata = redactMetadata({ safe: "ok", nested: { token: secret } });
  assert.equal(JSON.stringify(metadata).includes(secret), false);
  unregisterSecretValue(secret);
});

test("script profile audit metadata stores command arrays as redacted-free metadata only", () => {
  const audit = new AuditService();
  const profiles = new ScriptProfileService(undefined, audit);
  const command = ["npm", "run", "build"];

  profiles.create("project_redaction", { name: "Build", runtime: "node", command }, { actorId: "tester", requestId: "req_redaction" });

  const event = audit.list("project_redaction").find((candidate) => candidate.action === "script.profile.create");
  assert.ok(event);
  assert.equal(JSON.stringify(event?.metadata).includes("npm"), false);
  assert.deepStrictEqual(event?.metadata, { source: "user", runtime: "node" });
});
