const knownSecretValues = new Set<string>();

const tokenPatterns: RegExp[] = [
  /[A-Za-z0-9_-]{23,}\.[A-Za-z0-9_-]{6,}\.[A-Za-z0-9_-]{20,}/g,
  /gh[pousr]_[A-Za-z0-9_]{20,}/g,
  /(?:xox[baprs]-)[A-Za-z0-9-]{10,}/g,
  /https?:\/\/[^\s:@]+:[^\s@]+@[^\s]+/g,
  /-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----/g,
];

export function registerSecretValue(value: string): void {
  if (value.length >= 4) knownSecretValues.add(value);
}

export function unregisterSecretValue(value: string): void {
  knownSecretValues.delete(value);
}

export function redactSecrets(input: string): string {
  let output = input;
  for (const value of knownSecretValues) {
    output = output.split(value).join("[REDACTED_SECRET]");
  }
  for (const pattern of tokenPatterns) {
    output = output.replace(pattern, "[REDACTED_SECRET]");
  }
  return output;
}
