// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
export type BladePackRuntime = {  // line 7: executes this statement as part of this file's behavior
  type: "node" | "python" | "bun" | "workflow" | "unknown";  // line 8: executes this statement as part of this file's behavior
  versionRange?: string;  // line 9: executes this statement as part of this file's behavior
  packageManagers?: string[];  // line 10: executes this statement as part of this file's behavior
};  // line 11: executes this statement as part of this file's behavior

export type BladePackDetector =  // line 13: executes this statement as part of this file's behavior
  | { kind: "packageDependency"; name: string; weight: number }  // line 14: executes this statement as part of this file's behavior
  | { kind: "packageScript"; name: string; weight: number }  // line 15: executes this statement as part of this file's behavior
  | { kind: "sourceImport"; pattern: string; weight: number }  // line 16: executes this statement as part of this file's behavior
  | { kind: "envKey"; pattern: string; weight: number }  // line 17: executes this statement as part of this file's behavior
  | { kind: "fileExists"; path: string; weight: number }  // line 18: executes this statement as part of this file's behavior
  | { kind: "knownFilename"; path: string; weight: number }  // line 19: executes this statement as part of this file's behavior
  | { kind: "knownDirectory"; path: string; weight: number }  // line 20: executes this statement as part of this file's behavior
  | { kind: "jsonShape"; file: string; keys: string[]; weight: number };  // line 21: executes this statement as part of this file's behavior

export type BladePackSecret = {  // line 23: executes this statement as part of this file's behavior
  name: string;  // line 24: executes this statement as part of this file's behavior
  label: string;  // line 25: executes this statement as part of this file's behavior
  required: boolean;  // line 26: executes this statement as part of this file's behavior
  example?: string;  // line 27: executes this statement as part of this file's behavior
};  // line 28: executes this statement as part of this file's behavior

export type BladePackDiagnostic = {  // line 30: executes this statement as part of this file's behavior
  pattern: string;  // line 31: executes this statement as part of this file's behavior
  diagnosis: string;  // line 32: executes this statement as part of this file's behavior
  fix: string;  // line 33: executes this statement as part of this file's behavior
  action: string;  // line 34: executes this statement as part of this file's behavior
};  // line 35: executes this statement as part of this file's behavior

export type BladePackDoc = {  // line 37: executes this statement as part of this file's behavior
  label: string;  // line 38: executes this statement as part of this file's behavior
  url: string;  // line 39: executes this statement as part of this file's behavior
};  // line 40: executes this statement as part of this file's behavior

export type BladePackSupportedImport = {  // line 42: executes this statement as part of this file's behavior
  kind: string;  // line 43: executes this statement as part of this file's behavior
  notes: string;  // line 44: executes this statement as part of this file's behavior
};  // line 45: executes this statement as part of this file's behavior

export type BladePack = {  // line 47: executes this statement as part of this file's behavior
  id: string;  // line 48: executes this statement as part of this file's behavior
  name: string;  // line 49: executes this statement as part of this file's behavior
  version: string;  // line 50: executes this statement as part of this file's behavior
  license: string;  // line 51: executes this statement as part of this file's behavior
  runtime: BladePackRuntime;  // line 52: executes this statement as part of this file's behavior
  detectors: BladePackDetector[];  // line 53: executes this statement as part of this file's behavior
  templates: string[];  // line 54: executes this statement as part of this file's behavior
  commands: Partial<Record<"install" | "build" | "test" | "run" | "deploy", string>>;  // line 55: executes this statement as part of this file's behavior
  secrets: BladePackSecret[];  // line 56: executes this statement as part of this file's behavior
  diagnostics: BladePackDiagnostic[];  // line 57: executes this statement as part of this file's behavior
  panels: string[];  // line 58: executes this statement as part of this file's behavior
  docs: BladePackDoc[];  // line 59: executes this statement as part of this file's behavior
  supportedImports: BladePackSupportedImport[];  // line 60: executes this statement as part of this file's behavior
};  // line 61: executes this statement as part of this file's behavior
