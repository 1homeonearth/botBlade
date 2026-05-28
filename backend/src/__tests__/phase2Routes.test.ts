import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import test from "node:test";

const authValue = "phase2-route-auth";
process.env.NODE_ENV = "test";
process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([{ token: authValue, tokenId: "phase2_token", actorId: "phase2_actor", roles: ["admin"], projectIds: ["*"] }]);

const { createRequestListener } = await import("../server.js");

type Method = "GET" | "POST";

async function request(method: Method, url: string, body?: unknown, authenticated = true) {
  const chunks = body === undefined ? [] : [Buffer.from(JSON.stringify(body))];
  const headers: Record<string, string> = { host: "localhost", "x-request-id": `req_${randomUUID()}` };
  if (authenticated) headers.authorization = `Bearer ${authValue}`;
  if (body !== undefined) headers["content-type"] = "application/json";
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
  return { statusCode: res.statusCode, body: res.payload ? JSON.parse(res.payload) : null };
}

test("/api/health reports test persistence mode without auth", async () => {
  const response = await request("GET", "/api/health", undefined, false);

  assert.equal(response.statusCode, 200);
  assert.equal(response.body.ok, true);
  assert.equal(response.body.persistence, "memory");
});

test("/api/persistence/status requires auth and reports memory diagnostics in test mode", async () => {
  const denied = await request("GET", "/api/persistence/status", undefined, false);
  assert.equal(denied.statusCode, 401);

  const response = await request("GET", "/api/persistence/status");
  assert.equal(response.statusCode, 200);
  assert.equal(response.body.adapter, "memory");
  assert.equal(response.body.durable, false);
});

test("/api/auth/rotate returns an environment-backed rotation plan", async () => {
  const response = await request("POST", "/api/auth/rotate", { authMethod: "bearer", roles: ["admin"], projectIds: ["*"] });

  assert.equal(response.statusCode, 201);
  assert.equal(response.body.currentTokenId, "phase2_token");
  assert.equal(response.body.actorId, "phase2_actor");
  assert.equal(response.body.environmentVariable, "BOTBLADE_AUTH_TOKENS");
  assert.equal(response.body.nextEntry.actorId, "phase2_actor");
  assert.equal(response.body.revokeCurrentEntry.tokenId, "phase2_token");
});
