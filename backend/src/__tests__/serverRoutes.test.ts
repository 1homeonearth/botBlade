// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import test from "node:test";  // line 7: executes this statement as part of this file's behavior
import assert from "node:assert/strict";  // line 8: executes this statement as part of this file's behavior
import { randomUUID } from "node:crypto";  // line 9: executes this statement as part of this file's behavior

process.env.NODE_ENV = "test";  // line 11: executes this statement as part of this file's behavior
process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([  // line 12: executes this statement as part of this file's behavior
  { token: "admin-token", actorId: "admin_user", roles: ["admin"], projectIds: ["*"] },  // line 13: executes this statement as part of this file's behavior
  { token: "scoped-token", actorId: "scoped_user", roles: ["member"], projectIds: ["project_allowed"] },  // line 14: executes this statement as part of this file's behavior
]);  // line 15: executes this statement as part of this file's behavior
process.env.BOTBLADE_SESSION_TOKENS = JSON.stringify([{ token: "session-token", actorId: "session_user", roles: ["admin"], projectIds: ["*"] }]);  // line 16: executes this statement as part of this file's behavior
const { createRequestListener } = await import("../server.js");  // line 17: executes this statement as part of this file's behavior

type Method = "GET" | "POST" | "PATCH" | "DELETE" | "PUT";  // line 19: executes this statement as part of this file's behavior

async function request(method: Method, url: string, body?: unknown, options: { token?: string; sessionToken?: string; unauthenticated?: boolean } = {}) {  // line 21: executes this statement as part of this file's behavior
  const chunks = body === undefined ? [] : [Buffer.from(JSON.stringify(body))];  // line 22: executes this statement as part of this file's behavior
  const headers: Record<string, string> = { host: "localhost", "x-request-id": `req_test_${randomUUID()}` };  // line 23: executes this statement as part of this file's behavior
  if (!options.unauthenticated) {  // line 24: executes this statement as part of this file's behavior
    if (options.sessionToken) headers.cookie = `botBladeSession=${encodeURIComponent(options.sessionToken)}`;  // line 25: executes this statement as part of this file's behavior
    else headers.authorization = `Bearer ${options.token ?? "admin-token"}`;  // line 26: executes this statement as part of this file's behavior
  }  // line 27: executes this statement as part of this file's behavior
  const req = {  // line 28: executes this statement as part of this file's behavior
    method,  // line 29: executes this statement as part of this file's behavior
    url,  // line 30: executes this statement as part of this file's behavior
    headers,  // line 31: executes this statement as part of this file's behavior
    async *[Symbol.asyncIterator]() { for (const chunk of chunks) yield chunk; },  // line 32: executes this statement as part of this file's behavior
  };  // line 33: executes this statement as part of this file's behavior
  const res = {  // line 34: executes this statement as part of this file's behavior
    statusCode: 200,  // line 35: executes this statement as part of this file's behavior
    headers: {} as Record<string, string>,  // line 36: executes this statement as part of this file's behavior
    payload: "",  // line 37: executes this statement as part of this file's behavior
    setHeader(name: string, value: string) { this.headers[name.toLowerCase()] = value; },  // line 38: executes this statement as part of this file's behavior
    end(chunk?: string) { this.payload = chunk ?? ""; },  // line 39: executes this statement as part of this file's behavior
  };  // line 40: executes this statement as part of this file's behavior
  await createRequestListener()(req as never, res as never);  // line 41: executes this statement as part of this file's behavior
  return { statusCode: res.statusCode, body: res.payload ? JSON.parse(res.payload) : null, headers: res.headers };  // line 42: executes this statement as part of this file's behavior
}  // line 43: executes this statement as part of this file's behavior

test("health route returns ok", async () => {  // line 45: executes this statement as part of this file's behavior
  const response = await request("GET", "/api/health");  // line 46: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 200);  // line 47: executes this statement as part of this file's behavior
  assert.equal(response.body.ok, true);  // line 48: executes this statement as part of this file's behavior
});  // line 49: executes this statement as part of this file's behavior

test("startup diagnostics route returns unconfigured when no path is set", async () => {  // line 51: executes this statement as part of this file's behavior
  delete process.env.BOTBLADE_STARTUP_CRASH_ARTIFACT;  // line 52: executes this statement as part of this file's behavior
  const response = await request("GET", "/api/diagnostics/startup-crash");  // line 53: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 200);  // line 54: executes this statement as part of this file's behavior
  assert.equal(response.body.artifact, null);  // line 55: executes this statement as part of this file's behavior
});  // line 56: executes this statement as part of this file's behavior



test("protected routes reject unauthenticated requests", async () => {  // line 60: executes this statement as part of this file's behavior
  const response = await request("GET", "/api/projects", undefined, { unauthenticated: true });  // line 61: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 401);  // line 62: executes this statement as part of this file's behavior
  assert.equal(response.body.error.code, "AUTHENTICATION_REQUIRED");  // line 63: executes this statement as part of this file's behavior
});  // line 64: executes this statement as part of this file's behavior

test("project routes reject actors without project authorization", async () => {  // line 66: executes this statement as part of this file's behavior
  const created = await request("POST", "/api/projects", { name: "Authorization Denied" });  // line 67: executes this statement as part of this file's behavior
  const response = await request("GET", `/api/projects/${created.body.id}`, undefined, { token: "scoped-token" });  // line 68: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 403);  // line 69: executes this statement as part of this file's behavior
  assert.equal(response.body.error.code, "PROJECT_ACCESS_DENIED");  // line 70: executes this statement as part of this file's behavior
});  // line 71: executes this statement as part of this file's behavior

test("project routes allow authorized bearer and session actors", async () => {  // line 73: executes this statement as part of this file's behavior
  const created = await request("POST", "/api/projects", { name: "Authorization Allowed" });  // line 74: executes this statement as part of this file's behavior
  process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([  // line 75: executes this statement as part of this file's behavior
    { token: "admin-token", actorId: "admin_user", roles: ["admin"], projectIds: ["*"] },  // line 76: executes this statement as part of this file's behavior
    { token: "scoped-token", actorId: "scoped_user", roles: ["member"], projectIds: [created.body.id] },  // line 77: executes this statement as part of this file's behavior
  ]);  // line 78: executes this statement as part of this file's behavior

  const scoped = await request("GET", `/api/projects/${created.body.id}`, undefined, { token: "scoped-token" });  // line 80: executes this statement as part of this file's behavior
  assert.equal(scoped.statusCode, 200);  // line 81: executes this statement as part of this file's behavior
  assert.equal(scoped.body.id, created.body.id);  // line 82: executes this statement as part of this file's behavior

  const session = await request("GET", "/api/projects", undefined, { sessionToken: "session-token" });  // line 84: executes this statement as part of this file's behavior
  assert.equal(session.statusCode, 200);  // line 85: executes this statement as part of this file's behavior
  assert.ok(session.body.projects.some((project: { id: string }) => project.id === created.body.id));  // line 86: executes this statement as part of this file's behavior
});  // line 87: executes this statement as part of this file's behavior

test("legacy bot status route returns expected shape", async () => {  // line 89: executes this statement as part of this file's behavior
  const response = await request("GET", "/api/bot-status/");  // line 90: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 200);  // line 91: executes this statement as part of this file's behavior
  assert.equal(typeof response.body.running, "boolean");  // line 92: executes this statement as part of this file's behavior
  assert.equal(typeof response.body.status, "string");  // line 93: executes this statement as part of this file's behavior
});  // line 94: executes this statement as part of this file's behavior

test("legacy bot toggle validates action", async () => {  // line 96: executes this statement as part of this file's behavior
  await request("POST", "/api/projects", { name: "Toggle Test" });  // line 97: executes this statement as part of this file's behavior
  const response = await request("POST", "/api/bot-toggle/", { action: "bounce" });  // line 98: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 400);  // line 99: executes this statement as part of this file's behavior
  assert.equal(response.body.error.code, "VALIDATION_FAILED");  // line 100: executes this statement as part of this file's behavior
});  // line 101: executes this statement as part of this file's behavior

test("project creation validates name", async () => {  // line 103: executes this statement as part of this file's behavior
  const response = await request("POST", "/api/projects", { name: "" });  // line 104: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 400);  // line 105: executes this statement as part of this file's behavior
  assert.equal(response.body.error.code, "VALIDATION_FAILED");  // line 106: executes this statement as part of this file's behavior
});  // line 107: executes this statement as part of this file's behavior

test("project CRUD works and project create records audit event", async () => {  // line 109: executes this statement as part of this file's behavior
  const created = await request("POST", "/api/projects", { name: "Audit Project" });  // line 110: executes this statement as part of this file's behavior
  assert.equal(created.statusCode, 201);  // line 111: executes this statement as part of this file's behavior
  assert.equal(typeof created.body.auditEventId, "string");  // line 112: executes this statement as part of this file's behavior
  const projectId = created.body.id;  // line 113: executes this statement as part of this file's behavior

  const listed = await request("GET", "/api/projects");  // line 115: executes this statement as part of this file's behavior
  assert.ok(listed.body.projects.some((project: { id: string }) => project.id === projectId));  // line 116: executes this statement as part of this file's behavior

  const updated = await request("PATCH", `/api/projects/${projectId}`, { description: "Updated" });  // line 118: executes this statement as part of this file's behavior
  assert.equal(updated.statusCode, 200);  // line 119: executes this statement as part of this file's behavior
  assert.equal(updated.body.description, "Updated");  // line 120: executes this statement as part of this file's behavior

  const fetched = await request("GET", `/api/projects/${projectId}`);  // line 122: executes this statement as part of this file's behavior
  assert.equal(fetched.body.id, projectId);  // line 123: executes this statement as part of this file's behavior

  const events = await request("GET", `/api/projects/${projectId}/audit-events`);  // line 125: executes this statement as part of this file's behavior
  assert.ok(events.body.auditEvents.some((event: { action: string }) => event.action === "project.create"));  // line 126: executes this statement as part of this file's behavior
  assert.ok(events.body.auditEvents.some((event: { action: string; actorId: string }) => event.action === "project.create" && event.actorId === "admin_user"));  // line 127: executes this statement as part of this file's behavior
});  // line 128: executes this statement as part of this file's behavior


test("project PATCH preserves omitted nested config fields", async () => {  // line 131: executes this statement as part of this file's behavior
  const secret = await request("POST", "/api/secrets", { name: "Nested Patch Discord", type: "discord_bot_token", value: "nested-patch-token" });  // line 132: executes this statement as part of this file's behavior
  assert.equal(secret.statusCode, 201);  // line 133: executes this statement as part of this file's behavior

  const created = await request("POST", "/api/projects", {  // line 135: executes this statement as part of this file's behavior
    name: "Nested Patch Preserve",  // line 136: executes this statement as part of this file's behavior
    discord: {  // line 137: executes this statement as part of this file's behavior
      applicationId: "app_original",  // line 138: executes this statement as part of this file's behavior
      clientId: "client_original",  // line 139: executes this statement as part of this file's behavior
      defaultGuildId: "guild_original",  // line 140: executes this statement as part of this file's behavior
      tokenSecretRef: secret.body.id,  // line 141: executes this statement as part of this file's behavior
      commandRegistration: "global",  // line 142: executes this statement as part of this file's behavior
    },  // line 143: executes this statement as part of this file's behavior
    permissions: { intents: ["Guilds", "GuildMessages"], botPermissions: ["SendMessages", "ManageGuild"] },  // line 144: executes this statement as part of this file's behavior
    deployment: { targetId: "target_original", lastDeploymentId: "deployment_original" },  // line 145: executes this statement as part of this file's behavior
    github: { owner: "bot", repo: "blade", defaultBranch: "develop", lastPushedAt: "2026-05-01T00:00:00.000Z" },  // line 146: executes this statement as part of this file's behavior
  });  // line 147: executes this statement as part of this file's behavior
  assert.equal(created.statusCode, 201);  // line 148: executes this statement as part of this file's behavior
  const projectId = created.body.id;  // line 149: executes this statement as part of this file's behavior

  const discordPatched = await request("PATCH", `/api/projects/${projectId}`, { discord: { defaultGuildId: "guild_next" } });  // line 151: executes this statement as part of this file's behavior
  assert.equal(discordPatched.statusCode, 200);  // line 152: executes this statement as part of this file's behavior
  assert.equal(discordPatched.body.discord.defaultGuildId, "guild_next");  // line 153: executes this statement as part of this file's behavior
  assert.equal(discordPatched.body.discord.applicationId, "app_original");  // line 154: executes this statement as part of this file's behavior
  assert.equal(discordPatched.body.discord.clientId, "client_original");  // line 155: executes this statement as part of this file's behavior
  assert.equal(discordPatched.body.discord.tokenSecretRef, secret.body.id);  // line 156: executes this statement as part of this file's behavior
  assert.equal(discordPatched.body.discord.commandRegistration, "global");  // line 157: executes this statement as part of this file's behavior

  const permissionsPatched = await request("PATCH", `/api/projects/${projectId}`, { permissions: { intents: ["Guilds"] } });  // line 159: executes this statement as part of this file's behavior
  assert.equal(permissionsPatched.statusCode, 200);  // line 160: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(permissionsPatched.body.permissions.intents), JSON.stringify(["Guilds"]));  // line 161: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(permissionsPatched.body.permissions.botPermissions), JSON.stringify(["SendMessages", "ManageGuild"]));  // line 162: executes this statement as part of this file's behavior

  const deploymentPatched = await request("PATCH", `/api/projects/${projectId}`, { deployment: { targetId: "target_next" } });  // line 164: executes this statement as part of this file's behavior
  assert.equal(deploymentPatched.statusCode, 200);  // line 165: executes this statement as part of this file's behavior
  assert.equal(deploymentPatched.body.deployment.targetId, "target_next");  // line 166: executes this statement as part of this file's behavior
  assert.equal(deploymentPatched.body.deployment.lastDeploymentId, "deployment_original");  // line 167: executes this statement as part of this file's behavior

  const githubPatched = await request("PATCH", `/api/projects/${projectId}`, { github: { defaultBranch: "main" } });  // line 169: executes this statement as part of this file's behavior
  assert.equal(githubPatched.statusCode, 200);  // line 170: executes this statement as part of this file's behavior
  assert.equal(githubPatched.body.github.defaultBranch, "main");  // line 171: executes this statement as part of this file's behavior
  assert.equal(githubPatched.body.github.owner, "bot");  // line 172: executes this statement as part of this file's behavior
  assert.equal(githubPatched.body.github.repo, "blade");  // line 173: executes this statement as part of this file's behavior
  assert.equal(githubPatched.body.github.lastPushedAt, "2026-05-01T00:00:00.000Z");  // line 174: executes this statement as part of this file's behavior
});  // line 175: executes this statement as part of this file's behavior

test("secret create/rotate/delete audit and never return secret value", async () => {  // line 177: executes this statement as part of this file's behavior
  const secretValue = "test-secret-value-route-audit";  // line 178: executes this statement as part of this file's behavior
  const created = await request("POST", "/api/secrets", { name: "Discord", type: "discord_bot_token", value: secretValue });  // line 179: executes this statement as part of this file's behavior
  assert.equal(created.statusCode, 201);  // line 180: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(created.body).includes(secretValue), false);  // line 181: executes this statement as part of this file's behavior
  assert.equal(typeof created.body.auditEventId, "string");  // line 182: executes this statement as part of this file's behavior
  const secretId = created.body.id;  // line 183: executes this statement as part of this file's behavior

  const rotated = await request("POST", `/api/secrets/${secretId}/rotate`, { value: "next-secret-route-audit" });  // line 185: executes this statement as part of this file's behavior
  assert.equal(rotated.statusCode, 200);  // line 186: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(rotated.body).includes("next-secret-route-audit"), false);  // line 187: executes this statement as part of this file's behavior

  const deleted = await request("DELETE", `/api/secrets/${secretId}`);  // line 189: executes this statement as part of this file's behavior
  assert.equal(deleted.statusCode, 204);  // line 190: executes this statement as part of this file's behavior

  const audit = await request("GET", "/api/audit-events");  // line 192: executes this statement as part of this file's behavior
  const actions = audit.body.auditEvents.map((event: { action: string }) => event.action);  // line 193: executes this statement as part of this file's behavior
  assert.ok(actions.includes("secret.create"));  // line 194: executes this statement as part of this file's behavior
  assert.ok(actions.includes("secret.rotate"));  // line 195: executes this statement as part of this file's behavior
  assert.ok(actions.includes("secret.delete"));  // line 196: executes this statement as part of this file's behavior
  assert.equal(JSON.stringify(audit.body).includes(secretValue), false);  // line 197: executes this statement as part of this file's behavior
});  // line 198: executes this statement as part of this file's behavior

test("build job rejects missing project", async () => {  // line 200: executes this statement as part of this file's behavior
  const response = await request("POST", "/api/projects/project_missing/builds", {});  // line 201: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 404);  // line 202: executes this statement as part of this file's behavior
  assert.equal(response.body.error.code, "NOT_FOUND");  // line 203: executes this statement as part of this file's behavior
});  // line 204: executes this statement as part of this file's behavior

test("deployment rejects missing target/build", async () => {  // line 206: executes this statement as part of this file's behavior
  const created = await request("POST", "/api/projects", { name: "Deploy Missing" });  // line 207: executes this statement as part of this file's behavior
  const response = await request("POST", `/api/projects/${created.body.id}/deployments`, { targetId: "target_missing", buildId: "build_missing" });  // line 208: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 404);  // line 209: executes this statement as part of this file's behavior
  assert.equal(response.body.error.code, "TARGET_NOT_FOUND");  // line 210: executes this statement as part of this file's behavior
});  // line 211: executes this statement as part of this file's behavior


test("invalid request URL returns 400", async () => {  // line 214: executes this statement as part of this file's behavior
  const response = await request("GET", "http://%", undefined, { unauthenticated: true });  // line 215: executes this statement as part of this file's behavior
  assert.equal(response.statusCode, 400);  // line 216: executes this statement as part of this file's behavior
  assert.equal(response.body.error.code, "INVALID_REQUEST_URL");  // line 217: executes this statement as part of this file's behavior
});  // line 218: executes this statement as part of this file's behavior
