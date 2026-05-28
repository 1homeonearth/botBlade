export const BOT_PROFILE_SCHEMA_VERSION = "1.0.0";

export type PackageManager = "npm" | "pnpm" | "yarn" | "pip" | "unknown";

export interface BotProfilePackEvidence {
  id: string;
  name: string;
  score: number;
  confidence: "weak" | "possible" | "likely" | "high";
  matchedEvidence: string[];
}

export interface BotProfileCommandPlan {
  install: string[];
  build: string[];
  test: string[];
  validate: string[];
  start: string[];
  stop: string[];
  restart: string[];
  deploy: string[];
}

export interface BotProfileSecretRequirement {
  name: string;
  required: boolean;
  configured: boolean;
}

export interface BotProfileGitMetadata {
  branch: string | null;
  status: "clean" | "dirty" | "unknown";
  remotes: Array<{ name: string; url: string | null }>;
}

export interface BotProfileRepairCard {
  title: string;
  evidence?: string;
  safeAction: string;
}

export interface BotProfile {
  schemaVersion: typeof BOT_PROFILE_SCHEMA_VERSION;
  generatedBy: "botblade";
  generatedAt: string;
  project: {
    name: string;
    type: string;
    root: string;
    importSource: { kind: string; url?: string };
  };
  runtime: {
    type: string;
    version: string;
    packageManager: PackageManager;
    detectedLanguages: string[];
    detectedFrameworks: string[];
  };
  bladePack: {
    selected: string;
    version: string;
    detected: BotProfilePackEvidence[];
  };
  commandPlan: BotProfileCommandPlan;
  secrets: {
    required: BotProfileSecretRequirement[];
    optional: BotProfileSecretRequirement[];
  };
  permissions: string[];
  capabilities: string[];
  importantFiles: string[];
  warnings: string[];
  repairCards: BotProfileRepairCard[];
  git?: BotProfileGitMetadata;
}
