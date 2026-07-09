---
name: verify
description: Hoe je ChessTrainerFX draait en end-to-end verifieert op deze machine (JavaFX-GUI aansturen zonder xdotool).
---

# ChessTrainerFX verifiëren

## Bouwen & starten
- `./mvnw -q compile` / `./mvnw test` (13 tests, geen GUI-tests)
- App starten: `DISPLAY=<display> ./mvnw -q javafx:run` (venster is vast 1500×1000, niet-resizable)

## GUI aansturen — gotchas op deze machine
- **Geen** xdotool, wmctrl, Xvfb of python-Xlib aanwezig. Wel: `Xephyr`, `import` (ImageMagick), `xwininfo`, JDK.
- Gebruikersdisplay is `:1` (dual monitor ~3840×1080). **Het scherm kan vergrendeld zijn**: screenshots werken dan nog (compositor), maar synthetische klikken worden genegeerd. Check: `loginctl show-session <id> -p LockedHint`.
- Betrouwbare route: geneste X-server — `DISPLAY=:1 Xephyr :5 -screen 1600x1100 -ac &`, app op `DISPLAY=:5`.
- Klikken/typen: klein AWT-Robot-hulpje compileren (java.awt.Robot; `click x y`, `drag`, `type`, `key`) en op `:5` draaien; screenshots met `DISPLAY=:5 import -window root out.png`.
- Xephyr heeft **geen window manager** → PointerRoot-focus: JavaFX-popupmenu's sluiten zodra de muis erheen beweegt. Menu's daarom met toetsenbord bedienen: klik op de MenuButton, dan `key DOWN` + `key ENTER`.
- Venstergeometrie per stap checken met `xwininfo -root -tree | grep -i chess`; klikcoördinaten = venster-offset + FXML-positie. Na elke klik een screenshot ter controle.

## Nuttige flows
- Position Editor: Home → Puzzles-tegel (rechtsonder) → "Add Puzzles"-menu (linksboven) → "Position Editor".
- Scanpaneel: knop "Import from Image" togglet de linkerkolom (links van het bord); testafbeeldingen in `dev/test_images/` (FileChooser onthoudt de laatste map via Preferences).
- Een scheve crop over de preview slepen + Scan geeft gegarandeerd oranje onzeker-markeringen (handig om mark-gedrag te testen).
