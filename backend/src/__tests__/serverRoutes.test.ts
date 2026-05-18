import test from "node:test";
import assert from "node:assert/strict";

process.env.NODE_ENV = "test";
process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([
  { token: "admin-token", actorId: "admin_user", roles: ["admin"], projectIds: ["*"] },
  { token: "scoped-token", actorId: "scoped_user", roles: ["member"], projectIds: ["project_allowed"] },
]);
process.env.BOTBLADE_SESSION_TOKENS = JSON.stringify([{ token: "session-token", actorId: "session_user", roles: ["admin"], projectIds: ["*"] }]);
const { createRequestListener } = await import("../server.js");

type Method = "GET" | "POST" | "PATCH" | "DELETE" | "PUT";

async function request(method: Method, url: string, body?: unknown, options: { token?: string; sessionToken?: string; unauthenticated?: boolean } = {}) {
  const chunks = body === undefined ? [] : [Buffer.from(JSON.stringify(body))];
  const headers: Record<string, string> = { host: "localhost", "x-request-id": `req_test_${Math.random().toString(16).slice(2)}` };
  if (!options.unauthenticated) {
    if (options.sessionToken) headers.cookie = `botBladeSession=${encodeURIComponent(options.sessionToken)}`;
    else headers.authorization = `Bearer ${options.token ?? "admin-token"}`;
  }
  const req = {
    method,
    url,
    headers,
    async *[Symbol.asyncIterator]() { for (const chunk of chunks) yield chunk; },
  };
  const res = {
    statusCode: 200,
    headers: {} as Record<string, string>,
    payload: "",
    setHeader(name: string, value: string) { this.headers[name.toLowerCase()] = value; },
    end(chunk?: string) { this.payload = chunk ?? ""; },
  };
  await createRequestListener()(req as never, res as never);
  return { statusCode: res.statusCode, body: res.payload ? JSON.parse(res.payload) : null, headers: res.headers };
}

test("health route returns ok", async () => {
  const response = await request("GET", "/api/health");
  assert.equal(response.statusCode, 200);
  assert.equal(response.body.ok, true);
});



test("protected routes reject unauthenticated requests", async () => {
  const response = await request("GET", "/api/projects", undefined, { unauthenticated: true });
  assert.equal(response.statusCode, 401);
  assert.equal(response.body.error.code, "AUTHENTICATION_REQUIRED");
});

test("project routes reject actors without project authorization", async () => {
  const created = await request("POST", "/api/projects", { name: "Authorization Denied" });
  const response = await request("GET", `/api/projects/${created.body.id}`, undefined, { token: "scoped-token" });
  assert.equal(response.statusCode, 403);
  assert.equal(response.body.error.code, "PROJECT_ACCESS_DENIED");
});

test("project routes allow authorized bearer and session actors", async () => {
  const created = await request("POST", "/api/projects", { name: "Authorization Allowed" });
  process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([
    { token: "admin-token", actorId: "admin_user", roles: ["admin"], projectIds: ["*"] },
    { token: "scoped-token", actorId: "scoped_user", roles: ["member"], projectIds: [created.body.id] },
  ]);

  const scoped = await request("GET", `/api/projects/${created.body.id}`, undefined, { token: "scoped-token" });
  assert.equal(scoped.statusCode, 200);
  assert.equal(scoped.body.id, created.body.id);

  const session = await request("GET", "/api/projects", undefined, { sessionToken: "session-token" });
  assert.equal(session.statusCode, 200);
  assert.ok(session.body.projects.some((project: { id: string }) => project.id === created.body.id));
});

test("legacy bot status route returns expected shape", async () => {
  const response = await request("GET", "/api/bot-status/");
  assert.equal(response.statusCode, 200);
  assert.equal(typeof response.body.running, "boolean");
  assert.equal(typeof response.body.status, "string");
});

test("legacy bot toggle validates action", async () => {
  await request("POST", "/api/projects", { name: "Toggle Test" });
  const response = await request("POST", "/api/bot-toggle/", { action: "bounce" });
  assert.equal(response.statusCode, 400);
  assert.equal(response.body.error.code, "VALIDATION_FAILED");
});

test("project creation validates name", async () => {
  const response = await request("POST", "/api/projects", { name: "" });
  assert.equal(response.statusCode, 400);
  assert.equal(response.body.error.code, "VALIDATION_FAILED");
});

test("project CRUD works and project create records audit event", async () => {
  const created = await request("POST", "/api/projects", { name: "Audit Project" });
  assert.equal(created.statusCode, 201);
  assert.equal(typeof created.body.auditEventId, "string");
  const projectId = created.body.id;

  const listed = await request("GET", "/api/projects");
  assert.ok(listed.body.projects.some((project: { id: string }) => project.id === projectId));

  const updated = await request("PATCH", `/api/projects/${projectId}`, { description: "Updated" });
  assert.equal(updated.statusCode, 200);
  assert.equal(updated.body.description, "Updated");

  const fetched = await request("GET", `/api/projects/${projectId}`);
  assert.equal(fetched.body.id, projectId);

  const events = await request("GET", `/api/projects/${projectId}/audit-events`);
  assert.ok(events.body.auditEvents.some((event: { action: string }) => event.action === "project.create"));
  assert.ok(events.body.auditEvents.some((event: { action: string; actorId: string }) => event.action === "project.create" && event.actorId === "admin_user"));
});


test("project PATCH preserves omitted nested config fields", async () => {
  const secret = await request("POST", "/api/secrets", { name: "Nested Patch Discord", type: "discord_bot_token", value: "nested-patch-token" });
  assert.equal(secret.statusCode, 201);

  const created = await request("POST", "/api/projects", {
    name: "Nested Patch Preserve",
    discord: {
      applicationId: "app_original",
      clientId: "client_original",
      defaultGuildId: "guild_original",
      tokenSecretRef: secret.body.id,
      commandRegistration: "global",
    },
    permissions: { intents: ["Guilds", "GuildMessages"], botPermissions: ["SendMessages", "ManageGuild"] },
    deployment: { targetId: "target_original", lastDeploymentId: "deployment_original" },
    github: { owner: "bot", repo: "blade", defaultBranch: "develop", lastPushedAt: "2026-05-01T00:00:00.000Z" },
  });
  assert.equal(created.statusCode, 201);
  const projectId = created.body.id;

  const discordPatched = await request("PATCH", `/api/projects/${projectId}`, { discord: { defaultGuildId: "guild_next" } });
  assert.equal(discordPatched.statusCode, 200);
  assert.equal(discordPatched.body.discord.defaultGuildId, "guild_next");
  assert.equal(discordPatched.body.discord.applicationId, "app_original");
  assert.equal(discordPatched.body.discord.clientId, "client_original");
  assert.equal(discordPatched.body.discord.tokenSecretRef, secret.body.id);
  assert.equal(discordPatched.body.discord.commandRegistration, "global");

  const permissionsPatched = await request("PATCH", `/api/projects/${projectId}`, { permissions: { intents: ["Guilds"] } });
  assert.equal(permissionsPatched.statusCode, 200);
  assert.equal(JSON.stringify(permissionsPatched.body.permissions.intents), JSON.stringify(["Guilds"]));
  assert.equal(JSON.stringify(permissionsPatched.body.permissions.botPermissions), JSON.stringify(["SendMessages", "ManageGuild"]));

  const deploymentPatched = await request("PATCH", `/api/projects/${projectId}`, { deployment: { targetId: "target_next" } });
  assert.equal(deploymentPatched.statusCode, 200);
  assert.equal(deploymentPatched.body.deployment.targetId, "target_next");
  assert.equal(deploymentPatched.body.deployment.lastDeploymentId, "deployment_original");

  const githubPatched = await request("PATCH", `/api/projects/${projectId}`, { github: { defaultBranch: "main" } });
  assert.equal(githubPatched.statusCode, 200);
  assert.equal(githubPatched.body.github.defaultBranch, "main");
  assert.equal(githubPatched.body.github.owner, "bot");
  assert.equal(githubPatched.body.github.repo, "blade");
  assert.equal(githubPatched.body.github.lastPushedAt, "2026-05-01T00:00:00.000Z");
});

test("secret create/rotate/delete audit and never return secret value", async () => {
  const secretValue = "test-secret-value-route-audit";
  const created = await request("POST", "/api/secrets", { name: "Discord", type: "discord_bot_token", value: secretValue });
  assert.equal(created.statusCode, 201);
  assert.equal(JSON.stringify(created.body).includes(secretValue), false);
  assert.equal(typeof created.body.auditEventId, "string");
  const secretId = created.body.id;

  const rotated = await request("POST", `/api/secrets/${secretId}/rotate`, { value: "next-secret-route-audit" });
  assert.equal(rotated.statusCode, 200);
  assert.equal(JSON.stringify(rotated.body).includes("next-secret-route-audit"), false);

  const deleted = await request("DELETE", `/api/secrets/${secretId}`);
  assert.equal(deleted.statusCode, 204);

  const audit = await request("GET", "/api/audit-events");
  const actions = audit.body.auditEvents.map((event: { action: string }) => event.action);
  assert.ok(actions.includes("secret.create"));
  assert.ok(actions.includes("secret.rotate"));
  assert.ok(actions.includes("secret.delete"));
  assert.equal(JSON.stringify(audit.body).includes(secretValue), false);
});

test("build job rejects missing project", async () => {
  const response = await request("POST", "/api/projects/project_missing/builds", {});
  assert.equal(response.statusCode, 404);
  assert.equal(response.body.error.code, "NOT_FOUND");
});

test("deployment rejects missing target/build", async () => {
  const created = await request("POST", "/api/projects", { name: "Deploy Missing" });
  const response = await request("POST", `/api/projects/${created.body.id}/deployments`, { targetId: "target_missing", buildId: "build_missing" });
  assert.equal(response.statusCode, 404);
  assert.equal(response.body.error.code, "TARGET_NOT_FOUND");
});
