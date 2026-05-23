# Container Terminal

BotBlade should include a terminal that attaches only to the selected bot runtime container. It is a project tool, not a general Android shell and not a host shell.

## Scope

The terminal belongs to one project and one runtime target. The app should show the project name, runtime target, container boundary, working directory, shell mode, and session age.

Default mode is readonly logs. Writable shell access requires a backend-created project session.

## Reference direction

Use xterm.js as the terminal UI reference. Use ttyd as a websocket terminal behavior reference. Do not expose a generic shell directly from the phone or backend host.

## Backend boundary

The backend owns the command, working directory, target container, timeout, and audit event. A terminal session should attach to a BotBlade-managed container with labels such as:

```text
com.botblade.managed=true
com.botblade.project.id=<project-id>
com.botblade.runtime.target=<target-id>
```

The backend must verify authentication, project authorization, runtime ownership, container labels, and workspace path containment before opening a writable session.

## Suggested API shape

```text
GET /api/projects/:projectId/runtime-terminal/session
WS /api/projects/:projectId/runtime-terminal/ws/:sessionId
POST /api/projects/:projectId/runtime-terminal/:sessionId/resize
DELETE /api/projects/:projectId/runtime-terminal/:sessionId
```

## First UI slice

Add a Container Terminal panel with readonly logs, an open shell button, copy output, clear screen, send interrupt, resize handling, session timer, and a visible boundary badge.

## Audit events

Record session created, attached, resized, interrupted, closed, expired, and denied events. Do not record terminal keystrokes or full output by default.
