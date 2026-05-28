import test from "node:test";
import assert from "node:assert/strict";
import fsPromises from "node:fs/promises";
import childProcess from "node:child_process";
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

async function createZipWithEntry(sizeBytes: number): Promise<string> {
  const dir = path.join(tmpdir(), `botblade-zip-generated-${randomUUID()}`);
  await fsPromises.mkdir(dir, { recursive: true });
  const zipPath = path.join(dir, "generated.zip");
  const script = `import zipfile, sys\nsize=int(sys.argv[2])\nwith zipfile.ZipFile(sys.argv[1], 'w', compression=zipfile.ZIP_DEFLATED) as z:\n z.writestr('payload.txt', b'A' * size)`;
  await new Promise<void>((resolve, reject) => {
    (childProcess as any).execFile("python3", ["-c", script, zipPath, String(sizeBytes)], (error: any) => {
      if (error) return reject(error);
      resolve();
    });
  });
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

test("zip validator fails closed with runtime-unavailable violation when python3 is missing", async () => {
  const originalPath = process.env.PATH;
  const zipPath = await materializeFixture("absolute");
  process.env.PATH = "";
  const result = await validateZipArchive(zipPath);
  process.env.PATH = originalPath;
  assert.equal(result.ok, false);
  assert.ok(result.violations.some((v) => v.code === "ZIP_RUNTIME_UNAVAILABLE"));
});

test("zip validator enforces total uncompressed size limit", async () => {
  const zipPath = await createZipWithEntry(1024 * 1024);
  const result = await validateZipArchive(zipPath, {
    maxArchiveBytes: 20 * 1024 * 1024,
    maxEntryBytes: 5 * 1024 * 1024,
    maxFileCount: 500,
    maxTotalUncompressedBytes: 256 * 1024
  });
  assert.equal(result.ok, false);
  assert.ok(result.violations.some((v) => v.code === "TOTAL_UNCOMPRESSED_TOO_LARGE"));
});
