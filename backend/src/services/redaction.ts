// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
const knownSecretValues = new Set<string>();  // line 7: executes this statement as part of this file's behavior

const tokenPatterns: RegExp[] = [  // line 9: executes this statement as part of this file's behavior
  /[A-Za-z0-9_-]{23,}\.[A-Za-z0-9_-]{6,}\.[A-Za-z0-9_-]{20,}/g,  // line 10: executes this statement as part of this file's behavior
  /gh[pousr]_[A-Za-z0-9_]{20,}/g,  // line 11: executes this statement as part of this file's behavior
  /(?:xox[baprs]-)[A-Za-z0-9-]{10,}/g,  // line 12: executes this statement as part of this file's behavior
  /https?:\/\/[^\s:@]+:[^\s@]+@[^\s]+/g,  // line 13: executes this statement as part of this file's behavior
  /-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----/g,  // line 14: executes this statement as part of this file's behavior
];  // line 15: executes this statement as part of this file's behavior

export function registerSecretValue(value: string): void {  // line 17: executes this statement as part of this file's behavior
  if (value.length >= 4) knownSecretValues.add(value);  // line 18: executes this statement as part of this file's behavior
}  // line 19: executes this statement as part of this file's behavior

export function unregisterSecretValue(value: string): void {  // line 21: executes this statement as part of this file's behavior
  knownSecretValues.delete(value);  // line 22: executes this statement as part of this file's behavior
}  // line 23: executes this statement as part of this file's behavior

export function redactSecrets(input: string): string {  // line 25: executes this statement as part of this file's behavior
  let output = input;  // line 26: executes this statement as part of this file's behavior
  for (const value of knownSecretValues) {  // line 27: executes this statement as part of this file's behavior
    output = output.split(value).join("[REDACTED_SECRET]");  // line 28: executes this statement as part of this file's behavior
  }  // line 29: executes this statement as part of this file's behavior
  for (const pattern of tokenPatterns) {  // line 30: executes this statement as part of this file's behavior
    output = output.replace(pattern, "[REDACTED_SECRET]");  // line 31: executes this statement as part of this file's behavior
  }  // line 32: executes this statement as part of this file's behavior
  return output;  // line 33: executes this statement as part of this file's behavior
}  // line 34: executes this statement as part of this file's behavior
