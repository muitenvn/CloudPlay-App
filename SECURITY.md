🛡️ Security Policy & Compliance Guidelines
1. Supported Versions
Only the latest release from the main branch is actively supported with security updates and bug fixes.
| Version | Supported |
|---|---|
| Latest Release (main) | :white_check_mark: |
| Build from Source | :white_check_mark: |
| Older Releases | :x: |
2. Reporting a Vulnerability / Legal Notice
🔒 Reporting Security Vulnerabilities
If you discover a security vulnerability within CloudPlay (such as local credential storage issues or WebRTC leaks), please DO NOT open a public issue on GitHub.
Instead, please report it privately:
 * Private Disclosure: Create a Private Security Advisory on GitHub.
 * Direct Contact: Email the maintainer at support@cloudplay.app (or contact via GitHub Profile).
We aim to respond to security reports within 48 hours and release patches promptly.
⚖️ DMCA & Trademark Inquiries
CloudPlay strictly respects intellectual property rights. If you represent Microsoft Corporation or another copyright/trademark holder and believe that any content, branding, or feature in this repository infringes upon your rights or policies:
 * Please contact us directly via private advisory or official issue submission marked with [Legal/DMCA].
 * We commit to reviewing and taking appropriate remediation steps (including feature removal or code modification) within 7 business days.
3. Account Safety & Legal Risk Disclosure
⚠️ Microsoft Services Agreement Compliance
 * CloudPlay acts as a client/utility tool for Xbox Cloud Gaming.
 * Geo-Bypass Features: Using proxy routing or bypassing region restrictions may breach Section 1(b) / 1(m) of the Microsoft Services Agreement.
 * Account Ban Risks: Automatic detection systems maintained by Microsoft may flag modified stream handshakes or IP location shifts. Users accept 100% of the risk regarding potential Microsoft/Xbox account suspensions, permanent bans, or lost save data.
🚫 Anti-Piracy Statement
 * CloudPlay does not provide, distribute, or facilitate access to pirated games, activation keys, or paid subscription bypasses.
 * Users must maintain an active, valid Xbox Game Pass Ultimate subscription on their personal Microsoft account to stream content.
4. Privacy & Data Handling
 * Zero Data Collection: CloudPlay does not run external telemetry servers, collect personal user analytics, or store Microsoft login credentials on external servers.
 * Local Storage: All authentication tokens and user settings remain encrypted and stored locally on the user's Android device (SharedPreferences / EncryptedSharedPreferences).
 * Direct WebRTC Stream: Handshake and stream data flow directly between the user's device and Microsoft's official xCloud servers (*.gslb.xboxlive.com / *.xboxservices.com).
