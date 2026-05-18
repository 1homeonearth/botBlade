# Generated Bot Template

Generated bots are written under `backend/generated-projects/<project-id>/`.

## Structure

```text
package.json
tsconfig.json
Dockerfile
.dockerignore
.env.example
README.md
src/config.ts
src/config.test.ts
src/index.ts
src/node-env.d.ts
src/commands/<command>.ts
src/commands/load.test.ts
```

## package.json

Includes scripts:

- `npm run build` -> `tsc -p tsconfig.json`
- `npm start` -> `node dist/index.js`
- `npm test` -> Node test runner if generated tests exist

Dependencies include `discord.js`; dev dependencies include `typescript`.

## tsconfig.json

Targets ES2022 with `NodeNext` module and module resolution, outputs compiled JavaScript to `dist/`, and uses strict TypeScript checking.

## .env.example

Contains placeholder text only:

```text
DISCORD_TOKEN=<secret reference resolved at runtime>
```

No real token or sample token value should be added.

## src/config.ts and src/index.ts

`src/config.ts` reads `DISCORD_TOKEN` from the environment and fails clearly if it is missing. `src/index.ts` imports generated commands, loads config, creates a `discord.js` client, and logs readiness. Runtime service injects the token from the backend secret store for local development.

## Dockerfile

The generated Dockerfile uses Node 22 Alpine stages for dependencies, build, and runtime. It expects secrets to be supplied at runtime, not baked into the image.

## Command modules

Each configured command becomes `src/commands/<name>.ts`. If a project has no commands, the generator emits a default `ping` command.

## Manual run

From the generated project directory:

```bash
npm install
npm run build
DISCORD_TOKEN=<placeholder-from-your-secret-manager> npm start
```

Do not commit real `.env` files or token values.
