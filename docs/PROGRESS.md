# Avancement

_Dernière mise à jour : 27/10/2026, fin de la phase 0._

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

## En cours
- **En attente de ta décision** : choix de la direction artistique et validation du plan (questions ouvertes dans `PLAN.md` §11).

## Reste à faire
- Phase 1 : socle et design system de la direction choisie.
- Phases 2a/2b : pipelines de données du jeu et du lore (en parallèle).
- Phases 3 à 7 : Grimoire, Codex et recherche ; builds ; Ma partie, progression et compagnons ; outils, notes et sauvegarde ; finitions et livraison (APK signé, `INSTALLER.md`, `vitrine.png`, rapport final).
- V2 après validation de la V1.

## Problèmes connus
- **Pas d'émulateur** possible dans ce conteneur (pas de KVM) : la vérification sur appareil sera listée dans `docs/TEST_SUR_TELEPHONE.md`.
- **Noms VF de BG3** : pas de source ouverte complète ; badges et correction dans l'appli prévus.
- Les pages HTML des wikis Fandom renvoient une erreur 402 à l'outil de lecture web ; leur API répond (licences confirmées par ce biais).
- bg3.wiki n'expose pas de table Cargo pour les classes, sous-classes, races, dons, historiques, quêtes, approbations et trophées : ces catégories demanderont l'analyse du wikitexte (plus d'effort, voir les risques du plan).
- Google Code Scanner : fonctionnement sans permission réseau à confirmer sur le téléphone (D-021).
