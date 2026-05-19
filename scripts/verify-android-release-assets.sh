#!/usr/bin/env bash
set -euo pipefail

required_mipmap_xml=(
  app/src/main/res/mipmap-mdpi/ic_launcher.xml
  app/src/main/res/mipmap-mdpi/ic_launcher_round.xml
  app/src/main/res/mipmap-hdpi/ic_launcher.xml
  app/src/main/res/mipmap-hdpi/ic_launcher_round.xml
  app/src/main/res/mipmap-xhdpi/ic_launcher.xml
  app/src/main/res/mipmap-xhdpi/ic_launcher_round.xml
  app/src/main/res/mipmap-xxhdpi/ic_launcher.xml
  app/src/main/res/mipmap-xxhdpi/ic_launcher_round.xml
  app/src/main/res/mipmap-xxxhdpi/ic_launcher.xml
  app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.xml
)

for file in "${required_mipmap_xml[@]}"; do
  if [[ ! -s "$file" ]]; then
    echo "Missing launcher icon XML: $file" >&2
    exit 1
  fi
  if rg -q "\.png" "$file"; then
    echo "Launcher XML should not reference PNG assets: $file" >&2
    exit 1
  fi
  echo "Verified $file"
done

required_xml=(
  app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
  app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
  app/src/main/res/drawable/ic_launcher_foreground.xml
  app/src/main/res/xml/network_security_config.xml
  app/src/debug/res/xml/debug_network_security_config.xml
)

for file in "${required_xml[@]}"; do
  if [[ ! -s "$file" ]]; then
    echo "Missing or empty Android release asset/config: $file" >&2
    exit 1
  fi
  echo "Verified $file"
done

if ! rg -q 'android:allowBackup="false"' app/src/main/AndroidManifest.xml; then
  echo 'Release/main manifest must disable backup.' >&2
  exit 1
fi
if ! rg -q 'android:usesCleartextTraffic="false"' app/src/main/AndroidManifest.xml; then
  echo 'Release/main manifest must disable cleartext traffic.' >&2
  exit 1
fi
if ! rg -q 'android:allowBackup="true"' app/src/debug/AndroidManifest.xml; then
  echo 'Debug manifest must explicitly allow local backup workflows.' >&2
  exit 1
fi
if ! rg -q 'android:usesCleartextTraffic="true"' app/src/debug/AndroidManifest.xml; then
  echo 'Debug manifest must explicitly allow local cleartext traffic.' >&2
  exit 1
fi

echo 'Android release asset and manifest verification passed.'
