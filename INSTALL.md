# BotBlade Installation Guide

## End users (Android)

1. Go to [Latest Release](../../releases/latest).
2. Download `bot-blade.apk`.
3. Open the APK on your Android device and complete install prompts.
4. Optional but recommended: verify `SHA256SUMS.txt` before installing.

Checksum verification:

```bash
sha256sum -c SHA256SUMS.txt
```

## Developers

### Requirements

- Android SDK + command-line tools
- JDK compatible with current Gradle config
- Node.js 22+

### Build Android debug APK

```bash
gradle :app:assembleDebug
```

### Run backend locally

```bash
cd backend
npm install
npm run preflight:node
npm run build
BOTBLADE_SECRET_KEY=$(openssl rand -hex 32) npm start
```

Then set the app backend URL (Settings) to `http://10.0.2.2:8000` (emulator) or your LAN host IP.
