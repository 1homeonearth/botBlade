import { spawn } from "node:child_process";
import fs from "node:fs/promises";
import path from "node:path";
import { randomUUID } from "node:crypto";
import type { BotProject } from "../models/project.js";
import { redactSecrets } from "./redaction.js";
import { ProjectFileService } from "./projectFiles.js";
import { validateProject } from "./projectValidation.js";

export type BuildStatus = "queued" | "validating" | "installing" | "building" | "testing" | "packaging" | "succeeded" | "failed" | "canceled";

export interface BuildJob {
  buildId: string;
  projectId: string;
  status: BuildStatus;
  logUrl: string;
  auditEventId: string;
  source: string;
  clean: boolean;
  runTests: boolean;
  createDockerImage: boolean;
  startedAt: string;
  finishedAt: string | null;
  errorMessage: string | null;
}

export class BuildService {
  private readonly jobs = new Map<string, BuildJob[]>();
  private readonly logs = new Map<string, string>();

  constructor(private readonly files: ProjectFileService, private readonly secretExists: (secretId: string) => boolean) {}

  list(projectId: string): BuildJob[] {
    return [...(this.jobs.get(projectId) ?? [])].sort((a, b) => b.startedAt.localeCompare(a.startedAt));
  }

  get(projectId: string, buildId: string): BuildJob | undefined {
    return this.jobs.get(projectId)?.find((job) => job.buildId === buildId);
  }

  getLogs(projectId: string, buildId: string): string | undefined {
    if (!this.get(projectId, buildId)) return undefined;
    return this.logs.get(buildId) ?? "";
  }

  async create(project: BotProject, request: unknown): Promise<BuildJob> {
    const body = request && typeof request === "object" && !Array.isArray(request) ? request as Record<string, unknown> : {};
    const buildId = `build_${randomUUID()}`;
    const job: BuildJob = {
      buildId,
      projectId: project.id,
      status: "queued",
      logUrl: `/api/projects/${project.id}/builds/${buildId}/logs`,
      auditEventId: `audit_${randomUUID()}`,
      source: typeof body.source === "string" ? body.source : "current_project",
      clean: typeof body.clean === "boolean" ? body.clean : true,
      runTests: typeof body.runTests === "boolean" ? body.runTests : true,
      createDockerImage: typeof body.createDockerImage === "boolean" ? body.createDockerImage : false,
      startedAt: new Date().toISOString(),
      finishedAt: null,
      errorMessage: null,
    };
    this.jobs.set(project.id, [job, ...(this.jobs.get(project.id) ?? [])]);
    await this.run(project, job);
    return job;
  }

  private async run(project: BotProject, job: BuildJob): Promise<void> {
    const append = (line: string) => this.logs.set(job.buildId, `${this.logs.get(job.buildId) ?? ""}${redactSecrets(line)}\n`);
    try {
      job.status = "validating";
      const validation = validateProject(project, this.secretExists);
      if (!validation.valid) throw new Error(validation.errors.map((issue) => `${issue.field ?? "project"}: ${issue.message}`).join("; "));
      await this.files.ensureGenerated(project);
      const cwd = this.files.workspace(project.id);
      const cwdResolved = path.resolve(cwd);
      if (!cwdResolved.includes(`${path.sep}generated-projects${path.sep}`)) throw new Error("Refusing to build outside generated-projects workspace.");
      if (job.clean) await fs.rm(path.join(cwd, "dist"), { recursive: true, force: true });
      job.status = "installing";
      await runCommand("npm", [await fileExists(path.join(cwd, "package-lock.json")) ? "ci" : "install"], cwd, append);
      job.status = "building";
      await runCommand("npm", ["run", "build"], cwd, append);
      if (job.runTests && await hasTestScript(cwd)) {
        job.status = "testing";
        await runCommand("npm", ["test"], cwd, append);
      }
      job.status = "packaging";
      append("Packaging step completed (Docker image creation disabled for local phase).")
      job.status = "succeeded";
    } catch (error) {
      job.status = "failed";
      job.errorMessage = redactSecrets(error instanceof Error ? error.message : String(error));
      append(`Build failed: ${job.errorMessage}`);
    } finally {
      job.finishedAt = new Date().toISOString();
    }
  }
}

function runCommand(command: string, args: string[], cwd: string, append: (line: string) => void): Promise<void> {
  return new Promise((resolve, reject) => {
    append(`$ ${command} ${args.join(" ")}`);
    const child = spawn(command, args, { cwd, shell: false, env: { ...process.env } });
    const timeout = setTimeout(() => {
      child.kill("SIGTERM");
      reject(new Error(`${command} timed out.`));
    }, 120_000);
    child.stdout.on("data", (chunk) => append(String(chunk).trimEnd()));
    child.stderr.on("data", (chunk) => append(String(chunk).trimEnd()));
    child.on("error", (error) => { clearTimeout(timeout); reject(error); });
    child.on("close", (code) => {
      clearTimeout(timeout);
      if (code === 0) resolve(); else reject(new Error(`${command} exited with code ${code}.`));
    });
  });
}

async function fileExists(filePath: string): Promise<boolean> {
  return fs.access(filePath).then(() => true, () => false);
}

async function hasTestScript(cwd: string): Promise<boolean> {
  const pkg = JSON.parse(await fs.readFile(path.join(cwd, "package.json"), "utf8")) as { scripts?: Record<string, string> };
  return Boolean(pkg.scripts?.test);
}
