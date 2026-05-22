import { scanWorkspaceForBladePacks } from "./detector.js";
import { writeBotbladeMetadata } from "./botbladeMetadata.js";

export async function scanAndGenerateBotbladeMetadata(workspacePath: string, importSource?: { kind: string; url?: string }) {
  const detection = await scanWorkspaceForBladePacks(workspacePath);
  const botbladeJsonPath = await writeBotbladeMetadata(workspacePath, detection, importSource);
  return { detection, botbladeJsonPath };
}
