// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";  // line 7: executes this statement as part of this file's behavior
import { randomUUID } from "node:crypto";  // line 8: executes this statement as part of this file's behavior
import type { BotProject } from "../models/project.js";  // line 9: executes this statement as part of this file's behavior
import { ProjectFileService } from "./projectFiles.js";  // line 10: executes this statement as part of this file's behavior
import { redactSecrets } from "./redaction.js";  // line 11: executes this statement as part of this file's behavior

export interface RuntimeStatus {  // line 13: executes this statement as part of this file's behavior
  projectId: string;  // line 14: executes this statement as part of this file's behavior
  status: "stopped" | "starting" | "running" | "stopping" | "crashed" | "unknown";  // line 15: executes this statement as part of this file's behavior
  running: boolean;  // line 16: executes this statement as part of this file's behavior
  pid: number | null;  // line 17: executes this statement as part of this file's behavior
  startedAt: string | null;  // line 18: executes this statement as part of this file's behavior
  lastExitCode: number | null;  // line 19: executes this statement as part of this file's behavior
  message: string;  // line 20: executes this statement as part of this file's behavior
}  // line 21: executes this statement as part of this file's behavior

interface RuntimeRecord {  // line 23: executes this statement as part of this file's behavior
  child: ChildProcessWithoutNullStreams | null;  // line 24: executes this statement as part of this file's behavior
  status: RuntimeStatus;  // line 25: executes this statement as part of this file's behavior
  logs: string;  // line 26: executes this statement as part of this file's behavior
}  // line 27: executes this statement as part of this file's behavior

export class LocalProcessRuntimeService {  // line 29: executes this statement as part of this file's behavior
  private readonly runtimes = new Map<string, RuntimeRecord>();  // line 30: executes this statement as part of this file's behavior
  private static readonly MAX_LOG_BYTES = 256 * 1024;  // line 31: executes this statement as part of this file's behavior

  constructor(private readonly files: ProjectFileService, private readonly getSecretValue: (secretId: string) => string | undefined) {}  // line 33: executes this statement as part of this file's behavior

  getStatus(projectId: string): RuntimeStatus {  // line 35: executes this statement as part of this file's behavior
    return this.record(projectId).status;  // line 36: executes this statement as part of this file's behavior
  }  // line 37: executes this statement as part of this file's behavior

  getLogs(projectId: string): string {  // line 39: executes this statement as part of this file's behavior
    return this.record(projectId).logs;  // line 40: executes this statement as part of this file's behavior
  }  // line 41: executes this statement as part of this file's behavior

  async start(project: BotProject): Promise<RuntimeStatus> {  // line 43: executes this statement as part of this file's behavior
    const record = this.record(project.id);  // line 44: executes this statement as part of this file's behavior
    if (record.child && !record.child.killed) return record.status;  // line 45: executes this statement as part of this file's behavior
    if (!await this.files.hasGenerated(project.id)) throw structuredError("PROJECT_NOT_GENERATED", "Generate project files before starting the runtime.");  // line 46: executes this statement as part of this file's behavior
    if (!project.discord.tokenSecretRef) throw structuredError("MISSING_DISCORD_TOKEN_SECRET", "Project discord.tokenSecretRef is required before starting the runtime.");  // line 47: executes this statement as part of this file's behavior
    const token = this.getSecretValue(project.discord.tokenSecretRef);  // line 48: executes this statement as part of this file's behavior
    if (!token) throw structuredError("MISSING_DISCORD_TOKEN_SECRET", "The configured Discord token secret reference does not exist.");  // line 49: executes this statement as part of this file's behavior
    const cwd = this.files.workspace(project.id);  // line 50: executes this statement as part of this file's behavior
    record.status = { ...record.status, status: "starting", running: false, message: "Bot runtime is starting." };  // line 51: executes this statement as part of this file's behavior
    this.append(record, "Starting generated bot runtime.");  // line 52: executes this statement as part of this file's behavior
    const child = spawn("npm", ["start"], { cwd, shell: false, env: { ...allowedEnv(), DISCORD_TOKEN: token } });  // line 53: executes this statement as part of this file's behavior
    record.child = child;  // line 54: executes this statement as part of this file's behavior
    record.status = { projectId: project.id, status: "running", running: true, pid: child.pid ?? null, startedAt: new Date().toISOString(), lastExitCode: null, message: "Bot runtime is running." };  // line 55: executes this statement as part of this file's behavior
    child.stdout.on("data", (chunk) => this.append(record, String(chunk).trimEnd()));  // line 56: executes this statement as part of this file's behavior
    child.stderr.on("data", (chunk) => this.append(record, String(chunk).trimEnd()));  // line 57: executes this statement as part of this file's behavior
    child.on("close", (code) => {  // line 58: executes this statement as part of this file's behavior
      record.child = null;  // line 59: executes this statement as part of this file's behavior
      record.status = { ...record.status, status: code === 0 ? "stopped" : "crashed", running: false, pid: null, lastExitCode: code, message: code === 0 ? "Bot runtime stopped." : "Bot runtime crashed." };  // line 60: executes this statement as part of this file's behavior
      this.append(record, `Runtime exited with code ${code}.`);  // line 61: executes this statement as part of this file's behavior
    });  // line 62: executes this statement as part of this file's behavior
    child.on("error", (error) => {  // line 63: executes this statement as part of this file's behavior
      record.status = { ...record.status, status: "crashed", running: false, pid: null, message: redactSecrets(error.message) };  // line 64: executes this statement as part of this file's behavior
      this.append(record, `Runtime error: ${error.message}`);  // line 65: executes this statement as part of this file's behavior
    });  // line 66: executes this statement as part of this file's behavior
    return record.status;  // line 67: executes this statement as part of this file's behavior
  }  // line 68: executes this statement as part of this file's behavior

  async stop(projectId: string): Promise<RuntimeStatus> {  // line 70: executes this statement as part of this file's behavior
    const record = this.record(projectId);  // line 71: executes this statement as part of this file's behavior
    if (!record.child) {  // line 72: executes this statement as part of this file's behavior
      record.status = { ...record.status, status: "stopped", running: false, pid: null, message: "Bot runtime is stopped." };  // line 73: executes this statement as part of this file's behavior
      return record.status;  // line 74: executes this statement as part of this file's behavior
    }  // line 75: executes this statement as part of this file's behavior
    record.status = { ...record.status, status: "stopping", running: true, message: "Bot runtime is stopping." };  // line 76: executes this statement as part of this file's behavior
    record.child.kill("SIGTERM");  // line 77: executes this statement as part of this file's behavior
    record.child = null;  // line 78: executes this statement as part of this file's behavior
    record.status = { ...record.status, status: "stopped", running: false, pid: null, message: "Bot runtime is stopped." };  // line 79: executes this statement as part of this file's behavior
    this.append(record, "Runtime stop requested.");  // line 80: executes this statement as part of this file's behavior
    return record.status;  // line 81: executes this statement as part of this file's behavior
  }  // line 82: executes this statement as part of this file's behavior

  async restart(project: BotProject): Promise<RuntimeStatus> {  // line 84: executes this statement as part of this file's behavior
    await this.stop(project.id);  // line 85: executes this statement as part of this file's behavior
    return this.start(project);  // line 86: executes this statement as part of this file's behavior
  }  // line 87: executes this statement as part of this file's behavior

  private record(projectId: string): RuntimeRecord {  // line 89: executes this statement as part of this file's behavior
    const existing = this.runtimes.get(projectId);  // line 90: executes this statement as part of this file's behavior
    if (existing) return existing;  // line 91: executes this statement as part of this file's behavior
    const created: RuntimeRecord = { child: null, logs: "", status: { projectId, status: "stopped", running: false, pid: null, startedAt: null, lastExitCode: null, message: "Bot runtime is stopped." } };  // line 92: executes this statement as part of this file's behavior
    this.runtimes.set(projectId, created);  // line 93: executes this statement as part of this file's behavior
    return created;  // line 94: executes this statement as part of this file's behavior
  }  // line 95: executes this statement as part of this file's behavior

  private append(record: RuntimeRecord, line: string): void {  // line 97: executes this statement as part of this file's behavior
    const next = `${record.logs}${redactSecrets(line)}\n`;  // line 98: executes this statement as part of this file's behavior
    record.logs = next.length > LocalProcessRuntimeService.MAX_LOG_BYTES ? `[truncated]\n${next.slice(-LocalProcessRuntimeService.MAX_LOG_BYTES)}` : next;  // line 99: executes this statement as part of this file's behavior
  }  // line 100: executes this statement as part of this file's behavior
}  // line 101: executes this statement as part of this file's behavior

function allowedEnv(): Record<string, string> {  // line 103: executes this statement as part of this file's behavior
  const keys = ["PATH", "HOME", "TMPDIR", "TEMP", "TMP", "CI"];  // line 104: executes this statement as part of this file's behavior
  const env: Record<string, string> = {};  // line 105: executes this statement as part of this file's behavior
  for (const key of keys) if (process.env[key]) env[key] = process.env[key] as string;  // line 106: executes this statement as part of this file's behavior
  return env;  // line 107: executes this statement as part of this file's behavior
}  // line 108: executes this statement as part of this file's behavior

function structuredError(code: string, message: string): never {  // line 110: executes this statement as part of this file's behavior
  throw { statusCode: 400, code, message, details: {} };  // line 111: executes this statement as part of this file's behavior
}  // line 112: executes this statement as part of this file's behavior
