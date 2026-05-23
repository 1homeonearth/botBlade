import assert from "node:assert/strict";
import { describe, it } from "node:test";

import {
  findReferenceCatalogEntriesByHint,
  findReferenceCatalogEntriesByPackage,
  referenceCatalog,
} from "../services/referenceCatalog.js";

describe("referenceCatalog", () => {
  it("contains BotBlade-ready seed entries", () => {
    assert.ok(referenceCatalog.length >= 10);
    assert.ok(referenceCatalog.some((entry) => entry.id === "platform-discord"));
    assert.ok(referenceCatalog.some((entry) => entry.id === "framework-discord-js"));
  });

  it("finds entries by exact package hint", () => {
    const matches = findReferenceCatalogEntriesByPackage("discord.js");

    assert.equal(matches.length, 1);
    assert.equal(matches[0]?.id, "framework-discord-js");
  });

  it("finds entries by detector hint text", () => {
    const matches = findReferenceCatalogEntriesByHint("const { GatewayIntentBits } = require('discord.js');");
    const ids = matches.map((entry) => entry.id);

    assert.ok(ids.includes("platform-discord"));
    assert.ok(ids.includes("framework-discord-js"));
  });
});
