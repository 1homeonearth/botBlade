# Durable persistence, secrets, backup, and restore

botBlade supports durable service metadata through persistence interfaces and a checked-in SQLite adapter for local and development use. The same interfaces are intended to be implemented by a Postgres adapter for production deployments that provide `DATABASE_URL` / `BOTBLADE_DATABASE_URL` with a `postgres://` or `postgresql://` URL.

## What is persisted

The persistence layer covers:

- projects and project configuration
- secret metadata, fingerprints, and encrypted secret references
- audit events
- build jobs and build logs
- deployment targets
- deployment jobs and deployment logs
- runtime metadata table reserved for process status snapshots

Migrations live in `backend/migrations/` and are applied automatically by the SQLite adapter at startup. The `schema_migrations` table records applied migration file versions.

## Configuration

For local/dev SQLite persistence, set:

```bash
export BOTBLADE_DATABASE_URL=sqlite://./backend/data/botblade.sqlite
export BOTBLADE_SECRET_KEY=$(openssl rand -hex 32)
npm --prefix backend run start
```

If no database URL is set, non-test server runs use `sqlite://./backend/data/botblade.sqlite`. Tests stay in memory unless a database URL is explicitly set.

`BOTBLADE_SECRET_KEY` accepts a 64-character hex key, a 32-byte base64 key, or any passphrase. Passphrases are hashed with SHA-256 before use. Local development falls back to a deterministic development key, but production deployments must provide a managed secret key.

## Secret storage model

Normal tables store only secret summaries and fingerprints. Plaintext values are encrypted with AES-256-GCM and stored in `secret_values.encrypted_value`; API responses use `SecretSummary` and never include `value`. If an external vault is introduced, implement `SecretStorePersistence` so normal tables keep only vault references and fingerprints.

## Backup

SQLite backup should run while the service is stopped or through SQLite's online `VACUUM INTO` flow:

```bash
sqlite3 backend/data/botblade.sqlite "VACUUM INTO 'backups/botblade-$(date -u +%Y%m%dT%H%M%SZ).sqlite';"
```

Also back up the current `BOTBLADE_SECRET_KEY`; encrypted secret values cannot be decrypted without it.

## Restore

1. Stop the backend service.
2. Restore the secret key used for the backup: `export BOTBLADE_SECRET_KEY=...`.
3. Replace the database file with the backup copy:

   ```bash
   cp backups/botblade-YYYYMMDDTHHMMSSZ.sqlite backend/data/botblade.sqlite
   ```

4. Start the backend. Migrations run automatically and bring older backups forward.
5. Verify with `GET /api/health`, `GET /api/projects`, and `GET /api/audit-events`.

For Postgres production deployments, use managed snapshots or `pg_dump`/`pg_restore`, and store `BOTBLADE_SECRET_KEY` in the same disaster recovery system as the database credentials.
