# Décisions

Format : numéro, date, décision, raison. Les décisions marquées « à confirmer » attendent ton retour ou une vérification ultérieure.

## Phase 0 (27/10/2026)

**D-001 — Projet neuf.** Le dépôt ne contenait aucun fichier (aucun commit) : rien à analyser ni à reprendre.

**D-002 — SDK Android installé dans le projet.** Outils en ligne de commande 23.0, plateforme `android-37.0`, build-tools 37.0.0, platform-tools, dans `.android-sdk/` (non versionné) pour ne rien modifier hors du dossier du projet. Script de réinstallation : `tools/setup_android_sdk.sh` (le conteneur est éphémère).

**D-003 — Pas d'émulateur.** Le conteneur n'a pas `/dev/kvm` : un émulateur Android n'y tournerait pas. Vérification par tests JVM, Robolectric et captures Roborazzi ; démarrage à froid, fluidité 120 Hz, haptique, shaders, lecteur de QR et Baseline Profiles seront listés dans `docs/TEST_SUR_TELEPHONE.md` (ou testés si un téléphone est branché en adb).

**D-004 — Versions stables uniquement.** Instantané du 27/10/2026 dans `PLAN.md` §1.1. Material 3 reste en **1.4.0** : Material 3 Expressive (1.5.0) n'est qu'en bêta ; le design system sur mesure couvre l'expressivité, migration quand la 1.5.0 sera stable.

**D-005 — Navigation 3 (1.2.0, stable).** La pile de retour appartient à l'appli et se sérialise : c'est la façon la plus simple d'obtenir la « reprise exacte » demandée, et `adaptive-navigation3` prépare le liste + détail. Liens profonds gérés par un parseur maison (schéma `carnetdefaerun://`).

**D-006 — Room en Kotlin Multiplatform + SQLite embarqué.** `core:database` cible Android et JVM ; le pipeline JVM crée `content.db` avec les mêmes entités que l'appli : un seul schéma, aucun risque de divergence. `sqlite-bundled` garantit le même moteur (FTS) dans le pipeline et sur le téléphone. Coût : ~1 à 2 Mo de bibliothèque native (arm64 seulement en release).

**D-007 — Pipeline en Kotlin, lore en YAML.** Le pipeline réutilise les classes de `core:model` (une seule source de vérité). Les fiches de lore sont rédigées en YAML (une fiche par fichier, sections et paragraphes étiquetés, références par identifiant), lisibles et faciles à relire.

**D-008 — Licences des données.** Tout ce qui dérive de bg3.wiki est traité comme CC BY-NC-SA 4.0 (exigence du brief), attribué par fiche (URL + révision) et redistribué sous la même licence. Les textes de lore rédigés pour le projet sont aussi placés sous CC BY-NC-SA 4.0, par cohérence ; ils ne mélangent jamais de texte d'une autre source. *(À confirmer si tu préfères une autre licence pour tes textes.)*

**D-009 — Statuts de traduction.** `VF_BG3`, `VF_DND`, `NON_OFFICIELLE`, `CORRIGEE`, avec badges (voir `SOURCES.md` §4). Raison : aucune source ouverte ne donne la VF complète de BG3 ; il faut être honnête sur chaque nom et te laisser corriger.

**D-010 — Verrou anti-spoiler à deux axes.** Axe BG3 (acte + niveau) et axe « autres œuvres », tolérances indépendantes, révélation par bloc mémorisée et annulable (spécification dans `PLAN.md` §2). Dans la recherche, la ligne « Chercher aussi dans le contenu masqué » est toujours affichée pour ne pas trahir l'existence d'un résultat caché.

**D-011 — Navigation principale.** Cinq destinations en bas (Partie, Grimoire, Codex, Builds, Outils) et la **recherche intégrée à droite de la barre**, sous le pouce, sur tous les écrans de premier niveau ; sur les fiches, recherche dans la barre supérieure. Compagnons, Progression et Notes s'ouvrent depuis Ma partie et depuis les fiches.

**D-012 — Pas de portraits.** Les compagnons sont représentés par des emblèmes et sceaux originaux, pour ne pas reproduire l'apparence des personnages du jeu.

**D-013 — Identifiant d'application `fr.carnetdefaerun`.** Convention de domaine inversé, sans besoin de posséder le domaine pour une installation directe. Il ne changera plus après la première installation (sinon, désinstallation nécessaire).

**D-014 — Ton de l'interface.** Vouvoiement, phrases courtes, verbes à l'infinitif pour les actions (« Révéler », « Tirer le fil »).

**D-015 — Une seule permission conservée : `VIBRATE`.** Nécessaire aux compositions haptiques du lanceur de dés ; c'est une permission normale accordée à l'installation, sans rapport avec le réseau. Les permissions apportées par des bibliothèques sont retirées (voir D-021).

**D-016 — Maquettes en HTML/SVG rendues par Chromium.** Elles prouvent que chaque style se construit en vectoriel et en code. Écrans rendus à l'échelle ×2 (824 × 1836 px) pour limiter le poids du dépôt (~16 Mo pour `docs/design/`).

**D-017 — Contenus des maquettes.** Les textes de lore des maquettes (githyanki) sont exacts et référencés (vérifiés par la revue indépendante ; l'une des références est marquée « via Wikipédia » car le livre n'a pas été consulté directement). Les intitulés d'objectifs et de difficulté sont des exemples dont la VF sera validée avec les données. Les valeurs du build (points de vie, achat de points, emplacements, niveaux des dons) sont **illustratives** : elles ont été recoupées à la main avec bg3.wiki, « Hit points », révision 425339 du 13/09/2026, mais ne viennent pas encore du moteur de règles, qui les recalculera avec ses tests.

**D-018 — Version du jeu visée.** Patch 8 et tous les correctifs jusqu'à la mise à jour du 29/09/2026 (build 4.1.1.7631656, la plus récente d'après les notes de version de bg3.wiki ; le dernier correctif numéroté sur le site officiel est le #36 du 26/03/2026). Revérifié avant chaque import.

**D-019 — V2 « Archiviste » mise en garde.** La politique générale de Larian interdit de « prompter » une IA générative avec sa propriété intellectuelle. Formellement, le contenu BG3 relève des *BG3 Fan Content Terms*, qui n'ont pas cette clause, mais l'intention de Larian est claire : la conception de l'archiviste sera tranchée avec toi avant la V2 (options dans `PLAN.md` §11). Aucun effet sur la V1.

**D-020 — Contrastes mesurés automatiquement.** `tools/design/contrast_check.py` mesure chaque jeton de texte contre tous les fonds de son thème, plus le texte posé sur verre ou pastille ; résultat dans `docs/design/contrastes.md` (0 échec). Plusieurs jetons ont été corrigés après la revue indépendante (accents clairs, `muted` sur surface surélevée, verre saphir sous texte blanc).

**D-021 — Google Code Scanner sans INTERNET.** Le brief demande Google Code Scanner, mais sa dépendance `transport-backend-cct` (télémétrie ML Kit) déclare INTERNET et ACCESS_NETWORK_STATE. Choix : garder le scanner (il fonctionne via Google Play services, qui télécharge et exécute le module dans son propre processus) et **retirer ces deux permissions du manifeste fusionné** (`tools:node="remove"`), contrôlé par un test bloquant. Sans INTERNET, la télémétrie embarquée ne peut rien envoyer. À vérifier sur le téléphone : le scanner fonctionne hors permission réseau (ajouté à la future liste `TEST_SUR_TELEPHONE.md`). Repli toujours disponible : import d'une image par le sélecteur de photos (décodage ZXing) et collage du code texte.

**D-022 — Voiles de spoiler muets.** Un voile n'affiche ni le titre, ni l'acte, ni le niveau de ce qu'il cache (« Section masquée — au-delà de votre progression ») : un titre de section, ou même l'acte d'un passage caché sur un sujet donné, suffit à laisser deviner un rebondissement. Afficher l'acte reste possible par un réglage désactivé par défaut. Les liens vers une fiche masquée deviennent « une fiche masquée ».

**D-023 — Documents eux aussi sans spoiler.** Les documents du projet, les maquettes et les messages de commit que tu lis ne citent pas de noms ou d'événements qui trahissent l'intrigue au-delà du début de l'Acte I ; les listes nominatives sensibles (panthéon, personnages) vivent dans les données, étiquetées par acte. Corrections faites dans `PLAN.md` après la revue (listes du codex, idée « chemins de lecture », en-têtes d'acte : la tour a été remplacée par une ruine générique).

**D-024 — Licences Fandom confirmées.** L'API `siteinfo` du Forgotten Realms Wiki, de « Le Monde des Royaumes Oubliés » et de l'« Encyclopédie Baldur's Gate » renvoie « CC-BY-SA » (27/10/2026). Ces wikis restent des références, sans reprise de texte.

**D-025 — SRD 5.2.1 FR en vocabulaire seulement.** Le SRD impose sa propre mention et interdit toute autre attribution à Wizards, ce qui entre en conflit avec la mention de la politique de contenu de fans. On ne reproduit donc aucun texte du SRD : on n'en tire que des correspondances de termes, ce qui ne demande pas d'attribution.

## Validation de la phase 0 (27/10/2026)

**D-026 — Direction artistique : A « Grimoire enluminé ».** Choisie par l'utilisateur. `docs/DIRECTION_ARTISTIQUE.md` §2 devient la charte. Les schémas de la cosmologie et du panthéon reprennent le principe des diagrammes astronomiques des manuscrits médiévaux, **dessinés dans la gravure au trait de A** (pas la palette ni les polices de B).

**D-027 — Plan validé**, objectifs du codex compris (socle 220 fiches, cible 447).

**D-028 — Tolérance « légère »** (l'utilisateur m'a laissé trancher) : pour les actes futurs, seul le contenu étiqueté `AUCUN` est visible (existence d'un objet, d'un lieu, d'un sort), jamais l'intrigue ; pour l'acte courant, les indices `LEGER` sont visibles. Réglage par défaut de l'appli : tolérance « aucun ».

**D-029 — Noms VF** : démarrage avec les statuts et badges prévus (`SOURCES.md` §4) ; correction en un appui dans l'appli. Aucune extraction depuis les fichiers du jeu.

## Phase 1 (27/10/2026)

**D-030 — Libellés des difficultés à confirmer.** « Explorateur », « Équilibré », « Tacticien », « Honneur », « Personnalisé » sont affichés en attendant une vérification dans la VF du jeu (correction prévue dans l'appli).

**D-031 — Tests sur JVM : Robolectric en SDK 36.** Robolectric 4.17 ne gère pas encore le SDK 37 ; les tests d'interface et les captures tournent en SDK 36 (`robolectric.properties`), l'appli reste compilée et ciblée en 37. Robolectric télécharge ses images Android via le miroir de Maven Central, et la JVM de test reçoit `--add-exports java.base/jdk.internal.access` (exigence de Robolectric sous Java 21).

**D-032 — Locale UTF-8 pour Gradle.** Les noms de tests sont écrits en français, accents compris ; le conteneur doit tourner avec `LC_ALL=C.UTF-8` (consigné dans CLAUDE.md).

**D-033 — Navigation : une pile par onglet, persistée.** `NavigationState` (Kotlin pur, testé) garde une pile par onglet ; toucher l'onglet courant ramène à sa racine ; à la racine d'un onglet, « retour » ramène à « Ma partie », puis quitte. L'état est enregistré dans DataStore à chaque changement et relu avant la fin de l'écran de démarrage.

**D-034 — Préférences en un document JSON dans DataStore.** Un seul objet `UserPreferences` sérialisé : ajout de champs sans migration (valeurs par défaut), champs inconnus ignorés.

**D-035 — Mode session = écran allumé.** Le bouton « Session » de Ma partie et le réglage correspondant pilotent `keepScreenOn` sur la vue racine (aucune permission).

**D-036 — Lint sans avertissement.** Les avertissements sont corrigés plutôt qu'ignorés ; seule exception : `ChromeOsAbiSupport` (APK volontairement arm64).

**D-037 — Icône adaptative provisoire.** Médaillon d'or, plume et étoile sur fond d'encre, avec version monochrome ; elle sera retravaillée au lot « illustrations ».
