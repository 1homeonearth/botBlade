import fs from "node:fs/promises";
import path from "node:path";
import { randomUUID } from "node:crypto";
import { spawn } from "node:child_process";
import type { BotProject } from "../models/project.js";
import { redactSecrets } from "./redaction.js";
import { ProjectFileService } from "./projectFiles.js";
import { validateProject } from "./projectValidation.js";
import type { AuditAction } from "./auditService.js";

export type BuildStatus = "queued" | "validating" | "installing" | "building" | "testing" | "packaging" | "succeeded" | "failed" | "canceled";

type LogAppender = (line: string) => void;
export type CommandRunner = (command: string, args: string[], cwd: string, append: LogAppender) => Promise<void>;

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

  constructor(private readonly files: ProjectFileService, private readonly secretExists: (secretId: string) => boolean, private readonly audit?: (event: { action: AuditAction; projectId: string; resourceType: string; resourceId: string; metadata: Record<string, unknown>; requestId: string; actorId?: string }) => void, private readonly commandRunner: CommandRunner = runCommand) {}

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

  async create(project: BotProject, request: unknown, auditEventId = `audit_${randomUUID()}`, requestId = "system", actorId?: string): Promise<BuildJob> {
    const body = request && typeof request === "object" && !Array.isArray(request) ? request as Record<string, unknown> : {};
    const buildId = `build_${randomUUID()}`;
    const job: BuildJob = {
      buildId,
      projectId: project.id,
      status: "queued",
      logUrl: `/api/projects/${project.id}/builds/${buildId}/logs`,
      auditEventId,
      source: typeof body.source === "string" ? body.source : "current_project",
      clean: typeof body.clean === "boolean" ? body.clean : true,
      runTests: typeof body.runTests === "boolean" ? body.runTests : true,
      createDockerImage: typeof body.createDockerImage === "boolean" ? body.createDockerImage : false,
      startedAt: new Date().toISOString(),
      finishedAt: null,
      errorMessage: null,
    };
    this.jobs.set(project.id, [job, ...(this.jobs.get(project.id) ?? [])]);
    await this.run(project, job, requestId, actorId);
    return job;
  }

  private async run(project: BotProject, job: BuildJob, requestId: string, actorId?: string): Promise<void> {
    const append = (line: string) => this.logs.set(job.buildId, `${this.logs.get(job.buildId) ?? ""}${redactSecrets(sanitizeUrlsInText(line))}\n`);
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
      const installArgs = [await fileExists(path.join(cwd, "package-lock.json")) ? "ci" : "install"];
      try {
        await this.commandRunner("npm", installArgs, cwd, append);
      } catch (error) {
        await appendNpmInstallDiagnostics(cwd, error, append);
        throw error;
      }
      job.status = "building";
      await this.commandRunner("npm", ["run", "build"], cwd, append);
      if (job.runTests && await hasTestScript(cwd)) {
        job.status = "testing";
        await this.commandRunner("npm", ["test"], cwd, append);
      }
      job.status = "packaging";
      append("Packaging step completed (Docker image creation disabled for local phase).")
      job.status = "succeeded";
    } catch (error) {
      job.status = "failed";
      job.errorMessage = redactSecrets(sanitizeUrlsInText(error instanceof Error ? error.message : String(error)));
      append(`Build failed: ${job.errorMessage}`);
    } finally {
      job.finishedAt = new Date().toISOString();
      this.audit?.({
        action: job.status === "succeeded" ? "build.succeeded" : "build.failed",
        projectId: project.id,
        resourceType: "build",
        resourceId: job.buildId,
        metadata: { status: job.status, source: job.source, runTests: job.runTests, createDockerImage: job.createDockerImage, errorMessage: job.errorMessage },
        requestId,
        actorId,
      });
    }
  }
}

class CommandExecutionError extends Error {
  constructor(message: string, public readonly output: string) {
    super(message);
    this.name = "CommandExecutionError";
  }
}

function runCommand(command: string, args: string[], cwd: string, append: LogAppender): Promise<void> {
  return new Promise((resolve, reject) => {
    let output = "";
    const capture = (line: string) => {
      output += `${line}\n`;
      append(line);
    };
    capture(`$ ${command} ${args.join(" ")}`);
    const child = spawn(command, args, { cwd, shell: false, env: { ...process.env } });
    const timeout = setTimeout(() => {
      child.kill("SIGTERM");
      reject(new CommandExecutionError(`${command} timed out.`, output));
    }, 120_000);
    child.stdout.on("data", (chunk) => capture(String(chunk).trimEnd()));
    child.stderr.on("data", (chunk) => capture(String(chunk).trimEnd()));
    child.on("error", (error) => { clearTimeout(timeout); reject(new CommandExecutionError(error.message, output)); });
    child.on("close", (code) => {
      clearTimeout(timeout);
      if (code === 0) resolve(); else reject(new CommandExecutionError(`${command} exited with code ${code}.`, output));
    });
  });
}

async function appendNpmInstallDiagnostics(cwd: string, error: unknown, append: LogAppender): Promise<void> {
  const output = error instanceof CommandExecutionError ? error.output : error instanceof Error ? error.message : String(error);
  const status = output.match(/\b([1-5][0-9]{2})\b/)?.[1] ?? output.match(/\b(E[0-9]{3})\b/)?.[1] ?? "unknown";
  const url = output.match(/https?:\/\/[^\s)]+/)?.[0] ?? "unknown";
  const registry = await configuredRegistry(cwd);
  append(`npm install registry diagnostics: registry=${registry}; status=${status}; url=${sanitizeUrl(url)}`);
}

async function configuredRegistry(cwd: string): Promise<string> {
  const envRegistry = process.env.NPM_CONFIG_REGISTRY ?? process.env.npm_config_registry;
  if (envRegistry) return sanitizeUrl(envRegistry);
  const npmrc = await fs.readFile(path.join(cwd, ".npmrc"), "utf8").catch(() => "");
  const registryLine = npmrc.split(/\r?\n/).find((line) => line.trim().startsWith("registry="));
  if (registryLine) return sanitizeUrl(registryLine.split("=").slice(1).join("=").trim());
  return "https://registry.npmjs.org/";
}

function sanitizeUrl(value: string): string {
  return value.replace(/^(https?:\/\/)([^\s/@:]+):([^\s/@]+)@/i, "$1[REDACTED_CREDENTIALS]@").replace(/\?.*$/, "?[REDACTED_QUERY]");
}

function sanitizeUrlsInText(value: string): string {
  return value.replace(/https?:\/\/[^\s)]+/g, (url) => sanitizeUrl(url));
}

async function fileExists(filePath: string): Promise<boolean> {
  return fs.access(filePath).then(() => true, () => false);
}

async function hasTestScript(cwd: string): Promise<boolean> {
  const pkg = JSON.parse(await fs.readFile(path.join(cwd, "package.json"), "utf8")) as { scripts?: Record<string, string> };
  return Boolean(pkg.scripts?.test);
}
