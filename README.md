# Murmur

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
- Not done yet: offline download, subscriptions, episode progress bars.

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

