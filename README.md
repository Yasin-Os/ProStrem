<img src="./app_icon.png" alt="ProStream Official App Icon" width="140" height="140" style="border-radius: 28px;" />

# ProStream

ProStream is an Android streaming browser and video downloader. Add your favourite
streaming sites, browse them in one place with built-in ad-blocking, and download
videos (including HLS streams) for offline watching.

## Features

- **Multi-site browser** — add sites with a name, URL and colour; switch between
  them instantly from the floating hub. Each site keeps its own WebView.
- **Video downloader** — detects videos on the page (MP4, HLS/m3u8 with AES-128
  support) and downloads them with a parallel multi-thread engine, speed and
  progress notifications, and resume support.
- **Ad-block** — built-in blocklist for common ad, pop-up and betting domains.
- **App lock** — PIN (stored as a salted SHA-256 hash) and fingerprint unlock.
- **Smart DNS** — one-tap setup for ControlD free DNS (`p2.freedns.controld.com`)
  for ad-free browsing.
- **In-app updater** — checks GitHub Releases and installs new builds
  (APK signature is verified before install).
- **Custom theme** — RGB theme colour picker and dark mode.

## Build

This project is built with **AndroidIDE / CodeAssist** (no Gradle files).
Open the project in AndroidIDE and build the `debug` variant, or push to
`main` — GitHub Actions builds a debug APK and publishes a GitHub Release
automatically (`.github/workflows/main.yml`).

- `compileSdk` 36, `minSdk` 24, `targetSdk` 36, Java 8
- Default site: `https://prmovies.technology/`

## Security notes

- SSL errors are never ignored — a page with a bad certificate is not loaded.
- Websites must ask before using the camera, microphone or location.
- The app lock PIN is hashed (SHA-256 + salt); the default PIN is not `1234`
  any more — set your own PIN in App Lock settings.
- Update APKs are verified (same package name and signing certificate)
  before installation.

## License

MIT — see [LICENSE](LICENSE).
