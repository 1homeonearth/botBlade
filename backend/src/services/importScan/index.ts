// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import { scanWorkspaceForBladePacks } from "./detector.js";  // line 7: executes this statement as part of this file's behavior
import { writeBotbladeMetadata } from "./botbladeMetadata.js";  // line 8: executes this statement as part of this file's behavior

export async function scanAndGenerateBotbladeMetadata(workspacePath: string, importSource?: { kind: string; url?: string }) {  // line 10: executes this statement as part of this file's behavior
  const detection = await scanWorkspaceForBladePacks(workspacePath);  // line 11: executes this statement as part of this file's behavior
  const botbladeJsonPath = await writeBotbladeMetadata(workspacePath, detection, importSource);  // line 12: executes this statement as part of this file's behavior
  return { detection, botbladeJsonPath };  // line 13: executes this statement as part of this file's behavior
}  // line 14: executes this statement as part of this file's behavior
