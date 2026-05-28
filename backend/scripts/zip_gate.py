#!/usr/bin/env python3
import json, os, posixpath, re, stat, sys, zipfile

zip_path = sys.argv[1]
workspace = sys.argv[2] if len(sys.argv) > 2 else None
limits = {"max_archive_bytes": 50 * 1024 * 1024, "max_entry_bytes": 10 * 1024 * 1024, "max_file_count": 2000}
violations = []
plan = []

def add(code, detail, entry=None):
    v={"code":code,"detail":detail}
    if entry is not None: v["entry"]=entry
    violations.append(v)

def norm(name):
    slash = name.replace("\\", "/")
    if slash.startswith("/"):
        add("ABSOLUTE_PATH", "Absolute paths are not allowed.", name); return None
    if re.match(r"^[A-Za-z]:", slash):
        add("DRIVE_PREFIX", "Drive-prefixed paths are not allowed.", name); return None
    n = posixpath.normpath(slash)
    if n == ".." or n.startswith("../") or "/../" in n:
        add("PATH_TRAVERSAL", "Path traversal is not allowed.", name); return None
    return n[2:] if n.startswith("./") else n

if os.path.getsize(zip_path) > limits["max_archive_bytes"]:
    add("MAX_ARCHIVE_SIZE_EXCEEDED", "Archive size exceeds policy limit.")

try:
    with zipfile.ZipFile(zip_path) as z:
        for info in z.infolist():
            n = norm(info.filename)
            if info.file_size > limits["max_entry_bytes"]:
                add("MAX_ENTRY_SIZE_EXCEEDED", "Entry size exceeds policy limit.", info.filename)
            mode = (info.external_attr >> 16) & 0o170000
            is_dir = info.is_dir()
            is_symlink = mode == stat.S_IFLNK
            is_file = mode in (0, stat.S_IFREG)
            if is_symlink:
                add("SYMLINK_ENTRY", "Symlink entries are not allowed.", info.filename)
            if not is_dir and not is_file and not is_symlink:
                add("UNSUPPORTED_TYPE", f"Unsupported type mode={oct(mode)}", info.filename)
            if n:
                plan.append({"zipPath": info.filename, "normalizedPath": n, "isDirectory": is_dir, "size": info.file_size})
        if len(plan) > limits["max_file_count"]:
            add("MAX_FILE_COUNT_EXCEEDED", "Archive file count exceeds policy limit.")
        if violations:
            print(json.dumps({"ok": False, "violations": violations}))
            sys.exit(0)
        if workspace:
            for item in plan:
                target = os.path.join(workspace, item["normalizedPath"])
                os.makedirs(os.path.dirname(target), exist_ok=True)
                if item["isDirectory"]:
                    os.makedirs(target, exist_ok=True)
                else:
                    with z.open(item["zipPath"]) as src, open(target, "wb") as dst:
                        dst.write(src.read())
        print(json.dumps({"ok": True, "plan": plan}))
except Exception as exc:
    print(json.dumps({"ok": False, "violations": [{"code": "ZIP_PARSE_FAILED", "detail": str(exc)}]}))
