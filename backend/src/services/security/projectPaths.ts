export const MAX_PROJECT_RELATIVE_PATH_BYTES = 512;

export type ProjectPathViolationCode =
  | "PATH_NOT_STRING"
  | "PATH_EMPTY"
  | "PATH_TOO_LONG"
  | "PATH_NUL_BYTE"
  | "PATH_BACKSLASH"
  | "PATH_ABSOLUTE"
  | "PATH_DRIVE_PREFIXED"
  | "PATH_URL_SCHEME"
  | "PATH_TRAVERSAL";

export interface ProjectPathValidationResult {
  ok: boolean;
  path?: string;
  code?: ProjectPathViolationCode;
}

export function normalizeProjectRelativePath(value: unknown, options: { allowRoot?: boolean } = {}): ProjectPathValidationResult {
  if (typeof value !== "string") return { ok: false, code: "PATH_NOT_STRING" };
  const trimmed = value.trim();
  if (!trimmed) return { ok: false, code: "PATH_EMPTY" };
  if (Buffer.byteLength(trimmed, "utf8") > MAX_PROJECT_RELATIVE_PATH_BYTES) return { ok: false, code: "PATH_TOO_LONG" };
  if (trimmed.includes("\0")) return { ok: false, code: "PATH_NUL_BYTE" };
  if (trimmed.includes("\\")) return { ok: false, code: "PATH_BACKSLASH" };
  if (trimmed.startsWith("/")) return { ok: false, code: "PATH_ABSOLUTE" };
  if (/^[A-Za-z]:(?:\/|$)/.test(trimmed)) return { ok: false, code: "PATH_DRIVE_PREFIXED" };
  if (/^[A-Za-z][A-Za-z0-9+.-]*:/.test(trimmed)) return { ok: false, code: "PATH_URL_SCHEME" };

  const segments = trimmed.split("/").filter((segment) => segment.length > 0 && segment !== ".");
  if (segments.some((segment) => segment === "..")) return { ok: false, code: "PATH_TRAVERSAL" };
  const normalized = segments.join("/") || ".";
  if (normalized === "." && options.allowRoot === false) return { ok: false, code: "PATH_EMPTY" };
  if (Buffer.byteLength(normalized, "utf8") > MAX_PROJECT_RELATIVE_PATH_BYTES) return { ok: false, code: "PATH_TOO_LONG" };
  return { ok: true, path: normalized };
}

export function isSafeProjectRelativePath(value: unknown, options: { allowRoot?: boolean } = {}): boolean {
  return normalizeProjectRelativePath(value, options).ok;
}
