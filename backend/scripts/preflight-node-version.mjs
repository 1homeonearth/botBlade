// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
#!/usr/bin/env node  // line 7: executes this statement as part of this file's behavior

const minMajor = 22;  // line 9: executes this statement as part of this file's behavior
const rawVersion = process.version;  // line 10: executes this statement as part of this file's behavior
const match = /^v(\d+)\./.exec(rawVersion);  // line 11: executes this statement as part of this file's behavior

if (!match) {  // line 13: executes this statement as part of this file's behavior
  console.error(`[preflight] Unable to parse Node.js version: ${rawVersion}`);  // line 14: executes this statement as part of this file's behavior
  process.exit(1);  // line 15: executes this statement as part of this file's behavior
}  // line 16: executes this statement as part of this file's behavior

const major = Number.parseInt(match[1], 10);  // line 18: executes this statement as part of this file's behavior
if (major < minMajor) {  // line 19: executes this statement as part of this file's behavior
  console.error(`[preflight] Node.js ${minMajor}+ is required. Found ${rawVersion}.`);  // line 20: executes this statement as part of this file's behavior
  process.exit(1);  // line 21: executes this statement as part of this file's behavior
}  // line 22: executes this statement as part of this file's behavior

console.log(`[preflight] Node.js version OK: ${rawVersion}`);  // line 24: executes this statement as part of this file's behavior
