import test from "node:test";
import assert from "node:assert/strict";

process.env.NODE_ENV = "test";
const { createRequestListener } = await import("../server.js");

type Method = "GET" | "POST" | "PATCH" | "DELETE" | "PUT";

async function request(method: Method, url: string, body?: unknown) {
  const chunks = body === undefined ? [] : [Buffer.from(JSON.stringify(body))];
  const req = {
    method,
    url,
    headers: { host: "localhost", "x-request-id": `req_test_${Math.random().toString(16).slice(2)}` },
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
