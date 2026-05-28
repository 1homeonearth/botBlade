export type BladePackSchemaVersion = "0.2.0";

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

export type BladePackCommands = Partial<Record<BladePackCommandName, string>> & {
  /** @deprecated Use the canonical Phase 4 `start` command instead. */
  run?: string;
};

export type BladePackImportantFileKind = "entrypoint" | "config" | "workflow" | "commandDirectory" | "packageManifest";

export type BladePackImportantFilePattern = {
  kind: BladePackImportantFileKind;
  pattern: string;
  label: string;
  required?: boolean;
};

export type BladePackRepairSeverity = "info" | "warning" | "error" | "critical";

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
  source: BladePackSecretDetectorSource;
  names: string[];
  required: boolean;
  evidencePattern?: string;
  description?: string;
};

export type BladePackStarterFile = {
  path: string;
  description?: string;
};

export type BladePackTemplateOption = {
  id: string;
  label: string;
  description: string;
  runtime: BladePackRuntime["type"];
  starterFiles?: BladePackStarterFile[];
  backendTemplateRef?: string;
};

export type BladePackImportMode = "repository" | "zip" | "folder" | "workflow_json" | "template";

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
  schemaVersion?: BladePackSchemaVersion;
  id: string;
  name: string;
  version: string;
  license: string;
  runtime: BladePackRuntime;
  detectors: BladePackDetector[];
  /** @deprecated Use `templateOptions` for new Blade Pack template metadata. */
  templates?: string[];
  templateOptions?: BladePackTemplateOption[];
  commands: BladePackCommands;
  importantFilePatterns?: BladePackImportantFilePattern[];
  repairRules?: BladePackRepairRule[];
  secrets: BladePackSecret[];
  secretDetectors?: BladePackSecretDetector[];
  diagnostics: BladePackDiagnostic[];
  panels: string[];
  docs: BladePackDoc[];
  importModes?: BladePackImportMode[];
  /** @deprecated Use `importModes` for canonical import support metadata. */
  supportedImports?: BladePackSupportedImport[];
};
