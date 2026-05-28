import fsPromises from "node:fs/promises";
import childProcess from "node:child_process";

const DEFAULT_POLICY = { maxArchiveBytes: 20 * 1024 * 1024, maxEntryBytes: 5 * 1024 * 1024, maxFileCount: 500 };
export type ZipViolationCode = "ARCHIVE_NOT_FOUND"|"ARCHIVE_TOO_LARGE"|"ENTRY_COUNT_EXCEEDED"|"ENTRY_TOO_LARGE"|"PATH_TRAVERSAL"|"ABSOLUTE_PATH"|"DRIVE_PREFIX"|"UNSUPPORTED_ENTRY_TYPE"|"SYMLINK_ENTRY"|"ZIP_READ_ERROR"|"ZIP_RUNTIME_UNAVAILABLE";
export interface ZipViolation { code: ZipViolationCode; entryPath?: string; detail: string }
export interface ValidatedZipEntry { archivePath: string; normalizedPath: string; size: number }
export interface ZipValidationResult { ok: boolean; violations: ZipViolation[]; extractionPlan: ValidatedZipEntry[] }

export async function validateZipArchive(archivePath: string): Promise<ZipValidationResult> {
  const violations: ZipViolation[] = [];
  const extractionPlan: ValidatedZipEntry[] = [];
  try { const archiveInfo = await fsPromises.stat(archivePath); if (archiveInfo.size > DEFAULT_POLICY.maxArchiveBytes) violations.push({ code: "ARCHIVE_TOO_LARGE", detail: `Archive size ${archiveInfo.size} exceeds max ${DEFAULT_POLICY.maxArchiveBytes}.` }); }
  catch (error) { return { ok: false, violations: [{ code: "ARCHIVE_NOT_FOUND", detail: error instanceof Error ? error.message : String(error) }], extractionPlan }; }
  const payload = await runPythonZipTool("validate", archivePath, "");
  if (!payload.ok) violations.push(...payload.violations);
  if (payload.entryCount > DEFAULT_POLICY.maxFileCount) violations.push({ code: "ENTRY_COUNT_EXCEEDED", detail: `Entry count exceeds ${DEFAULT_POLICY.maxFileCount}.` });
  for (const entry of payload.entries as Array<{ archivePath: string; normalizedPath: string; size: number; type: string }>) {
    if (entry.size > DEFAULT_POLICY.maxEntryBytes) violations.push({ code: "ENTRY_TOO_LARGE", entryPath: entry.archivePath, detail: `Entry size ${entry.size} exceeds max ${DEFAULT_POLICY.maxEntryBytes}.` });
    if (violations.length === 0 && entry.type === "file") extractionPlan.push({ archivePath: entry.archivePath, normalizedPath: entry.normalizedPath, size: entry.size });
  }
  return { ok: violations.length === 0, violations, extractionPlan: violations.length === 0 ? extractionPlan : [] };
}
export async function extractZipFromPlan(archivePath: string, destinationRoot: string): Promise<void> {
  const payload = await runPythonZipTool("extract", archivePath, destinationRoot);
  if (!payload.ok) throw new Error(`zip_extract_failed:${JSON.stringify(payload.violations)}`);
}

function runPythonZipTool(mode: "validate" | "extract", archivePath: string, destinationRoot: string): Promise<any> {
  return new Promise((resolve, reject) => {
    const script = `import json, os, posixpath, sys, zipfile\nmode, archive, dest = sys.argv[1], sys.argv[2], sys.argv[3]\nviolations=[]; entries=[]; entry_count=0\ndef check(name, zi):\n n=posixpath.normpath(name.replace('\\\\','/'))
 while n.startswith('./'): n=n[2:]\n t=(zi.external_attr>>16)&0o170000\n kind='file'\n if name.endswith('/') or t==0o040000: kind='dir'\n elif t==0o120000: kind='symlink'\n elif t not in (0,0o100000): kind='other'\n if n.startswith('../') or n=='..': violations.append({'code':'PATH_TRAVERSAL','entryPath':name,'detail':f'Entry resolves outside workspace: {n}'})\n if n.startswith('/'): violations.append({'code':'ABSOLUTE_PATH','entryPath':name,'detail':'Absolute paths are forbidden.'})\n if len(n)>1 and n[1]==':' and n[0].isalpha() and n[2:3]=='/': violations.append({'code':'DRIVE_PREFIX','entryPath':name,'detail':'Drive-prefixed paths are forbidden.'})\n if kind=='symlink': violations.append({'code':'SYMLINK_ENTRY','entryPath':name,'detail':'Symlink entries are not supported.'})\n if kind=='other': violations.append({'code':'UNSUPPORTED_ENTRY_TYPE','entryPath':name,'detail':f'Unsupported entry type: {kind}'})\n return n, kind\ntry:\n z=zipfile.ZipFile(archive,'r')\n for zi in z.infolist():\n  entry_count+=1\n  n,k=check(zi.filename,zi)\n  entries.append({'archivePath':zi.filename,'normalizedPath':n,'size':zi.file_size,'type':k})\n if mode=='extract' and not violations:\n  for e in entries:\n   if e['type']!='file': continue\n   out=os.path.realpath(os.path.join(dest,e['normalizedPath']))\n   root=os.path.realpath(dest)\n   if not out.startswith(root+os.sep):\n    violations.append({'code':'PATH_TRAVERSAL','entryPath':e['archivePath'],'detail':'Realpath escaped destination root.'}); break\n   os.makedirs(os.path.dirname(out),exist_ok=True)\n   with z.open(e['archivePath'],'r') as src, open(out,'wb') as dst: dst.write(src.read())\nexcept Exception as ex:\n violations.append({'code':'ZIP_READ_ERROR','detail':str(ex)})\nprint(json.dumps({'ok': len(violations)==0, 'violations': violations, 'entries': entries, 'entryCount': entry_count}))`;
    (childProcess as any).execFile("python3", ["-c", script, mode, archivePath, destinationRoot], (error: any, stdout: string) => {
      if (error?.code === "ENOENT") {
        return resolve({ ok: false, violations: [{ code: "ZIP_RUNTIME_UNAVAILABLE", detail: "python3 runtime is required for ZIP import validation/extraction in this build." }], entries: [], entryCount: 0 });
      }
      if (error) {
        return resolve({ ok: false, violations: [{ code: "ZIP_READ_ERROR", detail: error.message }], entries: [], entryCount: 0 });
      }
      try { resolve(JSON.parse(stdout)); } catch (parseError) {
        resolve({ ok: false, violations: [{ code: "ZIP_READ_ERROR", detail: parseError instanceof Error ? parseError.message : String(parseError) }], entries: [], entryCount: 0 });
      }
    });
  });
}
