import test from "node:test";
import assert from "node:assert/strict";
import fsPromises from "node:fs/promises";
import { randomUUID } from "node:crypto";
import path from "node:path";
import { tmpdir } from "node:os";
import { validateZipArchive } from "../services/imports/zipSecurity.js";

async function materializeFixture(name: string): Promise<string> {
  const b64 = await fsPromises.readFile(path.resolve("src/__tests__/fixtures/zips", `${name}.zip.b64`), "utf8");
  const dir = path.join(tmpdir(), `botblade-zip-fixture-${randomUUID()}`);
  await fsPromises.mkdir(dir, { recursive: true });
  const zipPath = path.join(dir, `${name}.zip`);
  await fsPromises.writeFile(zipPath, Buffer.from(b64.trim(), "base64") as unknown as string);
  return zipPath;
}

test("zip validator rejects traversal payload deterministically", async () => {
  const zipPath = await materializeFixture("traversal");
  const result = await validateZipArchive(zipPath);
  assert.equal(result.ok, false);
  assert.equal(result.violations[0]?.code, "PATH_TRAVERSAL");
});

test("zip validator rejects absolute-path payload deterministically", async () => {
  const zipPath = await materializeFixture("absolute");
  const result = await validateZipArchive(zipPath);
  assert.equal(result.ok, false);
  assert.equal(result.violations[0]?.code, "ABSOLUTE_PATH");
});

test("zip validator rejects symlink metadata deterministically", async () => {
  const zipPath = await materializeFixture("symlink");
  const result = await validateZipArchive(zipPath);
  assert.equal(result.ok, false);
  assert.ok(result.violations.some((v) => v.code === "SYMLINK_ENTRY"));
});
