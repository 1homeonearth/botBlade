import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import os from 'node:os';
import path from 'node:path';
import { classifyProjectWorkspace } from '../services/projectClassifier.js';

test('classifies package.json framework and package manager', async () => {
  const root = path.join(os.tmpdir(), `botblade-classify-${randomUUID()}`);
  await fs.mkdir(root, { recursive: true });
  await fs.writeFile(path.join(root, 'package.json'), JSON.stringify({
    name: 'demo-bot',
    scripts: { build: 'tsc', start: 'node dist/index.js' },
    dependencies: { 'discord.js': '^14.0.0' },
    devDependencies: { typescript: '^5.0.0' },
    main: 'dist/index.js',
  }), 'utf8');
  await fs.writeFile(path.join(root, 'package-lock.json'), '{}', 'utf8');
  const result = await classifyProjectWorkspace(root);
  assert.equal(result.framework, 'discord.js');
  assert.equal(result.packageManager, 'npm');
  assert.equal(result.language, 'typescript');
  assert.ok(result.scripts.includes('build'));
});
