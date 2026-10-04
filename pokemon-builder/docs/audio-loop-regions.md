# BGM loop regions

The project's BGM carries Vorbis `LOOPSTART/LOOPEND` comments. libGDX `Music`
can only loop **whole files**, and `setPosition(loopStart)` is not a cheap
seek: the desktop backend resets the decoder and scans forward, which measured
0.28 s for a 17 s loop start and 0.64 s for a 49 s one (DecodeProbe, L8.1
correction). Seeking back on every loop would insert that much silence.

So the builder **splits** a tagged BGM into two files instead:

```
BGM/<name>.ogg          loop segment  = [LOOPSTART, LOOPEND)
BGM/<name>.intro.ogg    intro segment = [0, LOOPSTART)
```

The runtime plays the intro once and then the loop segment with whole-file
looping. The loop itself is gapless; only the intro -> loop handoff costs the
completion callback / one-frame latency (~20 ms, once per map entry). The
split is on by default for `build-audio` and for the audio step of the PC and
Android builds — a plain build already produces both segments. There is no
flag to disable it.

## Tag rules

Only BGM Vorbis loop tags are honored. `LOOPSTART` is an inclusive
source-rate sample index; `LOOPEND` is exclusive. When LOOPEND is absent,
positive `LOOPLENGTH` supplies the end as start + length; LOOPEND takes
precedence. A region with only a start loops to the file end (the intro stops
at LOOPSTART and the loop segment is the rest of the file). `LOOPSTART=0`
produces no intro file; the loop segment is the whole tagged range.

The source sample rate must be known. Malformed, unsafe, reversed and
out-of-file ranges are ignored (a broken end falls back to "loop to the file
end") rather than used for destructive cuts. Comments are read from the first
64 KB. `build/audio-loop-overrides.json` still supplies manual loop starts in
seconds for tracks without tags (`{"Battle wild": 3.5}`); those split the
track at the given time.

## ffmpeg and the manifest

Both segments are rendered with ffmpeg
(`atrim=start_sample=...:end_sample=...,asetpts=PTS-STARTPTS`, or `start=`
seconds for manual overrides). The intro uses
`atrim=end_sample=...,asetpts=PTS-STARTPTS`. An explicit region takes
precedence over `--trim-silence`, which would move a verified boundary.

The manifest entry keeps the source loop metadata (`loopStartSamples`,
`loopEndSamples`, `loopStartSeconds`, `loopEndSeconds`, `sampleRate`) and adds
`introFile` when an intro segment exists. `file` points at the loop segment.
Untagged BGMs are copied as a whole and still loop as a whole file.

Split BGMs are **always rebuilt** on every audio build (their old outputs — a
full-file copy or a previous split — must be replaced), reported as
`looped=N intros=M`. Other files keep the resume-by-existence rule.

## Runtime

`AudioManager` caches the loop segment under the logical id and the intro
under `id + "#intro"`. `playBgm`/cued playback starts the intro with
`setLooping(false)` and a completion listener; when it finishes (or when the
per-frame safety poll sees it stopped) the loop segment starts with
`setLooping(true)`. `stopBgm`, a new cue or a script command cancels a pending
loop start, so an interrupted intro never resurrects a stale loop.

Untagged tracks (no `introFile`) play exactly as before: one stream, whole-file
looping.

## Verification

`logs/check-l8-1b.mjs` (ffprobe) confirms for all 24 split tracks that the
intro lasts LOOPSTART and the loop segment lasts LOOPEND - LOOPSTART (measured
worst deviation 0.010 s). Automated checks confirm structure, not musical
authorship or listening quality — listen to I1–I3 samples and actual in-game
loops (enter a tagged map; the intro must play once, then the loop must run
without a restart).
