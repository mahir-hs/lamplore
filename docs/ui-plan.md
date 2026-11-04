# UI/UX plan

Written 2026-11-04 from the code only (nothing in this batch has been seen on a phone yet).
First execution step is a screenshot pass over every screen with the phone on adb.

## Status
Phase 1 is coded and builds (2026-11-04). Not yet seen on a phone: it needs the screenshot pass.
Done: pinned-transport player, scene-strip scrubber, sleep timer and save in the top bar, sheets for
Director/Tune/pictures, quieter Home (goal moved to You), tabs Tonight / Library / You, Add under Library,
Back returns to where the player was opened, paper-mat covers, 48 dp pills and buttons with spoken labels,
no looping animation outside the player, reduce-motion support, one verb for ending the night.
Deviations: the Home hero is 1.3:1 (a 4:5 hero would fill the screen); you-screen keeps the old Settings
sections under the goal for now.
Phase 2 is coded and builds too (2026-11-04), also unseen on a phone: first-run voice setup (Lessac, Ryan, Amy;
the sample plays after the 20 MB download, since a sample needs the voice installed), "Change voice" in the
player menu, Library length filter and online status line, You screen reordered (voices, Director, goal,
reflections, online library, storage), About screen with the credits, voice preview text no longer religious.
Phase 3 is not started.

## Brief
Murmur: audio stories that calm the heart, listened to mostly at night, to replace scrolling.
Audience: general, adults and families. Primary job: get someone from "about to scroll" to
"listening to a story" in one tap, and from the end of a story to "finished for tonight".

## What is wrong today (from the code)
1. Player: the transport (play, ±15 s) sits below the scene, title, scrubber, Director card and
   picture gallery. On a phone it is below the fold. The most important control is buried.
2. Home carries too much: greeting, streak, Director badge, hero, mood cards, shelf, weekly goal
   tracker, two action buttons, tagline. Five different ideas on a screen meant to be calm.
3. Navigation: "Add" is a top-level tab for a rare action; voice downloads (essential) are buried
   in Settings; Back from the player always goes to Library even when opened from Home.
4. First run: no onboarding, so the first story plays with the phone's built-in voice, the
   weakest one. First impression is the worst voice.
5. Motion everywhere: sky header, mood cards, hero cover, badge pulse, play ring, bobbing cover.
   Reads as noise, costs battery, no "reduce motion" respect.
6. Card kit: same 14-24 dp radius on everything, boxes inside boxes (Director card, plate card).
7. Covers are inconsistent: drawn scene+motif tiles next to real plates with different aspect
   and framing.
8. Accessibility: pills are ~36 dp tall (minimum is 48), drawn icons have no content
   descriptions, "finished" and "saved" are colour-only dots.
9. Copy drifts: "Finish tonight" / "Done for tonight" / "Reflect again"; status strings like
   "Preparing for smooth listening... 47%" are system talk.

## Design tokens
Colour (a lantern in a dark room; dark is a bedtime requirement, not a style choice)
- Night `#171311` background
- Lamp `#231C18` the only surface colour: sheets and the mini player
- Paper `#EFE3CF` the mat every cover sits on (new; the signature device)
- Ember `#E0A458` one accent: play, selected, progress
- Moth `#F2E8DC` text; Dusk `#9C8E82` secondary text; Moss `#7FA38A` finished

Type: Fraunces for greetings, story titles and headings; Manrope for everything else.
Scale tightened for a phone: 34 / 26 / 22 / 18 / 16 / 14 / 12 sp. Body 16 sp with 26 sp leading.
Left-aligned throughout; only the player title is centred under the art.

Layout: one column, 20 dp margins, no text in boxes. Sections separated by space, a 1 px rule
only where two lists touch. Covers are 4:5 with a 6 dp paper mat (plate or drawn), so shelves
look like one set. Radius only on images and pill buttons.

Principles
1. One picture per screen is the hero; everything else is type on night.
2. The story's own illustration is the identity. Real plate when there is one, scene+motif when
   there is not, always in the same mat.
3. Motion only where it means something: the living scene while a story plays, and the
   end-of-story dimming. Home does not animate. Respect the system "remove animations" setting.
4. Bedtime controls are one tap: play, sleep timer, finish tonight.
5. Copy is short, warm, and uses the same verbs everywhere.

## Screens

Tonight (Home)
```
Good evening
12 minutes today, 3 nights in a row.

+----------------------------+
|  +----------------------+  |   plate on paper mat, 4:5
|  |   Rackham plate      |  |
|  +----------------------+  |
|  The Goosegirl             |
|  12 min, Brothers Grimm    |
+----------------------------+
[          Listen            ]

What do you need right now?
(Calm down) (Sleep) (Learn) (Be inspired) (Patience)

Tonight
[cover] [cover] [cover] [cover] ->

Pause for a minute
-----------------------------------
 Tonight        Library        You
```
- Greeting, one hero, moods as text pills (not animated pictures), one shelf, one link.
- Weekly goal and reflections move to "You". Director badge leaves Home.

Player (fixed, no scrolling for the essentials)
```
<- Back                        (moon) 20 min   (heart)
+--------------------------------+
|   living scene, plate in mat   |   tap: pictures from the book
+--------------------------------+
            The Goosegirl
       12 min, Brothers Grimm
Narrator, wind, hopeful music, 0.9x       tap: Director sheet
|====scene strip as scrubber=======|  3:12 / 12:40
     |<    15<    ( > )    >15    >|
Tune
```
- The scene strip becomes the scrubber (one bar, not two).
- Waiting state: progress ring around the play button, one line "Preparing the voice",
  "Start now" link. No percentages in prose.
- Sleep timer: moon icon top right, cycles 10/20/30/off.
- Director details, voices, pictures, Report and Delete move into sheets/overflow.
- End of story: the one orchestrated moment. Scene fades to night, lamp dims, "Finish tonight".

Library
- Search, "Continue", "Saved", then shelves. Add a length filter: Short (under 6 min),
  Medium, Long. "Add your own" lives here as a row at the end of "Yours".
- Quiet one-line status for the online library (checking, offline, up to date).

You
- Voices (the big one: install, pick narrator, hear a 5-second sample).
- Director switches in one group with one sentence.
- This week's goal, reflections, streak.
- Storage, About (credits move here).

First run (one screen)
- "Murmur reads stories aloud. Pick a voice." Two cards: Ryan, Amy (20 MB each, progress bar,
  "Hear a sample"), and a text link "Use the phone's voice for now".

## Phases
1. Player layout, Home diet, 3-tab navigation with Back to origin, paper-mat covers, motion diet
   and reduce-motion, touch targets and content descriptions, copy pass. (One session.)
2. First-run voice setup with samples; voice picker reachable from the player; Library length
   filter and online status; You screen; About screen.
3. Player as a sheet that expands from the mini player; end-of-story moment; day palette for
   morning use; haptics; tablet/landscape.

## Process
- Screenshot pass first: `adb exec-out screencap -p > D:\DevData\shots\home.png` per screen,
  reviewed before and after each phase.
- Test reduce motion with `adb shell settings put global animator_duration_scale 0`.
