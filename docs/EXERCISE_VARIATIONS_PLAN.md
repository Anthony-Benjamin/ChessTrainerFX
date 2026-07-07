# Exercise Variations Plan

## Context

Het project ondersteunt oefeningen vanuit PGN/FEN, maar de oefenflow is nu lineair: een oefening heeft een startpositie, een opgeschoonde zettenreeks en telkens precies een verwachte volgende zet.

Variaties toelaten is mogelijk, maar vraagt meer dan alleen een parser-aanpassing. De PGN-invoer, sessiestate, validatie, hints, zetlijst en undo/redo moeten dezelfde gekozen variant volgen.

## Huidige aanknopingspunten

- `src/main/java/application/pgnreader/io/PGNReader.java`: varianten worden bij het inlezen verwijderd via `removeVariationsNested`.
- `src/main/java/application/chesstrainerfx/utils/PgnUtils.java`: varianten worden opnieuw weggegooid in `cleanMoveString`.
- `src/main/java/application/chesstrainerfx/utils/ExerciseSession.java`: sessie is een lineaire `List<Move>` plus een index.
- `src/main/java/application/chesstrainerfx/controller/Controller.java`: validatie accepteert precies een correcte zet via `exerciseSession.isCorrectMove(userMove)`.
- `src/main/java/application/chesstrainerfx/controller/ChapterPresenter.java`: hints gaan uit van een enkele verwachte SAN-zet.
- `src/main/java/application/chesstrainerfx/controller/PositionEditorController.java`: editor schrijft de tekstuele moves weg; varianten kunnen tekstueel worden opgeslagen, maar worden functioneel niet gebruikt bij teruglezen.

## Gewenst gedrag

Een puzzel mag meerdere correcte zetten hebben vanuit dezelfde positie. Als de speler een correcte optie kiest, gaat de oefening verder in de bijbehorende variant. Dit geldt bij voorkeur niet alleen voor de eerste zet, maar ook voor latere aftakkingen.

Voorbeeld:

```pgn
1. Nf3 (1. d4 d5 2. c4) 1... Nf6 2. g3
```

Vanaf de beginpositie zijn dan `Nf3` en `d4` allebei acceptabel, maar na de keuze moet de sessie de juiste voortzetting volgen.

## Plan

1. Doelgedrag vastleggen

   Bepaal eerst de exacte regelset:

   - Zijn meerdere eerste zetten voldoende, of moeten varianten op elke diepte werken?
   - Moet de computer altijd de eerste zet uit de gekozen variant spelen?
   - Moeten hints alle opties tonen of maar een optie?
   - Moet de moves-lijst alle varianten tonen of alleen de gekozen lijn?

2. PGN-varianten behouden

   `PGNReader` moet varianten niet langer onherroepelijk verwijderen. Bewaar minimaal de ruwe movetext naast de bestaande opgeschoonde moves, of laat `Exercise.getMoves()` voortaan variant-aware input bevatten.

   Backward compatibility is belangrijk: bestaande lineaire oefeningen moeten blijven werken.

3. Nieuw sessiemodel introduceren

   Vervang conceptueel `List<Move> + index` door een boomstructuur:

   - node = positie in de oefening
   - edge = toegestane zet vanaf die positie
   - edge bevat `Move`, SAN en eventueel comment/hint
   - sessie houdt een verwijzing naar de actieve node bij

   Dan kan `getExpectedMove()` worden vervangen door iets als `getCandidateMoves()`.

4. Variant-aware parser bouwen

   Maak een PGN movetext-parser die haakjesstructuur begrijpt:

   - mainline blijft hoofdpad
   - `( ... )` wordt een alternatieve tak vanaf de juiste vorige positie
   - comments, NAGs en resultaten worden genegeerd of als metadata bewaard
   - geneste varianten worden expliciet ondersteund of bewust afgewezen met duidelijke foutmelding

5. SAN-resolutie hergebruiken

   De SAN-resolver in `ExerciseSessionBuilder` kan grotendeels blijven, maar moet op een boom werken. Per tak moet de juiste bordstand beschikbaar zijn, via kopie van het temp-board of door opnieuw afspelen vanaf de root.

6. Controller aanpassen

   `isCorrectMove(userMove)` moet controleren of de zet overeenkomt met een van de toegestane edges vanaf de actieve node.

   Bij een match:

   - sessie springt naar de gekozen child-node
   - de spelerzet wordt uitgevoerd
   - de computertegenslag komt uit de gekozen variant
   - hints en vervolgvalidatie lezen vanaf dezelfde actieve node

7. Undo/redo variant-bewust maken

   Huidige history bewaart FEN, beurt en en-passant-state. Voor varianten moet history ook de actieve sessie-node of een variant-pad opslaan. Anders kan undo/redo na een aftakking terugkomen in de verkeerde lijn.

8. Hints en UI aanpassen

   `getExpectedSan()` moet meerdere SAN-opties kunnen teruggeven. Mogelijke eerste implementatie:

   ```text
   Next: Nf3 or d4
   ```

   De moves-list kan in eerste instantie de ruwe PGN tonen of alleen de gekozen lijn. Een latere verbetering is inspringing voor varianten of highlight van de gekozen lijn.

9. Position Editor

   De editor kan tekstuele varianten blijven opslaan in de moves-tekst. Na de parserwijziging worden die varianten functioneel gebruikt bij teruglezen. Eventueel later validatie toevoegen zodat de editor waarschuwt bij onparsebare varianten.

10. Tests toevoegen

   Gerichte tests:

   - bestaande PGN zonder varianten blijft werken
   - twee correcte eerste zetten
   - variant na een latere ply
   - geneste variant, ondersteund of bewust afgewezen
   - undo na gekozen variant
   - hint met meerdere opties
   - computerzet volgt gekozen tak

## Aanbevolen fasering

1. Variantsyntax behouden en parser/boommodel ontwerpen.
2. Alternatieve zetten vanaf dezelfde positie ondersteunen zonder complexe UI.
3. Controller, computerzet, hints en undo/redo variant-bewust maken.
4. Moves-weergave verbeteren met variant-inspringing of gekozen-lijn-highlight.

## Belangrijkste risico

Het grootste risico zit niet in de schaakregels, maar in state management. Zodra de speler een alternatieve tak kiest, moeten bord, sessie-node, hint, computerzet, finish-detectie en undo/redo allemaal dezelfde gekozen variant volgen.
