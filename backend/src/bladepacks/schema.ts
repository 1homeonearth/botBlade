export type BladePackRuntime = {
  type: "node" | "python" | "bun" | "workflow" | "unknown";
  versionRange?: string;
  packageManagers?: string[];
};

export type BladePackDetector =
  | { kind: "packageDependency"; name: string; weight: number }
  | { kind: "packageScript"; name: string; weight: number }
  | { kind: "sourceImport"; pattern: string; weight: number }
  | { kind: "envKey"; pattern: string; weight: number }
  | { kind: "fileExists"; path: string; weight: number }
  | { kind: "knownFilename"; path: string; weight: number }
  | { kind: "knownDirectory"; path: string; weight: number }
  | { kind: "jsonShape"; file: string; keys: string[]; weight: number };

export type BladePackSecret = {
  name: string;
  label: string;
  required: boolean;
  example?: string;
};

export type BladePackDiagnostic = {
  pattern: string;
  diagnosis: string;
  fix: string;
  action: string;
};

export type BladePackDoc = {
  label: string;
  url: string;
};

export type BladePackSupportedImport = {
  kind: string;
  notes: string;
};

export type BladePack = {
  id: string;
  name: string;
  version: string;
  license: string;
  runtime: BladePackRuntime;
  detectors: BladePackDetector[];
  templates: string[];
  commands: Partial<Record<"install" | "build" | "test" | "run" | "deploy", string>>;
  secrets: BladePackSecret[];
  diagnostics: BladePackDiagnostic[];
  panels: string[];
  docs: BladePackDoc[];
  supportedImports: BladePackSupportedImport[];
};
