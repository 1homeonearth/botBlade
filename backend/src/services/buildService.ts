// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import fs from "node:fs/promises";  // line 7: executes this statement as part of this file's behavior
import path from "node:path";  // line 8: executes this statement as part of this file's behavior
import { randomUUID } from "node:crypto";  // line 9: executes this statement as part of this file's behavior
import { spawn } from "node:child_process";  // line 10: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 11: executes this statement as part of this file's behavior
import { redactSecrets } from "./redaction.js";  // line 12: executes this statement as part of this file's behavior
import { ProjectFileService } from "./projectFiles.js";  // line 13: executes this statement as part of this file's behavior
import { validateProject } from "./projectValidation.js";  // line 14: executes this statement as part of this file's behavior
import type { AuditAction } from "./auditService.js";  // line 15: executes this statement as part of this file's behavior
import type { BuildServicePersistence } from "./persistence.js";  // line 16: executes this statement as part of this file's behavior

export type BuildStatus = "queued" | "validating" | "installing" | "building" | "testing" | "packaging" | "succeeded" | "failed" | "canceled";  // line 18: executes this statement as part of this file's behavior

type LogAppender = (line: string) => void;  // line 20: executes this statement as part of this file's behavior
export type CommandRunner = (command: string, args: string[], cwd: string, append: LogAppender) => Promise<void>;  // line 21: executes this statement as part of this file's behavior

export interface BuildJob {  // line 23: executes this statement as part of this file's behavior
  buildId: string;  // line 24: executes this statement as part of this file's behavior
  projectId: string;  // line 25: executes this statement as part of this file's behavior
  status: BuildStatus;  // line 26: executes this statement as part of this file's behavior
  logUrl: string;  // line 27: executes this statement as part of this file's behavior
  auditEventId: string;  // line 28: executes this statement as part of this file's behavior
  source: string;  // line 29: executes this statement as part of this file's behavior
  clean: boolean;  // line 30: executes this statement as part of this file's behavior
  runTests: boolean;  // line 31: executes this statement as part of this file's behavior
  createDockerImage: boolean;  // line 32: executes this statement as part of this file's behavior
  artifactPath: string | null;  // line 33: executes this statement as part of this file's behavior
  startedAt: string;  // line 34: executes this statement as part of this file's behavior
  finishedAt: string | null;  // line 35: executes this statement as part of this file's behavior
  errorMessage: string | null;  // line 36: executes this statement as part of this file's behavior
}  // line 37: executes this statement as part of this file's behavior

export class BuildService {  // line 39: executes this statement as part of this file's behavior
  private static readonly MAX_LOG_BYTES = 256 * 1024;  // line 40: executes this statement as part of this file's behavior
  private readonly jobs = new Map<string, BuildJob[]>();  // line 41: executes this statement as part of this file's behavior
  private readonly logs = new Map<string, string>();  // line 42: executes this statement as part of this file's behavior

  constructor(private readonly files: ProjectFileService, private readonly secretExists: (secretId: string) => boolean, private readonly audit?: (event: { action: AuditAction; projectId: string; resourceType: string; resourceId: string; metadata: Record<string, unknown>; requestId: string; actorId?: string }) => void, private readonly commandRunner: CommandRunner = runCommand, private readonly persistence?: BuildServicePersistence) {  // line 44: executes this statement as part of this file's behavior
    for (const { job, logs } of persistence?.loadBuildJobs() ?? []) {  // line 45: executes this statement as part of this file's behavior
      this.jobs.set(job.projectId, [job, ...(this.jobs.get(job.projectId) ?? [])]);  // line 46: executes this statement as part of this file's behavior
      this.logs.set(job.buildId, logs);  // line 47: executes this statement as part of this file's behavior
    }  // line 48: executes this statement as part of this file's behavior
  }  // line 49: executes this statement as part of this file's behavior

  list(projectId: string): BuildJob[] {  // line 51: executes this statement as part of this file's behavior
    return [...(this.jobs.get(projectId) ?? [])].sort((a, b) => b.startedAt.localeCompare(a.startedAt));  // line 52: executes this statement as part of this file's behavior
  }  // line 53: executes this statement as part of this file's behavior

  get(projectId: string, buildId: string): BuildJob | undefined {  // line 55: executes this statement as part of this file's behavior
    return this.jobs.get(projectId)?.find((job) => job.buildId === buildId);  // line 56: executes this statement as part of this file's behavior
  }  // line 57: executes this statement as part of this file's behavior

  getLogs(projectId: string, buildId: string): string | undefined {  // line 59: executes this statement as part of this file's behavior
    if (!this.get(projectId, buildId)) return undefined;  // line 60: executes this statement as part of this file's behavior
    return this.logs.get(buildId) ?? "";  // line 61: executes this statement as part of this file's behavior
  }  // line 62: executes this statement as part of this file's behavior

  async create(project: BotProject, request: unknown, auditEventId = `audit_${randomUUID()}`, requestId = "system", actorId?: string): Promise<BuildJob> {  // line 64: executes this statement as part of this file's behavior
    const body = request && typeof request === "object" && !Array.isArray(request) ? request as Record<string, unknown> : {};  // line 65: executes this statement as part of this file's behavior
    const buildId = `build_${randomUUID()}`;  // line 66: executes this statement as part of this file's behavior
    const job: BuildJob = {  // line 67: executes this statement as part of this file's behavior
      buildId,  // line 68: executes this statement as part of this file's behavior
      projectId: project.id,  // line 69: executes this statement as part of this file's behavior
      status: "queued",  // line 70: executes this statement as part of this file's behavior
      logUrl: `/api/projects/${project.id}/builds/${buildId}/logs`,  // line 71: executes this statement as part of this file's behavior
      auditEventId,  // line 72: executes this statement as part of this file's behavior
      source: typeof body.source === "string" ? body.source : "current_project",  // line 73: executes this statement as part of this file's behavior
      clean: typeof body.clean === "boolean" ? body.clean : true,  // line 74: executes this statement as part of this file's behavior
      runTests: typeof body.runTests === "boolean" ? body.runTests : true,  // line 75: executes this statement as part of this file's behavior
      createDockerImage: typeof body.createDockerImage === "boolean" ? body.createDockerImage : false,  // line 76: executes this statement as part of this file's behavior
      artifactPath: null,  // line 77: executes this statement as part of this file's behavior
      startedAt: new Date().toISOString(),  // line 78: executes this statement as part of this file's behavior
      finishedAt: null,  // line 79: executes this statement as part of this file's behavior
      errorMessage: null,  // line 80: executes this statement as part of this file's behavior
    };  // line 81: executes this statement as part of this file's behavior
    this.jobs.set(project.id, [job, ...(this.jobs.get(project.id) ?? [])]);  // line 82: executes this statement as part of this file's behavior
    this.persist(job);  // line 83: executes this statement as part of this file's behavior
    await this.run(project, job, requestId, actorId);  // line 84: executes this statement as part of this file's behavior
    return job;  // line 85: executes this statement as part of this file's behavior
  }  // line 86: executes this statement as part of this file's behavior

  private async run(project: BotProject, job: BuildJob, requestId: string, actorId?: string): Promise<void> {  // line 88: executes this statement as part of this file's behavior
    const append = (line: string) => {  // line 89: executes this statement as part of this file's behavior
      const next = `${this.logs.get(job.buildId) ?? ""}${redactSecrets(sanitizeUrlsInText(line))}\n`;  // line 90: executes this statement as part of this file's behavior
      this.logs.set(job.buildId, next.length > BuildService.MAX_LOG_BYTES ? `[truncated]\n${next.slice(-BuildService.MAX_LOG_BYTES)}` : next);  // line 91: executes this statement as part of this file's behavior
      this.persist(job);  // line 92: executes this statement as part of this file's behavior
    };  // line 93: executes this statement as part of this file's behavior
    try {  // line 94: executes this statement as part of this file's behavior
      job.status = "validating";  // line 95: executes this statement as part of this file's behavior
      const validation = validateProject(project, this.secretExists);  // line 96: executes this statement as part of this file's behavior
      if (!validation.valid) throw new Error(validation.errors.map((issue) => `${issue.field ?? "project"}: ${issue.message}`).join("; "));  // line 97: executes this statement as part of this file's behavior
      await this.files.ensureGenerated(project);  // line 98: executes this statement as part of this file's behavior
      const { root, workspace: cwd } = this.files.resolveWorkspace(project.id);  // line 99: executes this statement as part of this file's behavior
      const relativeWorkspace = path.relative(root, cwd);  // line 100: executes this statement as part of this file's behavior
      if (relativeWorkspace.startsWith("..") || path.isAbsolute(relativeWorkspace)) throw new Error("Refusing to build outside generated-projects workspace.");  // line 101: executes this statement as part of this file's behavior
      if (job.clean) await fs.rm(path.join(cwd, "dist"), { recursive: true, force: true });  // line 102: executes this statement as part of this file's behavior
      job.status = "installing";  // line 103: executes this statement as part of this file's behavior
      const installArgs = [await fileExists(path.join(cwd, "package-lock.json")) ? "ci" : "install"];  // line 104: executes this statement as part of this file's behavior
      try {  // line 105: executes this statement as part of this file's behavior
        await this.commandRunner("npm", installArgs, cwd, append);  // line 106: executes this statement as part of this file's behavior
      } catch (error) {  // line 107: executes this statement as part of this file's behavior
        await appendNpmInstallDiagnostics(cwd, error, append);  // line 108: executes this statement as part of this file's behavior
        throw error;  // line 109: executes this statement as part of this file's behavior
      }  // line 110: executes this statement as part of this file's behavior
      job.status = "building";  // line 111: executes this statement as part of this file's behavior
      await this.commandRunner("npm", ["run", "build"], cwd, append);  // line 112: executes this statement as part of this file's behavior
      if (job.runTests && await hasTestScript(cwd)) {  // line 113: executes this statement as part of this file's behavior
        job.status = "testing";  // line 114: executes this statement as part of this file's behavior
        await this.commandRunner("npm", ["test"], cwd, append);  // line 115: executes this statement as part of this file's behavior
      }  // line 116: executes this statement as part of this file's behavior
      job.status = "packaging";  // line 117: executes this statement as part of this file's behavior
      const artifactDir = path.join(cwd, "artifacts");  // line 118: executes this statement as part of this file's behavior
      await fs.mkdir(artifactDir, { recursive: true });  // line 119: executes this statement as part of this file's behavior
      const artifactPath = path.join(artifactDir, `${job.buildId}.tgz`);  // line 120: executes this statement as part of this file's behavior
      await this.commandRunner("tar", ["--exclude", "node_modules", "--exclude", "artifacts", "-czf", artifactPath, "."], cwd, append);  // line 121: executes this statement as part of this file's behavior
      job.artifactPath = artifactPath;  // line 122: executes this statement as part of this file's behavior
      append(`Packaged build artifact at ${artifactPath}.`);  // line 123: executes this statement as part of this file's behavior
      job.status = "succeeded";  // line 124: executes this statement as part of this file's behavior
    } catch (error) {  // line 125: executes this statement as part of this file's behavior
      job.status = "failed";  // line 126: executes this statement as part of this file's behavior
      job.errorMessage = redactSecrets(sanitizeUrlsInText(error instanceof Error ? error.message : String(error)));  // line 127: executes this statement as part of this file's behavior
      append(`Build failed: ${job.errorMessage}`);  // line 128: executes this statement as part of this file's behavior
    } finally {  // line 129: executes this statement as part of this file's behavior
      job.finishedAt = new Date().toISOString();  // line 130: executes this statement as part of this file's behavior
      this.persist(job);  // line 131: executes this statement as part of this file's behavior
      this.audit?.({  // line 132: executes this statement as part of this file's behavior
        action: job.status === "succeeded" ? "build.succeeded" : "build.failed",  // line 133: executes this statement as part of this file's behavior
        projectId: project.id,  // line 134: executes this statement as part of this file's behavior
        resourceType: "build",  // line 135: executes this statement as part of this file's behavior
        resourceId: job.buildId,  // line 136: executes this statement as part of this file's behavior
        metadata: { status: job.status, source: job.source, runTests: job.runTests, createDockerImage: job.createDockerImage, errorMessage: job.errorMessage },  // line 137: executes this statement as part of this file's behavior
        requestId,  // line 138: executes this statement as part of this file's behavior
        actorId,  // line 139: executes this statement as part of this file's behavior
      });  // line 140: executes this statement as part of this file's behavior
    }  // line 141: executes this statement as part of this file's behavior
  }  // line 142: executes this statement as part of this file's behavior

  private persist(job: BuildJob): void {  // line 144: executes this statement as part of this file's behavior
    this.persistence?.saveBuildJob(job, this.logs.get(job.buildId) ?? "");  // line 145: executes this statement as part of this file's behavior
  }  // line 146: executes this statement as part of this file's behavior
}  // line 147: executes this statement as part of this file's behavior

class CommandExecutionError extends Error {  // line 149: executes this statement as part of this file's behavior
  constructor(message: string, public readonly output: string) {  // line 150: executes this statement as part of this file's behavior
    super(message);  // line 151: executes this statement as part of this file's behavior
    this.name = "CommandExecutionError";  // line 152: executes this statement as part of this file's behavior
  }  // line 153: executes this statement as part of this file's behavior
}  // line 154: executes this statement as part of this file's behavior

export function runCommand(command: string, args: string[], cwd: string, append: LogAppender): Promise<void> {  // line 156: executes this statement as part of this file's behavior
  return new Promise((resolve, reject) => {  // line 157: executes this statement as part of this file's behavior
    let output = "";  // line 158: executes this statement as part of this file's behavior
    const capture = (line: string) => {  // line 159: executes this statement as part of this file's behavior
      output += `${line}\n`;  // line 160: executes this statement as part of this file's behavior
      append(line);  // line 161: executes this statement as part of this file's behavior
    };  // line 162: executes this statement as part of this file's behavior
    capture(`$ ${command} ${args.join(" ")}`);  // line 163: executes this statement as part of this file's behavior
    const child = spawn(command, args, { cwd, shell: false, env: allowedEnv() });  // line 164: executes this statement as part of this file's behavior
    const timeout = setTimeout(() => {  // line 165: executes this statement as part of this file's behavior
      child.kill("SIGTERM");  // line 166: executes this statement as part of this file's behavior
      reject(new CommandExecutionError(`${command} timed out.`, output));  // line 167: executes this statement as part of this file's behavior
    }, 120_000);  // line 168: executes this statement as part of this file's behavior
    child.stdout.on("data", (chunk) => capture(String(chunk).trimEnd()));  // line 169: executes this statement as part of this file's behavior
    child.stderr.on("data", (chunk) => capture(String(chunk).trimEnd()));  // line 170: executes this statement as part of this file's behavior
    child.on("error", (error) => { clearTimeout(timeout); reject(new CommandExecutionError(error.message, output)); });  // line 171: executes this statement as part of this file's behavior
    child.on("close", (code) => {  // line 172: executes this statement as part of this file's behavior
      clearTimeout(timeout);  // line 173: executes this statement as part of this file's behavior
      if (code === 0) resolve(); else reject(new CommandExecutionError(`${command} exited with code ${code}.`, output));  // line 174: executes this statement as part of this file's behavior
    });  // line 175: executes this statement as part of this file's behavior
  });  // line 176: executes this statement as part of this file's behavior
}  // line 177: executes this statement as part of this file's behavior

function allowedEnv(): Record<string, string> {  // line 179: executes this statement as part of this file's behavior
  const keys = ["PATH", "HOME", "TMPDIR", "TEMP", "TMP", "NPM_CONFIG_REGISTRY", "npm_config_registry", "CI"];  // line 180: executes this statement as part of this file's behavior
  const env: Record<string, string> = {};  // line 181: executes this statement as part of this file's behavior
  for (const key of keys) if (process.env[key]) env[key] = process.env[key] as string;  // line 182: executes this statement as part of this file's behavior
  return env;  // line 183: executes this statement as part of this file's behavior
}  // line 184: executes this statement as part of this file's behavior

async function appendNpmInstallDiagnostics(cwd: string, error: unknown, append: LogAppender): Promise<void> {  // line 186: executes this statement as part of this file's behavior
  const output = error instanceof CommandExecutionError ? error.output : error instanceof Error ? error.message : String(error);  // line 187: executes this statement as part of this file's behavior
  const status = output.match(/\b([1-5][0-9]{2})\b/)?.[1] ?? output.match(/\b(E[0-9]{3})\b/)?.[1] ?? "unknown";  // line 188: executes this statement as part of this file's behavior
  const url = output.match(/https?:\/\/[^\s)]+/)?.[0] ?? "unknown";  // line 189: executes this statement as part of this file's behavior
  const registry = await configuredRegistry(cwd);  // line 190: executes this statement as part of this file's behavior
  append(`npm install registry diagnostics: registry=${registry}; status=${status}; url=${sanitizeUrl(url)}`);  // line 191: executes this statement as part of this file's behavior
}  // line 192: executes this statement as part of this file's behavior

async function configuredRegistry(cwd: string): Promise<string> {  // line 194: executes this statement as part of this file's behavior
  const envRegistry = process.env.NPM_CONFIG_REGISTRY ?? process.env.npm_config_registry;  // line 195: executes this statement as part of this file's behavior
  if (envRegistry) return sanitizeUrl(envRegistry);  // line 196: executes this statement as part of this file's behavior
  const npmrc = await fs.readFile(path.join(cwd, ".npmrc"), "utf8").catch(() => "");  // line 197: executes this statement as part of this file's behavior
  const registryLine = npmrc.split(/\r?\n/).find((line) => line.trim().startsWith("registry="));  // line 198: executes this statement as part of this file's behavior
  if (registryLine) return sanitizeUrl(registryLine.split("=").slice(1).join("=").trim());  // line 199: executes this statement as part of this file's behavior
  return "https://registry.npmjs.org/";  // line 200: executes this statement as part of this file's behavior
}  // line 201: executes this statement as part of this file's behavior

function sanitizeUrl(value: string): string {  // line 203: executes this statement as part of this file's behavior
  return value.replace(/^(https?:\/\/)([^\s/@:]+):([^\s/@]+)@/i, "$1[REDACTED_CREDENTIALS]@").replace(/\?.*$/, "?[REDACTED_QUERY]");  // line 204: executes this statement as part of this file's behavior
}  // line 205: executes this statement as part of this file's behavior

function sanitizeUrlsInText(value: string): string {  // line 207: executes this statement as part of this file's behavior
  return value.replace(/https?:\/\/[^\s)]+/g, (url) => sanitizeUrl(url));  // line 208: executes this statement as part of this file's behavior
}  // line 209: executes this statement as part of this file's behavior

async function fileExists(filePath: string): Promise<boolean> {  // line 211: executes this statement as part of this file's behavior
  return fs.access(filePath).then(() => true, () => false);  // line 212: executes this statement as part of this file's behavior
}  // line 213: executes this statement as part of this file's behavior

async function hasTestScript(cwd: string): Promise<boolean> {  // line 215: executes this statement as part of this file's behavior
  const pkg = JSON.parse(await fs.readFile(path.join(cwd, "package.json"), "utf8")) as { scripts?: Record<string, string> };  // line 216: executes this statement as part of this file's behavior
  return Boolean(pkg.scripts?.test);  // line 217: executes this statement as part of this file's behavior
}  // line 218: executes this statement as part of this file's behavior
