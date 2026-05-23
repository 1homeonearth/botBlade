// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
import type { BotRuntimeStatus } from "../models/project.js";  // line 7: executes this statement as part of this file's behavior

export class BotRuntimeStore {  // line 9: executes this statement as part of this file's behavior
  private status: BotRuntimeStatus = {  // line 10: executes this statement as part of this file's behavior
    running: false,  // line 11: executes this statement as part of this file's behavior
    projectId: "default",  // line 12: executes this statement as part of this file's behavior
    status: "stopped",  // line 13: executes this statement as part of this file's behavior
    message: "No bot runtime is currently running.",  // line 14: executes this statement as part of this file's behavior
  };  // line 15: executes this statement as part of this file's behavior

  getStatus(): BotRuntimeStatus {  // line 17: executes this statement as part of this file's behavior
    return this.status;  // line 18: executes this statement as part of this file's behavior
  }  // line 19: executes this statement as part of this file's behavior

  toggle(action: "start" | "stop"): BotRuntimeStatus {  // line 21: executes this statement as part of this file's behavior
    this.status = action === "start"  // line 22: executes this statement as part of this file's behavior
      ? {  // line 23: executes this statement as part of this file's behavior
          running: true,  // line 24: executes this statement as part of this file's behavior
          projectId: "default",  // line 25: executes this statement as part of this file's behavior
          status: "running",  // line 26: executes this statement as part of this file's behavior
          message: "Bot marked as running in local compatibility mode.",  // line 27: executes this statement as part of this file's behavior
        }  // line 28: executes this statement as part of this file's behavior
      : {  // line 29: executes this statement as part of this file's behavior
          running: false,  // line 30: executes this statement as part of this file's behavior
          projectId: "default",  // line 31: executes this statement as part of this file's behavior
          status: "stopped",  // line 32: executes this statement as part of this file's behavior
          message: "Bot marked as stopped in local compatibility mode.",  // line 33: executes this statement as part of this file's behavior
        };  // line 34: executes this statement as part of this file's behavior
    return this.status;  // line 35: executes this statement as part of this file's behavior
  }  // line 36: executes this statement as part of this file's behavior
}  // line 37: executes this statement as part of this file's behavior
