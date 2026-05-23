import fs from 'node:fs/promises';
import path from 'node:path';

export interface PackageClassifierSummary {
  language: 'javascript' | 'typescript' | 'unknown';
  packageManager: 'npm' | 'pnpm' | 'yarn' | 'unknown';
  framework: string;
  scripts: string[];
  entrypoints: string[];
  hasEnvExample: boolean;
}

export async function classifyProjectWorkspace(workspace: string): Promise<PackageClassifierSummary> {
  const pkgPath = path.join(workspace, 'package.json');
  const raw = await fs.readFile(pkgPath, 'utf8').catch(() => null);
  if (!raw) {
    return { language: 'unknown', packageManager: 'unknown', framework: 'unknown', scripts: [], entrypoints: [], hasEnvExample: await exists(path.join(workspace, '.env.example')) };
  }
  const pkg = JSON.parse(raw) as Record<string, unknown>;
  const scriptsObj = pkg.scripts && typeof pkg.scripts === 'object' ? pkg.scripts as Record<string, unknown> : {};
  const deps = { ...(pkg.dependencies as Record<string, unknown> ?? {}), ...(pkg.devDependencies as Record<string, unknown> ?? {}) };
  const scripts = Object.keys(scriptsObj);
  const entrypoints = [pkg.main, pkg.module, (pkg.bin && typeof pkg.bin === 'string' ? pkg.bin : undefined)].filter((v): v is string => typeof v === 'string');
  return {
    language: detectLanguage(deps, entrypoints),
    packageManager: await detectPackageManager(workspace),
    framework: detectFramework(deps),
    scripts,
    entrypoints,
    hasEnvExample: await exists(path.join(workspace, '.env.example')),
  };
}

function detectFramework(deps: Record<string, unknown>): string {
  const keys = Object.keys(deps);
  if (keys.includes('discord.js')) return 'discord.js';
  if (keys.includes('telegraf')) return 'telegraf';
  if (keys.includes('grammy')) return 'grammy';
  if (keys.includes('@slack/bolt')) return 'slack-bolt';
  if (keys.includes('eris')) return 'eris';
  if (keys.includes('botkit')) return 'botkit-style';
  if (keys.includes('fastify')) return 'fastify-webhook';
  if (keys.includes('express')) return 'express-webhook';
  return 'node-cli';
}

function detectLanguage(deps: Record<string, unknown>, entrypoints: string[]): 'javascript' | 'typescript' | 'unknown' {
  if ('typescript' in deps) return 'typescript';
  if (entrypoints.some((ep) => ep.endsWith('.ts'))) return 'typescript';
  if (entrypoints.length > 0) return 'javascript';
  return 'unknown';
}

async function detectPackageManager(workspace: string): Promise<'npm'|'pnpm'|'yarn'|'unknown'> {
  if (await exists(path.join(workspace, 'pnpm-lock.yaml'))) return 'pnpm';
  if (await exists(path.join(workspace, 'yarn.lock'))) return 'yarn';
  if (await exists(path.join(workspace, 'package-lock.json'))) return 'npm';
  return 'unknown';
}

async function exists(filePath: string): Promise<boolean> { return fs.access(filePath).then(() => true, () => false); }
