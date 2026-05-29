import test from "node:test";
import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import fs from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";

process.env.NODE_ENV = "test";
process.env.BOTBLADE_AUTH_TOKENS = JSON.stringify([
  { token: "workflow-template-token", actorId: "import_tester", roles: ["admin"], projectIds: ["*"] },
]);

const { createRequestListener } = await import("../server.js");

type Method = "GET" | "POST";

async function request(method: Method, url: string, body?: unknown) {
  const chunks = body === undefined ? [] : [Buffer.from(JSON.stringify(body))];
  const headers: Record<string, string> = {
    host: "localhost",
    "x-request-id": `req_${randomUUID()}`,
    authorization: "Bearer workflow-template-token",
  };
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

async function tempWorkspace(prefix: string): Promise<string> {
  const dir = path.join(tmpdir(), `${prefix}${randomUUID()}`);
  await fs.mkdir(dir, { recursive: true });
  return dir;
}

function basename(value: string): string {
  return value.replace(/\\/g, "/").split("/").filter(Boolean).pop() ?? value;
}

test("workflow-json import materializes workflow.json in a managed workspace and scans it", async () => {
  const sourceDir = await tempWorkspace("botblade-workflow-source-");
  const workspaceRoot = await tempWorkspace("botblade-workflow-import-");
  const workflowPath = path.join(sourceDir, "source-name.json");
  await fs.writeFile(workflowPath, JSON.stringify({ nodes: [], connections: {}, meta: { instanceId: "test" } }), "utf8");

  const response = await request("POST", "/api/imports/workflow-json", { workflowPath, workspacePath: workspaceRoot, projectName: "Workflow Route Success" });

  assert.equal(response.statusCode, 201);
  assert.equal(response.body.source.type, "workflow_json");
  assert.equal(response.body.state, "ready");
  assert.ok(response.body.workspacePath.startsWith(workspaceRoot));
  assert.equal(basename(response.body.workspacePath), response.body.id);
  assert.equal(await fs.readFile(path.join(response.body.workspacePath, "workflow.json"), "utf8").then((content) => JSON.parse(content).meta.instanceId), "test");
  assert.ok(response.body.managedProject?.id);
});

test("template import allows a first-party template ID and scans materialized files", async () => {
  const workspaceRoot = await tempWorkspace("botblade-template-import-");

  const response = await request("POST", "/api/imports/template", { templateId: "n8n-empty-workflow", workspacePath: workspaceRoot });

  assert.equal(response.statusCode, 201);
  assert.equal(response.body.source.type, "template");
  assert.equal(response.body.state, "ready");
  assert.ok(response.body.workspacePath.startsWith(workspaceRoot));
  assert.equal(basename(response.body.workspacePath), response.body.id);
  assert.ok(await fs.readFile(path.join(response.body.workspacePath, "workflow.json"), "utf8"));
  assert.ok(response.body.managedProject?.id);
});

test("workflow-json import blocks invalid JSON before scanning", async () => {
  const workspaceRoot = await tempWorkspace("botblade-workflow-invalid-");

  const response = await request("POST", "/api/imports/workflow-json", { workflowJson: "{not valid json", workspacePath: workspaceRoot });

  assert.equal(response.statusCode, 201);
  assert.equal(response.body.state, "blocked_by_policy");
  assert.equal(response.body.securityCards[0].violations[0].code, "WORKFLOW_JSON_INVALID");
  assert.equal(response.body.managedProject, undefined);
});

test("workflow-json import blocks oversized files before scanning", async () => {
  const sourceDir = await tempWorkspace("botblade-workflow-large-source-");
  const workspaceRoot = await tempWorkspace("botblade-workflow-large-import-");
  const workflowPath = path.join(sourceDir, "large.json");
  await fs.writeFile(workflowPath, `{"payload":"${"a".repeat(513 * 1024)}"}`, "utf8");

  const response = await request("POST", "/api/imports/workflow-json", { workflowPath, workspacePath: workspaceRoot });

  assert.equal(response.statusCode, 201);
  assert.equal(response.body.state, "blocked_by_policy");
  assert.equal(response.body.securityCards[0].violations[0].code, "WORKFLOW_JSON_TOO_LARGE");
});

test("template import blocks template IDs outside the local allowlist", async () => {
  const workspaceRoot = await tempWorkspace("botblade-template-blocked-");

  const response = await request("POST", "/api/imports/template", { templateId: "../../untrusted-template", workspacePath: workspaceRoot });

  assert.equal(response.statusCode, 201);
  assert.equal(response.body.state, "blocked_by_policy");
  assert.equal(response.body.securityCards[0].violations[0].code, "TEMPLATE_ID_NOT_ALLOWED");
  assert.equal(response.body.managedProject, undefined);
});
