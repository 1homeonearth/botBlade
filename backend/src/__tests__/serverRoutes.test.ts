import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { execFileSync } from "node:child_process";
import fs from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";

process.env.NODE_ENV = "test";
process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([
  { token: "admin-token", actorId: "admin_user", roles: ["admin"], projectIds: ["*"] },
  { token: "scoped-token", actorId: "scoped_user", roles: ["member"], projectIds: ["project_allowed"] },
]);
process.env.BOTBLADE_SESSION_TOKENS = JSON.stringify([{ token: "session-token", actorId: "session_user", roles: ["admin"], projectIds: ["*"] }]);
const { createRequestListener } = await import("../server.js");

type Method = "GET" | "POST" | "PATCH" | "DELETE" | "PUT";

async function request(method: Method, url: string, body?: unknown, options: { token?: string; sessionToken?: string; unauthenticated?: boolean; authorizationHeader?: string | string[]; sessionHeader?: string | string[]; rawCookieHeader?: string } = {}) {
  const chunks = body === undefined ? [] : [Buffer.from(JSON.stringify(body))];
  const headers: Record<string, string | string[]> = { host: "localhost", "x-request-id": `req_test_${randomUUID()}` };
  if (options.sessionToken) headers.cookie = `botBladeSession=${encodeURIComponent(options.sessionToken)}`;
  if (options.rawCookieHeader) headers.cookie = options.rawCookieHeader;
  if (options.sessionHeader) reqHeader(headers, "x-session-token", options.sessionHeader);
  if (options.authorizationHeader) reqHeader(headers, "authorization", options.authorizationHeader);
  else if (!options.unauthenticated && !options.sessionToken && !options.sessionHeader) headers.authorization = `Bearer ${options.token ?? "admin-token"}`;
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


function reqHeader(headers: Record<string, string | string[]>, name: string, value: string | string[]): void {
  headers[name] = value;
}

function pythonAvailable(): boolean {
  try {
    execFileSync("python3", ["--version"]);
    return true;
  } catch {
    return false;
  }
}

async function createZipFixture(entries: Record<string, string>): Promise<{ zipPath: string; workspaceRoot: string }> {
  const dir = path.join(tmpdir(), `botblade-server-routes-zip-${randomUUID()}`);
  await fs.mkdir(dir, { recursive: true });
  const zipPath = path.join(dir, "fixture.zip");
  const script = `import json, sys, zipfile
entries=json.loads(sys.argv[2])
with zipfile.ZipFile(sys.argv[1], 'w', compression=zipfile.ZIP_DEFLATED) as z:
    [z.writestr(name, content) for name, content in entries.items()]`;
  execFileSync("python3", ["-c", script, zipPath, JSON.stringify(entries)]);
  return { zipPath, workspaceRoot: path.join(dir, "workspace") };
}

test("health route returns ok", async () => {
  const response = await request("GET", "/api/health");
  assert.equal(response.statusCode, 200);
  assert.equal(response.body.ok, true);
});

test("startup diagnostics route returns unconfigured when no path is set", async () => {
  delete process.env.BOTBLADE_STARTUP_CRASH_ARTIFACT;
  const response = await request("GET", "/api/diagnostics/startup-crash");
  assert.equal(response.statusCode, 200);
  assert.equal(response.body.artifact, null);
});



test("protected routes reject unauthenticated requests", async () => {
  const response = await request("GET", "/api/projects", undefined, { unauthenticated: true });
  assert.equal(response.statusCode, 401);
  assert.equal(response.body.error.code, "AUTHENTICATION_REQUIRED");
});



test("malformed session cookie fails closed with 401 instead of server error", async () => {
  const response = await request("GET", "/api/projects", undefined, { unauthenticated: true, rawCookieHeader: "botBladeSession=%" });
  assert.equal(response.statusCode, 401);
  assert.equal(response.body.error.code, "AUTHENTICATION_REQUIRED");
});


test("authorization header array accepts a valid bearer token", async () => {
  const response = await request("GET", "/api/projects", undefined, { authorizationHeader: ["Bearer admin-token"] });
  assert.equal(response.statusCode, 200);
});

test("x-session-token header array accepts a valid session token", async () => {
  const response = await request("GET", "/api/projects", undefined, { unauthenticated: true, sessionHeader: ["session-token"] });
  assert.equal(response.statusCode, 200);
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


test("file operation routes enforce auth and expose tree", async () => {
  const created = await request("POST", "/api/projects", { name: "File Route Auth" });
  const projectId = created.body.id as string;

  const unauthenticated = await request("GET", `/api/projects/${projectId}/files`, undefined, { unauthenticated: true });
  assert.equal(unauthenticated.statusCode, 401);
  assert.equal(unauthenticated.body.error.code, "AUTHENTICATION_REQUIRED");

  process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([
    { token: "admin-token", actorId: "admin_user", roles: ["admin"], projectIds: ["*"] },
    { token: "read-token-0001", actorId: "read_user", roles: ["member"], projectIds: [projectId] },
    { token: "exec-token-0001", actorId: "exec_user", roles: ["execute"], projectIds: [projectId] },
  ]);

  const deniedCreate = await request("POST", `/api/projects/${projectId}/files`, { path: "src/blocked.txt", content: "blocked" }, { token: "read-token-0001" });
  assert.equal(deniedCreate.statusCode, 403);
  assert.equal(deniedCreate.body.error.code, "EXECUTION_ACCESS_DENIED");

  const createdFolder = await request("POST", `/api/projects/${projectId}/folders`, { path: "src/routes" }, { token: "exec-token-0001" });
  assert.equal(createdFolder.statusCode, 201);
  assert.equal(createdFolder.body.path, "src/routes");
  assert.equal(createdFolder.body.created, true);
  assert.equal(typeof createdFolder.body.auditEventId, "string");

  const createdFile = await request("POST", `/api/projects/${projectId}/files`, { path: "src/routes/hello.ts", content: "export const hello = 'world';\n" }, { token: "exec-token-0001" });
  assert.equal(createdFile.statusCode, 201);
  assert.equal(createdFile.body.path, "src/routes/hello.ts");
  assert.equal(typeof createdFile.body.auditEventId, "string");

  const read = await request("GET", `/api/projects/${projectId}/files/src/routes/hello.ts`, undefined, { token: "read-token-0001" });
  assert.equal(read.statusCode, 200);
  assert.equal(read.body.content, "export const hello = 'world';\n");

  const deniedWrite = await request("PUT", `/api/projects/${projectId}/files/src/routes/hello.ts`, { content: "blocked" }, { token: "read-token-0001" });
  assert.equal(deniedWrite.statusCode, 403);
  assert.equal(deniedWrite.body.error.code, "EXECUTION_ACCESS_DENIED");

  const written = await request("PUT", `/api/projects/${projectId}/files/src/routes/hello.ts`, { content: "export const hello = 'again';\n" }, { token: "exec-token-0001" });
  assert.equal(written.statusCode, 200);
  assert.equal(written.body.content, "export const hello = 'again';\n");

  const files = await request("GET", `/api/projects/${projectId}/files`, undefined, { token: "read-token-0001" });
  assert.equal(files.statusCode, 200);
  assert.ok(Array.isArray(files.body.files));
  assert.ok(Array.isArray(files.body.tree));
  assert.ok(files.body.files.some((file: { path: string }) => file.path === "src/routes/hello.ts"));
  assert.ok(files.body.tree.some((node: { name: string; type: string }) => node.name === "src" && node.type === "directory"));
});

test("file operation routes allow execute actors to rename and delete", async () => {
  const created = await request("POST", "/api/projects", { name: "File Route Mutations" });
  const projectId = created.body.id as string;
  process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([
    { token: "admin-token", actorId: "admin_user", roles: ["admin"], projectIds: ["*"] },
    { token: "read-token-0002", actorId: "read_user", roles: ["member"], projectIds: [projectId] },
    { token: "exec-token-0002", actorId: "exec_user", roles: ["execute"], projectIds: [projectId] },
  ]);

  const createdFile = await request("POST", `/api/projects/${projectId}/files`, { path: "notes/todo.txt", content: "todo\n" }, { token: "exec-token-0002" });
  assert.equal(createdFile.statusCode, 201);

  const deniedRename = await request("PATCH", `/api/projects/${projectId}/files`, { fromPath: "notes/todo.txt", toPath: "notes/done.txt" }, { token: "read-token-0002" });
  assert.equal(deniedRename.statusCode, 403);
  assert.equal(deniedRename.body.error.code, "EXECUTION_ACCESS_DENIED");

  const renamed = await request("PATCH", `/api/projects/${projectId}/files`, { fromPath: "notes/todo.txt", toPath: "notes/done.txt" }, { token: "exec-token-0002" });
  assert.equal(renamed.statusCode, 200);
  assert.equal(renamed.body.fromPath, "notes/todo.txt");
  assert.equal(renamed.body.toPath, "notes/done.txt");
  assert.equal(typeof renamed.body.auditEventId, "string");

  const deniedDelete = await request("DELETE", `/api/projects/${projectId}/files`, { path: "notes/done.txt" }, { token: "read-token-0002" });
  assert.equal(deniedDelete.statusCode, 403);
  assert.equal(deniedDelete.body.error.code, "EXECUTION_ACCESS_DENIED");

  const deleted = await request("DELETE", `/api/projects/${projectId}/files`, { path: "notes/done.txt" }, { token: "exec-token-0002" });
  assert.equal(deleted.statusCode, 200);
  assert.equal(deleted.body.path, "notes/done.txt");
  assert.equal(deleted.body.deleted, true);
  assert.equal(typeof deleted.body.auditEventId, "string");

  const missing = await request("GET", `/api/projects/${projectId}/files/notes/done.txt`, undefined, { token: "exec-token-0002" });
  assert.equal(missing.statusCode, 404);
  assert.equal(missing.body.error.code, "FILE_NOT_FOUND");
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


test("invalid request URL returns 400", async () => {
  const response = await request("GET", "http://%", undefined, { unauthenticated: true });
  assert.equal(response.statusCode, 400);
  assert.equal(response.body.error.code, "INVALID_REQUEST_URL");
});

test("import git route creates ready import record", async () => {
  const response = await request("POST", "/api/imports/git", { repoUrl: "https://github.com/example/repo.git", workspacePath: "/tmp" });
  assert.equal(response.statusCode, 201);
  assert.equal(response.body.source.type, "git");
  assert.ok(["ready", "needs_secrets", "failed", "blocked_by_policy"].includes(response.body.state));

  const fetched = await request("GET", `/api/imports/${response.body.id}`);
  assert.equal(fetched.statusCode, 200);
  assert.equal(fetched.body.id, response.body.id);
});

test("folder import route is feature-gated when SAF disabled", async () => {
  delete process.env.BOTBLADE_SAF_IMPORT_ENABLED;
  const response = await request("POST", "/api/imports/folder", { folderPath: "/tmp", workspacePath: "/tmp" });
  assert.equal(response.statusCode, 403);
  assert.equal(response.body.error.code, "FEATURE_DISABLED");
});

test("zip import route persists import state and includes structured failure details when failed", async () => {
  const response = await request("POST", "/api/imports/zip", { archivePath: "/missing.zip", workspacePath: "/definitely-missing-workspace" });
  assert.equal(response.statusCode, 201);
  assert.equal(response.body.source.type, "zip");
  assert.ok(["ready", "needs_secrets", "failed", "blocked_by_policy"].includes(response.body.state));
  if (response.body.state === "failed") {
    assert.equal(typeof response.body.failure.cause, "string");
    assert.equal(typeof response.body.failure.evidence, "string");
    assert.equal(typeof response.body.failure.safeAction, "string");
  }
});


test("project git status route returns safe non-git summary", async () => {
  const created = await request("POST", "/api/projects", { name: "Git Status Project" });
  const response = await request("GET", `/api/projects/${created.body.id}/git/status`);
  assert.equal(response.statusCode, 200);
  assert.equal(typeof response.body.available, "boolean");
});




test("profile route preserves stored Git metadata while refreshing current status", async () => {
  const created = await request("POST", "/api/projects", { name: "Profile Git Metadata" });
  const workspace = path.join(process.cwd(), "generated-projects", created.body.id);
  await fs.mkdir(workspace, { recursive: true });
  await fs.writeFile(path.join(workspace, "botblade.json"), JSON.stringify({
    schemaVersion: "1.0.0",
    generatedBy: "botblade",
    generatedAt: new Date().toISOString(),
    project: { importSource: { kind: "local" } },
    git: {
      branch: "main",
      status: "dirty",
      dirtyFileCount: 7,
      remotes: [{ name: "origin", url: "https://example.com/repo.git" }],
      importCommit: "abc123",
    },
    repairCards: []
  }), "utf8");

  const response = await request("GET", `/api/projects/${created.body.id}/profile`);

  assert.equal(response.statusCode, 200);
  assert.equal(response.body.git.importCommit, "abc123");
  assert.equal(response.body.git.status, "unknown");
  assert.equal(response.body.git.branch, null);
  assert.deepStrictEqual(response.body.git.remotes, []);
  assert.equal("dirtyFileCount" in response.body.git, false);
});

test("profile route redacts importSource url credentials", async () => {
  const created = await request("POST", "/api/projects", { name: "Profile Redaction" });
  const workspace = path.join(process.cwd(), "generated-projects", created.body.id);
  await fs.mkdir(workspace, { recursive: true });
  await fs.writeFile(path.join(workspace, "botblade.json"), JSON.stringify({
    schemaVersion: "1.0.0",
    generatedBy: "botblade",
    generatedAt: new Date().toISOString(),
    project: { importSource: { kind: "git", url: "https://example.com/repo.git?access_token=abc123&expires=1" } },
    repairCards: []
  }), "utf8");
  const response = await request("GET", `/api/projects/${created.body.id}/profile`);
  assert.equal(response.statusCode, 200);
  const url = response.body.project.importSource.url as string;
  assert.equal(url.includes("abc123"), false);
  assert.equal(url.includes("access_token=[REDACTED]"), true);
});

test("script profile routes enforce project auth and CRUD profiles", async () => {
  const createdProject = await request("POST", "/api/projects", { name: "Script Profile Routes" });
  const projectId = createdProject.body.id as string;

  const unauthenticated = await request("GET", `/api/projects/${projectId}/script-profiles`, undefined, { unauthenticated: true });
  assert.equal(unauthenticated.statusCode, 401);
  assert.equal(unauthenticated.body.error.code, "AUTHENTICATION_REQUIRED");

  process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([
    { token: "admin-token", actorId: "admin_user", roles: ["admin"], projectIds: ["*"] },
    { token: "wrong-project-token", actorId: "wrong_project_user", roles: ["member"], projectIds: ["project_other"] },
  ]);

  const denied = await request("GET", `/api/projects/${projectId}/script-profiles`, undefined, { token: "wrong-project-token" });
  assert.equal(denied.statusCode, 403);
  assert.equal(denied.body.error.code, "PROJECT_ACCESS_DENIED");

  const created = await request("POST", `/api/projects/${projectId}/script-profiles`, {
    name: "Validate bot",
    source: "user",
    runtime: "node",
    command: ["npm", "run", "validate"],
    workingDirectory: "packages/bot",
    secretRefs: ["secret_discord_token", "DISCORD_TOKEN"],
    timeoutSeconds: 600,
    tags: ["validate"],
  });
  assert.equal(created.statusCode, 201);
  assert.equal(created.body.projectId, projectId);
  assert.deepStrictEqual(created.body.command, ["npm", "run", "validate"]);
  assert.equal(created.body.workingDirectory, "packages/bot");

  const listed = await request("GET", `/api/projects/${projectId}/script-profiles`);
  assert.equal(listed.statusCode, 200);
  assert.ok(listed.body.scriptProfiles.some((profile: { id: string }) => profile.id === created.body.id));

  const fetched = await request("GET", `/api/projects/${projectId}/script-profiles/${encodeURIComponent(created.body.id)}`);
  assert.equal(fetched.statusCode, 200);
  assert.equal(fetched.body.id, created.body.id);

  const patched = await request("PATCH", `/api/projects/${projectId}/script-profiles/${encodeURIComponent(created.body.id)}`, {
    runtime: "shell",
    command: ["bash", "scripts/validate.sh"],
    workingDirectory: ".",
    timeoutSeconds: 900,
  });
  assert.equal(patched.statusCode, 200);
  assert.equal(patched.body.runtime, "shell");
  assert.deepStrictEqual(patched.body.command, ["bash", "scripts/validate.sh"]);
  assert.equal(patched.body.timeoutSeconds, 900);

  const unsupportedPut = await request("PUT", `/api/projects/${projectId}/script-profiles/${encodeURIComponent(created.body.id)}`, { name: "Blocked" });
  assert.equal(unsupportedPut.statusCode, 404);

  const deleted = await request("DELETE", `/api/projects/${projectId}/script-profiles/${encodeURIComponent(created.body.id)}`);
  assert.equal(deleted.statusCode, 204);
  assert.equal(deleted.body, null);
});

test("script profile routes validate command, paths, secrets, timeout, runtime, and source", async () => {
  const createdProject = await request("POST", "/api/projects", { name: "Script Profile Validation" });
  const projectId = createdProject.body.id as string;
  const validProfile = {
    name: "Build bot",
    source: "user",
    runtime: "node",
    command: ["npm", "run", "build"],
    workingDirectory: ".",
    secretRefs: ["secret_build_token"],
    timeoutSeconds: 300,
  };

  const cases: Array<{ body: Record<string, unknown>; fields: string[] }> = [
    { body: { ...validProfile, command: "npm run build" }, fields: ["command"] },
    { body: { ...validProfile, command: ["npm", ""] }, fields: ["command"] },
    { body: { ...validProfile, workingDirectory: "../outside" }, fields: ["workingDirectory"] },
    { body: { ...validProfile, workingDirectory: "/tmp/project" }, fields: ["workingDirectory"] },
    { body: { ...validProfile, workingDirectory: "src/../outside" }, fields: ["workingDirectory"] },
    { body: { ...validProfile, secretRefs: ["sk-1234567890abcdefghijklmnopqrstuvwxyz"] }, fields: ["secretRefs"] },
    { body: { ...validProfile, secretRefs: ["token=value"] }, fields: ["secretRefs"] },
    { body: { ...validProfile, timeoutSeconds: 0 }, fields: ["timeoutSeconds"] },
    { body: { ...validProfile, timeoutSeconds: 86_401 }, fields: ["timeoutSeconds"] },
    { body: { ...validProfile, runtime: "bun" }, fields: ["runtime"] },
    { body: { ...validProfile, source: "uploaded" }, fields: ["source"] },
  ];

  for (const invalidCase of cases) {
    const response = await request("POST", `/api/projects/${projectId}/script-profiles`, invalidCase.body);
    assert.equal(response.statusCode, 400, `expected validation failure for ${invalidCase.fields.join(",")}`);
    assert.equal(response.body.error.code, "VALIDATION_FAILED");
    const problemFields = response.body.error.details.problems.map((problem: { field: string }) => problem.field);
    for (const field of invalidCase.fields) assert.ok(problemFields.includes(field), `expected ${field} problem in ${problemFields.join(",")}`);
  }

  const missingCommand = await request("POST", `/api/projects/${projectId}/script-profiles`, { name: "No command", runtime: "node" });
  assert.equal(missingCommand.statusCode, 400);
  assert.ok(missingCommand.body.error.details.problems.some((problem: { field: string }) => problem.field === "command"));

  const valid = await request("POST", `/api/projects/${projectId}/script-profiles`, validProfile);
  assert.equal(valid.statusCode, 201);

  const invalidPatch = await request("PATCH", `/api/projects/${projectId}/script-profiles/${encodeURIComponent(valid.body.id)}`, { command: [] });
  assert.equal(invalidPatch.statusCode, 400);
  assert.ok(invalidPatch.body.error.details.problems.some((problem: { field: string }) => problem.field === "command"));

  const noRunRoute = await request("POST", `/api/projects/${projectId}/script-profiles/${encodeURIComponent(valid.body.id)}/run`, {});
  assert.equal(noRunRoute.statusCode, 404);

  const noLogsRoute = await request("GET", `/api/projects/${projectId}/script-profiles/${encodeURIComponent(valid.body.id)}/logs`);
  assert.equal(noLogsRoute.statusCode, 404);

  const noCancelRoute = await request("POST", `/api/projects/${projectId}/script-profiles/${encodeURIComponent(valid.body.id)}/cancel`, {});
  assert.equal(noCancelRoute.statusCode, 404);
});

test("script profile detail routes decode detected profile IDs containing slashes", async () => {
  const createdProject = await request("POST", "/api/projects", { name: "Slash Profile IDs" });
  const projectId = createdProject.body.id as string;

  const createdScript = await request("POST", `/api/projects/${projectId}/files`, { path: "scripts/deploy.sh", content: "#!/usr/bin/env bash\necho deploy\n" });
  assert.equal(createdScript.statusCode, 201);

  const scan = await request("POST", `/api/projects/${projectId}/scan`, {});
  assert.equal(scan.statusCode, 200);

  const listed = await request("GET", `/api/projects/${projectId}/script-profiles`);
  assert.equal(listed.statusCode, 200);
  const slashProfile = listed.body.scriptProfiles.find((profile: { id: string }) => profile.id.includes("scripts/deploy.sh"));
  assert.ok(slashProfile);
  assert.equal(slashProfile.id.includes("/"), true);

  const encodedId = encodeURIComponent(slashProfile.id);
  const fetched = await request("GET", `/api/projects/${projectId}/script-profiles/${encodedId}`);
  assert.equal(fetched.statusCode, 200);
  assert.equal(fetched.body.id, slashProfile.id);

  const patched = await request("PATCH", `/api/projects/${projectId}/script-profiles/${encodedId}`, { tags: ["deploy", "review"] });
  assert.equal(patched.statusCode, 200);
  assert.deepStrictEqual(patched.body.tags, ["deploy", "review"]);

  const deleted = await request("DELETE", `/api/projects/${projectId}/script-profiles/${encodedId}`);
  assert.equal(deleted.statusCode, 204);
});

test("successful zip imports attach detected script profiles to a managed project", async () => {
  if (!pythonAvailable()) return;
  const { zipPath, workspaceRoot } = await createZipFixture({
    "package.json": JSON.stringify({ scripts: { start: "node index.js" } }),
    "index.js": "console.log('hello');\n",
  });

  const imported = await request("POST", "/api/imports/zip", { archivePath: zipPath, workspacePath: workspaceRoot, projectName: "ZIP Managed Profiles" });
  assert.equal(imported.statusCode, 201);
  assert.equal(imported.body.source.type, "zip");
  assert.ok(imported.body.managedProject?.id, "zip import should return a managed project");
  assert.ok(imported.body.managedProject.id !== imported.body.id);

  const listed = await request("GET", `/api/projects/${imported.body.managedProject.id}/script-profiles`);
  assert.equal(listed.statusCode, 200);
  assert.ok(listed.body.scriptProfiles.length > 0);
  assert.ok(listed.body.scriptProfiles.every((profile: { projectId: string }) => profile.projectId === imported.body.managedProject.id));
});
