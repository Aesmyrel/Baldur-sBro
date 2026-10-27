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

**D-015 — Une seule permission prévue : `VIBRATE`.** Nécessaire aux compositions haptiques du lanceur de dés ; c'est une permission normale accordée à l'installation, sans rapport avec le réseau.

**D-016 — Maquettes en HTML/SVG rendues par Chromium.** Elles prouvent que chaque style se construit en vectoriel et en code. Écrans rendus à l'échelle ×2 (824 × 1836 px) pour limiter le poids du dépôt (~16 Mo pour `docs/design/`).

**D-017 — Contenus des maquettes.** Les textes de lore des maquettes (githyanki) sont exacts et référencés, mais les intitulés d'objectifs et de difficulté sont des exemples : leur VF exacte sera validée avec les données.

**D-018 — Version du jeu visée.** Patch 8 et tous les correctifs jusqu'à la mise à jour du 29/09/2026 (build 4.1.1.7631656, la plus récente d'après les notes de version de bg3.wiki ; le dernier correctif numéroté sur le site officiel est le #36 du 26/03/2026). Revérifié avant chaque import.

**D-019 — V2 « Archiviste » mise en garde.** La politique de Larian interdit de « prompter » une IA générative avec sa propriété intellectuelle : la conception de l'archiviste sera tranchée avec toi avant la V2 (options dans `PLAN.md` §11). Aucun effet sur la V1.

**D-020 — Contrastes du thème clair.** Les accents clairs des trois maquettes ont été assombris pour atteindre au moins 4,5:1 (mesures dans `DIRECTION_ARTISTIQUE.md`).
