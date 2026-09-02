# Phase 3 — AI map generation

**Goal:** the player types a topic, the app asks a backend proxy to forge a realm, DeepSeek returns a validated word list, and that realm lands in Room permanently — replayable from a My Library screen, and indistinguishable from a seeded realm once it is there.

**Blocked until Phase 2 clears its exit checklist.** P3-5 writes into the same `Word`/`Realm`/`RealmWord` tables `SeedLoader` (P1-6) fills, and P3-7's replay path reuses `RunEngine.startRun` (P2-9) unchanged.

**Milestone:** P3-6. A realm that did not exist thirty seconds ago becomes a playable dungeon, and the run engine cannot tell the difference.

---

## The constraint that shapes everything

> **Never call DeepSeek from the client.**

The API key would be extractable from the APK with `apktool` in under a minute. Every generation call routes through `backend/`, whose key comes from the `DEEPSEEK_API_KEY` environment variable and is never written to a tracked file. `.gitignore` covers `.env`; `application.properties` holds `${DEEPSEEK_API_KEY:}` — a placeholder, not a value.

The second constraint follows from the first: the backend runs on a laptop for the demo, over plain HTTP on a LAN. That is only acceptable in a debug build, which is why cleartext is permitted in `src/debug/` and denied in `src/main/`.

---

## Task table

| ID | Task | Difficulty | Depends on | Unblocks | Track | Status |
|---|---|---|---|---|---|---|
| P3-1 | Generated-map JSON contract | high | P1-5 | everything below | · | ✅ done |
| P3-2 | Spring Boot proxy skeleton | medium | P3-1 | P3-3 | A | ✅ done |
| P3-3 | DeepSeek call + JSON hygiene | high | P3-2 | P3-6 | A | ✅ done |
| P3-4 | Android HTTP client + cleartext config | medium | P3-1 | P3-6 | A | ✅ done |
| P3-5 | Realm import into Room | high | P3-1, P1-4 | P3-6, P3-7 | A | ✅ done |
| P3-6 | Forge flow — loading, retry, offline fallback | high | P3-4, P3-5 | P3-7 | B | ✅ done |
| P3-7 | My Library screen | medium | P3-5 | — | B | ✅ done |
| P3-8 | Datamuse affix key source | medium | P2-5 | — | A | ✅ done |
| P3-9 | Settings — backend base URL | low | P3-4 | — | B | ✅ done |

**Critical path:** P3-1 → P3-3 → P3-5 → P3-6

**Dependency graph:**

```
                    ┌── P3-2 ── P3-3 ──┐
                    │                  │
P3-1 ───────────────┼── P3-4 ──────────┼── P3-6 ── P3-7
                    │        │         │
                    └── P3-5 ──────────┘
                             │
                             └── P3-9

P3-8 ── independent of the whole chain (touches only game/question/gen/)
```

**Parallel split.** P3-1 is the only bottleneck, same shape as P2-1.

- **Track A** (logic + backend): P3-2 → P3-3 → P3-4 → P3-5, plus P3-8 whenever.
- **Track B** (UI): P3-9 → P3-6 → P3-7. P3-9 first because it is small and unblocks manual testing against a real laptop IP.
- P3-8 touches one package nothing else in Phase 3 reads. Hand it to whoever finishes first.

**Deferred past Phase 3:** regenerating or editing an existing library realm, sharing realms between players, and any server-side persistence. The backend stays stateless — it forwards a prompt and validates a reply, nothing else.

---

# Task detail

## P3-1 · Generated-map JSON contract ✅ done

**Difficulty:** high · **Track:** · · **Depends on:** P1-5 · **Unblocks:** every other Phase 3 task

**Do this first and freeze it.** The backend produces this shape, the client consumes it, and the DeepSeek system prompt describes it. Three places drift the moment one of them changes silently.

**Files:** `android/app/src/main/java/com/lexicondepths/content/MapJson.java`, `backend/src/main/java/com/lexicondepths/proxy/MapValidator.java`

### The shape is `words_seed.json`, minus the version wrapper

```json
{
  "name": "Kitchen Alchemy",
  "topic": "cooking",
  "level": "B1",
  "words": [
    {
      "headword": "simmer",
      "cefr": "B1",
      "topic": "cooking",
      "pos": "verb",
      "definition": "to cook gently just below boiling point",
      "example": "Let the sauce simmer for twenty minutes.",
      "viGloss": "ninh nhỏ lửa",
      "synonyms": ["stew", "poach"],
      "antonyms": ["boil"],
      "collocations": ["simmer gently", "bring to a simmer"],
      "forms": ["simmer", "simmering", "simmered"],
      "affixKey": null
    }
  ]
}
```

Deliberately identical to a `words_seed.json` entry. That is not laziness for its own sake — it means `SeedLoader`'s parser and the generated-map parser are **the same code** (`MapJson.parseWord`), so a field added to the seed format cannot silently fail to parse from the network, and vice versa. One parser, one set of bugs.

### Validation is not optional

A malformed map corrupting the library table mid-demo is an avoidable disaster, and LLMs reliably produce two failure modes: markdown fences around the JSON, and plausible-looking objects with an empty string where a definition should be.

Both sides validate. The backend refuses with `502` so a bad generation never reaches a phone; the client validates anyway, because "the backend validated it" is an assumption and this is a trust boundary.

**Two severities, and the split matters.** A defect in the *map* is fatal. A defect in one *word* is not — it costs that word, nothing else.

Fatal, rejects the whole map:

| Rule | Why |
|---|---|
| Strip ` ```json ` / ` ``` ` fences, then parse | The single most common LLM output defect |
| Parses as a JSON object with a name, a topic, and a `words` array | Nothing usable without them |
| `words.length` ≤ 24 | More than 24 is the model padding |
| At least 8 words **survive** the per-word filter below | Fewer than 8 cannot fill a 12-node run |

Per-word, drops just that word:

| Rule | Why |
|---|---|
| `headword`, `definition`, `example`, `pos` non-blank | The empty-string failure mode |
| `cefr` parses as a `CefrLevel` | `valueOf` throws on garbage; catch it at the boundary, not in the run engine |
| `example` contains the headword | A cloze generated from an example that never mentions the word is unanswerable |
| Headword not already seen in this map | A duplicate makes two slots identical |

> **Why drop rather than reject** — found by running it, not by reasoning about it. A live `travel` generation returned eleven good words and one (`unwind`) whose example never said "unwind". Failing the whole map over one word out of twelve throws away a good realm for a defect that costs nothing to skip, and models slip on roughly one entry per generation. Dropping keeps the guarantee that actually matters — *everything that reaches the database is playable* — and the ≥8 survivor floor is what stops a wholesale-garbage response getting through.

**Done when**
- [x] The same `MapJson.parseWord` is called by both `SeedLoader` and the network import path
- [x] Every rule above has a unit test — fatal rules asserted on the rejection, per-word rules asserted on the *survivors*, not just the happy path
- [x] Fence stripping is tested against ` ```json `, bare ` ``` `, and leading prose before the fence
- [x] A map that is 11 good words plus 1 broken one yields an 11-word realm, not an error

---

## P3-2 · Spring Boot proxy skeleton ✅ done

**Difficulty:** medium · **Track:** A · **Depends on:** P3-1 · **Unblocks:** P3-3

**Files:** `backend/build.gradle`, `backend/settings.gradle`, `backend/src/main/java/com/lexicondepths/proxy/{ProxyApplication,MapController}.java`, `backend/src/main/resources/application.properties`

Two endpoints, no more:

| Endpoint | Purpose |
|---|---|
| `GET /health` | Reports whether a key is configured (`{"status":"ok","keyConfigured":true}`) — the single fastest way to diagnose a dead demo |
| `POST /generate-map` | `{"topic":"cooking","level":"B1"}` → the P3-1 shape |

`/health` deliberately reports **whether** a key is present, never any part of the key itself.

### Key handling

`application.properties` holds `deepseek.api-key=${DEEPSEEK_API_KEY:}` — an env-var reference with an empty default. The value never appears in a tracked file. Starting the server without the variable set is not a crash; it is a `/health` that says `keyConfigured: false` and a `/generate-map` that returns `503` with a readable message. Failing loudly at the right moment beats failing at startup with a stack trace.

`.gitignore` already covers `.env`, `backend/build/`, and `backend/.gradle/`.

**Done when**
- [x] `gradlew.bat bootRun` starts with no key set and `/health` reports `keyConfigured: false`
- [x] `grep -ri "sk-" backend/` finds nothing
- [x] No key value appears in any file `git status` would track

---

## P3-3 · DeepSeek call + JSON hygiene ✅ done

**Difficulty:** high · **Track:** A · **Depends on:** P3-2 · **Unblocks:** P3-6

**Files:** `backend/src/main/java/com/lexicondepths/proxy/{DeepSeekClient,MapValidator}.java`, `backend/src/test/java/com/lexicondepths/proxy/MapValidatorTest.java`

`HttpURLConnection` against `https://api.deepseek.com/chat/completions`, model `deepseek-chat`, `response_format: {"type":"json_object"}`. That flag makes fences unlikely; the stripper stays anyway, because "unlikely" is not "never" and the stripper is six lines.

The system prompt states the schema explicitly and demands JSON only. `temperature` sits at 1.0 — the DeepSeek-recommended value for creative writing, and a realm word list is closer to creative than to arithmetic.

### Error mapping

Every failure gets a status the client can act on, not a 500 with a stack trace:

| Condition | Status |
|---|---|
| No API key configured | `503` |
| Upstream refused, timed out, or returned non-200 | `502` |
| Upstream returned a 200 that fails P3-1 validation | `502` |
| Blank or over-long topic, unparseable level | `400` |

Timeouts are 10s connect / 60s read. Generation genuinely takes 15–30 seconds; a default timeout would fail every call.

**Done when**
- [x] `MapValidatorTest` covers each P3-1 rule's rejection path plus fence stripping
- [x] A 502 body carries a human-readable `error` field, and the client surfaces it verbatim
- [x] The API key is never logged, never echoed in an error body

---

## P3-4 · Android HTTP client + cleartext config ✅ done

**Difficulty:** medium · **Track:** A · **Depends on:** P3-1 · **Unblocks:** P3-6

**Files:** `content/MapApi.java`, `res/xml/network_security_config.xml` (main + debug), `AndroidManifest.xml`, `app/build.gradle`

`HttpURLConnection` and `org.json`, both in the SDK — §2 of `project-context.md` says networking needs no dependency, and one POST does not change that.

### ⚠️ Cleartext is a debug-only affordance

Android blocks plain HTTP from API 28. The demo backend runs on a laptop over HTTP on a LAN, so cleartext has to be permitted — but permitting it in the shipping manifest would be a real security regression, and a grader looking for exactly this will find it.

Two files, and the split is the point:

- `src/main/res/xml/network_security_config.xml` — cleartext **denied**. This is what a release build gets.
- `src/debug/res/xml/network_security_config.xml` — cleartext **permitted**. Debug builds only, and the file says why in a comment.

Because the debug config is permissive for all hosts, changing the base URL to a laptop's LAN IP needs no manifest edit and no rebuild — which is what makes P3-9 work.

`android.permission.INTERNET` is added, and nothing else. `buildConfig true` plus a `MAP_API_BASE_URL` field gives the compile-time default (`http://10.0.2.2:8080`, the emulator's alias for the host machine); P3-9 overrides it at runtime.

**Done when**
- [x] Airplane mode produces a caught `IOException` and a retry prompt, never a crash
- [x] A backend returning a 502 surfaces its `error` string to the player
- [x] The release `network_security_config.xml` denies cleartext

---

## P3-5 · Realm import into Room ✅ done

**Difficulty:** high · **Track:** A · **Depends on:** P3-1, P1-4 · **Unblocks:** P3-6, P3-7

**Files:** `content/RealmImport.java`, `db/dao/{WordDao,RealmDao}.java` (extend)

Takes a validated map, writes `Word` + `Realm` + `RealmWord` rows in one transaction, returns the new `realmId`. `Realm.generated = true` is what separates a forged realm from a seeded one everywhere downstream.

### The collision that matters

`Word.headword` is uniquely indexed. A generated "travel" map will absolutely contain words the seed already has.

Inserting with `IGNORE` and moving on would silently drop those words from the realm — the player forges a 12-word map and gets an 8-word dungeon, with no error anywhere. The fix is not to skip the collision but to **join the existing row**: insert what is new, look up what already existed, and build `RealmWord` rows for the union.

The existing word keeps its `WordProgress` untouched. A word you have been learning for a month does not reset its ease because it turned up in a new realm — that is the §5 permadeath boundary applied to a new path into the same tables.

### ⚠️ One active run at a time

Playing a library realm calls `RunEngine.startRun`, the same entry `RealmSelectActivity` uses. That screen guards against starting a run while one is already active; this one did not, and on-device testing produced **two `ACTIVE` rows at once**. `RunDao.getActiveRun()` is `LIMIT 1`, so the second run and its 24 nodes became invisible to Resume Run and were never cleaned up — precisely the leaked-run-rows case P2-12 forbids.

The guard now lives in `RunEngine.startRun` itself, not in either screen: every entry point routes through that method, so one check covers both callers and any future one. A run already in progress is returned rather than replaced.

**Done when**
- [x] Importing a map whose words all already exist still produces a realm with the full word count
- [x] `WordProgress` rows for pre-existing words are unchanged after an import
- [x] A failure partway through leaves no partial realm — the whole import is one transaction
- [x] Playing a library realm while a run is active resumes it instead of orphaning it

---

## P3-6 · Forge flow — loading, retry, offline fallback ✅ done

**Difficulty:** high · **Track:** B · **Depends on:** P3-4, P3-5 · **Unblocks:** P3-7

**This is the Phase 3 milestone.**

**Files:** `ui/library/LibraryActivity.java`, `res/layout/activity_library.xml`, `assets/fallback_map.json`

The forge lives **inside** the Library screen, not on one of its own. You forge a realm and it appears in the list below the button; a separate Activity would put a screen transition in the middle of the one moment the app has to feel like magic.

Flow: topic text + CEFR spinner → button → the button becomes a live status line (`Forging…` with a typewriter, reusing P1-11) → success drops a new row into the list, failure shows the error and a **Retry** that keeps the typed topic.

### ⚠️ The offline fallback is a demo-survival requirement

Wi-Fi fails in front of graders. `assets/fallback_map.json` is a hand-written realm in the exact P3-1 shape; when the network path fails, the error line offers **"Use the offline realm"** alongside Retry.

It routes through the identical `MapJson` → `RealmImport` path — same validation, same transaction, same `generated = true`. The only difference is where the bytes came from, which means the fallback cannot rot: if it broke, the live path would be broken too, and the tests would say so.

**Done when**
- [x] Generating with the backend down shows an error and a working retry, never a spinner that hangs
- [x] The offline fallback produces a genuinely playable realm, not a stub
- [x] Rotating or backgrounding mid-generation does not leak the Activity or crash on the callback

---

## P3-7 · My Library screen ✅ done

**Difficulty:** medium · **Track:** B · **Depends on:** P3-5

**Files:** `ui/library/LibraryActivity.java`, `db/dao/RealmDao.java` (extend)

Every generated realm as a row: name, topic, CEFR span, word count, generation date. Tap to play — straight into `RunEngine.startRun` with that `realmId`, which is the same call `RealmSelectActivity` already makes. A forged realm is a realm.

Filtering by topic and level collapses into **one** case-insensitive text box matching name, topic, or level. Two spinners would be the conventional answer and would be worse: the list is short, and typing `b1` or `cook` is faster than opening a dropdown.

Realms are observed as `LiveData`, so a forge completing pushes its row into the list with no manual refresh.

**Done when**
- [x] Seeded realms never appear here — only `generated = true`
- [x] Playing a library realm produces a run identical in structure to a seeded one
- [x] An empty library reads as an invitation to forge, not as a broken screen

---

## P3-8 · Datamuse affix key source ✅ done

**Difficulty:** medium · **Track:** A · **Depends on:** P2-5

**Files:** `game/question/gen/{DatamuseAffixKeySource,AffixKeySource,AffixHarvestGenerator}.java`

P2-5 built affix harvest behind `AffixKeySource` precisely so this swap would touch one class. It does.

`https://api.datamuse.com/words?sp=re*&max=100` — no key, no auth, so it is the one external call that can go direct from the client. `game/` still imports no Android classes: `java.net.HttpURLConnection` is the JDK, and the response is a fixed `[{"word":"…","score":…}]` shape that a six-line scanner reads without `org.json` (which is Android-only and would break the plain-JUnit tests).

### ⚠️ Widening the key would silently break balance

The naive swap is a balance bug, and it is worth stating because it is invisible until playtesting: ratio is `found / keySize`. Replacing a 5-word offline key with 100 Datamuse words means a player who types 5 correct answers scores 0.05 instead of 1.0 and takes near-full damage for a good answer.

Datamuse's actual value is **accepting** more correct answers, not demanding more. So the two sets separate:

- **Target** (`Question.correctAnswer`) — the offline seed key. Small. Sets how many words you need.
- **Accepted** (`Question.options`) — target ∪ Datamuse. Large. Sets which words count.

`Question.options` is already an empty list for this type, so this needs no contract change — `AffixKeySource` gains one `default` method and `OfflineAffixKeySource` is untouched.

Net effect: type five valid `re-` words that the seed has never heard of and you still clear the slot.

Failures — no network, a timeout, a 500 — fall back to the offline key silently. A Hydra fight in airplane mode plays exactly as it did in Phase 2. Results are cached per affix for the process lifetime; one fight asks Datamuse once.

**Done when**
- [x] Target size is unchanged from Phase 2 — the same offline key, so damage balance is untouched
- [x] A word only Datamuse knows still scores
- [x] Network failure falls back to offline with no visible difference
- [x] `game/` still has zero Android imports and the unit tests still run without an emulator

---

## P3-9 · Settings — backend base URL ✅ done

**Difficulty:** low · **Track:** B · **Depends on:** P3-4

**Files:** `Prefs.java`, `ui/settings/SettingsActivity.java`, `res/layout/activity_settings.xml`

One text field, defaulting to `BuildConfig.MAP_API_BASE_URL`. Blank restores the default.

Small task, real reason: the emulator reaches a laptop at `10.0.2.2`, a physical phone needs the laptop's actual LAN IP, and that IP changes with the Wi-Fi network. Without this, demoing on a real device means editing `build.gradle` and rebuilding — in the ten minutes before a presentation, on conference Wi-Fi. With it, you type an IP.

**Done when**
- [x] A URL typed here survives a force-quit
- [x] Clearing the field restores the build-config default rather than breaking every call

---

## Phase 3 exit checklist

1. `gradlew.bat bootRun` in `backend/` with no key set — `/health` reports `keyConfigured: false`, `/generate-map` returns a readable 503
2. Set `DEEPSEEK_API_KEY`, restart — `/health` reports `keyConfigured: true`
3. Forge a realm from Library with the backend running — it appears in the list and is playable
4. Forge with the backend stopped — an error and a retry, never a hang
5. Take the offline fallback — a playable realm with real words
6. Forge a topic overlapping the seed (`travel`) — the realm has its full word count, and the overlapping words' SRS progress is unchanged
7. Fight a Hydra with and without network — both work; the network run accepts words the seed lacks
8. Point the base URL at a laptop LAN IP in Settings — a physical device forges without a rebuild
9. `git grep "sk-"` finds nothing; the release `network_security_config.xml` denies cleartext
10. `gradlew.bat test` green in both `android/` and `backend/`

See [`report-phase3.md`](../report-phase3.md) for how each item was verified and which are manual/on-device vs. automated.
