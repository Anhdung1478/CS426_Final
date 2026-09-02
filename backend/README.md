# Lexicon Depths — realm-forge proxy

A Spring Boot service with two endpoints. It exists for one reason: **the DeepSeek API key must
not ship inside the APK.** Anyone with `apktool` would have it in a minute, so the app calls this
and this calls DeepSeek.

Stateless. It forwards a prompt, validates the reply against the P3-1 contract, and forgets both.

## The key

The key is read from the `DEEPSEEK_API_KEY` environment variable and is never written to a tracked
file. `application.properties` holds `${DEEPSEEK_API_KEY:}` — a reference, not a value. `.env` is
gitignored; `.env.example` shows the shape.

Starting without it is not an error: `/health` reports `keyConfigured: false` and `/generate-map`
returns a readable 503. Fail at the moment it matters, not with a startup stack trace.

**If a key has ever been pasted into a chat, a commit, or a screenshot, rotate it** at
<https://platform.deepseek.com>. Rotation is cheap; a leaked key is not.

## Run it

```powershell
$env:DEEPSEEK_API_KEY = "sk-..."      # bash: export DEEPSEEK_API_KEY=sk-...
cd backend
.\gradlew.bat bootRun
```

Then check it is alive:

```powershell
curl http://localhost:8080/health
# {"status":"ok","keyConfigured":true}
```

## Endpoints

| Method | Path | Body | Returns |
|---|---|---|---|
| `GET` | `/health` | — | `{"status":"ok","keyConfigured":bool}` — never any part of the key |
| `POST` | `/generate-map` | `{"topic":"cooking","level":"B1"}` | The validated map (see `docs/phase-3.md` §P3-1) |

Status codes the app reacts to:

| Status | Meaning |
|---|---|
| `400` | Blank or over-long topic, or a level that is not CEFR |
| `502` | DeepSeek unreachable, or it returned something that failed validation |
| `503` | No key configured on this proxy |

Error bodies carry a player-readable `error` string, and the app shows it verbatim — so
"No DEEPSEEK_API_KEY set on the proxy" reaches the phone screen intact.

## Pointing the app at it

| Client | Base URL |
|---|---|
| Emulator | `http://10.0.2.2:8080` (the built-in default — `10.0.2.2` is the emulator's alias for the host) |
| Physical device | `http://<laptop LAN IP>:8080`, same Wi-Fi — type it into **Settings → Realm-forge server** |

No rebuild is needed for either: debug builds permit cleartext, and the URL is a setting. Release
builds deny cleartext entirely (`src/main/res/xml/network_security_config.xml`).

## Tests

```powershell
.\gradlew.bat test
```

16 tests over `MapValidator` — one per contract rule, asserted on the rejection path, since the
happy path is the half that does not corrupt a library table.
