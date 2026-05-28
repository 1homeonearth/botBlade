import { spawn } from "node:child_process";
import path from "node:path";

export type ZipViolation = { code: string; detail: string; entry?: string };

function runZipGate(zipPath: string, workspacePath?: string): Promise<{ ok: boolean; plan?: unknown[]; violations?: ZipViolation[] }> {
  return new Promise((resolve, reject) => {
    const scriptPath = path.resolve(process.cwd(), "scripts/zip_gate.py");
    const args = workspacePath ? [scriptPath, zipPath, workspacePath] : [scriptPath, zipPath];
    const proc = spawn("python3", args);
    let output = "";
    let stderr = "";
    proc.stdout.on("data", (d: Buffer) => (output += d.toString("utf8")));
    proc.stderr.on("data", (d: Buffer) => (stderr += d.toString("utf8")));
    proc.on("error", reject);
    proc.on("close", (code) => {
      if (code !== 0 && !output.trim()) return reject(new Error(`zip gate failed: ${stderr.trim()}`));
      try { resolve(JSON.parse(output)); } catch { reject(new Error(`zip gate parse failed: ${output || stderr}`)); }
    });
  });
}

export function buildSecurityCards(violations: ZipViolation[]) {
  return violations.map((violation) => ({ category: "security", code: violation.code, summary: `Import blocked: ${violation.code}`, remediation: "Rebuild the archive to include only regular files/directories with safe relative paths.", entry: violation.entry ?? null }));
}

export async function importZipIntoWorkspace(zipPath: string, workspacePath: string): Promise<{ state: "imported"; importedCount: number } | { state: "blocked_by_policy"; violations: ZipViolation[]; cards: unknown[] }> {
  const result = await runZipGate(zipPath, workspacePath);
  if (!result.ok) {
    const violations = result.violations ?? [{ code: "ZIP_PARSE_FAILED", detail: "Unknown parse failure." }];
    return { state: "blocked_by_policy", violations, cards: buildSecurityCards(violations) };
  }
  return { state: "imported", importedCount: (result.plan ?? []).filter((item: any) => !item.isDirectory).length };
}
