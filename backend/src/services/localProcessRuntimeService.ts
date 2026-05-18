import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import { randomUUID } from "node:crypto";
import type { BotProject } from "../models/project.js";
import { ProjectFileService } from "./projectFiles.js";
import { redactSecrets } from "./redaction.js";

export interface RuntimeStatus {
  projectId: string;
  status: "stopped" | "starting" | "running" | "stopping" | "crashed" | "unknown";
  running: boolean;
  pid: number | null;
  startedAt: string | null;
  lastExitCode: number | null;
  message: string;
}

interface RuntimeRecord {
  child: ChildProcessWithoutNullStreams | null;
  status: RuntimeStatus;
  logs: string;
}

export class LocalProcessRuntimeService {
  private readonly runtimes = new Map<string, RuntimeRecord>();

  constructor(private readonly files: ProjectFileService, private readonly getSecretValue: (secretId: string) => string | undefined) {}

  getStatus(projectId: string): RuntimeStatus {
    return this.record(projectId).status;
  }

  getLogs(projectId: string): string {
    return this.record(projectId).logs;
  }

  async start(project: BotProject): Promise<RuntimeStatus> {
    const record = this.record(project.id);
    if (record.child && !record.child.killed) return record.status;
    if (!await this.files.hasGenerated(project.id)) throw structuredError("PROJECT_NOT_GENERATED", "Generate project files before starting the runtime.");
    if (!project.discord.tokenSecretRef) throw structuredError("MISSING_DISCORD_TOKEN_SECRET", "Project discord.tokenSecretRef is required before starting the runtime.");
    const token = this.getSecretValue(project.discord.tokenSecretRef);
    if (!token) throw structuredError("MISSING_DISCORD_TOKEN_SECRET", "The configured Discord token secret reference does not exist.");
    const cwd = this.files.workspace(project.id);
    record.status = { ...record.status, status: "starting", running: false, message: "Bot runtime is starting." };
    this.append(record, "Starting generated bot runtime.");
    const child = spawn("npm", ["start"], { cwd, shell: false, env: { ...process.env, DISCORD_TOKEN: token } });
    record.child = child;
    record.status = { projectId: project.id, status: "running", running: true, pid: child.pid ?? null, startedAt: new Date().toISOString(), lastExitCode: null, message: "Bot runtime is running." };
    child.stdout.on("data", (chunk) => this.append(record, String(chunk).trimEnd()));
    child.stderr.on("data", (chunk) => this.append(record, String(chunk).trimEnd()));
    child.on("close", (code) => {
      record.child = null;
      record.status = { ...record.status, status: code === 0 ? "stopped" : "crashed", running: false, pid: null, lastExitCode: code, message: code === 0 ? "Bot runtime stopped." : "Bot runtime crashed." };
      this.append(record, `Runtime exited with code ${code}.`);
    });
    child.on("error", (error) => {
      record.status = { ...record.status, status: "crashed", running: false, pid: null, message: redactSecrets(error.message) };
      this.append(record, `Runtime error: ${error.message}`);
    });
    return record.status;
  }

  async stop(projectId: string): Promise<RuntimeStatus> {
    const record = this.record(projectId);
    if (!record.child) {
      record.status = { ...record.status, status: "stopped", running: false, pid: null, message: "Bot runtime is stopped." };
      return record.status;
    }
    record.status = { ...record.status, status: "stopping", running: true, message: "Bot runtime is stopping." };
    record.child.kill("SIGTERM");
    record.child = null;
    record.status = { ...record.status, status: "stopped", running: false, pid: null, message: "Bot runtime is stopped." };
    this.append(record, "Runtime stop requested.");
    return record.status;
  }

  async restart(project: BotProject): Promise<RuntimeStatus> {
    await this.stop(project.id);
    return this.start(project);
  }

  private record(projectId: string): RuntimeRecord {
    const existing = this.runtimes.get(projectId);
    if (existing) return existing;
    const created: RuntimeRecord = { child: null, logs: "", status: { projectId, status: "stopped", running: false, pid: null, startedAt: null, lastExitCode: null, message: "Bot runtime is stopped." } };
    this.runtimes.set(projectId, created);
    return created;
  }

  private append(record: RuntimeRecord, line: string): void {
    record.logs += `${redactSecrets(line)}\n`;
  }
}

function structuredError(code: string, message: string): never {
  throw { statusCode: 400, code, message, details: {} };
}
