# BotBlade workstation actions

This branch attaches more visible UI controls to concrete behavior.

## Editor

The missing-secrets scan card now opens Settings/Vault when tapped. After a scan, the card explains whether required secrets were found and gives the user a direct action.

## Ops Deck

Ops Deck now has a visible Release Checklist button. It renders release readiness into the terminal/log pane using the current project, latest successful build, selected target, and release order.

## Dashboard

Dashboard cleanup should continue on a fresh branch from the current main because this branch started before later main changes and is behind. The intended Dashboard shape is four lanes: Start, Prepare, Build & Run, and Deploy.