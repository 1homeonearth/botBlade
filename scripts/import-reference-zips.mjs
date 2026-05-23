#!/usr/bin/env node
import { createHash } from "node:crypto";
import { existsSync, mkdirSync, rmSync } from "node:fs";
import { readdir, readFile } from "node:fs/promises";
import { basename, join, resolve } from "node:path";
import { spawnSync } from "node:child_process";

const repos = [
  {
    zip: "Acode-main.zip",
    sha256: "2b19580b63e3109672ad772873fbfdd5aa45f84ae6520fb1f24b97153321f846",
    root: "Acode-main",
    target: "vendor/upstream/acode",
  },
  {
    zip: "bots-master.zip",
    sha256: "6344dca7958029aa24cd78f8a9227a4339eba65670bced3347591778f796299e",
    root: "bots-master",
    target: "vendor/upstream/hackerkid-bots",
  },
  {
    zip: "jgit-master.zip",
    sha256: "df4f0543672fe579417cdbdaa49493d840333bca683a5dbf94bc5e4effbb33db",
    root: "jgit-master",
    target: "vendor/upstream/jgit",
  },
  {
    zip: "File-Manager-main.zip",
    sha256: "ed47178bd25a3d198658c2d0961dd36653069b25e23965010d3baaa16c385a7b",
    root: "File-Manager-main",
    target: "vendor/upstream/fossify-file-manager",
  },
];

const sourceDir = readArg("--source-dir") ?? process.cwd();
const force = process.argv.includes("--force");
const repoRoot = process.cwd();
const tempDir = join(repoRoot, ".tmp-reference-import");

mkdirSync(join(repoRoot, "vendor", "upstream"), { recursive: true });
if (existsSync(tempDir)) rmSync(tempDir, { recursive: true, force: true });
mkdirSync(tempDir, { recursive: true });

for (const repo of repos) {
  const zipPath = resolve(sourceDir, repo.zip);
  if (!existsSync(zipPath)) {
    throw new Error(`Missing ${repo.zip} in ${sourceDir}`);
  }

  const actualHash = createHash("sha256").update(await readFile(zipPath)).digest("hex");
  if (actualHash !== repo.sha256) {
    throw new Error(`${repo.zip} SHA-256 mismatch. Expected ${repo.sha256}, got ${actualHash}.`);
  }

  const extractDir = join(tempDir, basename(repo.zip, ".zip"));
  mkdirSync(extractDir, { recursive: true });
  const unzip = spawnSync("unzip", ["-q", zipPath, "-d", extractDir], { stdio: "inherit" });
  if (unzip.status !== 0) {
    throw new Error(`Failed to unzip ${repo.zip}`);
  }

  const rootPath = join(extractDir, repo.root);
  if (!existsSync(rootPath)) {
    const names = await readdir(extractDir);
    throw new Error(`Expected ${repo.root} inside ${repo.zip}; found ${names.join(", ")}`);
  }

  const targetPath = join(repoRoot, repo.target);
  if (existsSync(targetPath)) {
    if (!force) {
      throw new Error(`${repo.target} already exists. Re-run with --force to replace it.`);
    }
    rmSync(targetPath, { recursive: true, force: true });
  }

  mkdirSync(targetPath, { recursive: true });
  const copy = spawnSync("cp", ["-a", `${rootPath}/.`, targetPath], { stdio: "inherit" });
  if (copy.status !== 0) {
    throw new Error(`Failed to copy ${repo.root} to ${repo.target}`);
  }

  console.log(`Imported ${repo.zip} -> ${repo.target}`);
}

rmSync(tempDir, { recursive: true, force: true });

function readArg(name) {
  const index = process.argv.indexOf(name);
  if (index === -1) return undefined;
  return process.argv[index + 1];
}
