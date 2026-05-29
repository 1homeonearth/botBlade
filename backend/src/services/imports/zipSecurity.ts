import fsPromises from "node:fs/promises";
import childProcess from "node:child_process";
import { MAX_PROJECT_RELATIVE_PATH_BYTES } from "../security/projectPaths.js";

export interface ZipPolicy {
  maxArchiveBytes: number;
  maxEntryBytes: number;
  maxFileCount: number;
  maxTotalUncompressedBytes: number;
  maxPathBytes?: number;
}

const DEFAULT_POLICY: ZipPolicy = {
  maxArchiveBytes: 20 * 1024 * 1024,
  maxEntryBytes: 5 * 1024 * 1024,
  maxFileCount: 500,
  maxTotalUncompressedBytes: 100 * 1024 * 1024,
  maxPathBytes: MAX_PROJECT_RELATIVE_PATH_BYTES
};

export type ZipViolationCode = "ARCHIVE_NOT_FOUND"|"ARCHIVE_TOO_LARGE"|"ENTRY_COUNT_EXCEEDED"|"ENTRY_TOO_LARGE"|"TOTAL_UNCOMPRESSED_TOO_LARGE"|"PATH_TRAVERSAL"|"ABSOLUTE_PATH"|"DRIVE_PREFIX"|"PATH_TOO_LONG"|"UNSUPPORTED_ENTRY_TYPE"|"SYMLINK_ENTRY"|"ZIP_READ_ERROR"|"ZIP_RUNTIME_UNAVAILABLE"|"WORKFLOW_JSON_NOT_FOUND"|"WORKFLOW_JSON_NOT_FILE"|"WORKFLOW_JSON_TOO_LARGE"|"WORKFLOW_JSON_SOURCE_REQUIRED"|"WORKFLOW_JSON_INVALID"|"TEMPLATE_ID_NOT_ALLOWED";
export interface ZipViolation { code: ZipViolationCode; entryPath?: string; detail: string }
export interface ValidatedZipEntry { archivePath: string; normalizedPath: string; size: number }
export interface ZipValidationResult { ok: boolean; violations: ZipViolation[]; extractionPlan: ValidatedZipEntry[]; totalUncompressedBytes: number }

export async function validateZipArchive(archivePath: string, policy: ZipPolicy = DEFAULT_POLICY): Promise<ZipValidationResult> {
  const violations: ZipViolation[] = [];
  const extractionPlan: ValidatedZipEntry[] = [];
  try {
    const archiveInfo = await fsPromises.stat(archivePath);
    if (archiveInfo.size > policy.maxArchiveBytes) {
      violations.push({ code: "ARCHIVE_TOO_LARGE", detail: `Archive size ${archiveInfo.size} exceeds max ${policy.maxArchiveBytes}.` });
    }
  } catch (error) {
    return { ok: false, violations: [{ code: "ARCHIVE_NOT_FOUND", detail: error instanceof Error ? error.message : String(error) }], extractionPlan, totalUncompressedBytes: 0 };
  }

  const effectivePolicy = { ...policy, maxPathBytes: policy.maxPathBytes ?? MAX_PROJECT_RELATIVE_PATH_BYTES };
  const payload = await runPythonZipTool("validate", archivePath, "", effectivePolicy);
  if (!payload.ok) violations.push(...payload.violations);
  if (payload.entryCount > policy.maxFileCount) violations.push({ code: "ENTRY_COUNT_EXCEEDED", detail: `Entry count exceeds ${policy.maxFileCount}.` });
  if (payload.totalUncompressedBytes > policy.maxTotalUncompressedBytes) {
    violations.push({ code: "TOTAL_UNCOMPRESSED_TOO_LARGE", detail: `Total uncompressed size ${payload.totalUncompressedBytes} exceeds max ${policy.maxTotalUncompressedBytes}.` });
  }

  for (const entry of payload.entries as Array<{ archivePath: string; normalizedPath: string; size: number; type: string }>) {
    if (entry.size > policy.maxEntryBytes) violations.push({ code: "ENTRY_TOO_LARGE", entryPath: entry.archivePath, detail: `Entry size ${entry.size} exceeds max ${policy.maxEntryBytes}.` });
    if (violations.length === 0 && entry.type === "file") extractionPlan.push({ archivePath: entry.archivePath, normalizedPath: entry.normalizedPath, size: entry.size });
  }
  return { ok: violations.length === 0, violations, extractionPlan: violations.length === 0 ? extractionPlan : [], totalUncompressedBytes: payload.totalUncompressedBytes ?? 0 };
}

export async function extractZipFromPlan(archivePath: string, destinationRoot: string, extractionPlan: ValidatedZipEntry[], policy: ZipPolicy = DEFAULT_POLICY): Promise<void> {
  const effectivePolicy = { ...policy, maxPathBytes: policy.maxPathBytes ?? MAX_PROJECT_RELATIVE_PATH_BYTES };
  const payload = await runPythonZipTool("extract", archivePath, destinationRoot, effectivePolicy, extractionPlan);
  if (!payload.ok) throw new Error(`zip_extract_failed:${JSON.stringify(payload.violations)}`);
}

function runPythonZipTool(mode: "validate" | "extract", archivePath: string, destinationRoot: string, policy: ZipPolicy, extractionPlan: ValidatedZipEntry[] = []): Promise<any> {
  return new Promise((resolve) => {
    const script = `import json, os, posixpath, sys, zipfile\nmode, archive, dest, policy_raw, plan_raw = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5]\npolicy=json.loads(policy_raw)\nexpected_plan={(item['archivePath'], item['normalizedPath'], int(item['size'])) for item in json.loads(plan_raw)}\nviolations=[]; entries=[]; entry_count=0; total_uncompressed=0\ndef check(name, zi):\n n=posixpath.normpath(name.replace('\\\\','/'))\n while n.startswith('./'): n=n[2:]\n t=(zi.external_attr>>16)&0o170000\n kind='file'\n if name.endswith('/') or t==0o040000: kind='dir'\n elif t==0o120000: kind='symlink'\n elif t not in (0,0o100000): kind='other'\n if n.startswith('../') or n=='..': violations.append({'code':'PATH_TRAVERSAL','entryPath':name,'detail':f'Entry resolves outside workspace: {n}'})\n if n.startswith('/'): violations.append({'code':'ABSOLUTE_PATH','entryPath':name,'detail':'Absolute paths are forbidden.'})\n if len(n)>1 and n[1]==':' and n[0].isalpha() and n[2:3]=='/': violations.append({'code':'DRIVE_PREFIX','entryPath':name,'detail':'Drive-prefixed paths are forbidden.'})\n if len(n.encode('utf-8')) > int(policy.get('maxPathBytes', 512)): violations.append({'code':'PATH_TOO_LONG','entryPath':name,'detail':f"Entry path exceeds {policy.get('maxPathBytes', 512)} bytes."})\n if kind=='symlink': violations.append({'code':'SYMLINK_ENTRY','entryPath':name,'detail':'Symlink entries are not supported.'})\n if kind=='other': violations.append({'code':'UNSUPPORTED_ENTRY_TYPE','entryPath':name,'detail':f'Unsupported entry type: {kind}'})\n return n, kind\ntry:\n z=zipfile.ZipFile(archive,'r')\n for zi in z.infolist():\n  entry_count+=1\n  n,k=check(zi.filename,zi)\n  total_uncompressed += int(zi.file_size)\n  entries.append({'archivePath':zi.filename,'normalizedPath':n,'size':zi.file_size,'type':k})\n  if zi.file_size > policy['maxEntryBytes']:\n   violations.append({'code':'ENTRY_TOO_LARGE','entryPath':zi.filename,'detail':f"Entry size {zi.file_size} exceeds max {policy['maxEntryBytes']}."})\n if entry_count > policy['maxFileCount']:\n  violations.append({'code':'ENTRY_COUNT_EXCEEDED','detail':f"Entry count exceeds {policy['maxFileCount']}."})\n if total_uncompressed > policy['maxTotalUncompressedBytes']:\n  violations.append({'code':'TOTAL_UNCOMPRESSED_TOO_LARGE','detail':f"Total uncompressed size {total_uncompressed} exceeds max {policy['maxTotalUncompressedBytes']}."})\n if mode=='extract' and not violations:\n  actual_plan={(e['archivePath'], e['normalizedPath'], int(e['size'])) for e in entries if e['type']=='file'}\n  if actual_plan != expected_plan:\n   violations.append({'code':'ZIP_READ_ERROR','detail':'Archive contents changed between validation and extraction.'})\n  else:\n   root=os.path.realpath(dest)\n   for e in entries:\n    if e['type']!='file': continue\n    out=os.path.realpath(os.path.join(dest,e['normalizedPath']))\n    if not out.startswith(root+os.sep):\n     violations.append({'code':'PATH_TRAVERSAL','entryPath':e['archivePath'],'detail':'Realpath escaped destination root.'}); break\n    os.makedirs(os.path.dirname(out),exist_ok=True)\n    with z.open(e['archivePath'],'r') as src, open(out,'wb') as dst: dst.write(src.read())\nexcept Exception as ex:\n violations.append({'code':'ZIP_READ_ERROR','detail':str(ex)})\nprint(json.dumps({'ok': len(violations)==0, 'violations': violations, 'entries': entries, 'entryCount': entry_count, 'totalUncompressedBytes': total_uncompressed}))`;
    (childProcess as any).execFile("python3", ["-c", script, mode, archivePath, destinationRoot, JSON.stringify(policy), JSON.stringify(extractionPlan)], (error: any, stdout: string) => {
      if (error?.code === "ENOENT") {
        return resolve({ ok: false, violations: [{ code: "ZIP_RUNTIME_UNAVAILABLE", detail: "python3 runtime is required for ZIP import validation/extraction in this build." }], entries: [], entryCount: 0, totalUncompressedBytes: 0 });
      }
      if (error) {
        return resolve({ ok: false, violations: [{ code: "ZIP_READ_ERROR", detail: error.message }], entries: [], entryCount: 0, totalUncompressedBytes: 0 });
      }
      try { resolve(JSON.parse(stdout)); } catch (parseError) {
        resolve({ ok: false, violations: [{ code: "ZIP_READ_ERROR", detail: parseError instanceof Error ? parseError.message : String(parseError) }], entries: [], entryCount: 0, totalUncompressedBytes: 0 });
      }
    });
  });
}
