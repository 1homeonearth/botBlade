export type BladePackSchemaVersion = "2026-05-phase4";

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

export type BladePackCommandName = "install" | "build" | "test" | "validate" | "start" | "stop" | "restart" | "deploy";

export type BladePackCommandMap = Partial<Record<BladePackCommandName, string>>;

export type BladePackImportantFileKind = "entrypoint" | "config" | "workflow" | "commandDirectory" | "packageManifest";

export type BladePackImportantFilePattern = {
  kind: BladePackImportantFileKind;
  pattern: string;
  label?: string;
  required?: boolean;
};

export type BladePackRepairSeverity = "info" | "warning" | "error";

export type BladePackRepairRule = {
  id: string;
  title: string;
  severity: BladePackRepairSeverity;
  evidencePattern: string;
  safeAction: string;
  commandHint?: BladePackCommandName;
  affectedFiles?: string[];
};

export type BladePackSecretDetectorSource = "envExample" | "sourceReference" | "workflowCredentialReference" | "frameworkConvention";

export type BladePackSecretDetector = {
  name: string;
  label: string;
  required: boolean;
  sources: BladePackSecretDetectorSource[];
  patterns: string[];
};

export type BladePackTemplateOption = {
  id: string;
  label: string;
  description: string;
  runtime?: BladePackRuntime;
  starterFiles?: string[];
  backendTemplateRef?: string;
};

export type BladePackImportModeKind = "repository" | "zip" | "folder" | "workflow_json" | "template";

export type BladePackImportMode = {
  kind: BladePackImportModeKind;
  notes: string;
};

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
  schemaVersion: BladePackSchemaVersion;
  id: string;
  name: string;
  version: string;
  license: string;
  runtime: BladePackRuntime;
  detectors: BladePackDetector[];
  templateOptions: BladePackTemplateOption[];
  /** @deprecated Use templateOptions for labeled, runtime-aware templates. */
  templates?: string[];
  commands: BladePackCommandMap;
  importantFilePatterns: BladePackImportantFilePattern[];
  repairRules: BladePackRepairRule[];
  secretDetectors: BladePackSecretDetector[];
  /** @deprecated Use secretDetectors for declaration-only secret requirements. */
  secrets: BladePackSecret[];
  diagnostics: BladePackDiagnostic[];
  panels: string[];
  docs: BladePackDoc[];
  importModes: BladePackImportMode[];
  /** @deprecated Use importModes. */
  supportedImports?: BladePackSupportedImport[];
};
