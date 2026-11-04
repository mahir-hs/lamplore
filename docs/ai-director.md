# AI Director (on-device LLM) — plan

Status: researched 2026-11-04, not implemented yet. Today the Director is rule-based (`audio/Director.kt`).

## Goal
Read a story once on the phone and produce the plan the player already consumes: per paragraph a scene
(rain, storm, wind, ocean, stream, forest, night, fire, desert, cave, calm), a music mood
(calm, sleep, hopeful, adventure, tension, sad, warm), a pace 0.8–1.15; per quoted line the speaker's
gender. Zero cost, offline, no login-gated model.

## Recommendation
- Runtime: **LiteRT-LM** (`com.google.ai.edge.litertlm:litertlm-android:0.19.0`, Apache-2.0). MediaPipe
  `tasks-genai` is maintenance-only; ONNX Runtime GenAI has no Android package.
- Model: **Qwen3-1.7B int4** (`litert-community/Qwen3-1.7B`, `Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm`,
  977 MB) on `Backend.GPU()`; **Qwen3-0.6B int4** (343 MB) as the fast/low-RAM tier. Both ungated,
  direct `resolve/main` URLs, Apache-2.0. Gemma and Llama are gated — avoid.
- Structured output: `ConversationConfig(enableResponseFormat = true)` + `sendMessage(..., responseFormat =
  ResponseFormat.json(schema))` (LLGuidance; in Kotlin sources, undocumented on the web). Disable thinking
  (`ThinkingConfig(enableThinking = false)`), temperature 0, topK 1, maxOutputToken ~600.
- Expected wall time on the Redmi Note 14 Pro+ (Snapdragon 7s Gen 3, Adreno 810): 1.7B ≈ 30–60 s per
  1500-word story on GPU (2–3 min on CPU — unacceptable); 0.6B ≈ 15–30 s GPU. Extrapolated from Pixel 8a
  cards; measure on the real phone first (`Benchmark.kt` in LiteRT-LM).
- Memory: 1.7B int4 peaks ~1 GB RSS on GPU; close the engine when the app backgrounds. AAR has no
  armeabi-v7a — gate the feature on arm64.
- Prompt: number paragraphs and quotes, use short enum keys so the output stays ~200–300 tokens; split
  stories >2500 words into two calls (context 4096).
- Fallback: rule-based Director when the model is absent or fails; keep its output format identical.
- Plan B runtime: Llamatik (`com.llamatik:core:2.2.0`, llama.cpp, GBNF JSON grammar) with
  Qwen2.5-1.5B-Instruct Q4_K_M GGUF — CPU-only, ~1–2 min per story.

## Why not "our own model" yet
No labeled data. Once users correct the Director's choices (scene/voice overrides are already logged as
settings changes), that data can train a small classifier that replaces the LLM for the cheap decisions.

## Schema sketch
```json
{"p":[{"s":"forest","m":"calm","pace":0.95}], "q":[{"i":0,"g":"female"}]}
```


## Measured on a real phone (2026-11-04)
Xiaomi 24115RA8EC: Snapdragon 7s Gen 3 (SM7635), 11 GB RAM, Android 16, LiteRT-LM 0.19.0, one fable
(The Tortoise and the Hare, 9 paragraphs), JSON schema with exactly one entry per paragraph, temperature 0.

| Model | Chip | Load | Answer | Result |
|---|---|---|---|---|
| Qwen3 0.6B | graphics (OpenCL) | 8 s | 4 to 13 s | valid JSON, but it just cycled the scene options in order, all "calm", pace 0.8 |
| Qwen3 1.7B | graphics | 21 s | 27 s | valid JSON, but scenes and moods look arbitrary (rain, ocean for a meadow) |
| Qwen3 1.7B | processor | 6 s | 70 s | valid JSON, but "none, calm, 0.8" for every paragraph |

Findings:
- The GPU needs `<uses-native-library android:name="libOpenCL.so" android:required="false"/>` in the manifest on Android 12+, or it fails with "Can not find OpenCL library".
- Constrained JSON without `minItems` lets the model return an empty list; `minItems` = `maxItems` = paragraph count fixes the shape, not the quality.
- Speed is fine on the graphics chip; quality is not. With one prompt over the whole story, neither size reads the story.
- Decision: keep the rule-based Director for playback. The "Your phone and AI" page keeps the benchmark so the result can be re-checked.
- Next experiments, cheapest first: classify one paragraph at a time with two or three worked examples; ask for the scene only; let the model break ties between the rules' top two scenes; a larger model or a cloud call (costs money and needs internet).
