# Android release signing secrets

Configure these repository secrets for signed release APK output in `.github/workflows/android.yml`:

- `KEYSTORE_BASE64`: Base64-encoded Android signing keystore file contents.
- `KEYSTORE_PASSWORD`: Keystore password (also used as key password in the workflow).
- `KEY_ALIAS`: Alias name of the signing key entry inside the keystore.

If any of these secrets are missing, the signing step is skipped automatically and release uploads fall back to the unsigned APK.
