# Storage design

App is an Android APK distributed via GitHub Releases. Zero-cost constraint: free tiers only.

## Install flow
1. User installs the APK (~30-60 MB; no models, no books).
2. First launch: app downloads a voice model once (20-300 MB).
3. User browses the library; text and metadata come from the backend.
4. Tap a story: either stream a ready-made audio file, or generate it on the phone, cache it, and play.

## Where data lives
| Data | Location | Notes |
|---|---|---|
| App code, APK | GitHub repo + Releases | Code only; no books, audio or models in the repo |
| Voice models | GitHub Releases, downloaded once | Move to R2 if downloads grow |
| Story/book text | Supabase DB (or R2 files) | Tiny |
| Covers, metadata | Supabase / R2 | Small |
| Premium pre-generated audio | Cloudflare R2 (free tier, no egress fees) | Generated on the developer PC with high-quality models |
| Phone-generated audio (default) | On the phone only | Zero server cost; regenerate from text if lost |
| Shared audio cache (optional) | R2 | One generation can serve many listeners |
| User recordings | R2 | Watch the free-tier limit |
| Background sounds | Pack downloaded once (CC0) | Compressed |
| Accounts, progress, goals | Supabase | Small rows |
| Settings, downloads index | Phone SQLite | Works offline |

## Two audio sources per story
1. **Featured / pre-generated**: produced on the developer PC with the best free models (can use heavy, expressive ones that phones cannot run), encoded as Opus, uploaded to R2, streamed or downloaded by the app. Best quality, available instantly.
2. **On-device generated**: Piper/Kokoro via sherpa-onnx on the user's phone, for user-submitted stories and anything not pre-generated. Cached locally.

The app prefers (1) when it exists, otherwise falls back to (2). The listener can still pick a different voice, which uses (2).

## Capacity estimate
Speech Opus is roughly 15 MB per hour. R2's free tier (about 10 GB, verify current limits) holds roughly 600 hours of pre-generated audio.

Free alternative for public-domain audio: the Internet Archive (archive.org) hosts audio for free.

## Compression decisions
- Text, metadata, JSON: zstd (lossless).
- Audio: Opus, mono, 24 kbps (16 kbps minimum acceptable), encoded once from the original WAV; never re-encode lossy to lossy.
- Prefer storing text over audio wherever the phone can regenerate the voice.
- Voice models: int8-quantized where quality holds.
- Colab for GPU generation: deferred, decide later.

## Phone-side
- SQLite (Drift/sqflite) for library index, progress, settings.
- App storage folders for models, audio, sound packs.
- "Free up space" screen: per-book audio deletion.

## Backend portability
- App talks to its own repository/API layer, not scattered Supabase calls.
- Store file paths, not full URLs.
- Keep our own user IDs.
- Plain Postgres features so a move to another host (Oracle Always Free, Cloudflare, AWS) is a dump and restore.

## Updating
Obtainium (or F-Droid) auto-updates from GitHub Releases. Voice models update separately.

## Risks
- Free-tier limits (Supabase pauses on inactivity; keep-alive GitHub Action).
- R2 / Supabase storage fills with recordings; cap recording length.
- Takedowns: keep copyrighted content out of the repo; rights field and report button on every item.
