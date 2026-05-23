# Commercial UI Framework

BotBlade should feel like a polished commercial developer app while keeping its own black forge identity with baby-blue and hot-pink highlights.

## Product frame

Use a command-center pattern similar to commercial developer tools: a strong hero, active workspace state, primary actions, health cards, progressive capability lanes, and clear next actions.

The app should always answer these questions on the first screen:

- What project is active?
- Is the backend reachable?
- Is the bot running?
- What can I do next?
- What needs repair?
- Where are secrets, logs, updates, deployment targets, and Git state?

## Navigation model

Keep the bottom navigation simple:

```text
Dashboard
Projects
Editor
Deployments
Settings
```

Each tab owns a user job:

- Dashboard: command center, workspace health, runtime controls, and release/update status.
- Projects: create, import, classify, archive, and select workspaces.
- Editor: edit, scan, repair, build, inspect, and open container terminal.
- Deployments: build history, runtime targets, logs, rollback, and deploy actions.
- Settings: backend, updates, identity, GitHub, secrets, appearance, storage, diagnostics, and advanced controls.

## Visual system

Use black as the base, deep ink panels, baby-blue for primary structure, and hot pink for urgency, selection, and high-energy affordances.

Recommended roles:

```text
Black: app background
Ink: code areas and terminal blocks
Raised panel: hero and important surfaces
Baby blue: primary identity, headings, active controls
Hot pink: selected state, high-energy labels, warnings, CTAs
Muted blue-gray: explanatory text
```

## Feature sets

The visible framework should keep these lanes present even while implementation matures:

- Import from Git, zip, folder, and Blade Pack templates.
- Repair project structure, dependencies, secrets, entrypoints, and unsafe defaults.
- Edit with IDE-like models, language modes, diagnostics, and command palette behavior.
- Run inside BotBlade-managed runtime targets.
- Open terminal sessions only inside the selected bot container.
- Deploy through local process, local Docker, GitHub workflow, and later cloud targets.
- Observe logs, audit events, crashes, health, startup diagnostics, and build failures.
- Update via GitHub release checks with APK handoff and stable signing guidance.

## User-friendliness rules

Every screen should provide one obvious primary action, one status summary, and a repair path. Empty states should explain the next action in concrete product language. Settings should group controls by user job, not by internal implementation.

The app should surface advanced power without making first-run users configure everything at once.
