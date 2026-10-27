# CLAUDE.md — Carnet de Faerûn

Appli Android native, personnelle et non commerciale, compagnon de Baldur's Gate 3 (PS5, en français) : compagnon de jeu, codex du lore D&D / Royaumes Oubliés, direction artistique sur mesure. Sans affiliation avec Larian Studios ni Wizards of the Coast.

## À lire d'abord
- `docs/BRIEF.md` — **le brief fait foi**, surtout après une compaction du contexte. En cas de doute, il l'emporte sur tout le reste.
- `docs/PLAN.md` — architecture, schéma de données, verrou anti-spoiler, phases et critères de fin.
- `docs/SOURCES.md` — sources, licences, politiques de contenu de fans (Larian, BG3, Wizards), stratégie des noms VF.
- `docs/DIRECTION_ARTISTIQUE.md` — les directions artistiques ; celle choisie devient la charte.
- `docs/PROGRESS.md` (état) et `docs/DECISIONS.md` (choix et raisons) — **à tenir à jour à chaque étape**.

## État
Phase 0 validée : **direction artistique A « Grimoire enluminé »** (charte : `docs/DIRECTION_ARTISTIQUE.md` §2), plan validé. Développement de la V1 en cours, phase par phase (voir `docs/PROGRESS.md`).

## Règles non négociables
1. **Zéro spoiler par défaut.** Tout bloc affichable porte un `SpoilerTag` (acte, niveau BG3 `AUCUN`/`LEGER`/`MAJEUR`, niveau « autres œuvres »). Le filtrage passe **toujours** par `SpoilerGate` (fonction pure, `core:model`) dans les dépôts : un écran ne reçoit jamais de contenu verrouillé. Le verrou s'applique partout : fiches, listes, recherche, liens, anecdote, alertes, widget. Une info qui laisse deviner un rebondissement est au moins `LEGER`. Tout nouveau type de contenu ajoute ses cas aux tests de fuite. Un voile ne montre ni le titre, ni l'acte, ni le niveau de ce qu'il cache. **Les documents, maquettes et messages de commit sont eux aussi sans spoiler** (pas de nom ni d'événement qui trahisse l'intrigue au-delà du début de l'Acte I).
2. **Hors ligne.** Aucune permission INTERNET, aucun tracker, aucune analytics, aucune pub. Jamais de requête réseau à l'exécution. Une dépendance qui ajouterait INTERNET au manifeste fusionné doit être retirée ou justifiée dans `docs/DECISIONS.md`. Seule permission conservée : `VIBRATE`. Google Code Scanner apporte INTERNET et ACCESS_NETWORK_STATE (télémétrie `transport-backend-cct`) : elles sont retirées du manifeste fusionné (`tools:node="remove"`) et un test bloquant le vérifie (D-021).
3. **Règles de BG3, pas de D&D papier.** Chaque règle de `core:rules` cite sa source (page bg3.wiki + identifiant de révision) en commentaire et dans `DECISIONS.md`, et a ses tests. **Aucune valeur de règle écrite de mémoire.**
4. **Exactitude avant quantité. N'invente jamais rien.** Champ inconnu → vide + signalé. Chaque paragraphe de lore a ≥ 1 référence, une nature (`CANON_BG3`, `CANON_DND`, `INTERPRETATION`) et une édition quand le lore a varié. Versions contradictoires → groupe de variantes présenté sans trancher. Théories de fans exclues (ou clairement signalées comme telles).
5. **Droits.**
   - bg3.wiki : **toujours traité comme CC BY-NC-SA 4.0** ; attribution par fiche (URL + révision) ; données dérivées sous la même licence.
   - Jamais de texte du jeu (descriptions, dialogues, livres), jamais d'image, d'icône, de police, de son ou de logo issus du jeu, des wikis ou des livres D&D.
   - Lore **rédigé avec nos mots**, en synthèse de plusieurs sources ; jamais de copie ni de paraphrase serrée d'un seul texte.
   - Jamais deux textes sous licences incompatibles dans la même fiche. Vérifier la licence de toute nouvelle source avant import et l'ajouter à `SOURCES.md`.
   - Compagnons représentés par des emblèmes et sceaux symboliques, pas par des portraits.
6. **Français partout.** Interface en français (vouvoiement, ton sobre). Termes du jeu comme dans la VF de BG3, noms des Royaumes selon les traductions officielles de D&D, nom anglais en secondaire. Chaque nom porte un statut (`VF_BG3`, `VF_DND`, `NON_OFFICIELLE`, `CORRIGEE`) ; sans source fiable → badge « traduction non officielle ». Corrections de l'utilisateur stockées localement et prioritaires. Typographie française : espace fine insécable avant `; : ! ?` et dans les guillemets `« »`.

## Charte artistique (rappel)
- Lisibilité d'abord : texte ≥ 4,5:1, grands titres ≥ 3:1 (mesurer chaque nouveau jeton), cibles ≥ 48 dp, police système jusqu'à 200 %, colonne de lecture plafonnée.
- Material 3 = socle technique ; identité entièrement sur mesure via `core:designsystem` (source unique des jetons). Jamais de composant au style par défaut, jamais d'imitation de l'interface du jeu.
- Illustrations originales, vectorielles, générées en code, dans le style unique de la direction choisie.
- Shaders AGSL : un seul animé visible à la fois, < 2 ms GPU/image, pause hors écran, rendu statique en mouvement réduit ou économiseur de batterie.
- Animations 150–400 ms, interruptibles, fluides à 120 Hz ; fondus courts en mouvement réduit. Haptique via les constantes système (respect des réglages).

## Conventions de code (à partir de la phase 1)
- Kotlin, Jetpack Compose, Material 3 stable ; dernières versions **stables** (pas d'alpha sans justification dans `DECISIONS.md`) ; minSdk 33, targetSdk/compileSdk 37.
- Modules : `app`, `core:{model,rules,database,data,designsystem,ui,testing}`, `feature:*`, `benchmark`, `tools:pipeline`. Pas de dépendance `feature` → `feature`. `core:model` et `core:rules` sont du Kotlin pur.
- UDF : `ViewModel` + `StateFlow<UiState>` immuable ; Hilt ; Navigation 3 avec pile persistée et liens profonds `carnetdefaerun://`.
- Identifiants de contenu stables dérivés du nom anglais (`spell.fireball`, `lore.deity.selune`) ; jamais renommés.
- Chaînes d'interface dans les ressources (`values/strings.xml`, français) ; aucune chaîne en dur dans les composables.
- Le code, les identifiants et les commentaires techniques peuvent être en anglais ; la documentation du projet et les textes visibles sont en français.
- Les commentaires suivent la densité du code environnant ; une règle BG3 porte toujours sa source.

## Commandes
Environnement : SDK Android installé localement dans `.android-sdk/` (non versionné). Dans un nouveau conteneur : `bash tools/setup_android_sdk.sh`. Avant toute commande Gradle : `export ANDROID_HOME=$PWD/.android-sdk LC_ALL=C.UTF-8 LANG=C.UTF-8` (les noms de tests sont accentués : sans locale UTF-8, le compilateur échoue). Java 21. Le miroir Google de Maven Central est déclaré en premier dépôt (erreurs 429 sur Maven Central). Pas de KVM ici : **pas d'émulateur** ; la vérification passe par les tests JVM, Robolectric et Roborazzi.

Maquettes (phase 0) :
- `python3 tools/design/fetch_fonts.py` — polices OFL dans `tools/design/.fonts/`
- `node tools/design/render_mockups.js` — planches et écrans dans `docs/design/`
- `python3 tools/design/contrast_check.py` — contrastes de tous les jetons → `docs/design/contrastes.md`

Appli :
- `./gradlew check` — lint (aucun avertissement toléré), tests unitaires, vérification des captures, contrôle bloquant des permissions du manifeste fusionné (`verifyReleasePermissions`)
- `python3 tools/lint_typographie.py [--fix]` — typographie française des chaînes (espaces fines insécables)
- `./gradlew testDebugUnitTest` — tests JVM (règles, verrou anti-spoiler, recherche)
- `./gradlew recordRoborazziDebug` / `verifyRoborazziDebug` — captures (sombre, OLED, parchemin, police 200 %, pliable) dans `*/src/test/screenshots/` ; Robolectric tourne en SDK 36 (`robolectric.properties`)
- `./gradlew :tools:pipeline:run --args="all"` — pipeline de données (fetch → normalize → validate → build-db)
- `./gradlew :app:assembleRelease` — APK release R8 (arm64) ; signé si `keystore.properties` existe à la racine (clé locale hors dépôt, voir `docs/INSTALLER.md`), sinon non signé

## Méthode
- Un commit Git par étape cohérente, message clair en français ; **jamais de réécriture d'historique** ; travail sur la branche désignée de la session.
- Ne rien modifier en dehors du dossier du projet.
- Chaque phase se termine par une **vérification indépendante** par des agents qui n'ont pas produit le travail : build/tests/lint ; échantillon de données et d'affirmations de lore comparé aux références ; fuites de spoilers, licences, accessibilité ; revue artistique des captures. Itérer jusqu'à ce que ce soit beau, pas seulement correct.
- Regarder soi-même les captures produites et corriger ce qui cloche.
- Ne poser de question à l'utilisateur que pour une décision difficile à défaire ou qui change le produit ; sinon trancher, noter dans `DECISIONS.md`, avancer.
- Ne jamais committer de secret : clé de signature, `keystore.properties`, `local.properties` restent hors dépôt.
