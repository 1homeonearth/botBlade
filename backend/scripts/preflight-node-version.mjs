#!/usr/bin/env node

const minMajor = 22;
const rawVersion = process.version;
const match = /^v(\d+)\./.exec(rawVersion);

if (!match) {
  console.error(`[preflight] Unable to parse Node.js version: ${rawVersion}`);
  process.exit(1);
}

const major = Number.parseInt(match[1], 10);
if (major < minMajor) {
  console.error(`[preflight] Node.js ${minMajor}+ is required. Found ${rawVersion}.`);
  process.exit(1);
}

console.log(`[preflight] Node.js version OK: ${rawVersion}`);
