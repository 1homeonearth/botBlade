# Royal Scepter Privacy Policy and Data Safety Notes

_Last updated: 2026-05-18_

This repository does not publish a hosted privacy policy URL. Before public distribution, copy this policy to an HTTPS page controlled by the project owner and use that URL in store listings.

## Summary

Royal Scepter is an Android client for managing Discord bot projects through a backend that the user or operator configures. The app is intended to communicate with a Royal Scepter backend API, display project/build/runtime status, and submit user-entered project configuration.

## Data the Android app may process

- Backend API URL saved by the Settings screen.
- Bot project names, descriptions, command definitions, generated file contents, deployment target metadata, build status, runtime status, audit metadata, and GitHub repository metadata returned by the configured backend.
- Secret references and fingerprints returned by the backend. Secret values should not be displayed after creation, and API responses are expected to return metadata only.
- Network diagnostics such as health-check success or failure messages.

## Data not intentionally collected by the Android app

- The Android client does not intentionally collect precise location, contacts, photos, microphone audio, camera data, advertising identifiers, or analytics events.
- The Android client does not include third-party advertising or analytics SDKs.
- The app should not store Discord bot tokens, GitHub tokens, or other production secrets in Android resources, docs, screenshots, or source control.

## Data sharing

The app sends data only to the backend API URL configured by the user/operator. Distribution builds should point to an HTTPS production backend. Local development builds may point to an emulator or LAN backend.

## Security controls

- Release builds disable Android backup and cleartext network traffic by default.
- Debug builds enable cleartext traffic only for local development hostnames/IPs through a debug-only network security config.
- Backend URL validation rejects embedded credentials, query parameters, and fragments.
- Secret values should be handled by backend secret storage and exposed to Android as references/fingerprints only.

## Data retention and deletion

Android stores the backend URL override until the user changes app data or uninstalls the app. Project data, secret metadata, audit logs, generated files, and runtime/deployment records are retained by the configured backend according to that backend operator's retention policy.

## Play Console data safety notes

Use these notes as the starting point for the Play Console Data Safety form; verify them against the exact production backend before submission.

| Data type | Collected? | Shared? | Purpose | Notes |
| --- | --- | --- | --- | --- |
| App activity / app interactions | No third-party analytics in the Android client. | No. | Not applicable. | Backend audit records may log project/build/deployment actions for operational security. |
| User-provided content | Yes, when users create bot projects, commands, generated files, or deployment metadata. | Sent to the configured backend only. | App functionality. | Mark according to production backend behavior. |
| Personal info | Not intentionally requested by the Android client. | No. | Not applicable. | GitHub owner/repository strings may identify organizations or users. |
| Financial info, location, contacts, photos/videos, audio, health, messages, files outside app workflows | No. | No. | Not applicable. | Do not add permissions/SDKs without updating this document. |
| App diagnostics | Not collected by third-party SDKs. | No. | Not applicable. | Local error messages may be displayed in-app. |
| Device identifiers | No advertising ID or analytics identifier collection. | No. | Not applicable. | Android package/install identifiers are not intentionally read by current code. |

## Screenshot privacy rules

Screenshots used in store listings or docs must use placeholder projects, placeholder repository names, and fake secret references. Never include real Discord tokens, GitHub tokens, private repository names, backend hostnames, user emails, or production audit details.
