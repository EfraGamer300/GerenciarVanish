# Vanish+ v1.2.0

## Security Fixes
- **UUID Validation**: `data.yml` now validates UUID format and version (v3/v4) on load. Invalid entries are logged and discarded.
- **Folia Safe Fallback**: If Folia scheduler reflection fails, the plugin now returns safely instead of crashing by falling back to Bukkit scheduler.
- **Rate-Limit (Cooldown)**: Anti-spam cooldown on vanish toggle (default 1000ms, min 250ms). Prevents mass hide/show event DoS.
- **Staff Real-Time Notify**: Staff with `vanish.see` permission see `[Vanish] <player> is now ON/OFF` in chat.
- **File Logging**: All vanish actions logged to `plugins/Vanish+/logs/vanish.log` with timestamps.
- **Safe Reload**: `/vanish reload` auto-reveals all players if `vanish-enabled` is set to `false`.
- **Force Reveal**: `/vanish reveal <player>` — force a specific player out of vanish.
- **Emergency Reveal All**: `/vanish revealall` — reveal all vanished players at once.
- **Complete Reveal**: `revealPlayer()` now also cleans `savedVanish` to prevent re-vanish on reconnect.

## Languages (5)
| Code | Language | Status |
|------|----------|--------|
| `en` | English | ✅ Default |
| `pt` | Português | ✅ |
| `es` | Español | ✅ |
| `fr` | Français | ✅ |
| `de` | Deutsch | ✅ |

Language set in `config.yml` → `language: "en"`. Each server configures its own language.

## Compatibility
| Server | Versions |
|--------|----------|
| Spigot | 1.19 — 1.26.1.2 |
| Paper | 1.19 — 1.26.1.2 |
| Purpur | 1.19 — 1.26.1.2 |
| Folia | 1.19 — 1.26.1.2 |

## Build Variants
Each MC version produces one JAR that works on all backends (Spigot/Paper/Purpur/Folia).
Folia support is detected at runtime — no separate JAR needed.

## Permissions
| Permission | Description | Default |
|------------|-------------|---------|
| `vanish.use` | Use /vanish | op |
| `vanish.see` | See vanished players | op |
| `vanish.others` | Toggle vanish for others | op |
| `vanish.reload` | Reload config | op |
| `vanish.reveal` | Force reveal a player | op |
| `vanish.revealall` | Reveal all vanished | op |
| `vanish.config` | Open config GUI | op |
| `vanish.silentchest` | Silent chest access | op |
| `vanish.bypass.protection` | Bypass vanish protections | op |

## Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/vanish` | Toggle vanish | `vanish.use` |
| `/vanish <player>` | Toggle vanish for player | `vanish.others` |
| `/vanish list` | List vanished players | `vanish.see` |
| `/vanish reload` | Reload configuration | `vanish.reload` |
| `/vanish reveal <player>` | Force reveal player | `vanish.reveal` |
| `/vanish revealall` | Reveal all vanished | `vanish.revealall` |
| `/vanishconfig` | Open config GUI | `vanish.config` |

## Changelog from v1.1.0
- 8 security vulnerabilities fixed
- 4 new languages added (es, fr, de + existing en, pt)
- 2 new commands (`reveal`, `revealall`)
- File-based audit logging
- Rate-limit on vanish toggle
- Staff real-time notifications
- UUID data validation
- Folia crash prevention
- Multi-version build system (15 MC versions)
