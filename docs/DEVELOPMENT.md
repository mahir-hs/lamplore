# Lamplore

Audio stories that calm the heart. For everyone: fables, wisdom tales from around the world, and sleep stories.

## Vision
A screen-free listening app that replaces doom-scrolling with stories: public-domain and user-written, narrated by text-to-speech or by humans.

## Decisions so far
- Audience: general and secular. The app is not religious; the old faith shelf became "Wisdom tales" (folk tales and parables from many cultures) (decided 2026-11-04)
- Platforms: mobile only (Android first, APK via GitHub Releases); iOS and web are out of scope for now
- Distribution: GitHub only, no app stores. Keep copyrighted book text/audio out of the repo; content lives on the backend
- Language: English first, Bangla later
- Content: public-domain seed library + user-submitted stories
- Voices: TTS with selectable voices and tones; optional self-recorded narration
- Background: real field recordings (10 scenes) and 7 instrumental music beds bundled as Opus loops in `app/src/main/assets/sounds` (8.8 MB); the Director picks ambience per scene and one music mood per story (tension bed on tense scenes). Credits and licenses: `docs/licenses/SOUNDS.md`. Raw sources and ffmpeg live on D:, not in the repo.
- Moderation: required from day one (family-safe tags, reporting, review queue)
- No ads; subscription + creator tips

## Build (Windows, everything on D:)
Versions: Gradle 9.8.2, AGP 9.5.0, Kotlin 2.4.21, Compose BOM 2026.09.00, compileSdk 37, minSdk 26.
```
set JAVA_HOME=C:\Program Files\Java\jdk-21
set ANDROID_HOME=D:\Android\Sdk
set GRADLE_USER_HOME=D:\DevData\gradle-home
D:\DevData\gradle-9.8.2\bin\gradle.bat assembleDebug
```
Output: `app\build\outputs\apk\debug\app-debug.apk`. Install on a phone with `adb install -r <apk>`.
The wrapper (`gradlew`) points at the same Gradle version but its built-in download can stall; the local copy at `D:\DevData\gradle-9.8.2` (fetched with aria2) avoids that.

## Design
Lantern-at-night palette (`ui/Theme.kt`: Night, Lamp, Ember, Moth, Dusk, Moss). Type: Fraunces for titles,
Manrope for UI — static instances baked with fontTools from the Google Fonts variable files (runtime axis
settings were not applied on the test phone); OFL texts in `docs/licenses/`. Every story gets a generated
paper-cut night-landscape cover (`ui/Covers.kt`), seeded by its id and tinted by shelf. The player shows a
scene strip: one band per segment, colored by the background the Director chose.

## Animated scenes
`ui/Scenes.kt` draws a living scene for every background sound (rain, storm, wind, ocean, stream, forest, night, fire, desert, cave, tension, calm) on a Canvas, driven by one shared `Clock`. The player shows the scene the Director is currently using and cross-fades when it changes; Home uses the same scenes for the header and mood cards. No image files, so the APK does not grow.

## Online books
New books are not bundled. `data/RemoteContent.kt` downloads `catalog.json` (story text plus plate credits) from the GitHub release tagged `content`, and each picture the first time its story is shown; pictures are cached on the phone (Settings > Online library shows size and can clear them). Offline, the last catalog is used and covers fall back to the drawn motif. Build the catalog with the scripts kept on D: (`D:\DevData\downloads\workuild_content.py`, output in the gitignored `content/`), then publish it:
`gh release create content content/* --title "Content" --notes "Online books"` (later: `gh release upload content content/* --clobber`). Rights and credits: `docs/licenses/IMAGES.md`.

## Listen: free audiobooks and podcasts (streamed)
`data/Listen.kt`, `ui/ListenScreen.kt`, `ui/RemoteImage.kt` (tab "Listen"). Nothing is bundled or re-hosted; everything is looked up live and streamed.
- **Audiobooks:** the Internet Archive hosts all of LibriVox (about 22,000 free public-domain audiobooks) plus old-time radio. Shelves are live `advancedsearch` queries sorted by downloads (children, fairy tales, fables, mystery, poetry...). A book opens through `archive.org/metadata/<id>` (chapters = 64 kbps MP3 files) and its cover is `archive.org/services/img/<id>`. Search matches title, author or subject.
- **Podcasts:** Apple's free iTunes charts (`rss/toppodcasts/genre=<id>`) and Search API, no key. Episodes are read from the show's own RSS feed, like any podcast app.
- Playback reuses `PlayerHub` (`Story.tracks` non-empty = stream): background service, lock screen, sleep timer, per-story resume, 0.8x-2x speed. Streams skip the Director, voices and background sound. Covers are downloaded once into the app cache. Recently played items are kept in the `murmur_listen` prefs.
- **Offline** (`data/Downloads.kt`, `audio/DownloadService.kt`): download a whole book, chosen chapters ("Choose chapters", "Next 5/10") or one chapter or episode. Requests queue and run one chapter at a time, resume from `.part` files, retry archive.org's transient 500s, keep going in a foreground service, and show speed and time left (sliding-window rate, the idea from aniyomi's `RateTracker`). The player uses the saved file for any chapter that has one and streams the rest. Files: `files/downloads/<id>/<chapter>.mp3`.
- **My list:** star a book or show (`Listen.starred`), mark finished; "In progress" and "Finished" come from listening progress. The **Downloads** pill lists the queue and what is on the phone and works offline.
- **Activity** (You > Your activity, `data/Activity.kt`, `ui/ActivityPage.kt`): overview, last 7 days, a 17-week heatmap, time per topic and a history, all kept on the phone.
- **Picture book and subtitles:** stories with plates show them whole, turning over as the story moves on, with the sentence being read underneath (`PlayerHub.caption`). Menu: Hide/Show subtitles.
- Not done yet: subscriptions, per-episode download queue ordering, playlists across books.

## Features adapted from aniyomi/Yomi
Chosen with the owner from the aniyomi-personal project (manga/anime specific parts left out).
- **Memory:** History (`data/History.kt`, You > History): one entry per title with progress and status, Remove/Clear only hide, Restore brings back; permanent activity journal (`data/Activity.kt`); "Heard" marks on titles; Incognito; Backup and restore (`data/Backup.kt`, one JSON file, replace-on-restore so it never double-counts, optional automatic backups); Your taste (genres, authors, rising/fading, come-back-to) and weekday/hour charts.
- **Library:** subscriptions and an Updates feed with a background check (`data/Subs.kt`, WorkManager), auto-download of new episodes, delete after finishing, Wi-Fi only, parallel downloads with pause/resume/reorder/retry (`data/Downloads.kt`), custom lists with sort, group, filter and saved views (`data/Lists.kt`).
- **Finding things:** one search across your list, audiobooks and shows with exact matches first, suggestions, recent and saved searches checked in the background (`data/Searches.kt`), Discover shelves, What next, More like this, author pages.
- **Player and app:** bookmarks with notes (`data/Bookmarks.kt`), podcast transcripts as subtitles (`data/Transcripts.kt`, timed WebVTT/SubRip/JSON), Midnight or AMOLED black and accent colours (`ui/Theme.kt` Looks), optional biometric app lock (`data/Lock.kt`), storage manager and diagnostics with the last crash (`data/Crash.kt`).
- Not done: Add by RSS link / OPML import (left out by choice), a light theme (scenes and player are painted for the dark).

## Covers
Every cover is the scene the story lives in plus a drawing of its subject (`ui/Motifs.kt`: tortoise and hare, lion, lighthouse, night train...). Seed stories name their motif and scene; your own stories get one guessed from the title and text (`Motif.guess`), falling back to an open book.

## Story Director (shown in the app as "AI Director")
`audio/Director.kt` reads a story and plans, per segment, who speaks (narrator / male / female character
for quoted lines), the pace, and the background sound. It learns characters' genders from attributed lines
("the mouse ... she said"), drops short speech tags, and uses keyword scenes with hysteresis. Each of the
three decisions can be overridden in the Tune sheet or Settings. The on-device LLM version is planned in
`docs/ai-director.md`.

## Voices (Piper and Kokoro, on-device)
Voice packs are downloaded inside the app (Settings > Better voices) from the sherpa-onnx `tts-models` release and run offline through sherpa-onnx.
- **Library:** `app/libs/sherpa-onnx-1.13.8.aar` (not committed, 47 MB). Fetch it with
  `aria2c -x16 -s16 -k1M -c -d app/libs -o sherpa-onnx-1.13.8.aar https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar`
- **Pinned to 1.13.8 on purpose:** sherpa-onnx 2.0 removed espeak-ng, which these Piper/Kokoro models need.
- **Measured on a Xiaomi 24115RA8EC (arm64, Android 16):** Piper (int8) generates about 2.7x faster than real time, so stories start almost instantly. Kokoro (int8) generates about 0.5x real time, so the app measures speed and waits for a head start (predictive buffering in `PlayerHub`) before playing; "Start now anyway" skips the wait.
- **Reading pace:** Piper Ryan reads about 237 words per minute; use Speed 0.8x-0.9x for calm listening (default is 0.9x for new installs).
- **Character voices:** the Director gives each quote a voice. Besides narrator, male and female it has *young* (small creatures, children) and *deep* (lions, giants): each takes its own voice when a second woman or man is installed, and is pitch-shifted at playback (young up about 22%, deep down about 14%, elders down 6% and slower). Cues come from words around the quote (`Director.childCues/deepCues/elderCues`). The pace is baked into Piper/Kokoro speech with real pauses between sentences (`SpeechPacing`), not stretched.
- **Kokoro v1.0** (28 English voices, ~330 MB, experimental) is an optional pack; the library supports its lexicon settings but it has not been tried on a phone.
- **Engines** sit behind `VoiceEngine` (`audio/TtsGenerator.kt`): Android system TTS, `SherpaEngine`, and `CompositeEngine` routing between them. Voice ids look like `sherpa:<packId>:<speakerId>`.

## Storage
See [docs/storage-design.md](docs/storage-design.md). Short version: text and accounts in the cloud (free tiers), audio either pre-generated on the developer PC and hosted on free object storage, or generated on the phone and cached locally.

## Core loop
Intention -> Listen -> Reflect -> "Done for today"

## Phases
1. MVP: player, public-domain library, sleep timer, daily done screen
2. Writer upload, text-to-audio, moderation queue
3. Pause-before-scroll, weekly goals, reflection journal
4. Circles, tipping, premium subscription

## Open items
- Name availability check (stores, domain, trademark)
- Tech stack: DECIDED - native Android, Kotlin + Jetpack Compose, Media3 for playback
- TTS: DECIDED - zero cost, on-device (Piper first, Kokoro later) via sherpa-onnx; cloud TTS and Colab deferred
- Content licensing review for seed library


## Sounds, Create and sleep stories (0.5)
- **Sound mixer** (`audio/Mixer.kt`, `audio/MixerService.kt`, `ui/MixerScreen.kt`, tab "Sounds"): one lazily created looping ExoPlayer per bundled sound, volume = slider squared, own audio focus (pauses on calls), a sleep timer that fades the last 30 s, presets in the `lamplore_mixer` prefs (included in backups). A foreground service of type mediaPlayback keeps it alive with the screen off.
- **Create** (`ui/CreateScreen.kt`, tab "Create"):
  - Personalised tales (`data/Templates.kt`): hand-written texts with `{name}`, `{an}`/`{animal}`, `{They}`/`{they}`/`{them}`/`{their}`. Verbs after a pronoun are past tense and never was/were so they agree for she, he and they.
  - AI stories (`ai/StoryWriter.kt`): "My API key" posts to `api.anthropic.com/v1/messages` (default model `claude-haiku-5-5`, editable); "This phone" runs the Qwen3 LiteRT-LM model from `AiModels` on the CPU. The key is encrypted with an Android Keystore AES-GCM key (`data/Secret.kt`), stored in `lamplore_secret` prefs, and not part of backups. Results are saved as your own stories (`Rights.GENERATED`).
  - Honest limits: small on-device models write plain, sometimes odd stories (see `docs/ai-director.md`). Cloud and on-device generation have not been tested on a phone yet.
- **Bedtime, read by people**: an Audiobooks shelf (`ui/ListenScreen.kt`) querying LibriVox for bedtime, lullaby, nursery rhyme and fairy tale subjects.
