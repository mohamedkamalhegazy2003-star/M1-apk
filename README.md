# Price Quotes (M1 Android companion)

A lightweight Android WebView client for the existing M1 Quotation Management server. It reuses the existing M1 web interface, login, roles, permissions, and shared data; it does not create a second database. The connection screen lets users choose English/Arabic and Light/Dark/System appearance; preferences are applied to M1 using its existing `qm-lang` and `qm-theme` settings.

## Server connection
- On the same Wi-Fi: enter `http://SERVER-LAN-IP:3210` (replace with the Windows server's LAN IP).
- Away from the office: use a private VPN hostname/IP (for example, a Tailscale address) or a properly configured HTTPS reverse proxy. Do **not** expose the M1 HTTP port directly to the public internet.
- The M1 server must be running and reachable. Sign in with an existing M1 account.

## Build
GitHub Actions builds a debug APK on pushes to `main` and on manual workflow runs. Create a tag such as `v1.0.0` to publish the APK as a GitHub Release asset.

## Security notes
The Android client permits HTTP because the current M1 server uses plain HTTP on trusted local networks. Use a VPN or HTTPS for remote access. The client stores only the server URL and display preferences; authentication/session handling remains in the M1 web app/server.
