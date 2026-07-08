# Tactics Info Texts Plan

## Context

Sinds commit `9372bf4` toont de header boven het bord per oefening de titel, de positie-subtitel en de uitlegtekst uit de PGN-comments; zonder eigen comments blijft de hoofdstuk-intro (comments van de eerste game) staan. In de praktijk tonen de meeste tactics-hoofdstukken echter nog steeds geen uitleg. Analyse van alle 27 bestanden in `<tactics-dir>` (standaard `/home/badev/ChessTrainerFX/pgn/tactics/`) wijst uit dat dit een datakwestie is, geen weergavekwestie:

| Groep | Bestanden | Situatie |
|---|---|---|
| Intro zichtbaar | `2_SKEWER`, `7_DISCOVERED_CHECK_Intro`, `8_DOUBLE_CHECK`, `10_DISCOVERED_ATTACK_Intro` | Uitleg-comment zit in de **eerste game** → wordt als hoofdstuk-intro getoond |
| Uitleg verstopt | `1_PIN`, `3_FORK`, `5_DEFLECTION`, `6_DISTRACTION_Intro`, `9_WINDMILL` | Uitleg zit in een **latere oefening** (bv. "Whats Pinning and Pinned Piece"); de intro blijft leeg tot je precies die oefening opent |
| Geen tekst | de overige 18 bestanden | Nergens uitlegtekst aanwezig; moet geschreven worden |

Extra gegeven: de tactics-PGN's bestaan alleen in de datamap van de gebruiker. Er is géén gebundelde kopie in de app: `ResourceSeeder.seedIfEmpty(config.tacticsDir(), "/pgn/tactics")` in `Main.start` verwijst naar een resource-pad dat niet bestaat en doet dus niets.

## Deel A — code-fix: intro-fallback naar eerste oefening mét comments

Kleine aanpassing in `src/main/java/application/chesstrainerfx/view/ChapterWindow.java`, methode `buildHeader` (rond regel 135):

- `chapterTheoryText` niet vullen uit `exercises.getFirst().getComments()`, maar uit de **eerste oefening in de lijst waarvan de comments na `formatTheoryText` niet leeg zijn**.
- De per-oefening weergave en fallback in `setExerciseInfo` blijven ongewijzigd; ook `showExerciseList()` blijft `chapterTheoryText` herstellen.

Effect: `1_PIN`, `3_FORK`, `5_DEFLECTION`, `6_DISTRACTION_Intro` en `9_WINDMILL` tonen direct een intro zonder dat er content geschreven hoeft te worden. Mating Patterns en Puzzles veranderen niet: daar heeft de eerste game al comments (Mating) of heeft geen enkele game comments (Puzzles), dus de gekozen bron blijft dezelfde.

## Deel B — Engelse uitlegteksten toevoegen aan de 18 PGN's zonder tekst

Patroon = zoals `2_SKEWER.pgn` nu al doet: een `{ ... }`-comment in de **eerste game** (de titel-game) van het bestand, vóór de afsluitende `*`. Die comment wordt automatisch de hoofdstuk-intro; er is geen code voor nodig.

Bestanden (18):

```
4_ANNIHILATION_Into    15_COUNTERTHREATS      21_UNDERPROMOTION
11_DOUBLE_ATTACK       16_INTERFERENCE        22_DESTROYING
12_DECOY__ATTRACTION   17_BLOCKING            23_COUNTERTACTICS
13_XRRAY               18_THROWING_a_BOMB     24_HOW_to_NOTICE_TACTICS
14_INTERMEDIATE_MOVES  19_CLEARANCE           25_THE_TREE_KEY_QUESTIONS
                       20_PROMOTION           26_HOW_to_NOTICE_HIDDEN_TACTIC
                                              27_MIXED_TACTICS
```

Richtlijnen voor de teksten:

- Engels, 2–4 zinnen per tactiek, zelfde toon als de bestaande teksten ("A pin is a tactic that occurs when a piece is attacked & it can't move without exposing a more valuable piece behind it. …").
- Voor de didactische hoofdstukken (`24`–`27`) een korte beschrijving van het lesdoel in plaats van een tactiekdefinitie (bv. wat "how to notice tactics" traint).
- Bewerkingsregels: UTF-8, regels op ~80 tekens afbreken zoals de bestaande bestanden, comment vóór de `*` van de eerste game, verder niets aan de games wijzigen.
- **Vooraf een backup van de tactics-map maken** — de bestanden staan buiten de repo.

Optionele vervolgstap: de aangevulde PGN's ook bundelen onder `src/main/resources/pgn/tactics/`, zodat verse installaties ze via het bestaande seed-pad in `Main.start` meekrijgen.

## Verificatie

1. Scan herhalen (moet voor alle 27 bestanden ≥ 1 game met tekst opleveren, en na Deel B voor alle 27 een eerste game mét tekst):
   ```bash
   for f in <tactics-dir>/*.pgn; do
     awk 'BEGIN{RS="\\[Event "} NR==2 {s=$0; gsub(/\[%[^\]]*\]/,"",s);
       f=0; while (match(s,/\{[^}]*\}/)) {c=substr(s,RSTART+1,RLENGTH-2);
       gsub(/[^A-Za-z]/,"",c); if (length(c)>=20) f=1; s=substr(s,RSTART+RLENGTH)}
       printf "%-40s eerste_game_tekst=%d\n", FILENAME, f}' "$f"
   done
   ```
2. `./mvnw test` — bestaande tests groen.
3. App starten: elk tactics-hoofdstuk openen en controleren dat er een informatieve intro boven het bord staat, dat oefeningen met eigen uitleg (bv. "Whats Pinning and Pinned Piece") die tonen, en dat Mating Patterns en Puzzles onveranderd zijn.
