# Plan — Carnet de Faerûn

> Plan de phase 0, à valider. Le brief (`docs/BRIEF.md`) fait foi en cas de doute.
> Instantané des versions relevé le 27/10/2026 sur Google Maven, Maven Central et le portail Gradle ; il sera revérifié au premier jour de la phase 1.

## 0. En une page

- **Trois piliers, une seule base** : une base de contenu embarquée (jeu + lore), produite hors de l'appli par un pipeline versionné, et une base utilisateur séparée.
- **Le verrou anti-spoiler est une fonction pure** (`SpoilerGate`) appliquée à chaque bloc de contenu, partout : listes, fiches, recherche, liens, anecdote du jour, widget. Elle est testée de manière exhaustive, et des tests de fuite parcourent toute la base.
- **Les règles de BG3 sont un module Kotlin pur** (`core:rules`), sans Android, où chaque règle est sourcée (page bg3.wiki + révision) et testée.
- **Le lore est écrit par nous**, fiche par fiche, en sections étiquetées (acte, niveau de spoiler, nature du canon, édition), chaque paragraphe rattaché à des références.
- **La direction artistique est un design system sur mesure** posé sur Material 3 (accessibilité, composants), avec ornements vectoriels, illustrations codées, polices libres embarquées et shaders AGSL sous budget.
- **Environnement vérifié** : SDK Android installable ici (installé : plateforme 37, build-tools 37.0.0). **Pas de KVM, donc pas d'émulateur** : vérification par tests JVM, Robolectric et captures Roborazzi ; les mesures sur appareil (démarrage à froid, 120 Hz, haptique, Baseline Profiles) iront dans `docs/TEST_SUR_TELEPHONE.md`.

## 1. Architecture

### 1.1 Socle technique (instantané du 27/10/2026)

| Brique | Version stable retenue | Remarque |
|---|---|---|
| Kotlin | 2.4.21 | 2.5.0 en bêta, non retenue |
| Android Gradle Plugin | 9.4.1 | API 37 max prise en charge ; 9.5 en RC |
| Gradle | 9.8.2 | AGP 9.4 exige ≥ 9.6.0 |
| KSP | 2.3.12 | |
| Compose BOM | 2026.09.00 | Compose UI/Foundation 1.12.1 |
| Material 3 | 1.4.0 | **Material 3 Expressive (1.5.0) n'est pas stable** (bêta 02 du 21/10/2026) : on reste sur 1.4.0 stable ; nos composants sur mesure couvrent l'expressivité. Réévaluation dès la sortie stable |
| Material 3 Adaptive | 1.3.0 (+ `adaptive-navigation3` 1.3.0) | mises en page adaptatives |
| Navigation 3 | 1.2.0 | pile de retour possédée par l'appli : idéale pour la « reprise exacte » |
| Lifecycle (dont `viewmodel-navigation3`) | 2.11.0 | |
| Activity Compose | 1.13.0 | edge-to-edge, retour prédictif |
| Hilt (Dagger) | 2.60.1 ; `hilt-lifecycle-viewmodel-compose` 1.4.0 | |
| Room | 2.8.5 (Kotlin Multiplatform) | même schéma pour l'appli et pour le pipeline JVM |
| SQLite embarqué (`sqlite-bundled`) | 2.7.1 | même moteur SQLite (FTS) sur l'appareil et dans le pipeline |
| DataStore | 1.3.0 | réglages |
| kotlinx.serialization | 1.12.0 | JSON du pipeline, export des données, codes de build |
| ZXing core | 3.5.4 | génération des QR codes (aucune permission) |
| Google Code Scanner (Play services) | 16.1.0 | lecture de QR sans permission caméra ; vérification que le manifeste fusionné n'ajoute pas INTERNET |
| Glance | 1.2.0 | V2 (widget) |
| ML Kit GenAI Prompt | 1.0.0-beta5 (**bêta**) | V2 seulement, sous réserve (voir questions ouvertes) |
| Roborazzi / Robolectric | 1.77.0 / 4.17 | captures sur JVM ; Robolectric exécuté sur le SDK le plus récent qu'il prend en charge |
| Benchmark / Baseline Profile | 1.5.0 | nécessite un appareil ou un émulateur |
| SDK | compileSdk 37, targetSdk 37, minSdk 33 | build-tools 37.0.0 |

Aucune alpha. Seules exceptions envisagées, en V2 et justifiées le moment venu : ML Kit Prompt (bêta, aucune version stable n'existe).

### 1.2 Modules Gradle

```
app/                         Activité unique, NavDisplay, graphe Hilt, liens profonds, écran d'accueil système
build-logic/                 Plugins de convention (android-library, compose, hilt, feature, jvm-library, kmp-room)
core/model/                  Kotlin pur : entités du domaine, Acte, SpoilerTag, SpoilerProfile, SpoilerGate, ids
core/rules/                  Kotlin pur : moteur de règles BG3 (achat de points, PV, maîtrise, emplacements, CA, jets, probabilités)
core/database/               Room KMP (cibles Android + JVM) : base de contenu (lecture seule) + base utilisateur
core/data/                   Dépôts, recherche, application du SpoilerGate, export/import, préférences (DataStore)
core/designsystem/           Thème, jetons, polices, ornements, illustrations, shaders AGSL, composants
core/ui/                     Composants liés au domaine : voile de spoiler, carte de fiche, badge de canon, note de référence
core/testing/                Doubles de test, jeux de données réduits, règles JUnit, utilitaires de capture
feature/home/                Ma partie (accueil), anecdote du jour, alerte point de non-retour, mode session
feature/encyclopedia/        Classes, races, historiques, dons, sorts, objets, états, actions : listes filtrables et fiches
feature/codex/               Codex du lore : panthéon, cosmologie, peuples, bestiaire, factions, lieux et carte, chronologie, saga, livres du jeu, lectures, glossaire, mode lecture
feature/builds/              Planificateur, détail des calculs, groupe de 4, partage QR et code texte
feature/companions/          Recrutement, approbations, moments clés, romance, quête personnelle
feature/progress/            Quêtes, lieux, manquables, trophées par acte
feature/tools/               Probabilité d'un jet, lanceur de dés
feature/search/              Recherche globale (surcouche accessible partout)
feature/notes/               Notes rattachables
feature/settings/            Progression et spoilers, apparence, session, données (export/import), corrections VF, crédits et licences, à propos
benchmark/                   Macrobenchmark + génération des Baseline Profiles (exécution sur appareil)
tools/pipeline/              Application JVM : fetch → normalize → validate → build-db (dépend de core:model et core:database JVM)
```

Règles de dépendance : `feature:*` → `core:ui`, `core:designsystem`, `core:data`, `core:model` ; jamais de `feature` vers `feature` (navigation par clés déclarées dans `core:model`). `core:rules` et `core:model` sont des modules JVM purs, testables sans Android.

### 1.3 Flux de données
- **UDF** : `ViewModel` → `StateFlow<UiState>` immuable ; événements utilisateur → fonctions du ViewModel ; effets ponctuels via un canal dédié.
- Les dépôts exposent des `Flow` déjà filtrés par le `SpoilerGate` : un écran ne reçoit **jamais** de contenu verrouillé, seulement un marqueur `Verrouillé(acte, niveau)` qui s'affiche comme un voile.
- Le profil de spoiler (`SpoilerProfile`) est un `StateFlow` global : changer d'acte met à jour instantanément tous les écrans ouverts.

### 1.4 Navigation
- **Navigation 3** : la pile de retour est une liste de clés sérialisables. Elle est **persistée** (DataStore) à chaque changement : à la relance, l'appli rouvre exactement l'écran, l'onglet, le filtre et la position de défilement où tu étais.
- Navigation principale en bas, dans la zone du pouce (5 entrées) : **Partie · Grimoire** (encyclopédie du jeu) **· Codex** (lore) **· Builds · Outils**. Recherche : bouton flottant en bas à droite, présent sur tous les écrans de premier niveau, et action de recherche dans chaque barre supérieure. Compagnons, Progression et Notes s'ouvrent depuis Ma partie et depuis les fiches.
- Largeur moyenne ou étendue (pliable ouvert, paysage, tablette) : rail de navigation, colonnes de lecture plafonnées, grilles à deux colonnes. Le vrai liste + détail côte à côte est prévu en V2 (point 25), mais l'architecture Navigation 3 + `adaptive-navigation3` le permet dès la V1 sans refonte.
- **Liens profonds** : schéma `carnetdefaerun://` (`/fiche/{id}`, `/codex/{id}#{section}`, `/build/import?code=…`, `/recherche?q=…`), analysés par un parseur maison qui reconstruit une pile cohérente. Le contenu verrouillé reste verrouillé même par lien profond.

### 1.5 Contraintes plateforme (Android 17, Pixel 10 Pro XL)
- **Edge-to-edge** et **retour prédictif** (animations de retour personnalisées : la page se referme comme un livre).
- **Ciblage d'Android 17** : sur les écrans de plus de 600 dp, l'orientation et le redimensionnement ne peuvent plus être bloqués. Toutes les mises en page sont donc adaptatives et l'état survit à la rotation, au pliage et au redimensionnement.
- **120 Hz** : listes paresseuses avec clés et types de contenu stables, état immuable (`kotlinx-collections-immutable`), pas d'allocation dans le dessin, ornements mis en cache (`drawWithCache`), shaders sous budget.
- **Haptique** : `performHapticFeedback` avec les constantes système (respecte automatiquement les réglages de l'appareil) ; compositions `VibrationEffect` (primitives `THUD`, `SPIN`, `LOW_TICK`) pour le lancer de dés, avec l'usage « toucher » pour que le réglage système s'applique. La permission `VIBRATE` (normale, accordée à l'installation) sera la seule permission demandée.
- **Mouvement réduit** : si l'échelle d'animation système est à 0, si l'économiseur de batterie est actif ou si l'option de l'appli est cochée, les transitions deviennent des fondus courts et les shaders animés s'arrêtent (rendu statique).
- **Écran toujours allumé** pendant une session : `keepScreenOn` sur la vue racine, sans permission, désactivé automatiquement en quittant le mode session.
- **Aucune permission INTERNET** : un test vérifie le manifeste fusionné de la variante release.

## 2. Verrou anti-spoiler (spécification)

### 2.1 Étiquettes
Chaque **bloc** affichable (titre d'une fiche, résumé, section de lore, paragraphe, relation, objet de progression, anecdote, entrée de chronologie, résultat de recherche) porte un `SpoilerTag` :
- `acte` : premier moment où l'info est connue en jouant (`PROLOGUE`, `ACTE_1`, `ACTE_2`, `ACTE_3`, `EPILOGUE`) ;
- `niveau` BG3 : `AUCUN` (descriptif, sans révélation), `LEGER` (laisse deviner, donne un indice), `MAJEUR` (révèle un rebondissement ou une issue) ;
- `autresOeuvres` : `AUCUN`, `LEGER` ou `MAJEUR` pour les révélations sur les jeux précédents, romans et aventures.
« Une info qui laisse deviner un rebondissement » vaut au moins `LEGER` à l'acte du rebondissement.

### 2.2 Profil
`SpoilerProfile(acteCourant, toleranceBG3, toleranceAutresOeuvres)` avec tolérances `AUCUN`, `LEGER`, `COMPLET`.

### 2.3 Règle de visibilité
Un bloc est visible si **les deux** conditions sont vraies, ou s'il a été révélé à la demande :
1. **BG3** :
   - acte du bloc **antérieur** à l'acte courant → visible ;
   - acte du bloc **égal** à l'acte courant → visible si niveau ≤ `AUCUN` (tolérance aucun), ≤ `LEGER` (tolérance légère), toujours (tolérance complète) ;
   - acte du bloc **postérieur** → invisible (tolérance aucun), visible seulement si niveau = `AUCUN` (tolérance légère : on sait qu'un objet ou un lieu existe plus tard, sans rien sur l'intrigue), toujours (tolérance complète).
2. **Autres œuvres** : niveau ≤ seuil de la tolérance « autres œuvres » (aucun → `AUCUN` seulement ; léger → jusqu'à `LEGER` ; complet → tout).

Conséquence voulue : en tolérance « aucun », un rebondissement de fin d'Acte 1 étiqueté `MAJEUR` reste voilé tant que tu es en Acte 1, et se lève dès que tu passes en Acte 2.

### 2.4 Affichage du contenu verrouillé
- **Section ou paragraphe** : voile de brume avec « Acte 2 — révéler ? ». Révélation au toucher (la brume se dissipe sous le doigt, retour haptique), mémorisée par bloc, annulable (« re-voiler »), et un réglage « tout re-voiler ».
- **Fiche dont le titre est verrouillé** : absente des listes et de la recherche. Les liens qui y mènent deviennent « une fiche de l'Acte 2 (masquée) ».
- **Recherche** : les résultats verrouillés ne sont ni affichés ni comptés. Une ligne fixe, toujours présente quel que soit le résultat, propose « Chercher aussi dans le contenu masqué » : elle ne révèle donc pas l'existence d'un résultat caché.
- **Anecdote du jour, widget, alertes** : choisies uniquement parmi le contenu visible.

### 2.5 Tests de non-fuite
1. Tests unitaires exhaustifs de `SpoilerGate` : toutes les combinaisons acte du bloc × niveau BG3 × acte courant × tolérance BG3 × niveau autres œuvres × tolérance autres œuvres (5 × 3 × 5 × 3 × 3 × 3 = 2 025 cas) comparées à une table de vérité écrite à la main.
2. Tests de la base : chaque bloc a une étiquette ; un titre ne peut pas être moins protégé que ses sections les plus sensibles qu'il résume ; une relation est au moins aussi protégée que ses deux extrémités.
3. **Test de fuite global** (Robolectric + Compose) : pour chaque profil de référence (Prologue/aucun, Acte 1/aucun, Acte 2/léger…), on rend chaque fiche, liste et résultat de recherche, on collecte tout le texte de l'arbre sémantique et on vérifie qu'aucun texte d'un bloc verrouillé n'y figure.
4. Recherche : chaque titre verrouillé, tapé tel quel, ne renvoie rien.
5. Les mêmes tests s'appliqueront au widget en V2.

## 3. Schéma de données

### 3.1 Principes
- **Une seule source de vérité** : les classes Kotlin de `core:model` (sérialisables) définissent le format des JSON du pipeline ; les entités Room de `core:database` (module KMP) sont utilisées à la fois par le pipeline JVM, qui crée et remplit `content.db`, et par l'appli, qui la lit. Le schéma Room exporté (`core/database/schemas/`) est versionné et testé.
- **Identifiants stables** dérivés du nom anglais : `spell.fireball`, `item.blood-of-lathander`, `lore.deity.selune`. Ils ne changent jamais ; une suppression passe par une liste de tombstones pour que la base utilisateur ne perde rien.
- Champs communs à **toute** entrée : `id`, `nom_fr`, `nom_en`, `statut_vf`, `alias[]`, `tag` (acte, niveau, autres œuvres), `tag_titre`, `source_id`, `licence_id`, `attribution` (URL + révision), `date_import`.

### 3.2 Base de contenu (`content.db`, lecture seule, livrée dans l'APK)

| Table | Contenu principal |
|---|---|
| `entry` | Socle commun de toute fiche (champs ci-dessus + `type`, `resume_fr`) |
| `class`, `subclass`, `class_level` | Dé de vie, maîtrises, caractéristique d'incantation, progression par niveau, niveau d'obtention de la sous-classe |
| `race`, `subrace`, `background`, `feat` | Traits, prérequis, effets structurés |
| `spell` | Niveau, école, temps d'incantation, portée, zone, durée, concentration, rituel, jet de sauvegarde, dégâts (dés, type), mise à l'échelle, classes |
| `item` | Type, rareté, emplacement, poids, valeur, propriétés, **acte, lieu, marchand, manquable (jusqu'à quel point)** |
| `condition`, `action` | États et actions, effets structurés |
| `companion`, `approval_event`, `companion_step` | Recrutement (lieu, acte, pièges), approbations (+/−, acte, contexte résumé), étapes de romance et de quête |
| `quest`, `place`, `achievement`, `progress_item` | Progression cochable par acte ; `point_of_no_return` (ce qui devient manquable à chaque changement d'acte) |
| `lore_section` | Fiche, ordre, **type** (`GENERAL`, `BG3`, `AUTRES_OEUVRES`, `ECARTS_CANON`, `POUR_ALLER_PLUS_LOIN`), titre, étiquette, temps de lecture |
| `lore_paragraph` | Texte (rédigé par nous), **nature du canon** (`CANON_BG3`, `CANON_DND`, `INTERPRETATION`), **édition** (`AD&D 1`, `AD&D 2`, `3e`, `3.5`, `4e`, `5e 2014`, `5e 2024`, `multi-éditions`), groupe de variantes (pour présenter des versions contradictoires côte à côte, sans trancher) |
| `reference`, `paragraph_reference` | Référence (type, titres VO/VF, éditeur, année, édition, URL, révision, ISBN, statut « consultée directement / citée par une source secondaire ») et localisation (chapitre, page) |
| `relation` | Relations typées entre fiches : `VENERE`, `ALLIE_DE`, `RIVAL_DE`, `MEMBRE_DE`, `DIRIGE`, `SITUE_DANS`, `PARENT_DE`, `CREE_PAR`, `APPARAIT_DANS`, `EVOQUE_DANS`, `ACCORDE_SORT`… avec étiquette de spoiler propre (base de la future Toile du lore) |
| `timeline_event` | Année de début et de fin en CV, précision (exacte, environ, ère), ère, titre, résumé, étiquette, références, fiches liées |
| `map_place`, `plane_node` | Coordonnées sur **notre** carte ; nœuds et liens du schéma des plans |
| `game_book` | Livres et notes du jeu : lieu, acte, résumé de quelques lignes avec nos mots, fiches de lore liées (jamais le texte original) |
| `work` | Œuvres « pour aller plus loin » : type, titres VO/VF, année, édition, ce que l'œuvre apporte, fiches liées |
| `glossary_term`, `daily_fact` | Glossaire VF/VO ; anecdotes étiquetées et référencées |
| `source`, `license` | Registre des sources et des licences (NC, SA, attribution) |
| `search_name`, `search_fts` | Index des noms (FR, EN, alias, formes normalisées) ; index plein texte FTS4 (`unicode61`, `remove_diacritics=2`) sur résumés et sections, avec l'étiquette de chaque ligne pour filtrer en SQL |
| `meta` | Version des données, version du jeu couverte, date de génération |

### 3.3 Base utilisateur (`user.db`, séparée, sauvegardée)
`checked_item`, `build` (JSON versionné), `party`, `note` (+ cible : acte, quête, compagnon, fiche), `favorite`, `reading_position`, `name_correction`, `revealed_block`, `session_log`. Réglages et pile de navigation dans DataStore.
- Toutes les références au contenu passent par les identifiants stables : une nouvelle `content.db` ne touche jamais `user.db`. Un test simule une mise à jour de la base de contenu et vérifie que tout est retrouvé.
- **Export/import** : fichier `.carnet` (JSON compressé, versionné) via le sélecteur de documents du système (aucune permission). **Sauvegarde Android** : règles d'extraction de données incluant `user.db` et les réglages, excluant `content.db` (réinstallée avec l'APK).

### 3.4 Pipeline (`tools/pipeline`)
1. `fetch` : interroge l'API Cargo et l'API de pages de bg3.wiki (débit limité, User-Agent identifié), met en cache les réponses brutes (non versionnées) et note la révision de chaque page.
2. `normalize` : produit `data/game/*.json` (identifiants stables, unités, énumérations) ; fusionne les noms FR de `data/i18n/noms.fr.yaml` (avec statut et source).
3. `validate` : schéma, champs obligatoires, unicité des identifiants, références croisées, étiquettes de spoiler présentes, licence et attribution sur chaque entrée, chaque paragraphe de lore rattaché à au moins une référence existante, édition renseignée pour le canon D&D, cohérence des étiquettes (titre vs sections, relations vs extrémités), comptes par catégorie comparés aux objectifs.
4. `build-db` : crée `content.db` avec les entités Room (moteur SQLite embarqué), remplit les index de recherche, écrit `meta`, copie la base dans les assets de l'appli.
- Le lore est rédigé dans `content/lore/<thème>/<id>.yaml` (une fiche par fichier, sections et paragraphes étiquetés, références par identifiant du registre `content/references.yaml`). Le format est validé par le même code Kotlin.
- Tout est rejouable : `./gradlew :tools:pipeline:run --args="all"`. Rien n'est récupéré par l'appli à l'exécution.

## 4. Recherche
- **Noms** (FR, EN, alias) : index en mémoire chargé à la première recherche (quelques milliers de noms), normalisé (minuscules, sans accents, ligatures œ/æ dépliées, apostrophes unifiées), préfixes + trigrammes, puis distance de Damerau-Levenshtein bornée (1 faute jusqu'à 5 lettres, 2 au-delà) pour tolérer les fautes de frappe.
- **Texte** : FTS4 avec suppression des diacritiques, filtré en SQL par étiquette.
- **Classement** : correspondance exacte > préfixe > mot > faute de frappe > texte intégral ; bonus pour l'acte courant et les favoris.
- Résultats groupés par type (Sorts, Objets, Lore…), affichage dès la première lettre. **Objectif < 50 ms** (mesuré par un test de performance JVM sur la base complète, puis sur appareil).

## 5. Moteur de règles BG3 (`core:rules`)

Chaque règle est implémentée à partir d'une page de bg3.wiki dont la révision est notée dans le code et dans `docs/DECISIONS.md`, et couverte par des tests (cas nominaux, bornes, exemples tirés des pages). **Aucune valeur n'est écrite de mémoire.** Catalogue à couvrir :

| Règle | Contenu à extraire et tester |
|---|---|
| Achat de points | Budget, bornes avant bonus, coût par valeur ; bonus +2/+1 au choix (BG3 n'impose pas de bonus raciaux fixes) ; plafond à la création |
| Bonus de maîtrise | Par niveau total, jusqu'au niveau 12 |
| Points de vie | Niveau 1 de la première classe, gain fixe par niveau ensuite, modificateur de CON rétroactif, bonus (Robuste, Ténacité naine, Résilience draconique…) |
| Multiclasse | Conditions éventuelles, maîtrises obtenues en multiclassant, niveau d'obtention de chaque sous-classe |
| Emplacements de sorts | Table du lanceur multiclassé (lanceurs complets, demi-lanceurs, tiers de lanceur), Magie de pacte séparée, sorts connus/préparés par classe et niveau |
| Dons et améliorations | Niveaux d'obtention par classe (exceptions de certaines classes), effets structurés |
| Classe d'armure estimée | Armures légère/intermédiaire/lourde, bouclier, Défense sans armure (barbare, moine), Armure de mage, Résilience draconique, style Défense, objets choisis |
| DD et attaques | DD de sauvegarde des sorts, bonus d'attaque des sorts et des armes (finesse, maîtrise, enchantement) |
| Jets de d20 | Avantage/désavantage, **réussites et échecs critiques selon le type de jet** (attaque, test de caractéristique et de dialogue, jet de sauvegarde), dés de bonus (Assistance, Inspiration bardique selon le niveau du barde, Bénédiction…), relances (Chanceux du halfelin, Inspiration), seuil de critique élargi (ex. Champion), effets éventuels de la difficulté (Explorateur, Équilibré, Tacticien, Honneur, Personnalisée) ; dés karmiques signalés comme non modélisables |
| Probabilités | Calcul **exact** par convolution des distributions (pas de simulation), vérifié par énumération exhaustive en test |

Le **détail du calcul** de chaque statistique est un arbre de termes (`8 + 4 (maîtrise) + 5 (Intelligence 20) = 17`) produit par le moteur lui-même : l'affichage au toucher ne peut pas diverger du calcul.

Partage de build : format binaire compact versionné → Base45 → QR (ZXing) ; même charge utile en code texte `CF1-…` copiable. Import : Google Code Scanner (sans permission caméra), collage du code, ou image choisie dans le sélecteur de photos (décodage ZXing).

## 6. Écrans (V1)

| Écran | Contenu | Accès |
|---|---|---|
| **Ma partie** (accueil) | En-tête illustré de l'acte ; sélecteur d'acte en un appui ; difficulté ; groupe actif (4 emplacements) ; 3 prochains objectifs cochables ; **alerte point de non-retour** (« Avant l'Acte 2 : 6 éléments deviendront manquables ») ; « Reprendre » (dernier écran et dernière lecture) ; « Le saviez-vous ? » ; bouton **Mode session** (écran allumé) | Onglet Partie |
| Progression | Onglets par acte ; Quêtes, Lieux, Manquables, Trophées ; cases à cocher en un appui (sceau qui s'imprime), pourcentage par acte et par catégorie | Ma partie, fiches |
| Compagnons | Liste en sceaux ; fiche : recrutement (où, quand, pièges), approuve/désapprouve, moments clés par acte, romance, quête personnelle (étapes cochables) | Ma partie, Grimoire |
| **Grimoire** (encyclopédie) | Catégories → liste filtrable (feuille de filtres : classe, niveau, école, concentration, rareté, acte, type de dégâts…) → fiche reliée (liens vers sorts, classes, objets, lore) | Onglet Grimoire |
| **Codex** (lore) | Accueil du codex par thème ; **fiche de lore en mode lecture** : lettrine, sections étiquetées, badges de canon et d'édition, appels de références et notes, voiles de spoiler, « Tirer le fil » (fiches reliées), temps de lecture, taille du texte, favoris, reprise de lecture | Onglet Codex |
| Panthéon | Grille de sceaux divins ; filtres (domaine, alignement, présence dans BG3) ; relations alliés/rivaux | Codex |
| Cosmologie | Schéma original des plans, interactif ; fiches de plans | Codex |
| Carte | Carte originale de la région, zoom, lieux reliés aux fiches (filtrés par acte) | Codex, fiches de lieu |
| Chronologie | Frise verticale par ères, en CV, filtrable (ère, thème, lié à BG3), chaque événement relié | Codex |
| Saga, Livres du jeu, Pour aller plus loin, Glossaire | Listes et fiches | Codex |
| **Builds** | Liste de builds ; éditeur (frise des niveaux 1 → 12, classes, sous-classes, achat de points, dons, sorts) ; panneau de statistiques dérivées avec détail au toucher ; vue **groupe de 4** (rôles, compétences, types de dégâts couverts ou manquants) ; partage et import | Onglet Builds |
| **Outils** | Probabilité d'un jet ; lanceur de dés (lancer avec élan, haptique) | Onglet Outils |
| **Recherche** | Surcouche plein écran, résultats instantanés groupés, FR/EN, tolérante aux fautes | Partout |
| Notes | Liste, éditeur, rattachement (acte, quête, compagnon, fiche) | Ma partie, chaque fiche |
| Réglages | Progression et spoilers ; apparence (sombre, noir OLED, parchemin, taille du texte de lecture, animations) ; session ; données (export, import) ; corrections VF ; **Crédits et licences** ; À propos (mentions de non-affiliation) | Ma partie |

## 7. Arborescence du codex de lore

Objectifs en nombre de fiches. **Socle** = ce que la V1 doit contenir au minimum, entièrement référencé ; **Cible** = objectif de la V1 complète si la vérification suit. L'exactitude passe avant ces chiffres : une fiche non vérifiée ne sort pas.

| Thème | Contenu | Socle | Cible |
|---|---|---|---|
| Panthéon | Divinités présentes ou évoquées dans BG3 (Shar, Selûne, Bhaal, Bane, Myrkul, Jergal, Mystra, Lathander, Tyr, Ilmater, Silvanus, Lolth, Gith comme figure fondatrice…), puissances apparentées (archidiables, Vlaakith) | 18 | 32 |
| Cosmologie | Plan matériel et Toril, Astral, Gisombre, Féerie, plans élémentaires, Neuf Enfers et leurs strates (Avernus…), Abysses, Plan de la Fugue, demi-plans, Guerre sanglante | 10 | 16 + schéma |
| Peuples | Races jouables et sous-races (origines, culture selon D&D, regard de BG3), autres peuples (githzerai, flagelleurs mentaux, myconides, kuo-toa, gobelins…) | 14 | 22 |
| Bestiaire | Créatures croisées dans BG3 : écologie selon D&D, conseils de combat propres à BG3 | 20 | 45 |
| Factions | Ménestrels, Poing enflammé, Zhentarim, Guilde, cultes, ordres religieux, githyanki de Vlaakith… | 10 | 18 |
| Lieux | Côte des Épées, la Porte de Baldur et ses quartiers, Chionthar, Outreterre, Château-Suif, Eauprofonde, Elturel… (+ carte) | 15 | 30 |
| Personnages | Le volet « Dans l'univers de D&D » des compagnons et des grandes figures (Elminster, Volo, Minsc…), figures historiques | 14 | 30 |
| Objets légendaires | Artefacts et reliques dont le lore dépasse le jeu | 6 | 15 |
| Chronologie | Événements datés en CV, des empires disparus (Netheril…) à 1492 CV, versions contradictoires présentées côte à côte | 40 | 80 |
| Saga Baldur's Gate | Jeux précédents et extensions, aventures et romans liés, échos dans BG3 (réglage « autres œuvres » essentiel ici) | 8 | 14 |
| Livres et notes du jeu | Index (lieu, acte), résumé avec nos mots, lien vers le lore | 40 | 100 |
| Pour aller plus loin | Suppléments (dont *Heroes of Faerûn* et *Adventures in Faerûn*), aventures, romans, jeux : titre VF, apport | 25 | 45 |
| Glossaire | Termes et noms propres, VF/VO | 120 | 250 |
| Le saviez-vous ? | Anecdotes étiquetées et référencées | 100 | 250 |
| **Total fiches (hors glossaire et anecdotes)** | | **220** | **447** |

Production : une fiche pilote par thème d'abord (revue indépendante du format, du ton, des références et des étiquettes), puis des lots parallèles par thème.

## 8. Données de jeu — objectifs de couverture V1

| Catégorie | Objectif |
|---|---|
| Classes / sous-classes | 12 / 100 % (dont les 12 du Patch 8) |
| Races / sous-races | 100 % des jouables |
| Historiques, dons | 100 % |
| Sorts | 100 % des sorts apprenables par une classe ou une sous-classe ; sorts d'objets ensuite |
| Objets | 100 % des Rares, Très rares, Légendaires et des objets d'histoire ; Inhabituels notables ; le reste plus tard |
| États, actions | 100 % des états courants et des actions de base et de classe |
| Compagnons | 10 compagnons d'origine (recrutement, approbations, romance, quête) |
| Progression | Quêtes principales, de compagnons et secondaires notables ; manquables ; trophées PS5 |
| Noms VF | Taux de `VF_BG3` mesuré et affiché dans le rapport ; tout le reste badgé |

Le rapport final donnera la couverture réelle par catégorie en %.

## 9. Phases, critères de fin et lots

Chaque phase se termine par une **vérification indépendante** menée par des agents qui n'ont pas produit le travail : (1) build, tests, lint ; (2) échantillon de données et d'affirmations de lore comparé aux références ; (3) fuites de spoilers, licences, accessibilité ; (4) revue artistique des captures selon `docs/DIRECTION_ARTISTIQUE.md`. Itération jusqu'à ce que ce soit beau, pas seulement correct. Un commit par étape cohérente, `docs/PROGRESS.md` et `docs/DECISIONS.md` tenus à jour.

| Phase | Contenu | Critères de fin |
|---|---|---|
| **0** | Brief, sources, plan, directions artistiques, maquettes, CLAUDE.md | **Ton choix de direction et ta validation du plan** |
| **1 — Socle et design system** | Modules, plugins de convention, catalogue de versions, Hilt, Navigation 3 persistée, réglages, `SpoilerGate`, design system complet de la direction choisie (jetons, polices, ornements, composants, shaders, thèmes sombre/OLED/parchemin, accents par section), écran catalogue, Roborazzi (sombre, parchemin, police 200 %, largeur pliable), release signée avec R8, test « pas d'INTERNET » | `./gradlew check` vert, APK release produit, table de vérité du verrou validée, captures du catalogue revues et approuvées par la revue artistique |
| **2a — Pipeline jeu** | fetch/normalize/validate/build-db sur bg3.wiki ; couche de noms FR | Validation verte ; comptes conformes aux objectifs ; 5 % d'échantillon vérifié sans écart ; licences et attributions présentes partout |
| **2b — Pipeline lore** (en parallèle de 2a) | Format YAML, registre des références, validateurs, fiches pilotes (1 par thème) puis lots par thème | Chaque paragraphe référencé, étiquettes cohérentes, aucune reprise de texte (contrôle de similarité avec les sources consultées), 10 % des affirmations vérifiées par un agent indépendant |
| **3 — Grimoire, Codex, Recherche** | Listes, filtres, fiches reliées, mode lecture, carte, schéma des plans, chronologie, recherche globale | Recherche < 50 ms sur la base complète (test JVM), tests de fuite verts, captures revues |
| **4 — Builds** | Moteur de règles complet, éditeur, statistiques avec détail, groupe de 4, QR et code texte | Toutes les règles du catalogue testées et sourcées ; aller-retour export/import d'un build identique |
| **5 — Ma partie, Progression, Compagnons** | Accueil complet, points de non-retour, cases cochables, pourcentages, compagnons | Coche en un appui, reprise exacte après arrêt du processus (test), alertes correctes par acte |
| **6 — Outils, Notes, Sauvegarde** | Probabilités, dés, notes rattachables, export/import, sauvegarde Android | Probabilités = énumération exhaustive ; export → réinstallation → import sans perte |
| **7 — Finitions et livraison** | Performance (Baseline Profiles si appareil), accessibilité (TalkBack, 48 dp, police 200 %), polish artistique, Crédits et licences, `vitrine.png`, `INSTALLER.md`, `TEST_SUR_TELEPHONE.md`, README, APK signé, rapport final | Revue indépendante complète verte |
| V2 (après ta validation de la V1) | Archiviste, Toile du lore, widget Glance, liste + détail sur pliables et tablettes | À définir |

**Lots parallélisables**
- Après la phase 1 : **D1…D8** données de jeu (classes et sous-classes ; races ; historiques et dons ; sorts ; objets ; états et actions ; compagnons ; quêtes, lieux, trophées) ‖ **L1…L12** lore par thème ‖ **R** moteur de règles (ne dépend que de `core:model` et des données de classes) ‖ **I** illustrations (12 emblèmes de classe, 8 glyphes d'école, 13 icônes de type de dégâts, en-têtes de section et des 5 actes, carte, schéma des plans, icône adaptative et monochrome).
- Puis les écrans des phases 3 à 6 se répartissent par module `feature:*`, une fois le design system et les dépôts stabilisés.

## 10. Risques

| Risque | Impact | Parade |
|---|---|---|
| **Noms VF de BG3** sans source ouverte complète | Fort (principe 6) | Statuts de traduction visibles, correction en un appui dans l'appli, vocabulaire officiel D&D (SRD FR, AideDD) comme base, recoupement ; question ouverte sur l'accès PC |
| Volume du lore face à l'exigence de références | Fort | Socle/cible, pilote par thème, références « citées par une source secondaire » clairement signalées, revue indépendante par échantillon |
| Contradictions de canon (ex. datations autour de 1492 CV) | Moyen | Groupes de variantes présentés côte à côte, sans trancher |
| Pas d'émulateur (pas de KVM) ni d'appareil | Moyen | Tests JVM + Robolectric + Roborazzi ; `TEST_SUR_TELEPHONE.md` pour le démarrage à froid, le 120 Hz, l'haptique, les shaders, le lecteur de QR ; Baseline Profiles générés seulement si un appareil est branché |
| Démarrage à froid < 1 s avec une base de plusieurs Mo | Moyen | Base non compressée dans l'APK, copie différée au premier lancement, ouverture paresseuse, Baseline Profile |
| Coût GPU des shaders AGSL | Moyen | Budget strict (§ DA), un seul shader animé à l'écran, arrêt hors écran, rendu statique en mouvement réduit |
| Bibliothèque Play services qui ajouterait INTERNET | Faible | Test du manifeste fusionné ; retrait par fusion de manifeste et justification dans DECISIONS.md si besoin |
| Material 3 Expressive non stable | Faible | Design system sur mesure ; migration quand 1.5.0 sera stable |
| Évolution des politiques Larian/WotC | Faible | Mentions et contraintes centralisées dans `SOURCES.md` et l'écran Crédits |
| API bg3.wiki modifiée ou limitée | Faible | Cache des réponses, instantanés reproductibles, débit limité |

## 11. Questions ouvertes

1. **Direction artistique** : A « Grimoire enluminé », B « Atlas céleste » ou C « Vitrail et sceaux » (ou un mélange précis) ? Voir `docs/DIRECTION_ARTISTIQUE.md` et `docs/design/`.
2. **Validation du plan**, notamment des objectifs du codex (socle 220 fiches, cible 447).
3. **Noms VF** : acceptes-tu de démarrer avec des badges « VF à confirmer » sur une partie des termes du jeu, que tu corriges en jouant ? As-tu, ou auras-tu, accès à une version PC du jeu dont on pourrait extraire uniquement les noms français ?
4. **V2 — Archiviste** (à trancher avant la V2, sans effet sur la V1) : la politique de Larian interdit de « prompter » une IA générative avec sa propriété intellectuelle. Options : (a) archiviste limité au lore D&D/Royaumes (propriété de Wizards) ; (b) archiviste « extractif » qui retrouve et cite les passages de nos fiches sans générer de texte sur BG3 ; (c) renoncer. Ma recommandation : (b), éventuellement combinée à (a).

## 12. Idées supplémentaires (classées par valeur, puis effort)

| # | Idée | Valeur | Effort |
|---|---|---|---|
| 1 | **« Je suis ici »** : choisir sa zone actuelle (en un appui depuis Ma partie) affiche tout ce qui la concerne et que tu peux voir : objets manquables, livres à ramasser, quêtes, lore du lieu | Élevée | Moyen |
| 2 | **Déverrouillage fin « Déjà vu »** : marquer qu'on a rencontré un personnage, un lieu ou un livre lève son voile avant la fin de l'acte, ce qui réduit encore les spoilers à l'intérieur d'un acte | Élevée | Moyen |
| 3 | **Chemins de lecture** : parcours de lore guidés (« Les dieux morts et revenus », « Netheril en dix fiches »), avec le fil d'Ariane des fiches lues | Élevée | Moyen |
| 4 | **Bilan de session** : en quittant le mode session, résumé de ce qui a été coché, noté et lu pendant la partie | Moyenne | Faible |
| 5 | **Lecture à voix haute hors ligne** des fiches de lore, avec la synthèse vocale du système (voix françaises du Pixel, sans réseau) | Moyenne | Faible |
