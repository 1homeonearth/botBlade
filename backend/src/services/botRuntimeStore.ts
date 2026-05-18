import type { BotRuntimeStatus } from "../models/project.js";

export class BotRuntimeStore {
  private status: BotRuntimeStatus = {
    running: false,
    projectId: "default",
    status: "stopped",
    message: "No bot runtime is currently running.",
  };

  getStatus(): BotRuntimeStatus {
    return this.status;
  }

  toggle(action: "start" | "stop"): BotRuntimeStatus {
    this.status = action === "start"
      ? {
          running: true,
          projectId: "default",
          status: "running",
          message: "Bot marked as running in local compatibility mode.",
        }
      : {
          running: false,
          projectId: "default",
          status: "stopped",
          message: "Bot marked as stopped in local compatibility mode.",
        };
    return this.status;
  }
}
