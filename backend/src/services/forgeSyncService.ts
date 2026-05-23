import { spawn } from 'node:child_process';
import fs from 'node:fs/promises';
import path from 'node:path';
import { randomUUID } from 'node:crypto';
import { RequestValidationError } from './projectStore.js';
import type { ProjectFileService } from './projectFiles.js';

const MAX_URL_LENGTH = 2048;
const MAX_LOCAL_ITEMS = 4000;

export interface ForgeImportResult {
  operationId: string;
  projectId: string;
  url: string;
  branch: string;
  head: string;
  workspacePath: string;
  warnings: string[];
  importedAt: string;
}

export interface ForgeStatusSummary {
  branch: string;
  head: string;
  dirty: boolean;
  ahead: number;
  behind: number;
  lastCommit: { hash: string; subject: string } | null;
}

export function parseForgeImportInput(value: unknown): { url: string; branch?: string } {
  const v = value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, unknown> : {};
  if (typeof v.url !== 'string' || v.url.trim().length === 0) throw new RequestValidationError([{ field: 'url', message: 'Repository URL is required.' }]);
  if (v.url.length > MAX_URL_LENGTH) throw new RequestValidationError([{ field: 'url', message: 'Repository URL is too long.' }]);
  if (typeof v.branch !== 'undefined' && (typeof v.branch !== 'string' || v.branch.trim().length === 0)) {
    throw new RequestValidationError([{ field: 'branch', message: 'Branch must be a non-empty string when provided.' }]);
  }
  return { url: v.url.trim(), branch: typeof v.branch === 'string' ? v.branch.trim() : undefined };
}

export class ForgeSyncService {
  constructor(private readonly files: ProjectFileService) {}

  async importFromUrl(projectId: string, url: string, branch?: string): Promise<ForgeImportResult> {
    assertSafeRepoUrl(url);
    const workspace = this.files.workspace(projectId);
    await fs.mkdir(workspace, { recursive: true });
    await ensureWorkspaceEmptyOrGitSafe(workspace);
    const args = ['clone', '--no-tags', '--depth', '1'];
    if (branch) args.push('--branch', branch);
    args.push(url, workspace);
    await execGit(args);
    const status = await this.status(projectId);
    const warnings = await this.guardrailWarnings(workspace);
    return {
      operationId: `forgeop_${randomUUID()}`,
      projectId,
      url,
      branch: status.branch,
      head: status.head,
      workspacePath: workspace,
      warnings,
      importedAt: new Date().toISOString(),
    };
  }

  async status(projectId: string): Promise<ForgeStatusSummary> {
    const workspace = this.files.workspace(projectId);
    const [branch, head, dirtyLine, aheadBehind, lastCommitRaw] = await Promise.all([
      gitOut(['rev-parse', '--abbrev-ref', 'HEAD'], workspace),
      gitOut(['rev-parse', 'HEAD'], workspace),
      gitOut(['status', '--porcelain'], workspace),
      gitOut(['rev-list', '--left-right', '--count', 'HEAD...@{upstream}'], workspace).catch(() => '0\t0'),
      gitOut(['log', '-1', '--pretty=%H\t%s'], workspace).catch(() => ''),
    ]);
    const [aheadRaw, behindRaw] = aheadBehind.trim().split(/\s+/);
    const [hash = '', subject = ''] = lastCommitRaw.split('\t');
    return {
      branch: branch.trim(),
      head: head.trim(),
      dirty: dirtyLine.trim().length > 0,
      ahead: Number.parseInt(aheadRaw ?? '0', 10) || 0,
      behind: Number.parseInt(behindRaw ?? '0', 10) || 0,
      lastCommit: hash ? { hash, subject } : null,
    };
  }

  private async guardrailWarnings(workspace: string): Promise<string[]> {
    const warnings: string[] = [];
    const count = await countEntries(workspace, MAX_LOCAL_ITEMS + 1);
    if (count > MAX_LOCAL_ITEMS) warnings.push('Large repository detected; some mobile surfaces may load lazily.');
    if (await hasNestedGit(workspace)) warnings.push('Nested Git directories detected. Nested repositories are read-only in Forge Sync.');
    return warnings;
  }
}

async function ensureWorkspaceEmptyOrGitSafe(workspace: string): Promise<void> {
  const entries = await fs.readdir(workspace, { withFileTypes: true }).catch(() => []);
  if (entries.length === 0) return;
  if (entries.length === 1 && entries[0].name === '.git') return;
  throw { statusCode: 409, code: 'WORKSPACE_NOT_EMPTY', message: 'Project workspace is not empty. Create a new project or clear workspace first.', details: {} };
}

async function execGit(args: string[], cwd?: string): Promise<void> {
  await new Promise<void>((resolve, reject) => {
    const child = spawn('git', args, { cwd });
    let stderr = '';
    child.stderr.on('data', (d) => { stderr += String(d); });
    child.on('error', reject);
    child.on('close', (code) => {
      if (code === 0) resolve();
      else reject({ statusCode: 400, code: 'FORGE_SYNC_FAILED', message: 'Forge Sync operation failed.', details: { args, stderr: stderr.trim() } });
    });
  });
}

async function gitOut(args: string[], cwd: string): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    const child = spawn('git', args, { cwd });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (d) => { stdout += String(d); });
    child.stderr.on('data', (d) => { stderr += String(d); });
    child.on('error', reject);
    child.on('close', (code) => code === 0 ? resolve(stdout.trim()) : reject(new Error(stderr.trim() || 'git failed')));
  });
}

function assertSafeRepoUrl(raw: string): void {
  if (raw.includes('\n') || raw.includes('\r')) throw new RequestValidationError([{ field: 'url', message: 'Repository URL is invalid.' }]);
  const url = raw.trim();
  if (/^https:\/\//.test(url) || /^ssh:\/\//.test(url) || /^git@/.test(url)) return;
  throw new RequestValidationError([{ field: 'url', message: 'Only HTTPS and SSH repository URLs are allowed.' }]);
}

async function countEntries(root: string, limit: number): Promise<number> {
  let count = 0;
  const stack = [root];
  while (stack.length > 0) {
    const current = stack.pop()!;
    const entries = await fs.readdir(current, { withFileTypes: true }).catch(() => []);
    for (const entry of entries) {
      count += 1;
      if (count > limit) return count;
      if (entry.isDirectory() && entry.name !== '.git' && entry.name !== 'node_modules') stack.push(path.join(current, entry.name));
    }
  }
  return count;
}

async function hasNestedGit(root: string): Promise<boolean> {
  const stack = [root];
  while (stack.length > 0) {
    const current = stack.pop()!;
    const entries = await fs.readdir(current, { withFileTypes: true }).catch(() => []);
    for (const entry of entries) {
      if (!entry.isDirectory()) continue;
      const next = path.join(current, entry.name);
      if (entry.name === '.git' && next !== path.join(root, '.git')) return true;
      if (entry.name !== '.git' && entry.name !== 'node_modules') stack.push(next);
    }
  }
  return false;
}
