// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
declare module "node:http" {  // line 7: executes this statement as part of this file's behavior
  export interface IncomingMessage {  // line 8: executes this statement as part of this file's behavior
    method?: string;  // line 9: executes this statement as part of this file's behavior
    url?: string;  // line 10: executes this statement as part of this file's behavior
    headers: Record<string, string | string[] | undefined>;  // line 11: executes this statement as part of this file's behavior
    [Symbol.asyncIterator](): AsyncIterableIterator<unknown>;  // line 12: executes this statement as part of this file's behavior
  }  // line 13: executes this statement as part of this file's behavior
  export interface ServerResponse {  // line 14: executes this statement as part of this file's behavior
    statusCode: number;  // line 15: executes this statement as part of this file's behavior
    setHeader(name: string, value: string): void;  // line 16: executes this statement as part of this file's behavior
    end(chunk?: string): void;  // line 17: executes this statement as part of this file's behavior
  }  // line 18: executes this statement as part of this file's behavior
  export function createServer(handler: (req: IncomingMessage, res: ServerResponse) => void | Promise<void>): { listen(port: number, callback?: () => void): void };  // line 19: executes this statement as part of this file's behavior
}  // line 20: executes this statement as part of this file's behavior

declare class Buffer extends Uint8Array {  // line 22: executes this statement as part of this file's behavior
  static isBuffer(value: unknown): value is Buffer;  // line 23: executes this statement as part of this file's behavior
  static from(value: unknown, encoding?: string): Buffer;  // line 24: executes this statement as part of this file's behavior
  static byteLength(value: string, encoding?: string): number;  // line 25: executes this statement as part of this file's behavior
  static concat(chunks: Buffer[]): Buffer;  // line 26: executes this statement as part of this file's behavior
  toString(encoding?: string): string;  // line 27: executes this statement as part of this file's behavior
}  // line 28: executes this statement as part of this file's behavior


declare const console: {  // line 31: executes this statement as part of this file's behavior
  info(message?: unknown, ...optionalParams: unknown[]): void;  // line 32: executes this statement as part of this file's behavior
  error(message?: unknown, ...optionalParams: unknown[]): void;  // line 33: executes this statement as part of this file's behavior
  warn(message?: unknown, ...optionalParams: unknown[]): void;  // line 34: executes this statement as part of this file's behavior
};  // line 35: executes this statement as part of this file's behavior

declare class URL {  // line 37: executes this statement as part of this file's behavior
  constructor(input: string, base?: string);  // line 38: executes this statement as part of this file's behavior
  pathname: string;  // line 39: executes this statement as part of this file's behavior
}  // line 40: executes this statement as part of this file's behavior

declare module "node:test" {  // line 42: executes this statement as part of this file's behavior
  const test: (name: string, fn: () => void | Promise<void>) => void;  // line 43: executes this statement as part of this file's behavior
  export default test;  // line 44: executes this statement as part of this file's behavior
}  // line 45: executes this statement as part of this file's behavior

declare module "node:assert/strict" {  // line 47: executes this statement as part of this file's behavior
  const assert: {  // line 48: executes this statement as part of this file's behavior
    equal(actual: unknown, expected: unknown, message?: string): void;  // line 49: executes this statement as part of this file's behavior
    ok(value: unknown, message?: string): void;  // line 50: executes this statement as part of this file's behavior
  };  // line 51: executes this statement as part of this file's behavior
  export default assert;  // line 52: executes this statement as part of this file's behavior
}  // line 53: executes this statement as part of this file's behavior


declare module "node:fs" {  // line 56: executes this statement as part of this file's behavior
  export function mkdirSync(path: string, options?: { recursive?: boolean }): void;  // line 57: executes this statement as part of this file's behavior
  export function readdirSync(path: string | URL): string[];  // line 58: executes this statement as part of this file's behavior
  export function readFileSync(path: string | URL, encoding: string): string;  // line 59: executes this statement as part of this file's behavior
  export function mkdtempSync(prefix: string): string;  // line 60: executes this statement as part of this file's behavior
  export function existsSync(path: string): boolean;  // line 61: executes this statement as part of this file's behavior
}  // line 62: executes this statement as part of this file's behavior

declare module "node:os" {  // line 64: executes this statement as part of this file's behavior
  export function tmpdir(): string;  // line 65: executes this statement as part of this file's behavior
}  // line 66: executes this statement as part of this file's behavior

declare module "node:fs/promises" {  // line 68: executes this statement as part of this file's behavior
  export function mkdir(path: string, options?: { recursive?: boolean }): Promise<void>;  // line 69: executes this statement as part of this file's behavior
  export function writeFile(path: string, data: string, encoding?: string): Promise<void>;  // line 70: executes this statement as part of this file's behavior
  export function readFile(path: string, encoding?: string): Promise<string>;  // line 71: executes this statement as part of this file's behavior
  export function readdir(path: string, options?: { withFileTypes?: boolean }): Promise<Dirent[]>;  // line 72: executes this statement as part of this file's behavior
  export function stat(path: string): Promise<{ size: number; mtime: Date; isFile(): boolean; isDirectory(): boolean }>;  // line 73: executes this statement as part of this file's behavior
  export function access(path: string): Promise<void>;  // line 74: executes this statement as part of this file's behavior
  export function rm(path: string, options?: { recursive?: boolean; force?: boolean }): Promise<void>;  // line 75: executes this statement as part of this file's behavior
  export interface Dirent { name: string; isDirectory(): boolean; isFile(): boolean; }  // line 76: executes this statement as part of this file's behavior
}  // line 77: executes this statement as part of this file's behavior

declare module "node:path" {  // line 79: executes this statement as part of this file's behavior
  export function join(...paths: string[]): string;  // line 80: executes this statement as part of this file's behavior
  export function dirname(path: string): string;  // line 81: executes this statement as part of this file's behavior
  export function resolve(...paths: string[]): string;  // line 82: executes this statement as part of this file's behavior
  export function relative(from: string, to: string): string;  // line 83: executes this statement as part of this file's behavior
  export function isAbsolute(path: string): boolean;  // line 84: executes this statement as part of this file's behavior
  export const sep: string;  // line 85: executes this statement as part of this file's behavior
}  // line 86: executes this statement as part of this file's behavior

declare module "node:crypto" {  // line 88: executes this statement as part of this file's behavior
  export default crypto;  // line 89: executes this statement as part of this file's behavior
  export function randomUUID(): string;  // line 90: executes this statement as part of this file's behavior
  export function randomBytes(size: number): Buffer;  // line 91: executes this statement as part of this file's behavior
  export function createHash(algorithm: string): { update(data: string): { digest(): Buffer; digest(encoding: "hex"): string } };  // line 92: executes this statement as part of this file's behavior
  export function createCipheriv(algorithm: string, key: Buffer, iv: Buffer): { update(data: string, inputEncoding: string): Buffer; final(): Buffer; getAuthTag(): Buffer };  // line 93: executes this statement as part of this file's behavior
  export function createDecipheriv(algorithm: string, key: Buffer, iv: Buffer): { setAuthTag(tag: Buffer): void; update(data: Buffer): Buffer; final(): Buffer };  // line 94: executes this statement as part of this file's behavior
  const crypto: { randomUUID(): string; randomBytes(size: number): Buffer; createHash(algorithm: string): { update(data: string): { digest(): Buffer; digest(encoding: "hex"): string } }; createCipheriv: typeof createCipheriv; createDecipheriv: typeof createDecipheriv };  // line 95: executes this statement as part of this file's behavior
}  // line 96: executes this statement as part of this file's behavior

declare module "node:child_process" {  // line 98: executes this statement as part of this file's behavior
  import { EventEmitter } from "node:events";  // line 99: executes this statement as part of this file's behavior
  export interface StreamLike { on(event: "data", listener: (chunk: unknown) => void): void; }  // line 100: executes this statement as part of this file's behavior
  export interface ChildProcessWithoutNullStreams extends EventEmitter {  // line 101: executes this statement as part of this file's behavior
    pid?: number;  // line 102: executes this statement as part of this file's behavior
    killed: boolean;  // line 103: executes this statement as part of this file's behavior
    stdout: StreamLike;  // line 104: executes this statement as part of this file's behavior
    stderr: StreamLike;  // line 105: executes this statement as part of this file's behavior
    kill(signal?: string): boolean;  // line 106: executes this statement as part of this file's behavior
    on(event: "close", listener: (code: number | null) => void): this;  // line 107: executes this statement as part of this file's behavior
    on(event: "error", listener: (error: Error) => void): this;  // line 108: executes this statement as part of this file's behavior
  }  // line 109: executes this statement as part of this file's behavior
  export function spawn(command: string, args?: string[], options?: { cwd?: string; shell?: boolean; env?: Record<string, string | undefined> }): ChildProcessWithoutNullStreams;  // line 110: executes this statement as part of this file's behavior
  export function execFileSync(command: string, args?: string[], options?: { input?: string; encoding?: string }): string;  // line 111: executes this statement as part of this file's behavior
}  // line 112: executes this statement as part of this file's behavior

declare module "node:events" {  // line 114: executes this statement as part of this file's behavior
  export class EventEmitter {  // line 115: executes this statement as part of this file's behavior
    on(event: string, listener: (...args: unknown[]) => void): this;  // line 116: executes this statement as part of this file's behavior
  }  // line 117: executes this statement as part of this file's behavior
}  // line 118: executes this statement as part of this file's behavior

declare function setTimeout(callback: () => void, ms: number): unknown;  // line 120: executes this statement as part of this file's behavior
declare function clearTimeout(timeout: unknown): void;  // line 121: executes this statement as part of this file's behavior


declare const process: {  // line 124: executes this statement as part of this file's behavior
  env: Record<string, string | undefined>;  // line 125: executes this statement as part of this file's behavior
  cwd(): string;  // line 126: executes this statement as part of this file's behavior
};  // line 127: executes this statement as part of this file's behavior
