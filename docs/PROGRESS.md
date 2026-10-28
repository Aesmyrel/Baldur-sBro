# Avancement

_Dernière mise à jour : 28/10/2026, phase 1 corrigée après vérification indépendante ; phase 2 en cours._

## Fait
- **Phase 0 — plan et direction artistique**
  - `docs/BRIEF.md` : brief copié mot pour mot.
  - `docs/SOURCES.md` : évaluation de bg3.wiki, Forgotten Realms Wiki, Wikipédia, AideDD, SRD 5.2.1 FR, wikis francophones, sources officielles ; résumé des politiques de contenu de fans de Larian (générale et BG3) et de Wizards ; stratégie des noms VF ; version du jeu visée.
  - `docs/PLAN.md` : architecture, modules, verrou anti-spoiler, schéma de données, recherche, moteur de règles, écrans, arborescence du codex (socle 220 fiches, cible 447), couverture des données de jeu, phases et critères de fin, lots parallélisables, risques, questions ouvertes, 5 idées supplémentaires.
  - `docs/DIRECTION_ARTISTIQUE.md` : trois directions (A Grimoire enluminé, B Atlas céleste, C Vitrail et sceaux) avec palettes mesurées, typographies, ornements, illustration, matière, mouvement.
  - `docs/design/` : 3 planches, un comparatif 3 × 3 et 12 écrans (3 écrans × 3 directions + fiche de lore en thème clair), générés en code depuis `docs/design/src/`.
  - `CLAUDE.md` : règles et conventions du projet.
  - Environnement : SDK Android installé (plateforme 37, build-tools 37.0.0) ; script `tools/setup_android_sdk.sh`.
  - **Vérification indépendante de la phase 0** (agent relecteur) : versions, politiques de contenu de fans, licences, dates et lore des maquettes vérifiés. Corrigé ensuite : voiles de spoiler qui laissaient deviner un rebondissement, noms trop révélateurs dans le plan, permissions apportées par Google Code Scanner (D-021), contrastes (mesure automatique, 0 échec), absence de tables Cargo pour plusieurs catégories (risque ajouté), Lae'zel au Prologue, références multiples, accueils B et C complétés (groupe actif, anecdote), licences Fandom confirmées.

- **Phase 1 — socle et design system**
  - Projet Gradle multi-modules (AGP 9.4.1, Kotlin 2.4.21, Hilt, Compose, Navigation 3), plugins de convention.
  - Verrou anti-spoiler `SpoilerGate` : table de vérité, 2 025 combinaisons testées.
  - Design system A : jetons sombre / OLED / parchemin, polices OFL, gravure procédurale, 5 en-têtes d'acte, 12 emblèmes de classe, composants signature, shaders avec repli, haptique, mouvement réduit.
  - Coquille : navigation par onglet persistée, liens profonds, barre ou rail selon la largeur, écran de démarrage, thèmes, mode session (écran allumé), réglages complets, crédits et mentions légales, icône adaptative et monochrome.
  - Qualité : `./gradlew check` vert (lint sans avertissement, tests, captures, contrôle des permissions), release R8 de 2,8 Mo sans permission réseau.
  - Vérification indépendante (technique ; art et accessibilité) : 9 points bloquants et 25 importants relevés, tous corrigés (D-038 à D-052) : voile sur toucher confirmé et lisible, étiquettes fermées par défaut, poignée opaque, navigation par onglet sans perte d'état, DataStore résistant, `check` qui compare vraiment les captures, EmojiCompat retiré, polices complétées et renommées, 200 % et largeurs, contrastes réels, OLED en noir pur. 25 captures, dont 200 %, 360 dp et pliable.

## En cours
- Contre-vérification indépendante des corrections de la phase 1.
- **Phase 2 — pipelines de données du jeu et du lore**
  - Fait : modèle de contenu ; pipeline bg3.wiki (récupération, normalisation, assemblage, validation bloquante, rapport) ; 1 776 entrées sans texte du jeu (234 sorts, 638 objets, 245 états, 494 capacités, 165 lieux) ; base embarquée générée à la compilation ; dépôt de contenu filtré par le verrou, avec tests de non-fuite sur toute la base.
  - À faire : noms français (1 558 propositions) et effets rédigés en français (lots d'écriture) ; classes, sous-classes, races, historiques, dons ; compagnons, quêtes, trophées ; pipeline du lore (format, références, fiches pilotes) ; vérification indépendante d'un échantillon.

## Reste à faire
- Phases 2a/2b : pipelines de données du jeu et du lore (en parallèle).
- Phases 3 à 7 : Grimoire, Codex et recherche ; builds ; Ma partie, progression et compagnons ; outils, notes et sauvegarde ; finitions et livraison (APK signé, `INSTALLER.md`, `vitrine.png`, rapport final).
- V2 après validation de la V1.

## Problèmes connus
- **Pas d'émulateur** possible dans ce conteneur (pas de KVM) : la vérification sur appareil sera listée dans `docs/TEST_SUR_TELEPHONE.md`.
- **Noms VF de BG3** : pas de source ouverte complète ; badges et correction dans l'appli prévus.
- Les pages HTML des wikis Fandom renvoient une erreur 402 à l'outil de lecture web ; leur API répond (licences confirmées par ce biais).
- bg3.wiki n'expose pas de table Cargo pour les classes, sous-classes, races, dons, historiques, quêtes, approbations et trophées : ces catégories demanderont l'analyse du wikitexte (plus d'effort, voir les risques du plan).
- Google Code Scanner : fonctionnement sans permission réseau à confirmer sur le téléphone (D-021).
