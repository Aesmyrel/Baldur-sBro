# Carnet de Faerûn

Application Android compagnon de *Baldur's Gate 3*, pour un usage personnel : encyclopédie et planificateur de builds pendant la partie, grand codex du lore de Donjons & Dragons et des Royaumes Oubliés pour les soirées de lecture, le tout sans spoiler par défaut, hors ligne et en français.

> **Contenu de fan non officiel.** Carnet de Faerûn n'est ni commandé, ni sponsorisé, ni approuvé par Larian Studios ni par Wizards of the Coast.
> *Carnet de Faerûn is unofficial Fan Content permitted under the Fan Content Policy. Not approved/endorsed by Wizards. Portions of the materials used are property of Wizards of the Coast. ©Wizards of the Coast LLC.*
> Baldur's Gate, Dungeons & Dragons, Forgotten Realms et Faerûn sont des marques de Wizards of the Coast. Baldur's Gate 3 est développé et édité par Larian Studios.

## État du projet
**Phase 1** (socle technique, design system de la direction A « Grimoire enluminé », coquille de l’appli) : vérifiée de façon indépendante, corrigée, contre-vérification en cours. **Phase 2 en cours** : pipelines des données du jeu et du lore. Le détail est dans [docs/PROGRESS.md](docs/PROGRESS.md).

| Document | Contenu |
|---|---|
| [docs/BRIEF.md](docs/BRIEF.md) | Le brief, qui fait foi |
| [docs/PLAN.md](docs/PLAN.md) | Architecture, données, écrans, phases, risques, questions ouvertes |
| [docs/SOURCES.md](docs/SOURCES.md) | Sources, licences, politiques de contenu de fans, noms VF |
| [docs/DIRECTION_ARTISTIQUE.md](docs/DIRECTION_ARTISTIQUE.md) | Les trois directions artistiques |
| [docs/design/comparatif.png](docs/design/comparatif.png) | Les trois directions côte à côte |
| [docs/PROGRESS.md](docs/PROGRESS.md) · [docs/DECISIONS.md](docs/DECISIONS.md) | Avancement et décisions |

## Construire et vérifier
```bash
bash tools/setup_android_sdk.sh                                   # SDK Android local (une fois)
export ANDROID_HOME=$PWD/.android-sdk LC_ALL=C.UTF-8 LANG=C.UTF-8
./gradlew check                 # lint, tests, captures, contrôle des permissions
./gradlew :app:assembleRelease  # APK release (R8, arm64)
```

## Régénérer les maquettes
```bash
python3 tools/design/fetch_fonts.py     # polices libres (SIL OFL) depuis Google Fonts
node tools/design/render_mockups.js     # planches et écrans dans docs/design/
```
