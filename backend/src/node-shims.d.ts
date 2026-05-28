declare module "node:http" {
  export interface IncomingMessage {
    method?: string;
    url?: string;
    headers: Record<string, string | string[] | undefined>;
    [Symbol.asyncIterator](): AsyncIterableIterator<unknown>;
  }
  export interface ServerResponse {
    statusCode: number;
    setHeader(name: string, value: string): void;
    end(chunk?: string): void;
  }
  export function createServer(handler: (req: IncomingMessage, res: ServerResponse) => void | Promise<void>): { listen(port: number, callback?: () => void): void };
}

declare class Buffer extends Uint8Array {
  static isBuffer(value: unknown): value is Buffer;
  static from(value: unknown, encoding?: string): Buffer;
  static byteLength(value: string, encoding?: string): number;
  static concat(chunks: Buffer[]): Buffer;
  toString(encoding?: string): string;
}


declare const console: {
  info(message?: unknown, ...optionalParams: unknown[]): void;
  error(message?: unknown, ...optionalParams: unknown[]): void;
  warn(message?: unknown, ...optionalParams: unknown[]): void;
};

declare class URL {
  constructor(input: string, base?: string);
  pathname: string;
}

declare module "node:test" {
  const test: (name: string, fn: () => void | Promise<void>) => void;
  export default test;
}

declare module "node:assert/strict" {
  const assert: {
    equal(actual: unknown, expected: unknown, message?: string): void;
    ok(value: unknown, message?: string): void;
    rejects(fn: () => Promise<unknown>, expected?: RegExp): Promise<void>;
  };
  export default assert;
}


declare module "node:fs" {
  export function mkdirSync(path: string, options?: { recursive?: boolean }): void;
  export function readdirSync(path: string | URL): string[];
  export function readFileSync(path: string | URL, encoding: string): string;
  export function mkdtempSync(prefix: string): string;
  export function existsSync(path: string): boolean;
}

declare module "node:os" {
  export function tmpdir(): string;
}

declare module "node:fs/promises" {
  export function mkdir(path: string, options?: { recursive?: boolean }): Promise<void>;
  export function writeFile(path: string, data: string, encoding?: string): Promise<void>;
  export function readFile(path: string, encoding?: string): Promise<string>;
  export function readdir(path: string, options?: { withFileTypes?: boolean }): Promise<Dirent[]>;
  export function stat(path: string): Promise<{ size: number; mtime: Date; isFile(): boolean; isDirectory(): boolean }>;
  export function access(path: string): Promise<void>;
  export function rm(path: string, options?: { recursive?: boolean; force?: boolean }): Promise<void>;
  export function rename(oldPath: string, newPath: string): Promise<void>;
  export interface Dirent { name: string; isDirectory(): boolean; isFile(): boolean; }
}

declare module "node:path" {
  export function join(...paths: string[]): string;
  export function dirname(path: string): string;
  export function resolve(...paths: string[]): string;
  export function relative(from: string, to: string): string;
  export function isAbsolute(path: string): boolean;
  export const sep: string;
}

declare module "node:crypto" {
  export default crypto;
  export function randomUUID(): string;
  export function randomBytes(size: number): Buffer;
  export function createHash(algorithm: string): { update(data: string): { digest(): Buffer; digest(encoding: "hex"): string } };
  export function createCipheriv(algorithm: string, key: Buffer, iv: Buffer): { update(data: string, inputEncoding: string): Buffer; final(): Buffer; getAuthTag(): Buffer };
  export function createDecipheriv(algorithm: string, key: Buffer, iv: Buffer): { setAuthTag(tag: Buffer): void; update(data: Buffer): Buffer; final(): Buffer };
  const crypto: { randomUUID(): string; randomBytes(size: number): Buffer; createHash(algorithm: string): { update(data: string): { digest(): Buffer; digest(encoding: "hex"): string } }; createCipheriv: typeof createCipheriv; createDecipheriv: typeof createDecipheriv };
}

declare module "node:child_process" {
  import { EventEmitter } from "node:events";
  export interface StreamLike { on(event: "data", listener: (chunk: unknown) => void): void; }
  export interface ChildProcessWithoutNullStreams extends EventEmitter {
    pid?: number;
    killed: boolean;
    stdout: StreamLike;
    stderr: StreamLike;
    kill(signal?: string): boolean;
    on(event: "close", listener: (code: number | null) => void): this;
    on(event: "error", listener: (error: Error) => void): this;
  }
  export function spawn(command: string, args?: string[], options?: { cwd?: string; shell?: boolean; env?: Record<string, string | undefined> }): ChildProcessWithoutNullStreams;
  export function execFileSync(command: string, args?: string[], options?: { input?: string; encoding?: string }): string;
}

declare module "node:events" {
  export class EventEmitter {
    on(event: string, listener: (...args: unknown[]) => void): this;
  }
}

declare function setTimeout(callback: () => void, ms: number): unknown;
declare function clearTimeout(timeout: unknown): void;


declare const process: {
  env: Record<string, string | undefined>;
  cwd(): string;
};
