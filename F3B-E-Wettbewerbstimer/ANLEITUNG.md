# F3B-E Wettbewerbstimer – APK erstellen

## Über GitHub (vorhandenes Repository)

1. ZIP entpacken und den Ordner `F3B-E-Wettbewerbstimer` öffnen.
2. Im Repository: **Code → Add file → Upload files**.
3. Den **Inhalt** des Ordners hineinziehen (`app`, `gradle`, `.github`, `build.gradle.kts`,
   `settings.gradle.kts`, `gradle.properties`) → **Commit changes**.
   Der versteckte Ordner `.github` wird sichtbar mit:
   Windows-Explorer „Ansicht → Ausgeblendete Elemente“, Mac `Cmd + Shift + .`
4. Reiter **Actions** → Lauf „APK bauen“ abwarten (3–5 Minuten).
5. Grünen Lauf anklicken → unten bei **Artifacts** `F3B-E-Wettbewerbstimer-APK` herunterladen.
6. ZIP entpacken → `F3B-E-Wettbewerbstimer.apk` aufs Handy kopieren und antippen.
   Android fragt einmalig, ob Installationen aus dieser Quelle erlaubt sind → erlauben.

## Bedienung

- **Start / Stopp / Reset** unten (mit Klickgeräusch), **ⓘ** = Info, **Zahnrad** oben rechts = Flugzeit einstellen
  (wird dauerhaft gespeichert, „Standard 3:20“ setzt zurück).
- Ansagen: ab der letzten vollen Minute alle 15 s, ab 1:00 alle 10 s,
  ab 0:10 jede Sekunde, bei 0 „Landen“.
- Danach läuft die Zeit als Überzeit weiter (rot, mit Minus: -00:01, -00:02 …), jede Sekunde ein kurzer Pips.

## Sprachausgabe

Falls nichts zu hören ist:
**Einstellungen → Bedienungshilfen → Text-in-Sprache** → Deutsch installieren.
- Uhr genau bei 00:00 angehalten → Fanfare „Tätärätä“ und Konfetti („Punktlandung!“).
- Im Zahnrad-Menü lassen sich Zeitansage, Pipston nach Überzeit und Treffer-Zeit (Tätärätä) einzeln ausschalten.
