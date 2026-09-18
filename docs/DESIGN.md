# Seasons — design plan

Status: **planning, not started.** Written 2026-09-18.
Target: Minecraft **26.3**, Fabric, server-side authoritative with a client rendering half.

---

## 1. The pitch

A seasons mod that feels like it shipped with the game.

Two rules generate the whole system:

1. **Only temperate biomes have seasons.** Every other biome is locked to the season it already
   visually represents. Cherry Grove is eternally spring, Dappled Forest eternally autumn,
   snowy biomes eternally winter, desert/jungle/savanna eternally summer.
2. **Crops belong to seasons.** Winter changes *what you farm*, never *how long you wait*.

Those two combine into the actual hook: **where you settle determines what you can grow
year-round.** Biome choice gains a mechanical consequence, which vanilla almost entirely lacks —
and it emerges from two rules rather than a bolted-on system.

### Why this wins

Measured 2026-09-17/18 (see `research/mod-opportunities.md`):

- **Demand:** ["Seasons For Most Biomes"](https://feedback.minecraft.net/hc/en-us/community/posts/360009353652-Seasons-for-most-biomes) — **19,979 votes, 893 comments**, "under review" by Mojang, open since 2018. Highest-voted non-meme Gameplay suggestion.
- **Supply:** a ~24M-download Fabric ecosystem is stranded. **Fabric Seasons** (9,526,283 dl, 1,841 followers) died on **1.21.1 in Oct 2024**, taking Fabric Seasons Extras (6.3M), Delight Compat (6.0M) and Terralith Compat (2.6M) with it. Only **2 seasons mods exist on 26.3**, the largest at 12,272 downloads.
- **Only real competitor:** [Serene Seasons](https://modrinth.com/mod/serene-seasons) — 6,960,847 dl, 2,719 followers, on 26.2, actively maintained. But **All-Rights-Reserved**, Forge-origin, multi-loader. A Fabric-native, open-source, data-driven alternative is a position, not a duplicate.

---

## 2. Design rules (the guardrails)

**This is not a content mod.** Hard limits:

- ❌ No new blocks
- ❌ No new items
- ❌ No new mobs
- ❌ No HUD widget or season overlay
- ✅ Everything is config or datapack JSON

You learn the season by **looking at the world**. (Optional v1.1: holding a clock shows it.)

Scope creep into a content mod is the single most likely way this project dies. When in doubt,
cut. Note the multipurpose-items rule from the Matcha project does not bind here — this mod
registers no items at all, which is the point.

---

## 3. The three systems

### 3.1 Biome season classes — the spine

Every biome is one of five classes:

| Class | Biomes | Behaviour |
|---|---|---|
| `cycles` | plains, forest, birch forest, dark forest, meadow, swamp, taiga, river, beach | Follows the world season |
| `eternal_spring` | **cherry grove** | Always spring |
| `eternal_summer` | desert, jungle, savanna, badlands, mesa, warm ocean | Always summer |
| `eternal_autumn` | **dappled forest** (26.3) | Always autumn |
| `eternal_winter` | snowy taiga, snowy plains, ice spikes, frozen peaks, grove, frozen ocean | Always winter |

**Why this is the most important decision in the mod:**

1. **It removes the ugliest failure mode.** Snow in the desert, bare jungles — the thing that
   makes seasons mods feel wrong. Those biomes don't participate, so the problem never exists.
2. **Scope collapses.** Colour ramps for ~8 temperate biomes, not ~60. This is the difference
   between a 4-week mod and a 10-week one.
3. **It's already canon.** Vanilla biomes already have fixed seasonal identities. You are naming
   an existing rule, not inventing one.

### 3.2 Visual seasons

Seasonal appearance, in priority order:

1. **Grass and foliage tint** — blend the vanilla biome colour toward a season palette.
   **Autumn reuses the Dappled Forest's own colour ramp**, so autumn looks like Mojang drew it.
   This is the single best answer to "make it feel vanilla".
2. **Snow spread and melt** — winter lays snow layers on surface blocks in `cycles` biomes;
   spring melts them. Uses the existing snow layer block. High visual payoff, zero new content.
3. **Weather frequency** — more rain in spring, more snow in winter, drier summers.

#### The leaf rule — inherited from vanilla, verified

Verified on [minecraft.wiki/w/Leaves](https://minecraft.wiki/w/Leaves):

| Leaf type | Vanilla behaviour | In this mod |
|---|---|---|
| Oak, jungle, acacia, dark oak, mangrove | Biome-colormapped | **Changes with season** |
| **Spruce** | Fixed `#619961`, *"not affected by biome"* | **Never changes** |
| **Birch** | Fixed `#80a755`, *"not affected by biome"* | **Never changes** |
| Cherry, azalea, pale oak, **poplar** | Not colormapped — pre-coloured textures | **Never changes** |

So the builder-control rule writes itself, with **zero new blocks and zero config**: want a tree
that stays green all year? Plant spruce. Permanent pink? Cherry. Permanent autumn? Poplar.
You inherit a vocabulary Mojang already shipped.

> **Note:** poplar being uncoloured means 26.3's new tree is *already* permanently autumn by
> texture. The eternal-autumn Dappled Forest is literally how Mojang built it.

> **Rejected: "only naturally-generated blocks change colour."** Technically infeasible —
> grass/leaf colour is computed at render time from the biome, never stored per-block. Natural
> and player-placed grass are the same block in the same biome. Tagging origin would mean
> per-block state across millions of blocks: severe save bloat for no gain. The spruce/birch
> rule above achieves the same intent for free.

### 3.3 Crop seasonality — the gameplay hook

**Never slow growth. Change what grows.** A growth multiplier just makes the player wait, which
is the least interesting mechanic available and what every existing seasons mod does.

Sketch (all datapack-overridable):

| Crop | Spring | Summer | Autumn | Winter |
|---|---|---|---|---|
| Wheat, potato, carrot | ✅ | ✅ | ✅ | ❌ |
| Melon, sugar cane | ✅ | ✅ | ❌ | ❌ |
| Pumpkin | ❌ | ✅ | ✅ | ❌ |
| Beetroot | ✅ | ❌ | ✅ | ✅ (hardy) |
| Nether/End flora | — | — | — | unaffected |

Out of season, a crop simply doesn't advance its growth stage. It isn't destroyed — losing a
farm to a date change would feel punitive and un-vanilla.

**Combined with §3.1:** settle in Cherry Grove for permanent spring crops, Dappled Forest for
autumn crops, temperate land for everything in rotation. *This is the mod's actual selling point.*

**Open decision:** should bone meal bypass seasonality? Proposed **yes** — bone meal is the
player's existing override for growth impatience, and letting it work keeps winter farming
possible-but-expensive rather than impossible. Config flag either way.

---

## 4. Data model — the compat fix

**Fabric Seasons died partly of hardcoded compat.** It needed separate addon mods for Terralith
(2.6M dl) and Farmer's Delight (6.0M dl). Maintaining per-pack addons is a treadmill that kills
solo projects.

**Fix: biome→season is a datapack table keyed on biome TAGS, not biome IDs.**

```
data/<ns>/season_biome/eternal_winter.json
{ "values": ["#minecraft:is_taiga", "#minecraft:is_frozen_ocean", "minecraft:snowy_plains"] }

data/<ns>/season_crop/summer.json
{ "values": ["minecraft:melon_stem", "minecraft:sugar_cane"] }
```

Modded biomes almost always carry vanilla tags (`is_taiga`, `is_jungle`, `is_forest`), so
**Terralith and friends get sane defaults automatically with no code from us**, and anyone can
ship a refinement as a datapack instead of waiting on an addon mod.

This also lands on the two strongest demand findings in the research: **config/customisation is
the #1 most-requested theme** (192 mod issues) and **a data-driven API is #7** — so the
architecture is also the feature.

---

## 5. Technical architecture

### Season state

Season is a **pure function of world day count** — `season = f(level.getDayTime()/24000, config)`.
No saved state needed for the base case, which is a large simplification. `SavedData` is only
required if `/season set` is supported (v1.1); it stores an offset, not the season.

- **Server-authoritative.** The server computes the season.
- **Client sync** on join and on change, via Fabric networking. Client needs it for rendering.
- Config: season length in days (default 24 = 6 days per season), starting season.

### Rendering (the hard part)

Grass/foliage colour resolves client-side through a colour resolver that reads biome climate and
the colormap textures. Plan: **intercept the resolver and blend the result toward the season
palette**, rather than swapping colormap textures.

Critically, **vanilla caches biome colours per chunk** — changing the season must invalidate that
cache and force a re-render, or nothing visibly changes until chunks reload.

⚠️ **This is where the incumbent died.** Rendering and worldgen mixins are the most
version-fragile code in the mod. Keep this surface as small as possible and isolated in
`src/client` (the Matcha build already uses `splitEnvironmentSourceSets()` — anything touching
rendering MUST live there or a dedicated server crashes).

### Class names — verify, don't assume

26.x uses **Mojang official mappings** (not yarn), so most online tutorials won't match. The loom
cache is currently cleared, so **step 0 is a build to regenerate it**, then index
`~/.gradle/caches/fabric-loom/26.3/minecraft-common.jar` to confirm every class name before
writing mixins. Targets to confirm: the biome colour resolver, `CropBlock` random tick,
`SavedData`, the level renderer's cache-invalidation entry point.

### Toolchain (verified 2026-09-18)

| Component | Version |
|---|---|
| Minecraft | **26.3** (never `1.26.3`) |
| Fabric API | **0.160.7+26.3** (published 2026-09-17) |
| Fabric loader | **0.19.5** (stable for 26.3) |
| Loom | 1.17-SNAPSHOT |
| JDK | **25** — `org.gradle.java.home` must point at it; system `java` is 17 |
| Gradle | 9.5.1 wrapper |

Mirror the Matcha build: `splitEnvironmentSourceSets()`, Modrinth maven, and the `registerDeploy`
Prism task pattern. **Loom 1.17 has no `remapJar`** — the plain `jar` task emits the remapped
artifact. Per standing preference: **build and deploy only, never launch Minecraft** — the author
relaunches their own instance to test.

---

## 6. Build order

Each step is independently verifiable, and the riskiest work is deliberately *not* first.

| Ver | Scope | Proves |
|---|---|---|
| **0.1** | Project skeleton, season computed from day count, `/season` command prints current season. No visuals. | Toolchain + season maths, zero rendering risk |
| **0.2** | Biome season classes as datapack JSON + tags. `/season` reports the season *at the player's position*. | The spine works; Cherry Grove reads "spring" |
| **0.3** | Crop seasonality via tags. Out-of-season crops stop advancing. | The gameplay hook, server-side only |
| **0.4** | ⚠️ Foliage/grass tint + cache invalidation. Autumn reuses the Dappled Forest ramp. | The hard part, in isolation |
| **0.5** | Snow spread and melt in winter; weather frequency. | Visual payoff |
| **0.6** | Config file, README, Modrinth page with seasonal screenshots. | Shippable |
| **1.0** | Polish, Terralith spot-check, publish. | — |

**Rationale:** 0.1–0.3 are all server-side and low-risk, so you have a working, testable mod
before touching the rendering code that killed the incumbent. If 0.4 proves intractable, 0.1–0.3
still ship as a legitimate mod.

### Deferred (not v1)

- **Hemisphere seasons** (north/south opposite) — genuinely novel, but a second headline feature
  dilutes the first. Strong v2 candidate.
- `/season set` and the `SavedData` offset it needs
- Clock showing the season
- Seasonal mob spawn variation
- ❌ **Day-length changes — permanently rejected.** Breaks sleep and spawning; universally disliked.

---

## 7. Open decisions

1. **Mod name.** Needs one before the package/id is fixed. Candidates: *Turning*, *Solstice*,
   *Equinox*, *Wane*, *Turn of the Year*. Should not be "Fabric Seasons 2" — distinct identity,
   given the original is MPL-2.0 and someone else's.
2. **Licence.** Recommend **MIT or Apache-2.0** — maximally reusable, and a deliberate contrast
   with Serene Seasons' All-Rights-Reserved.
3. **Season length default** — 24 days (6/season) proposed. Longer feels grander, shorter shows
   off the mod faster. Affects first impressions on a Modrinth page.
4. **Bone meal bypass** — proposed yes (§3.3).
5. **Clean-room vs. reference.** Fabric Seasons is **MPL-2.0**, so reuse is legally permitted with
   attribution and per-file source disclosure. Recommend **clean-room** — avoids the obligation
   entirely and the codebase is 2 years stale anyway.
6. **Repo/account.** Published under `pyth0n354`, kept separate from any personal account.
   Decide before the first push; per-repo `user.email` needs setting to avoid cross-attribution.

---

## 8. Critical analysis — read before committing

Written 2026-09-18 at request: maximally critical, still fair. Every claim below is
checked against the data in `research/mod-opportunities.md`.

### 8.1 ⚠️ The thesis is weaker than §1 implies

**Serene Seasons supports Fabric.** Loaders: `['fabric', 'forge', 'neoforge']`, on 26.2, updated
2026-09-05, 2,719 followers, 6,960,847 downloads. §1's framing implied a vacancy. There isn't
one. A Fabric player who wants seasons **today** installs Serene Seasons and it works.

The honest claim is narrower: *there is no Fabric-native, open-source, data-driven seasons mod.*
That is a **differentiation play against a healthy incumbent**, not a land grab. Every
downstream estimate should be revised accordingly.

### 8.2 The 19,979 votes are the weakest evidence here

Votes on feedback.minecraft.net measure demand for **Mojang to add a vanilla feature**. That is
aspirational, not revealed preference, and it does not convert 1:1 into mod installs. The number
is also inflated by age — the post has been open since 2018.

**The evidence that actually matters is Fabric Seasons' 9,526,283 downloads.** That proves mod
demand empirically. Lead with it; treat the vote count as colour, not proof.

### 8.3 The incumbent's death is a warning, not just an opening

Fabric Seasons' author had **9.5M downloads and walked away anyway.** The most probable causes
are both structural and both still apply:

- version-fragile rendering/worldgen mixins that break every MC release
- an endless queue of per-modpack compat requests (hence the Terralith and Farmer's Delight addons)

The datapack-tag architecture in §4 addresses the second. **Nothing addresses the first.** If a
mod with 9.5M downloads couldn't sustain it, a solo student should assume the same maintenance
load and plan for it, not assume they'll out-persist them.

### 8.4 Design flaws

**① "Only temperate biomes cycle" reads as broken.** A player who installs the mod in a desert,
jungle or savanna sees *nothing change, ever.* That is an excellent way to earn "doesn't work"
reviews. The rule is good design and terrible onboarding.
→ *Mitigation:* eternal biomes should still show **weather and particle** changes even with
colour locked, so something visibly happens everywhere. Plus a first-join chat line naming the
local biome's season.

**② The hook is undiscoverable.** The eternal-biome farming strategy is the mod's actual selling
point and a no-HUD mod never explains it. "Purity" is fighting "players must learn the system".
→ *Mitigation:* `/season` reports the season *and* the biome's class; ship an advancement for
harvesting an out-of-season crop in an eternal biome.

**③ Crop seasonality is both the differentiator and the first thing users will disable.** Anyone
who wants pretty colours turns it off — at which point the mod is a colour mod competing directly
with Serene Seasons on its strongest ground. If a majority disable it, the thesis collapses.
→ *Decision needed:* on by default (some bounce) or off (invisible differentiator). Recommend
**on**, with the toggle documented at the top of the README.

**④ Snow spread is destructive.** Snow layers on farmland, redstone, rails and paths already
annoy people in vanilla. Automatically spreading it in winter risks breaking builds and farms.
→ *Mitigation:* never place snow on functional blocks; restrict to natural surfaces; config off.

**⑤ Multi-base play is an endgame behaviour.** The "settle in Cherry Grove for year-round spring
crops" hook only lands for players who maintain several bases. Most players have one.

**⑥ Beetroot as the winter crop is a dud.** *(correct.)* Beetroot is one of
vanilla's weakest foods, so "snowy biomes gain a purpose" really means "gain the ability to farm
the worst crop". The niche is technically new and practically worthless.
→ **Revision: make potato the winter-hardy crop.** Historically the cold-climate staple
(Ireland, the Andes), mechanically worthwhile, and it gives eternal-winter biomes a genuinely
useful year-round farm. Beetroot moves to autumn. Winter then has exactly one solid staple —
survivable, not comfortable, which is the right feel.

### 8.5 Technical risks

**① The visual payoff is scheduled last (0.4).** If the rendering work proves intractable, the
mod ships as *invisible crop restrictions* — strictly worse than not shipping. The build order
protects the schedule but concentrates all product value in the riskiest step.
→ *Mitigation:* spike the colour resolver for one afternoon **before** committing to the full
plan. Do not build 0.1–0.3 on faith.

**② Cache invalidation causes frame hitches.** Forcing a full re-render on season change will
visibly stutter at high render distances. Needs a staged or radius-limited invalidation.

**③ 26.3 is three days old.** Fabric API `0.160.7+26.3` published 2026-09-17. Mappings and APIs
may still shift, and yarn is gone so most tutorials don't apply. Expect churn.

**④ Client/server split.** If required on both sides, server adoption suffers. Graceful
degradation (vanilla clients connect, just no tint) is worth designing for early, not retrofitting.

### 8.6 Project risk

**This is a 4+ week build, and the research's own recommendation was to ship it third.** Doing it
first contradicts that advice for a reason: it is the hardest of the candidates, it competes with
a maintained incumbent, and a solo student's first published mod benefits enormously from being
small and finished. Custom Hardcore Rules (~1 week) exists precisely to de-risk this.

### 8.7 What genuinely holds up

- **A 9.5M-download precedent** proves the ceiling is real.
- **The two-rule design is elegant** and the eternal-biome idea solves the ugliest failure mode
  in the category for free.
- **The leaf rule** (spruce/birch/cherry/poplar never change) gives builders control with zero
  new blocks — a real, defensible piece of design.
- **26.3's Dappled Forest** is a timely, specific hook nobody else has used yet.
- **Data-driven compat** is the correct fix for the thing that strangled the incumbent, and it
  targets the #1 and #7 most-requested themes in the ecosystem.
- **Permissive licensing vs. an ARR incumbent** is a genuine wedge with modpack authors, who are
  the main distribution channel.

### 8.8 Verdict

The project is **viable but oversold in §1**. It is not an empty niche; it is a credible
differentiation play against a healthy competitor, in a category with a proven audience and a
known-fatal maintenance burden.

Proceed only with: the potato revision (8.4⑥), the rendering spike moved before 0.1 (8.5①), the
onboarding fixes (8.4①②), and the expectation that this is the *second or third* mod shipped,
not the first.

---

## 9. Multiloader (Fabric + NeoForge)

**Decision: structure for both from day 1, ship Fabric first.**

Verified 2026-09-18:
- **Architectury API** supports 26.3 (99.6M downloads) — but forces a runtime dependency on users.
- **543 mods** already ship Fabric+NeoForge on 26.3, so the path is well-trodden.
- ⚠️ **NeoForge 26.3 is beta-only** — 5 builds, latest `26.3.0.4-beta`. No stable release yet.

**Use the MultiLoader template, not Architectury.** The loader-specific surface here is small —
entrypoint, networking, config, and the client colour hook. Everything else (season maths, tags,
crop ticking, trades, loot) is common code against vanilla APIs. Avoiding a user-facing
dependency is worth the small extra setup.

```
common/     season maths, biome classes, crop tags, trade + loot logic
fabric/     entrypoint, networking, colour hook
neoforge/   entrypoint, networking, colour hook
```

Retrofitting multiloader later is painful; doing it now is nearly free. **Bonus:** 26.x uses
Mojang mappings on *both* loaders, so class names match — the usual multiloader naming headache
doesn't apply.

⚠️ **Real cost:** the colour resolver is loader-specific and must be written **twice**. The single
riskiest piece of the mod roughly doubles. Release cadence: Fabric on 26.3 now, NeoForge when it
leaves beta.

---

## 10. Feature set (revised 2026-09-18)

Positioning, now that the incumbents' own pages are known:

> Serene Seasons and Fabric Seasons **change how the world looks** — tint, temperature, weather,
> and crop growth *speed*. This mod **changes how the world behaves**.

### 10.1 Crops span multiple seasons

Crops are **not** locked to one season — most span two or three. Winter is lean, not empty.

| Crop | Spring | Summer | Autumn | Winter |
|---|---|---|---|---|
| Wheat | ✅ | ✅ | ✅ | ❌ |
| Carrot | ✅ | ✅ | ✅ | ❌ |
| **Potato** | ✅ | ✅ | ✅ | ✅ **hardy** |
| Beetroot | ✅ | ❌ | ✅ | ❌ |
| Melon, sugar cane | ✅ | ✅ | ❌ | ❌ |
| Pumpkin | ❌ | ✅ | ✅ | ❌ |
| Sweet berries | ✅ | ✅ | ✅ | ❌ |
| Saplings | ✅ | ✅ | ✅ | ❌ (dormant) |
| Nether / End flora | — | — | — | unaffected |

**Potato is the winter-hardy crop**, not beetroot (see §8.4⑥). Out of season a crop simply stops
advancing; it is never destroyed. Bone meal bypasses (config).

⚠️ **Bee interaction — must not be missed.** Verified: bees pollinating a crop *"advances it to
another growth stage, similar to using bone meal."* **Bees are therefore a season bypass.**
Either bee pollination respects seasonality, or it follows the bone-meal config flag. Decide
explicitly; do not leave it to chance.

### 10.2 Hemisphere seasons ✅ *(reinstated)*

North and south have **opposite** seasons; the equator barely changes. Derived from the Z
coordinate — costs nothing to compute, no storage. Genuinely novel; no existing seasons mod does
it. Creates a real reason to travel: winter at home is summer far enough south.

### 10.3 Biome-typed villager trades — the strongest novel feature

**Verified on [minecraft.wiki/w/Trading](https://minecraft.wiki/w/Trading):** *"Farmer trades
remain identical across biome variants... Appearance is purely cosmetic."*

Vanilla ships **7 villager appearances** (plains, desert, taiga, snowy, savanna, jungle, swamp)
with **zero behavioural difference**. Mojang built the visual vocabulary and never used it. This
fills in a gap the game itself flags — the definition of vanilla+.

Vanilla farmer trades (verified) for reference:

| Level | Buys | Sells |
|---|---|---|
| Novice | wheat ×20, potato ×26, carrot ×22, beetroot ×15 | bread ×6 |
| Apprentice | pumpkin ×6 | pumpkin pie ×4, apple ×4 |
| Journeyman | melon ×4 | cookies ×18 |
| Expert | — | suspicious stew, cake |
| Master | — | golden carrot ×3, glistering melon ×3 |

**The design:** a farmer buys only what its biome and the current season can actually produce.
The Novice tier already buys exactly the four staples, so it slices cleanly.

| Villager type | Buys |
|---|---|
| **Snowy** | **Potato** — the winter-hardy crop |
| Taiga | Sweet berries, potato |
| Jungle | Melon, cocoa |
| Desert / savanna | Melon, dried goods |
| Plains / temperate | Rotates with the season |
| Swamp | Sugar cane, potato |

Effects: trading becomes a reason to **travel** rather than to breed one villager hall; seasonal
scarcity gets a price signal; snowy villages gain a purpose that matches the potato decision.

**Open:** apply to Novice only, or all tiers? Novice-only is safer — higher tiers stay
predictable, so players don't lose access to cake and golden carrots for a quarter of the year.

### 10.4 Animal behaviour — mostly emergent, barely any code

**Verified:** breeding foods **are** the seasonal crops — cow/sheep/goat eat wheat; pig eats
carrot/potato/beetroot; chicken eats seeds; rabbit eats carrot/dandelion.

**So seasonal animal scarcity emerges for free from §10.1.** No wheat in winter means fewer cows,
without a single line of animal code. Do not design a separate system for something the crop
rules already produce.

Vanilla precedent for conditional breeding exists: pandas need bamboo within 7×7×3, turtles need
sand. Seasonal conditions are consistent with that.

Explicit additions, deliberately minimal:
- **Spring:** breeding cooldown shorter than the vanilla 5 minutes.
- **Winter:** bees stay in the hive. Vanilla already does this for rain and night — *"Bees return
  to their nest when it rains and during the night"* — so winter dormancy reuses an existing
  behaviour rather than inventing one. It also resolves the §10.1 bypass in the season where it
  matters most.

### 10.5 Seasonal fishing — cheap, data-driven, vanilla-precedented

**Verified:** base rates are 85% fish / 10% junk / 5% treasure; fish split cod 60%, salmon 25%,
pufferfish 13%, tropical 2%. **Jungle biomes already alter the loot table** (salmon 34.78%,
bamboo and cocoa in junk).

So **biome-varying fishing is already a vanilla mechanic** — seasonal variation is an extension,
not an invention. Implementation is pure loot-table JSON, which fits the §4 datapack architecture
at near-zero cost. Salmon runs in autumn, sparse winter catches, weather already modifies wait
time (rain −20%).

### 10.6 Kept and cut

- ✅ **Water freezing / thawing in temperate winter — KEPT.** Previously cut for not being a
  differentiator; that was a category error. It is **table stakes**: a feature users expect,
  simply not the thing to lead the Modrinth page with. Config-gated, never on functional blocks.
- ❌ **Advancement tree — CUT.** Reads as questy rather than vanilla+. The retention argument
  doesn't justify the tonal mismatch.
- ❌ Day-length changes — permanently rejected (§6).
- ❌ Bare trees / leaf block removal — destructive (§3.2).

---

## 11. Greenhouses — out-of-season growing

### 11.1 Why this rule needs teeth

⚠️ **A too-permissive greenhouse destroys the mod.** If any roof enables out-of-season growth,
every player builds a dirt shed on day one and seasons stop affecting farming entirely. The
differentiator evaporates. The rule must carry real cost.

### 11.2 The mechanic

Minecraft supplies a free discriminator: **skylight passes through glass, but `canSeeSky` returns
false beneath it.**

> **Out-of-season growth requires high skylight AND no direct sky access.**

| Roof | Skylight | Sees sky | Out-of-season growth |
|---|---|---|---|
| Open field | 15 | ✅ | ❌ — in-season only |
| **Glass / stained glass** | **15** | ❌ | ✅ **greenhouse** |
| Dirt, stone, wood, slab | 0 | ❌ | ❌ |
| **Tinted glass** | **0** | ❌ | ❌ — blocks light in vanilla |
| Underground + torches | 0 sky | ❌ | ❌ |

**No hardcoded block list.** The behaviour falls out of the light engine, so glass and stained
glass qualify while tinted glass doesn't — because vanilla already makes tinted glass block light.
Proposed threshold: **skylight ≥ 12** (glass = 15; leaves and water attenuate below it, so
canopy-farming doesn't accidentally qualify).

### 11.3 Artificial light does not count

Only **skylight** enables out-of-season growth. If torches qualified, every existing underground
farm would become season-immune and the greenhouse would mean nothing.

**Vanilla's light-level-9 growth requirement is untouched.** Existing torch-lit farms keep working
exactly as before *during a crop's season* — no player's build breaks. The mod only gates
*out-of-season* farming behind glass.

### 11.4 Binary, not a growth penalty

Considered and rejected: penalising greenhouse growth rate instead of gating it.

- A rate penalty reintroduces **waiting**, the exact mechanic §10.1 exists to avoid.
- An invisible multiplier is unreadable — players can't tell whether it's working.

Instead the cost is **up-front** (smelting glass at scale) rather than ongoing. One-time
investment, permanent benefit — a far better shape than a permanent tax.

Consequently **bone meal never bypasses seasonality** (§10.1 resolved). It stays exactly what it
is in vanilla: an accelerator *within* a valid growing season. The out-of-season answer is a
greenhouse, not fertiliser — which matches the real-world logic that drove the bee decision.

### 11.5 Eternal-winter biomes: greenhouses allowed

Blocking them would undo the potato decision (§8.4⑥) and make snowy biomes worthless again. The
progression already has texture with a single rule:

> **Snowy biome = potato outdoors for free; everything else needs glass.**

**Deferred to v2:** requiring a nearby heat source (campfire, lava, furnace) in cold biomes.
Thematically excellent — real greenhouses in cold climates need heating — but it is a *second
hidden condition*, and discoverability is already a known weakness (§8.4②). One rule for v1.

### 11.6 ⚠️ Must be tested in-game

**Snow may accumulate on glass roofs and block skylight**, breaking greenhouses in winter — exactly
when they matter. Whether a 1-layer snow block meaningfully attenuates skylight could not be
confirmed from the wiki and **must be verified in-game**.

Preferred fix: exempt glass from snow accumulation. Roof-sweeping as a recurring chore is bad
design.

---

## 12. AI disclosure and publishing conduct

### 12.1 Modrinth's rules (verified 2026-09-18, modrinth.com/legal/rules)

The **"Contains AI-generated content"** label is required when:
- *"a substantial portion of the project's code is a product of AI output"*
- the project includes assets primarily or entirely from AI output
- *"the project's design or functionality relies on the use of generative AI"*
- any part of the project page relies on generative AI

**Hard bans:**
- ❌ *"No images uploaded to a gallery, icon, description, or any other part of a project page may be created or derived from generative AI output."*
- ❌ A project may not be *"entirely or primarily comprised of"* AI output.

**For this project:** apply the label. All screenshots and the icon **must be genuine in-game
captures** — which they would be regardless.

*Limitation: the AI label is not exposed in the Modrinth v2 API, so its prevalence and effect on
downloads could not be surveyed systematically.*

### 12.2 Tone — learn from the bad examples

Two mods that disclose AI use:

- **ViveMonkeCraft** (2,100 dl, 2 followers): *"Claude was heavily used in making of this project
  so if you dislike AI Half Vibe coded projects dont download this Becouse its almost 45% - 60%
  VibeCo[ded]"*
- **Jeck230NOTREALWORLD** (2,506 dl, 5 followers): *"created almost entirely using Artificial
  Intelligence (Codex) in about 2 days."*

Both are low-engagement, but both are also small rough mods — **the label cannot be blamed for
that** on this evidence. The transferable lesson is tonal: those disclaimers *apologise*.

Use a confident one-liner instead:

> *Developed with AI assistance. All code is reviewed, understood and tested by me.*

### 12.3 The scene's actual standard

**76 Minecraft mod repos commit a `CLAUDE.md`**, including highly respected ones — **VazkiiMods/Zeta**
(Quark's library, 15.2M downloads) and **MrTJP/ProjectRed** (which commits a whole `.claude/`
directory). Committing agent tooling is **normalised, not shameful**.

But read what Vazkii put in theirs — it is a gate, not a welcome:

> *"Due to an influx of poor quality Pull Requests, this project does not accept unverified code
> written agentically."*

Their policy requires a contributor to understand the codebase independently, *"independently
verify, vouch for, and explain in your own words"* what the code does, write the PR description
themselves, and disclose LLM use.

**The standard is: AI-assisted is fine; AI-unverified is not.** The objection is to people
shipping code they cannot explain.

**How to apply it here:**
1. Be able to explain **every mixin** in this mod without assistance. Mixins are where unverified
   code causes crashes in other people's modpacks.
2. The staged build order (§6) exists partly for this — each step is small enough to fully
   understand before the next.
3. Test in a real instance before every release.
4. Write release notes and the Modrinth description yourself.

### 12.4 What to commit

ViveMonkeCraft commits code only. Zeta and ProjectRed commit agent *instructions*
(`CLAUDE.md`, `AGENTS.md`) but not planning documents.

Committing `docs/DESIGN.md` is therefore slightly unusual — but it is good practice, it is what
the Matcha project already does, and for someone using open source as a **commissions portfolio**
a visible design document demonstrating this level of research is an asset, not a liability.
Recommend committing it.

---

## 13. ⚠️ Major revision — 26.x environment attributes change the architecture

Researched 2026-09-18 against the 26.3 changelogs and the Loom jar. **This supersedes parts of
§5 and §8.5.** Read it before writing any rendering code.

### 13.1 Rendering has been rewritten three releases running

| Version | Change |
|---|---|
| **26.1** | *"Changed the internals of how chunk geometry data is stored in GPU memory and how they are rendered."* Lightmap algorithm **fully rewritten**. Java 25, ZGC, 4 GB default heap. |
| **26.2** | **OpenGL → Vulkan transition begins.** *"It is intended to switch the game from OpenGL to Vulkan."* Reversed depth buffer. Beds/signs become block models. |
| **26.3** | `/posteffect` command — post-processing shaders become data-driven. |

Confirmed in the jar: a new `com.mojang.renderpearl.*` GPU abstraction, and **`LevelRenderer.allChanged()` no longer exists** — the 26.3 equivalent is
`invalidateCompiledGeometry(ClientLevel, Options, Camera, BlockColors)`.

⚠️ **Every tutorial, and Fabric Seasons' own code, targets APIs that are gone.** Mixin-based
rendering is materially more fragile than §8.5③ assumed — the pipeline is mid-rewrite and each
release may break it.

### 13.2 But most of the mod no longer needs mixins

26.1 introduced **environment attributes**: data-driven values that *"control various visual and
gameplay features depending on the dimension, biome, time, and weather."* Verified from
`net.minecraft.world.attribute.EnvironmentAttributes` in the 26.3 jar.

**Attributes that map directly onto Tilt's design:**

| Attribute | Tilt use |
|---|---|
| **`BEES_STAY_IN_HIVE`** | §10.4 winter bee dormancy — *already a data-driven attribute* |
| `NATURAL_MOB_SPAWNS`, `CREATURE_WORLD_GEN_SPAWN_PROBABILITY` | Seasonal spawning |
| `VILLAGER_ACTIVITY`, `BABY_VILLAGER_ACTIVITY` | Seasonal villager behaviour |
| `SNOW_GOLEM_MELTS`, `WATER_EVAPORATES`, `MONSTERS_BURN` | Seasonal world rules |
| `SKY_COLOR`, `FOG_COLOR`, `SUNRISE_SUNSET_COLOR`, `CLOUD_COLOR` | Seasonal atmosphere |
| `AMBIENT_LIGHT_COLOR`, `SKY_LIGHT_COLOR`, `SKY_LIGHT_FACTOR`, `BLOCK_LIGHT_TINT` | Winter's cold flat light, autumn's warm light |
| `AMBIENT_SOUNDS`, `BACKGROUND_MUSIC`, `AMBIENT_PARTICLES` | Seasonal atmosphere, no new content |
| `STAR_BRIGHTNESS`, `MOON_PHASE` | Longer, clearer winter nights |

**And the layer system is exactly the machinery seasons need** — verified signatures:

```java
EnvironmentAttributeLayer$TimeBased   → applyTimeBased(Value, int)
EnvironmentAttributeLayer$Positional  → applyPositional(Value, Vec3, SpatialAttributeInterpolator)
EnvironmentAttributeLayer$Constant    → applyConstant(Value)
```

- **`TimeBased`** expresses the season cycle natively.
- **`Positional`** expresses **hemispheres** (§10.2) natively — and because it takes a
  `SpatialAttributeInterpolator`, the north/south transition is *smoothly interpolated* rather
  than a hard boundary at z=0. That solves a problem the hemisphere design hadn't addressed.

Attributes are datapack-definable, and *"dimension types and biome definitions can overwrite and
modify environment attributes"* — which composes perfectly with the tag-based biome classes in §4.

### 13.3 ❌ What attributes do NOT cover

**Grass and foliage block colour is not in the attribute list.** Verified — the only colour
attributes are fog, sky, cloud, sunrise/sunset, and the light tints. Block-level biome tinting
still goes through `BiomeColors`, so **the foliage mixin is still required** and still carries the
fragility in §13.1.

Also still mixin work: crop seasonality, greenhouse checks, villager trade filtering.

### 13.4 Revised architecture — two layers

> **Data layer (safe, version-stable):** atmosphere, light, bees, spawns, sounds, villager
> activity — environment attributes in a datapack.
>
> **Mixin layer (fragile, isolated):** foliage/grass tint, crops, greenhouses, trades.

This is a **much better risk profile than §5 assumed**. If the rendering pipeline breaks the
foliage mixin on some future release, the data layer keeps working and the mod still delivers
seasonal atmosphere, bees, spawns and farming. The single point of failure is gone.

### 13.5 Revised build order

Supersedes §6. The safe layer now comes first and delivers visible value on its own.

| Ver | Scope | Risk |
|---|---|---|
| **0.1** | Season maths + biome classes via tags; `/season` reports season at position | none |
| **0.2** | **Environment attributes**: seasonal sky, fog, ambient light, `BEES_STAY_IN_HIVE` | low — pure data |
| **0.3** | Crop seasonality + greenhouse rule | low — server-side |
| **0.4** | Biome-typed villager trades | low — server-side |
| **0.5** | ⚠️ Foliage/grass tint mixin + cache invalidation | **high** |
| **0.6** | Hemispheres via `Positional` layers | medium |
| **1.0** | Config, README, Modrinth page | — |

**0.1–0.4 produce a genuinely useful mod with no rendering risk at all.** 0.5 becomes an
enhancement rather than a dependency.

### 13.6 Unverified — check before relying on it

- Whether datapacks can register **new attribute layers**, or only assign and modify values. The
  documented JSON shows value assignment and `modifier`/`argument` only. Registering a custom
  `TimeBased` layer may need Java — small and stable code compared to rendering mixins, but
  confirm before planning around it.
- What the `int` in `applyTimeBased` actually is (day time? world time? tick?).
- Whether `SpatialAttributeInterpolator` is usable from a datapack or code-only.

---

## 14. Working notes from the 0.0.1 spike

Findings from actually building and running it, kept because several contradict what section 5
and section 13 assumed.

### 14.1 What the spike proved

- Tinting grass, foliage and dry foliage through `BiomeColors` works, and works under Sodium.
  Sodium calls the same public statics (`getAverageGrassColor`, `getAverageFoliageColor`), so one
  hook covers both renderers.
- Hooking the public methods rather than the `ColorResolver` lambdas is the right call. Fabric
  Seasons injected into `method_23791`, a synthetic lambda whose name moves whenever the class is
  recompiled.
- `LevelRenderer.allChanged()` no longer exists in 26.3. The replacement is
  `invalidateCompiledGeometry(ClientLevel, Options, Camera, BlockColors)`, and calling it
  standalone **drops chunk geometry without requeueing a rebuild**, leaving holes in the world.
  F3+A is the correct way to force a reload by hand.

### 14.2 Bugs found, and what caused them

| Symptom | Cause |
|---|---|
| See-through leaves | `tint()` packed `(r<<16)\|(g<<8)\|b` and discarded the top 8 bits, zeroing alpha |
| Autumn looked like desert | RGB channel interpolation green to orange passes through desaturated yellow. At 0.55 it landed on almost exactly vanilla's desert foliage colour. Fixed by blending through HSV and raising strength |
| Autumn looked like savanna | One tint applied to both grass and leaves. Straw ground under straw leaves is savanna; straw ground under amber leaves is autumn |
| New defaults never applied | Gson rebuilds nested objects through their no-arg constructor, so a field missing from an older config comes back as zero, not as the initializer value. Presents as a rendering bug. Fixed with a schema version that backs up and regenerates |
| Transparent geometry degrading under Sodium | `BlockColorsMixin` returned a fresh wrapper object per call to `getTintSources`. Sodium keys caches on these identities, so they grew without bound. Confirmed by a Sodium-only run with 15 rapid chunk reloads showing no fault at all |

### 14.3 Spruce and birch are still unsolved

They use `BlockTintSources.constant(int)` with fixed values, `#619961` and `#80A755`, and are
documented as not affected by biome. They do not resolve through `BiomeColors`, so the main hook
cannot reach them.

The first attempt wrapped what `BlockColors` returned. It never tinted them in any build, and it
caused 14.2's last row. **Removed rather than repaired.** Any future attempt should replace the
registered tint source once at startup instead of wrapping per call, so object identity stays
stable.

`tintFixedColourLeaves` remains in the config, defaulting to false, as the switch for whatever
replaces it.

### 14.4 Colour decisions

Autumn's leaf and litter targets are Mojang's own, from the Dappled Forest biome added in 26.3:
foliage `#e68e30`, dry foliage `#8c3a04`.

Autumn's grass deliberately does **not** use that biome's `#df6827`. Dappled Forest is permanently
autumnal and stylised to match. Real grass dries to straw rather than turning orange, and blending
toward `#df6827` at usable strength produced `#da8830`, which is unmistakably orange. Grass uses a
straw target and shifts a shorter distance than leaves.

### 14.5 To do

- **Gradual transitions instead of F3+A.** Make the season a continuous value and rebuild a small
  number of chunk sections per tick, rated by how much the colour actually changed rather than on
  a fixed timer. Atmosphere via environment attributes needs no rebuilds at all and can be smooth
  immediately. This is the open request Fabric Seasons never answered.
- Replace the spruce and birch approach per 14.3.
- Decide whether water, the fourth biome tinted resolver, should shift in winter.
- Mod icon. Modrinth bans AI generated images, so it must be hand drawn.
