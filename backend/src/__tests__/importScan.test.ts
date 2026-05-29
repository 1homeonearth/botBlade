import test from "node:test";
import assert from "node:assert/strict";
import * as path from "node:path";
import fs from "node:fs/promises";
import os from "node:os";
import { scanWorkspaceForBladePacks } from "../services/importScan/detector.js";
import { writeBotbladeMetadata } from "../services/importScan/botbladeMetadata.js";
import { detectScriptProfiles } from "../services/scriptProfiles/scriptProfileDetector.js";

const fixturesRoot = path.join(
  process.cwd(),
  "src",
  "__tests__",
  "fixtures",
  "import-scan",
);

async function copyFixture(fixtureName: string, targetPath: string): Promise<void> {
  await copyDirectory(path.join(fixturesRoot, fixtureName), targetPath);
}

async function copyDirectory(sourcePath: string, targetPath: string): Promise<void> {
  await fs.mkdir(targetPath, { recursive: true });
  const entries = await fs.readdir(sourcePath, { withFileTypes: true });
  for (const entry of entries) {
    const sourceEntryPath = path.join(sourcePath, entry.name);
    const targetEntryPath = path.join(targetPath, entry.name);
    if (entry.isDirectory()) {
      await copyDirectory(sourceEntryPath, targetEntryPath);
    } else if (entry.isFile()) {
      await fs.writeFile(targetEntryPath, await fs.readFile(sourceEntryPath, "utf8"), "utf8");
    }
  }
}

test("detects discord-js from package dependency", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "discord"),
  );
  assert.equal(result.recommendedPackId, "discord-js");
});

test("detects telegraf", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "telegraf"),
  );
  assert.equal(result.recommendedPackId, "telegraf");
});

test("detects slack-bolt", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "slack"),
  );
  assert.equal(result.recommendedPackId, "slack-bolt");
});

test("detects generic-python", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "python"),
  );
  assert.equal(result.recommendedPackId, "generic-python");
});

test("detects generic-shell from scripts and shell task metadata", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "shell"),
  );

  assert.equal(result.recommendedPackId, "generic-shell");
  assert.equal(result.commandPlan.start.length, 0);
  assert.equal(result.commandPlan.stop.length, 0);
  assert.equal(result.commandPlan.restart.length, 0);
  assert.equal(result.commandPlan.validate.length, 0);
  assert.deepStrictEqual(result.detectedLanguages, ["shell"]);
  assert.ok(
    result.matches
      .find((match) => match.id === "generic-shell")
      ?.matchedEvidence.includes("dir:scripts"),
  );
  assert.ok(result.importantFiles.includes("Makefile"));
  assert.ok(result.importantFiles.includes("Taskfile.yml"));
  assert.ok(result.importantFiles.includes("justfile"));
});

test("mixed Node Python shell repositories produce deterministic profile signals", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-mixed-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );

  try {
    await fs.mkdir(path.join(workspace, "src"), { recursive: true });
    await fs.mkdir(path.join(workspace, "scripts"), { recursive: true });
    await fs.mkdir(path.join(workspace, ".github", "workflows"), { recursive: true });
    await fs.mkdir(path.join(workspace, ".botpress"), { recursive: true });
    await fs.writeFile(
      path.join(workspace, "package.json"),
      JSON.stringify({
        name: "mixed-bot",
        scripts: { start: "node src/index.ts" },
        dependencies: { "@botpress/client": "^1.0.0" },
        devDependencies: { typescript: "^5.0.0" },
      }),
      "utf8",
    );
    await fs.writeFile(path.join(workspace, "src", "index.ts"), "console.log(process.env.PORT);\n", "utf8");
    await fs.writeFile(path.join(workspace, "main.py"), "print('hello')\n", "utf8");
    await fs.writeFile(path.join(workspace, "requirements.txt"), "pytest\n", "utf8");
    await fs.writeFile(path.join(workspace, "scripts", "deploy.sh"), "#!/usr/bin/env bash\necho deploy\n", "utf8");
    await fs.writeFile(path.join(workspace, "Dockerfile"), "FROM node:22-alpine\n", "utf8");
    await fs.writeFile(path.join(workspace, "docker-compose.yml"), "services: {}\n", "utf8");
    await fs.writeFile(path.join(workspace, ".github", "workflows", "ci.yml"), "name: ci\n", "utf8");
    await fs.writeFile(path.join(workspace, "Makefile"), "test:\n\techo test\n", "utf8");
    await fs.writeFile(path.join(workspace, "Taskfile.yml"), "version: '3'\n", "utf8");
    await fs.writeFile(path.join(workspace, "justfile"), "test:\n\techo test\n", "utf8");
    await fs.writeFile(path.join(workspace, "botpress.config.json"), "{}\n", "utf8");
    await fs.writeFile(path.join(workspace, ".botpress", "bot.json"), "{}\n", "utf8");

    const result = await scanWorkspaceForBladePacks(workspace);

    assert.deepStrictEqual(result.detectedLanguages, ["javascript", "typescript", "python", "shell"]);
    assert.deepStrictEqual(result.detectedFrameworks, [
      "Generic Node Project",
      "Botpress Bot-as-Code",
      "Generic Python Project",
    ]);
    assert.equal(
      result.matches.find((match) => match.id === "generic-shell")?.score,
      15,
    );
    assert.deepStrictEqual(
      result.importantFiles.filter((file) =>
        [
          ".botpress/bot.json",
          ".github/workflows/ci.yml",
          "Dockerfile",
          "Makefile",
          "Taskfile.yml",
          "botpress.config.json",
          "docker-compose.yml",
          "justfile",
          "package.json",
          "requirements.txt",
          "scripts/deploy.sh",
        ].includes(file),
      ),
      [
        "botpress.config.json",
        "justfile",
        "Makefile",
        "package.json",
        "requirements.txt",
        "Taskfile.yml",
        "docker-compose.yml",
        "Dockerfile",
        ".github/workflows/ci.yml",
        ".botpress/bot.json",
        "scripts/deploy.sh",
      ],
    );
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("make automation remains supplemental when a language manifest is present", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-manifest-make-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );

  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(path.join(workspace, "pyproject.toml"), "[project]\nname = \"python-bot\"\n", "utf8");
    await fs.writeFile(path.join(workspace, "Makefile"), "test:\n\techo test\n", "utf8");

    const result = await scanWorkspaceForBladePacks(workspace);

    assert.equal(result.matches[0]?.id, "generic-python");
    assert.equal(result.matches.find((match) => match.id === "generic-shell")?.score, 15);
    assert.deepStrictEqual(result.detectedLanguages, ["python", "shell"]);
    assert.deepStrictEqual(result.commandPlan.install, ["pip install -r requirements.txt"]);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("detects generic-shell from a standalone Makefile", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "makefile"),
  );

  assert.equal(result.recommendedPackId, "generic-shell");
  assert.ok(
    result.matches
      .find((match) => match.id === "generic-shell")
      ?.matchedEvidence.includes("file:Makefile"),
  );
});

test("generic-shell evidence does not displace stronger framework packs", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-stronger-pack-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await copyFixture("discord", workspace);
    await fs.mkdir(path.join(workspace, "scripts"), { recursive: true });
    await fs.writeFile(
      path.join(workspace, "scripts", "validate.sh"),
      "#!/usr/bin/env bash\necho validate\n",
      "utf8",
    );

    const result = await scanWorkspaceForBladePacks(workspace);

    assert.equal(result.recommendedPackId, "discord-js");
    assert.ok(result.matches.some((match) => match.id === "generic-shell"));
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("detects n8n workflow", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "n8n"),
  );
  assert.equal(result.recommendedPackId, "n8n-workflow");
});

test("unknown folder returns unknown", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "unknown"),
  );
  assert.equal(result.recommendedPackId, "unknown");
});

test("dependency prefix does not trigger exact package detector", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "prefix"),
  );
  assert.equal(result.recommendedPackId, "unknown");
});


test("important files prioritize root manifests before bulk generated entries", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-important-priority-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );

  try {
    await fs.mkdir(path.join(workspace, ".github", "workflows"), { recursive: true });
    await fs.mkdir(path.join(workspace, ".botpress"), { recursive: true });
    await fs.writeFile(path.join(workspace, "package.json"), JSON.stringify({ scripts: { start: "node index.js" } }), "utf8");
    await fs.writeFile(path.join(workspace, "requirements.txt"), "pytest\n", "utf8");
    await fs.writeFile(path.join(workspace, "pyproject.toml"), "[project]\nname = \"priority\"\n", "utf8");
    for (let index = 0; index < 18; index += 1) {
      await fs.writeFile(path.join(workspace, ".github", "workflows", `early-${String(index).padStart(2, "0")}.yml`), "name: ci\n", "utf8");
      await fs.writeFile(path.join(workspace, ".botpress", `early-${String(index).padStart(2, "0")}.json`), "{}\n", "utf8");
    }

    const detection = await scanWorkspaceForBladePacks(workspace);

    assert.equal(detection.importantFiles.length, 20);
    assert.ok(detection.importantFiles.includes("package.json"));
    assert.ok(detection.importantFiles.includes("requirements.txt"));
    assert.ok(detection.importantFiles.includes("pyproject.toml"));
    assert.deepStrictEqual(detection.importantFiles.slice(0, 3), [
      "package.json",
      "pyproject.toml",
      "requirements.txt",
    ]);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("missing workspace path returns unknown instead of throwing", async () => {
  const result = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "does-not-exist"),
  );
  assert.equal(result.recommendedPackId, "unknown");
  assert.equal(result.matches.length, 0);
  assert.deepStrictEqual(result.git, { branch: null, status: "unknown", remotes: [] });
});


test("scan detection includes redacted Git branch status and remote metadata", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-git-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );

  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(path.join(workspace, "package.json"), JSON.stringify({ dependencies: { "discord.js": "^14.0.0" } }), "utf8");
    await fs.writeFile(path.join(workspace, "index.js"), "console.log('bot')\n", "utf8");
    const { execFileSync } = await import("node:child_process");
    execFileSync("git", ["-C", workspace, "init"]);
    execFileSync("git", ["-C", workspace, "symbolic-ref", "HEAD", "refs/heads/main"]);
    execFileSync("git", [
      "-C",
      workspace,
      "remote",
      "add",
      "origin",
      "https://user:pass@example.com/repo.git?access_token=abc123&expires=1",
    ]);

    const detection = await scanWorkspaceForBladePacks(workspace);

    assert.equal(detection.git.branch, "main");
    assert.equal(detection.git.status, "unknown");
    assert.equal("dirtyFileCount" in detection.git, false);
    assert.equal(detection.git.remotes[0]?.name, "origin");
    assert.equal(detection.git.remotes[0]?.url?.includes("user:pass"), false);
    assert.equal(detection.git.remotes[0]?.url?.includes("abc123"), false);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("botblade metadata persists script profiles and only secret metadata without raw values", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  const sentinelTokenValue = "DO_NOT_PERSIST_TOKEN_VALUE";
  const sentinelClientIdValue = "DO_NOT_PERSIST_CLIENT_ID_VALUE";

  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(
      path.join(workspace, "package.json"),
      JSON.stringify({
        name: "discord-bot",
        dependencies: { "discord.js": "^14.0.0" },
        scripts: {
          build: "tsc",
          start: `DISCORD_TOKEN=${sentinelTokenValue} node index.js`,
        },
      }),
      "utf8",
    );
    await fs.writeFile(
      path.join(workspace, ".env.example"),
      `DISCORD_TOKEN=${sentinelTokenValue}
CLIENT_ID=${sentinelClientIdValue}`,
      "utf8",
    );

    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection, {
      kind: "git",
      url: "https://github.com/example/repo",
    });
    const metadataText = await fs.readFile(metadataPath, "utf8");
    const metadata = JSON.parse(metadataText) as {
      scriptProfiles: Array<{
        id: string;
        source: string;
        runtime: string;
        command: string[];
        secretRefs: string[];
      }>;
      secrets: {
        required: Array<{ name: string; configured: boolean }>;
        optional: Array<{ name: string; configured: boolean }>;
      };
    };

    const allSecrets = [
      ...metadata.secrets.required,
      ...metadata.secrets.optional,
    ];
    const startScriptProfile = metadata.scriptProfiles.find(
      (profile) => profile.id === "package-json:package.json:start",
    );
    const bladePackStartProfile = metadata.scriptProfiles.find(
      (profile) => profile.id === "blade-pack:bladepack.commands:start",
    );
    assert.ok(allSecrets.length > 0);
    assert.equal(
      allSecrets.every((secret) => secret.configured === false),
      true,
    );
    assert.ok(startScriptProfile);
    assert.equal(startScriptProfile?.source, "package_json");
    assert.equal(startScriptProfile?.runtime, "node");
    assert.deepStrictEqual(startScriptProfile?.command, ["npm", "run", "start"]);
    assert.equal(
      JSON.stringify(startScriptProfile?.secretRefs),
      JSON.stringify(allSecrets.map((secret) => secret.name)),
    );
    assert.ok(bladePackStartProfile);
    assert.equal(bladePackStartProfile?.source, "blade_pack");
    assert.deepStrictEqual(bladePackStartProfile?.command, ["npm", "start"]);
    assert.equal(metadataText.includes(sentinelTokenValue), false);
    assert.equal(metadataText.includes(sentinelClientIdValue), false);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("unknown metadata profile remains valid", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-unknown-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(workspace, { recursive: true });
    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      bladePack: { selected: string };
      project: { type: string };
    };
    assert.equal(metadata.bladePack.selected, "unknown");
    assert.equal(metadata.project.type, "unknown");
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("package manager detection remains correct", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-pm-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(
      path.join(workspace, "pnpm-lock.yaml"),
      "lockfileVersion: '9.0'",
      "utf8",
    );
    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      runtime: { packageManager: string };
    };
    assert.equal(metadata.runtime.packageManager, "pnpm");
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("scored match evidence persists in metadata", async () => {
  const detection = await scanWorkspaceForBladePacks(
    path.join(fixturesRoot, "discord"),
  );
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-evidence-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(workspace, { recursive: true });
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      bladePack: { detected: Array<{ id: string; matchedEvidence: string[] }> };
    };
    const discord = metadata.bladePack.detected.find(
      (m) => m.id === "discord-js",
    );
    assert.ok(discord);
    assert.ok((discord?.matchedEvidence ?? []).length > 0);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});


test("script profile detector normalizes Node package scripts and package manager", async () => {
  const { packageManager, profiles } = await detectScriptProfiles(
    path.join(fixturesRoot, "node"),
  );
  const build = profiles.find((profile) => profile.id === "package-json:package.json:build");
  const deploy = profiles.find((profile) => profile.id === "package-json:package.json:deploy");

  assert.equal(packageManager, "pnpm");
  assert.equal(build?.name, "pnpm: build");
  assert.deepStrictEqual(build?.command, ["pnpm", "run", "build"]);
  assert.equal(deploy?.requiresConfirmation, true);
});


test("script profile detector keeps unsafe package script names as argv tokens", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-unsafe-script-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  const unsafeScriptName = "lint$(touch /tmp/botblade-pwn)`whoami`";

  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(
      path.join(workspace, "package.json"),
      JSON.stringify({ scripts: { [unsafeScriptName]: "eslint ." } }),
      "utf8",
    );

    const { profiles } = await detectScriptProfiles(workspace);
    const profile = profiles.find((candidate) =>
      candidate.command[0] === "npm" &&
      candidate.command[1] === "run" &&
      candidate.command[2] === unsafeScriptName,
    );

    assert.ok(profile);
    assert.deepStrictEqual(profile?.command, ["npm", "run", unsafeScriptName]);

    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      scriptProfiles: Array<{ command: string[] }>;
    };
    assert.ok(
      metadata.scriptProfiles.some(
        (candidate) =>
          JSON.stringify(candidate.command) ===
          JSON.stringify(["npm", "run", unsafeScriptName]),
      ),
    );
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("script profile detector detects Python entrypoints, tests, and requirements install", async () => {
  const { profiles } = await detectScriptProfiles(path.join(fixturesRoot, "python"));
  const main = profiles.find((profile) => profile.id === "file:main.py:start");
  const pytest = profiles.find((profile) => profile.id === "file:python-tests:pytest");
  const install = profiles.find((profile) => profile.id === "file:requirements.txt:install");

  assert.deepStrictEqual(main?.command, ["python", "main.py"]);
  assert.deepStrictEqual(pytest?.command, ["python", "-m", "pytest"]);
  assert.deepStrictEqual(install?.command, ["python", "-m", "pip", "install", "-r", "requirements.txt"]);
  assert.equal(install?.requiresConfirmation, true);
});

test("script profile detector detects shell files under scripts and executable-looking shell files", async () => {
  const { profiles } = await detectScriptProfiles(path.join(fixturesRoot, "shell"));
  const script = profiles.find((profile) => profile.id === "file:scripts/build.sh:shell");
  const repair = profiles.find((profile) => profile.id === "file:repair.bash:shell");

  assert.deepStrictEqual(script?.command, ["bash", "scripts/build.sh"]);
  assert.deepStrictEqual(repair?.command, ["bash", "repair.bash"]);
  assert.equal(repair?.requiresConfirmation, true);
});

test("script profile detector detects common Makefile targets", async () => {
  const { profiles } = await detectScriptProfiles(path.join(fixturesRoot, "makefile"));
  const ids = profiles.map((profile) => profile.id);
  assert.deepStrictEqual(
    ids.filter((id) => id.startsWith("file:makefile:")),
    ["file:makefile:build", "file:makefile:deploy", "file:makefile:install", "file:makefile:test"],
  );
  assert.deepStrictEqual(
    profiles.find((profile) => profile.id === "file:makefile:deploy")?.command,
    ["make", "deploy"],
  );
});

test("script profile detector emits n8n workflow metadata-only profiles", async () => {
  const { profiles } = await detectScriptProfiles(path.join(fixturesRoot, "workflow"));
  const validate = profiles.find((profile) => profile.id === "file:workflow.json:n8n-validate-metadata");
  const exportMetadata = profiles.find((profile) => profile.id === "file:workflow.json:n8n-export-metadata");

  assert.deepStrictEqual(validate?.command, ["botblade", "workflow", "validate", "workflow.json"]);
  assert.deepStrictEqual(exportMetadata?.command, ["botblade", "workflow", "export-metadata", "workflow.json"]);
  assert.equal(validate?.command.includes("n8n"), false);
  assert.equal(exportMetadata?.requiresConfirmation, true);
});

test("scan metadata persists package script profiles from detection", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-node-persist-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await copyFixture("node", workspace);
    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      runtime: { packageManager: string };
      scriptProfiles: Array<{ id: string; source: string; command: string[] }>;
    };

    assert.equal(metadata.runtime.packageManager, "pnpm");
    assert.deepStrictEqual(
      metadata.scriptProfiles.find(
        (profile) => profile.id === "package-json:package.json:build",
      )?.command,
      ["pnpm", "run", "build"],
    );
    assert.equal(
      metadata.scriptProfiles.find(
        (profile) => profile.id === "package-json:package.json:deploy",
      )?.source,
      "package_json",
    );
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("scan metadata persists Python script profiles from detection", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-python-persist-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await copyFixture("python", workspace);
    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      scriptProfiles: Array<{ id: string; source: string; command: string[] }>;
    };

    assert.deepStrictEqual(
      metadata.scriptProfiles.find((profile) => profile.id === "file:main.py:start")
        ?.command,
      ["python", "main.py"],
    );
    assert.deepStrictEqual(
      metadata.scriptProfiles.find(
        (profile) => profile.id === "file:requirements.txt:install",
      )?.command,
      ["python", "-m", "pip", "install", "-r", "requirements.txt"],
    );
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("scan metadata persists shell script profiles from detection", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-shell-persist-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await copyFixture("shell", workspace);
    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      scriptProfiles: Array<{ id: string; source: string; command: string[] }>;
    };

    assert.deepStrictEqual(
      metadata.scriptProfiles.find(
        (profile) => profile.id === "file:scripts/build.sh:shell",
      )?.command,
      ["bash", "scripts/build.sh"],
    );
    assert.deepStrictEqual(
      metadata.scriptProfiles.find((profile) => profile.id === "file:repair.bash:shell")
        ?.command,
      ["bash", "repair.bash"],
    );
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("scan metadata persists Blade Pack command profiles from detection", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-blade-pack-persist-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await copyFixture("discord", workspace);
    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      scriptProfiles: Array<{ id: string; source: string; command: string[] }>;
    };

    assert.deepStrictEqual(
      detection.scriptProfiles.find(
        (profile) => profile.id === "blade-pack:bladepack.commands:start",
      )?.command,
      ["npm", "start"],
    );
    assert.deepStrictEqual(
      metadata.scriptProfiles.find(
        (profile) => profile.id === "blade-pack:bladepack.commands:start",
      )?.command,
      ["npm", "start"],
    );
    assert.equal(
      metadata.scriptProfiles.find(
        (profile) => profile.id === "blade-pack:bladepack.commands:start",
      )?.source,
      "blade_pack",
    );
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("static import scan skips node_modules dist and git directories", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-skip-dirs-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(path.join(workspace, "node_modules", "discord.js"), {
      recursive: true,
    });
    await fs.mkdir(path.join(workspace, "dist"), { recursive: true });
    await fs.mkdir(path.join(workspace, ".git"), { recursive: true });
    await fs.writeFile(
      path.join(workspace, "node_modules", "package.json"),
      JSON.stringify({ scripts: { start: "node ignored.js" } }),
      "utf8",
    );
    await fs.writeFile(
      path.join(workspace, "node_modules", "discord.js", "package.json"),
      JSON.stringify({ name: "discord.js" }),
      "utf8",
    );
    await fs.writeFile(
      path.join(workspace, "dist", "index.js"),
      "import { App } from '@slack/bolt';",
      "utf8",
    );
    await fs.writeFile(path.join(workspace, ".git", "config"), "ignored", "utf8");

    const detection = await scanWorkspaceForBladePacks(workspace);

    assert.equal(detection.recommendedPackId, "unknown");
    assert.deepStrictEqual(detection.importantFiles, []);
    assert.equal(
      detection.scriptProfiles.some((profile) =>
        profile.command.includes("ignored.js"),
      ),
      false,
    );
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("scan detection deduplicates script profiles by command working directory and source", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-dedupe-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(path.join(workspace, ".botpress"), { recursive: true });
    await fs.writeFile(
      path.join(workspace, "package.json"),
      JSON.stringify({ dependencies: { "@botpress": "latest" } }),
      "utf8",
    );

    const detection = await scanWorkspaceForBladePacks(workspace);
    const duplicateBladePackBuildCommands = detection.scriptProfiles.filter(
      (profile) =>
        profile.source === "blade_pack" &&
        profile.workingDirectory === "." &&
        JSON.stringify(profile.command) === JSON.stringify(["npm", "run", "build"]),
    );

    assert.equal(detection.recommendedPackId, "botpress");
    assert.equal(duplicateBladePackBuildCommands.length, 1);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

function cardTitles(result: { diagnostics: { repairCards: Array<{ title: string; safeAction: string }> } }): string[] {
  return result.diagnostics.repairCards.map((card) => card.title);
}

test("repair cards flag unknown profile, low confidence, absent command profiles, and unavailable Git", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "unknown"));
  const titles = cardTitles(result);

  assert.ok(titles.includes("Unknown project profile"));
  assert.ok(titles.includes("No start/build/test command profile found"));
  assert.ok(titles.includes("Git metadata unavailable"));
  assert.equal(
    result.diagnostics.repairCards.every((card) => !/\b(npm|pip|python|bash|sh|git)\s+(?:install|run|start|test|build|init)\b/i.test(card.safeAction)),
    true,
  );
});

test("repair cards flag low detection confidence from weak scan evidence", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-repair-low-confidence-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(path.join(workspace, "package.json"), JSON.stringify({ name: "weak-node" }), "utf8");

    const result = await scanWorkspaceForBladePacks(workspace);

    assert.equal(result.recommendedPackId, "unknown");
    assert.ok(cardTitles(result).includes("Low detection confidence"));
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("repair cards flag missing package.json scripts for Node projects", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-repair-node-scripts-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(path.join(workspace, "package.json"), JSON.stringify({ scripts: { start: "node index.js" } }), "utf8");
    await fs.writeFile(path.join(workspace, "index.js"), "console.log(process.version)\n", "utf8");

    const result = await scanWorkspaceForBladePacks(workspace);
    const card = result.diagnostics.repairCards.find((candidate) => candidate.title === "Missing package.json command scripts");

    assert.ok(card);
    assert.match(card?.evidence ?? "", /build, test/);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("repair cards flag missing Python requirements or pyproject manifest", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-repair-python-manifest-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(path.join(workspace, "main.py"), "import discord\n", "utf8");

    const result = await scanWorkspaceForBladePacks(workspace);

    assert.ok(cardTitles(result).includes("Missing Python dependency manifest"));
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("repair cards flag missing required secret metadata", async () => {
  const result = await scanWorkspaceForBladePacks(path.join(fixturesRoot, "discord"));
  const card = result.diagnostics.repairCards.find((candidate) => candidate.title === "Missing required secret metadata");

  assert.ok(card);
  assert.match(card?.evidence ?? "", /DISCORD_TOKEN/);
  assert.equal(JSON.stringify(result.diagnostics.repairCards).includes("DO_NOT_PERSIST"), false);
});

test("repair cards flag invalid or unsupported n8n workflow shape", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-repair-n8n-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(path.join(workspace, "workflow.json"), JSON.stringify({ nodes: {} }), "utf8");

    const result = await scanWorkspaceForBladePacks(workspace);

    assert.ok(cardTitles(result).includes("Invalid or unsupported n8n workflow shape"));
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("botblade metadata includes generated repair cards", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-repair-metadata-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(workspace, { recursive: true });
    await fs.writeFile(path.join(workspace, "main.py"), "print('repair me')\n", "utf8");
    const detection = await scanWorkspaceForBladePacks(workspace);
    const metadataPath = await writeBotbladeMetadata(workspace, detection);
    const metadata = JSON.parse(await fs.readFile(metadataPath, "utf8")) as {
      repairCards: Array<{ title: string; safeAction: string }>;
    };

    assert.ok(metadata.repairCards.some((card) => card.title === "Missing Python dependency manifest"));
    assert.equal(metadata.repairCards.every((card) => typeof card.safeAction === "string"), true);
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
  }
});

test("static import scan ignores symlinked project paths and keeps profiles project-relative", async () => {
  const workspace = path.join(
    os.tmpdir(),
    `botblade-import-scan-symlink-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  const outside = path.join(
    os.tmpdir(),
    `botblade-import-scan-symlink-outside-${Date.now()}-${Math.random().toString(16).slice(2)}`,
  );
  try {
    await fs.mkdir(path.join(workspace, "scripts"), { recursive: true });
    await fs.mkdir(outside, { recursive: true });
    await fs.writeFile(path.join(outside, "escaped.sh"), "#!/usr/bin/env bash\necho escaped\n", "utf8");
    await fs.symlink(path.join(outside, "escaped.sh"), path.join(workspace, "scripts", "escaped.sh"));
    await fs.writeFile(path.join(workspace, "scripts", "safe.sh"), "#!/usr/bin/env bash\necho safe\n", "utf8");

    const result = await scanWorkspaceForBladePacks(workspace);

    assert.ok(result.importantFiles.includes("scripts/safe.sh"));
    assert.equal(result.importantFiles.includes("scripts/escaped.sh"), false);
    assert.ok(result.scriptProfiles.some((profile) => profile.command.includes("scripts/safe.sh")));
    assert.equal(result.scriptProfiles.some((profile) => profile.command.includes("scripts/escaped.sh")), false);
    assert.ok(result.scriptProfiles.every((profile) => profile.workingDirectory === "." || !profile.workingDirectory.startsWith("..")));
  } finally {
    await fs.rm(workspace, { recursive: true, force: true });
    await fs.rm(outside, { recursive: true, force: true });
  }
});
