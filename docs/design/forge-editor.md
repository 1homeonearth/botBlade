# Forge Editor

BotBlade's editor should be shaped like a mobile IDE for bot projects, with Monaco Editor and VS Code as the primary inspiration, CodeMirror 6 as the lighter embeddable editor reference, and Acode as the Android-first mobile editing reference.

## Reference stack

Use these projects as inspiration and possible dependency candidates:

- Monaco Editor / VS Code editor model: file models, language services, command palette, diagnostics, diff editor, minimap concepts, view state, and editor actions.
- CodeMirror 6: modular editor extensions, mobile-friendly embeddability, syntax packages, themes, autocomplete, lint extensions, and smaller runtime footprint.
- Acode: Android-first code editing expectations, file browsing, mobile tab behavior, preview workflows, plugin ideas, and command ergonomics.

## BotBlade editor identity

BotBlade should not become a generic text editor. The editor exists to import, repair, edit, run, deploy, audit, and ship bots.

The editor should understand:

```text
package.json
package-lock.json
tsconfig.json
.env.example
.env.local
botblade manifest files
Discord command modules
Telegram bot entrypoints
Slack Bolt apps
webhook handlers
Dockerfile
docker-compose files
GitHub workflow files
Markdown docs
YAML configs
shell scripts
Kotlin/Java Android support files
Python/Rust bot files
```

## Near-term mobile UI

The first practical layer should show:

- Project tree.
- Current file buffer.
- Language badge.
- Dirty/saved badge.
- Save and revert actions.
- Project scan cards.
- Missing secrets card.
- Build button.
- First-check button.
- Command palette card.
- Container terminal lane.

## IDE-quality capabilities

Future implementation should add:

- Multi-tab buffers.
- Search current file.
- Search project.
- Find and replace.
- Syntax highlighting.
- Format file.
- Diff view.
- Problem panel.
- Command palette.
- Bot map.
- Manifest editor.
- Secrets repair panel.
- Build diagnostics.
- Runtime logs panel.
- Container terminal panel.
- Import repair queue.
- Git status ribbon.

## Integration rules

Keep production code BotBlade-native. Use upstream editors as inspiration or bundled dependencies only when their license and size fit the Android package.

If Monaco is too heavy for the Android WebView surface, prefer CodeMirror 6 for the embedded editor and keep Monaco as the design reference for models, commands, diagnostics, and diff behavior.

A writable terminal must attach only to the selected BotBlade-managed container. Do not route editor terminal commands to the Android host shell.
