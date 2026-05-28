import assert from "node:assert/strict";
import test from "node:test";
import { AuditService } from "../services/auditService.js";

test("AuditService enforces max event retention and list limits", () => {
  const service = new AuditService(undefined, { maxEvents: 2 });

  service.record({ action: "project.create", projectId: "project_a", resourceType: "project", resourceId: "project_a", requestId: "req_1" });
  service.record({ action: "project.update", projectId: "project_a", resourceType: "project", resourceId: "project_a", requestId: "req_2" });
  service.record({ action: "project.archive", projectId: "project_a", resourceType: "project", resourceId: "project_a", requestId: "req_3" });

  const events = service.list("project_a");
  assert.equal(events.length, 2);
  assert.equal(events[0].requestId, "req_3");
  assert.equal(events[1].requestId, "req_2");
  assert.equal(service.list("project_a", { limit: 1 }).map((event) => event.requestId).join(","), "req_3");
});

test("AuditService redacts metadata before storage", () => {
  const service = new AuditService(undefined, { maxEvents: 5 });
  const event = service.record({
    action: "secret.create",
    projectId: "project_a",
    resourceType: "secret",
    resourceId: "secret_a",
    requestId: "req_secret",
    metadata: { token: "super-secret-token", nested: { password: "open-sesame" } },
  });

  assert.equal(event.metadata.token, "[REDACTED]");
  assert.equal(JSON.stringify(event.metadata.nested), JSON.stringify({ password: "[REDACTED]" }));
});
