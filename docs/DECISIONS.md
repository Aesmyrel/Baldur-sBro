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

## Phase 1 — après la vérification indépendante (28/10/2026)

Deux vérifications indépendantes (technique ; direction artistique et accessibilité) ont relevé 9 points bloquants et 25 importants. Corrections et décisions :

**D-038 — Voile : révélation sur toucher confirmé.** Le voile ne se lève qu'après un toucher relâché sans glissement (jamais au premier contact, ce qui révélait la section quand on commençait à faire défiler la page). Animation de 380 ms (charte : 150–400 ms). Le message est posé sur un cartouche opaque : sa lisibilité ne dépend plus de la brume. Pour TalkBack, un seul nœud (« Section masquée, au-delà de votre progression ») et une action « Révéler ». Tests : défilement → rien ; toucher → une révélation ; description générique ; acte affiché seulement sur demande.

**D-039 — Un seul shader animé à la fois.** Un jeton global (`AmbientAnimationSlot`, fourni par le thème) est pris par le premier voile visible à l'écran et rendu dès qu'il sort de l'écran ou a fini d'onduler (8 s). Passer en mouvement réduit ou en économiseur de batterie coupe le shader d'un voile déjà affiché.

**D-040 — Étiquettes fermées par défaut.** `SpoilerTag` n'a plus de valeurs par défaut : une étiquette absente ou mal nommée est une erreur de lecture, jamais un « visible par tous ». Les entrées et les blocs doivent porter leurs étiquettes ; les liens attendent leur calcul sous l'étiquette la plus stricte (`LOCKED`). Tests de décodage à l'appui.

**D-041 — Poignée opaque pour un bloc masqué.** Un écran reçoit `Gated.Hidden(RevealKey)` : un condensé SHA-256 de l'identifiant du bloc, jamais l'identifiant lui-même (dérivé d'un nom anglais, il pouvait trahir un titre). Les ancres de section des liens profonds sont des numéros d'ordre (`#s3`), jamais des titres.

**D-042 — Épilogue et tolérance « aucun ».** Un bloc `EPILOGUE` / `MAJEUR` ne devient jamais visible par la seule progression, puisqu'aucun acte ne suit l'Épilogue. C'est voulu : le joueur le révèle individuellement ou relève sa tolérance.

**D-043 — Navigation : entrées décorées par onglet.** Chaque onglet a sa propre liste d'entrées décorées (état sauvegardable, ViewModels) : changer d'onglet ne détruit plus rien. La clé d'état est préfixée par l'onglet. L'état relu est normalisé (racine en tête, destinations mal formées écartées, profondeur ≤ 30) et les liens profonds sont bornés (ancre `s{n}`, recherche ≤ 200 caractères, identifiant de build > 0). Un lien reçu avant la relecture est enregistré aussitôt. La position de lecture d'une fiche sera persistée par le dépôt de lecture (phase 3).

**D-044 — DataStore résistant à la corruption.** Fichier corrompu remplacé par un fichier vide (`ReplaceFileCorruptionHandler`), erreurs d'entrée/sortie absorbées : l'appli repart des valeurs par défaut au lieu de planter à chaque démarrage. Tests sur fichiers corrompus.

**D-045 — `check` compare vraiment les captures.** `check` dépend de `verifyRoborazziDebug`, et les captures de référence sont des entrées des tests (une référence modifiée relance la comparaison ; vérifié avec une capture falsifiée). Lint : `warningsAsErrors = true`. Le contrôle des permissions lit aussi `uses-permission-sdk-23` et `uses-permission-sdk-m` et a un test négatif (INTERNET refusé sous chacune des balises). Wrapper Gradle avec somme de contrôle.

**D-046 — EmojiCompat retiré.** Son initialiseur interroge le fournisseur de polices téléchargeables des services Google au démarrage : accès réseau par un tiers. Il est retiré du manifeste fusionné (`tools:node="remove"`) ; les émojis viennent de la police du système.

**D-047 — Sauvegarde.** Sauvegarde dans le nuage seulement si elle est chiffrée (`disableIfNoEncryptionCapabilities`) ; `user.db` et son journal WAL inclus, le fichier `-shm` exclu ; règles `fullBackupContent` supprimées (sans effet en minSdk 33).

**D-048 — Polices : typographie française et licence.** Les polices d'origine n'avaient pas l'espace fine insécable (U+202F) : le texte passait par une police de repli. Le script d'import l'ajoute (glyphe de l'espace fine U+2009, ou glyphe vide de 0,16 cadratin), ainsi que l'espace insécable quand elle manque. Almendra déclare un nom réservé (OFL §3) : nos versions sous-ensemblées sont renommées « Carnet Titres » et « Carnet Titres SC », et l'écran Crédits l'explique. Test : chaque police dessine U+202F, U+00A0, ’, « », œ, É, et aucune ne porte le nom réservé.

**D-049 — Apostrophe typographique.** Toutes les chaînes et les noms de contenu emploient ’ ; le contrôle typographique la corrige, couvre aussi les tableaux et pluriels, et ses corrections sont idempotentes.

**D-050 — 200 % et largeurs.** Plus aucune hauteur fixe sur un élément qui porte du texte ; le choix segmenté passe en liste verticale quand un libellé ne tient pas sur une ligne (jamais de mot coupé) et marque le choix par un filet, pas seulement par la couleur ; étiquettes et badges passent à la ligne ; libellés de navigation et chiffres d'acte plafonnés à 1,3–1,4 fois (doublés d'un libellé accessible) ; colonne de lecture plafonnée à 32 cadratins de la taille de lecture. L'en-tête gravé se met à l'échelle « couvrante » à toute largeur, s'efface vers la page (aucune couture), laisse la barre d'état libre, et son titre tient sur une ligne (taille ajustée) ; à très grande police, la pastille Session passe au-dessus du titre.

**D-051 — Contrastes réels.** Le grain du vélin est posé en incrustation (sa teinte moyenne ne change pas le fond, donc ni les contrastes mesurés), plus léger ; OLED en noir pur (ni halo ni grain) ; texte sur feuille d'or : variante sans bande sombre et encre `#1A1206` (≥ 4,5:1 partout) ; en parchemin, les ornements gravés utilisent un or sombre (l'or clair y disparaissait) ; pastille Session opaque ; coche du sceau lisible sur la cire (≥ 3:1).

**D-052 — Écran de démarrage selon le thème.** L'appli déclare au système le mode clair (parchemin) ou sombre choisi (`setApplicationNightMode`) : l'écran de démarrage est en vélin ou en encre (`values-notnight`), sans éclair sombre en parchemin. Conséquence assumée : changer de thème (parchemin ↔ sombre) change la configuration `uiMode` et recrée l'activité, comme le fait le système ; l'état est restauré (pile persistée, état sauvegardable). À vérifier sur le téléphone (`docs/TEST_SUR_TELEPHONE.md`).

**D-053 — Reste à faire, planifié.** Gravure plus riche des emblèmes et du reste des illustrations (lot « I ») ; composants Références, appels de note, feuille « Détail du calcul », ligne de source (phases 3–4, avec les écrans qui les emploient) ; accueil complet (objectifs, alertes, anecdote : phase 5, quand les données existent) ; reflet d'or et transitions « encre » (phase 7).

**D-063 — Contre-vérification de la phase 1 : corrections.**
- Polices : les notices d'origine (copyright, licence et son adresse) ne sont plus touchées par le renommage ; espace fine insécable dédiée de 55 % de l'espace-mot (l'espace fine des graisses grasses était plus large qu'une espace-mot) ; points de suspension ajoutés là où ils manquaient. Un test le vérifie.
- En-tête de section : l'action ne se coupe jamais ; elle passe sous le filet quand le libellé perdrait plus de la moitié de la largeur (revu par D-070).
- Contrastes : lavis d'or d'une option choisie à 8 % en parchemin (18 % en sombre) ; or profond du parchemin à `#96732C` (3,5:1 pour les contours). Tous les jetons sont désormais mesurés par un test (texte ≥ 4,5:1 sur les quatre fonds de chaque thème, contours ≥ 3:1).
- En-tête d'acte : halo de page serré autour du libellé et du titre (aucun astre ni hachure sous les lettres, à toutes les tailles) ; la pastille passe au-dessus du titre dès qu'elle prendrait près de la moitié de la largeur ; la gravure est rognée sur son cadre.
- Divers : fin de l'animation du voile même si la révélation reste sans effet ; plus de fabrique d'étiquette à valeurs ouvertes ; lecture des préférences relancée après une erreur ; accueil sans état provisoire ; hauteurs minimales pour la barre du haut et le rail ; libellés de la barre plafonnés à 125 % ; tuiles de statistiques de même hauteur ; valeur du curseur affichée ; relance depuis les tâches récentes sans rejouer le lien profond ; test de conservation de l'état par onglet.

**D-064 — Versions revues à la main, pas par lint.** Les contrôles de lint qui interrogent les dépôts en ligne (`AndroidGradlePluginVersion`, `GradleDependency`, `NewerVersionAvailable`) sont désactivés : ils rendaient `check` dépendant de la date du jour (AGP 9.5.0, publié le 28/10/2026, faisait échouer une compilation inchangée). Les versions sont revues en fin de phase, et une version n'est retenue que stable et publiée depuis au moins deux semaines. AGP reste en 9.4.1 pour l'instant.

## Phase 2 — données de jeu (28/10/2026)

**D-054 — Lecture du wikitexte, pas de Cargo.** L'API de bg3.wiki refuse les requêtes Cargo arbitraires (« permissiondenied ») ; le pipeline lit donc le wikitexte des pages par l'API standard (lots de 50, une requête par seconde, `maxlag`, User-Agent identifié, sans adresse personnelle), recense les pages par modèle d'inclusion (`Feature page`, `Passive feature page`, `Equipment page`, `WeaponPage`, `Condition page`, `Location page`) et ne rafraîchit que les pages dont la révision a changé. Le cache (`tools/cache/`) n'est pas versionné : il contient les descriptions du jeu citées par le wiki.

**D-055 — Aucun texte du jeu dans les données.** Les paramètres de description, de citation, de notes et d'emplacement en prose ne sont jamais importés. Seuls des faits structurés le sont (niveau, école, coûts, dés, portée, zone, sauvegarde, rareté, poids, prix, effets reconnus, lieu et acte d'obtention, méthode, nom du marchand) ; tout le reste est rédigé en français par le projet (lots d'écriture), dans un style télégraphique et factuel. Une ligne d'effet non reconnue reste vide, signalée (`unknown`), et part dans la liste de travail.

**D-056 — Vocabulaire et alias de bg3.wiki.** Les ressources d'action viennent de la table d'alias `Module:Resource/data` (révision 429231) ; les types d'objets et d'armes, des types employés par le wiki. Une valeur non reconnue n'est jamais devinée.

**D-057 — Étiquettes de spoiler des données de jeu.** Sorts de classe ou de race : visibles dès la création du personnage (`prologue`, `aucun`). Objet : acte de sa source la plus précoce (lieu → acte de la page du lieu) ; sans lieu connu, Acte III par prudence, signalé. Source d'obtention : le lieu et la méthode (coffre, sol) sont descriptifs ; qui vend, porte ou récompense est au moins `léger`. Objets d'histoire : nom au moins `léger`. Capacités et états : l'étiquette de l'entrée la plus précoce qui y mène (jamais moins stricte qu'elle). Lien : la plus stricte de ses deux extrémités. Tant qu'elle n'est pas calculée, une étiquette vaut `LOCKED` et la validation refuse l'entrée.

**D-058 — Noms français.** Ordre : nom choisi à la main (`data/i18n/noms.fr.yaml`), puis lexique officiel de D&D 5e (listes d'AideDD, sorts et objets magiques, vocabulaire seulement), puis nom anglais marqué « traduction non officielle » et ajouté à la liste de travail. Un lexique ne sert qu'à sa sorte d'entrée (un nom de sort ne nomme jamais un objet). La comparaison ramène l'orthographe britannique de BG3 à l'américaine des livres (« Armour » / « Armor »), des deux côtés.

**D-059 — Format JSON.** `data/game/*.json` est exactement le type `Entry` de `core:model` (discriminant `@type`), un fichier par type, trié par identifiant ; relu en mode strict par la validation. `data/game/RAPPORT.md` donne la couverture et les champs inconnus.

**D-060 — Base de contenu en SQLite simple, schéma partagé (révise D-006).** La base de contenu est en lecture seule et n'a que quelques requêtes : plutôt que Room (dont l'empreinte de schéma lierait le pipeline à la compilation de l'appli), son schéma est écrit une fois dans `core:model` (`ContentSchema`, versionné), créé par le pipeline avec JDBC et lu par l'appli avec le SQLite d'Android. Room reste prévu pour la base du joueur (`user.db`, migrations). `content.db` est générée à chaque compilation par la tâche `buildContentDb` de `core:database`, à partir des données validées (jamais versionnée), compressée dans l'APK, copiée une fois par version de l'appli dans le stockage privé non sauvegardé, puis ouverte en lecture seule. Tests : la base relue avec le SQLite d'Android redonne exactement les fiches validées.

**D-061 — Le dépôt de contenu, seule porte des écrans.** `ContentRepository` (constructeur interne à `core:data`, les écrans ne voient pas la base) applique `SpoilerGate` à chaque fiche, nom, source d'obtention, bloc et lien. Une liste n'énumère que les noms visibles : ni le nombre ni la place alphabétique des entrées masquées ne sont donnés, seulement un indicateur pour afficher un voile générique. Une fiche retire les sources masquées (un seul voile, sans compte), les effets qui renvoient à une entrée masquée, et tout identifiant d'entrée masquée de ses faits. Tests de non-fuite sur toute la base et 18 profils de joueur (PLAN §2.5).

**D-062 — Lots d'écriture : noms et effets rédigés.** Les noms français sans source officielle et les effets que les données structurées n'expriment pas sont rédigés par lots (guide : `docs/LOTS_ECRITURE.md`), un fichier par lot (`data/i18n/noms/`, `data/effets/`), relus par le pipeline. Effets : style télégraphique et factuel, déclencheur puis effet, chiffres tirés des faits structurés, vocabulaire officiel de D&D ; jamais une traduction phrase à phrase du texte du jeu ou du wiki, jamais d'ambiance ni d'anecdote. `VF_DND` exige que le terme figure dans le lexique d'AideDD ou dans le SRD 5.2.1 français ; `VF_BG3` est réservé à ce qui a été vu dans le jeu. Un nom ou un effet révélateur porte une étiquette plus stricte, appliquée par le pipeline avant le calcul des liens. Chaque effet note la révision de la page consultée : une page modifiée depuis est signalée. Un échantillon de chaque lot est contrôlé par un vérificateur indépendant.

**D-065 — Noms français des données de jeu : règles retenues.** 1 580 noms rédigés dans `data/i18n/noms/` (objets, capacités, sorts, états, lieux), qui complètent ceux du lexique officiel pour les 1 776 entrées ; relus sur échantillon (D-069).
- `VF_DND` seulement quand le nom anglais est exactement un terme du lexique AideDD ou du SRD 5.2.1 FR (335 noms) ; un nom composé de termes officiels (« Hache à deux mains +1 », « Amulette de foulée brumeuse ») reste `NON_OFFICIELLE`, puisque ce nom-là n'est publié nulle part.
- Les noms propres sans traduction connue gardent leur forme d'origine.
- Un nom qui laisse deviner une intrigue reçoit une étiquette de titre plus stricte (11 noms) ; l'étiquette de la fiche suit.
- La validation refuse un nom officiel sans source et une source de nom inconnue.
- Aucun nom n'est en `VF_BG3` : la VF du jeu reste à relever sur la console, et les corrections du joueur, stockées localement, priment.

**D-066 — Classes : lues dans le wikitexte, recoupées.** bg3.wiki n'a ni table Cargo ni module de données pour les classes : le pipeline lit les 12 pages de classe et la page `Classes` (maîtrises de multiclasse) avec un lecteur générique de tableaux wiki (fusions de cellules dépliées).
- Faits retenus : points de vie (niveau 1, puis par niveau), jets de sauvegarde, caractéristique d'incantation (à défaut, celle que les « caractéristiques clés » réservent aux sorts), maîtrises de départ et de multiclasse, niveau de la sous-classe, magie de pacte, et pour chaque niveau : bonus de maîtrise, capacités, don, sorts mineurs et sorts connus, emplacements, ressources, colonnes propres (dégâts de rage, attaque sournoise…).
- Recoupement : les emplacements annoncés dans la section de chaque niveau doivent égaler ceux du tableau, sinon le champ est signalé. Le rôdeur a 4 emplacements de niveau 3 au niveau 12 : la page le confirme comme un écart voulu par BG3.
- Les capacités renvoient à leur fiche quand elle existe (75) ; les 12 capacités de choix sans fiche (style de combat, manifestations occultes…) portent un nom résolu comme les autres.
- Noms des classes et de 7 capacités de choix : termes du SRD 5.2.1 FR ; les 5 autres sont proposés et signalés.
- Un instantané de référence (`ClassDataTest`) fige les valeurs vérifiées à la main sur les pages, avec leur révision.

**D-067 — Sous-classes : lues dans le wikitexte, choix compris.** Les 58 sous-classes (galerie de la page `Classes`) sont lues section par section (« Level N »), par blocs : définition, liste, tableau ou boîte d'options.
- Faits retenus : classe parente, maîtrises ajoutées, capacités par niveau, sorts accordés (toujours préparés : domaine, serment, cercle ; ou ajoutés à la liste : liste étendue de l'occultiste), ressources, emplacements des tiers de lanceur, et **choix** (nombre à prendre, options, et ce que chaque option accorde : sorts d'un terrain, actions d'un cœur bestial).
- Les pages incluses (`{{:Manoeuvres}}`, ascendance draconique) sont récupérées et lues ; une table de tirage (magie sauvage) n'est pas un choix.
- Les deux tiers de lanceur divergent au niveau 10 (l'escroc annonce 6 emplacements de niveau 1, le chevalier 3 de niveau 2) : faute de pouvoir trancher, les emplacements de ce niveau restent vides et signalés pour les deux, et sont à vérifier en jeu.
- Deux sous-classes de même nom (« Wild Magic ») ont un identifiant suffixé par la classe ; les autres gardent le nom de leur page sans qualificatif.
- Les noms français des sous-classes et des nouvelles capacités viendront de la seconde vague d'écriture ; un instantané de référence (`SubclassDataTest`) fige les valeurs vérifiées sur les pages.

**D-068 — Mécanique des capacités-actions.** Les pages d'action utilisent le même modèle que les sorts : portée, zone, jet d'attaque, sauvegarde et son effet, dégâts et durée y sont désormais lus par le même code (`Mechanics`) et portés par la fiche de capacité, au lieu d'être laissés aux effets rédigés. Des dés « du type de l'arme » ont un type vide. Les formules qui ne sont pas des dés (bonus de maîtrise, dé de supériorité, niveau du personnage, mains nues) restent signalées comme inconnues : le texte rédigé les décrit.

**D-069 — Relecture indépendante des effets et des noms : corrections avant intégration.** Échantillon de 80 effets et 60 noms comparé aux pages : 87,5 % d'effets exacts et 90 % de noms corrects, aucun faux ; typographie, révisions et étiquettes sans défaut sur les 773 effets.
- À corriger : 10 effets approximatifs, les actions sans leurs chiffres, des termes anglais dans 10 états, 18 couples de noms divergents pour une même chose, 12 statuts officiels contraires à D-065.
- Deux règles ajoutées à `docs/LOTS_ECRITURE.md` §4 : comportement constaté plutôt qu'infobulle contredite ; notes mécaniques admises depuis les sections « notes » et « bugs », reformulées. La mécanique des actions est désormais portée par les fiches (D-068).

**D-070 — Seconde contre-vérification de la phase 1 : derniers correctifs.** Aucun point bloquant ; trois points importants corrigés.
- **Test des polices** : les espaces insécables du test de couverture sont écrites en échappements (`\u202F`, `\u00A0`), qu'aucun éditeur ne normalise.
- **Halo de l'en-tête** : rectangle fondu qui épouse le bloc de texte (et non plus une ellipse), cadre gravé redessiné au-dessus du halo. Opacité 0,78 en sombre et en OLED : c'est le minimum qui garde 4,5:1 pour l'or du libellé sur la feuille d'or la plus claire, au pire cas. Elle reste 0,82 en parchemin, d'après la mesure sur les pixels. La pastille active dit « Session active » et reste à droite du titre à 412 dp.
- **En-tête de section** : largeur naturelle du libellé mesurée ; action sur la ligne seulement si tout y tient ; filet seulement si le libellé tient sur une ligne avec lui ; interlettrage resserré plutôt qu'un mot coupé. Un test vérifie qu'aucun mot n'est coupé à 320 et 360 dp, de 100 à 200 %.
- **Mineurs corrigés** : avertissements Kotlin, règle de test dépréciée, largeur non bornée, action trop longue terminée par « … ». Le contrôle typographique couvre « % » et le point médian (insécable avant « · »).
- **Consignés, pour plus tard** :
  - tests du réabonnement des préférences et du lien non rejoué depuis l'historique ;
  - notice de Carnet Titres SC reprise telle quelle dans les crédits ;
  - « Grimo… » dans la barre de navigation à 320 dp et 200 %, cas limite ;
  - espace fine d'Alegreya Sans à 0,55 de l'espace-mot.

**D-071 — Races, historiques et dons ; seconde vague de noms.**
- **Origines** : lues dans les pages de synthèse `Races` (grand tableau : vitesse, maîtrises et capacités par race et sous-race), `Backgrounds` (deux compétences par historique, la liste des personnages n'est pas lue) et `Feats` (modèles `table feat` : capacités, et augmentation de caractéristique lue dans la phrase qui la décrit). Résultat : 11 races, 28 sous-races, 12 historiques et 41 dons, figés par un instantané (`OriginDataTest`).
- **Sorts raciaux** : les pages de sorts citent une sous-race comme une race (« High Elf ») ; la source est rattachée à la sous-race. Une source inconnue dont la précision nomme une sous-classe (« via Necromancy School ») est rattachée à celle-ci. La validation refuse désormais toute source d'apprentissage absente de la base.
- **Noms, seconde vague** : 627 noms (capacités, états, sous-classes, noms des choix).
- **« Même nom anglais, même nom français »** : la règle est désormais un contrôle bloquant de la validation, sans aucune exception à ce jour.
- **Éditions de la VF de D&D** : à défaut de la VF de BG3, les termes officiels viennent du SRD 5.2.1 (règles de 2024) ou du lexique AideDD. Certains diffèrent de la VF de 2014, que le jeu a pu suivre (« Témérité » ou « Attaque téméraire »). Ils gardent le statut `VF_DND`, affiché « VF à confirmer » pour un terme du jeu, et la correction du joueur prime.

**D-072 — Phase 1 validée.** La seconde contre-vérification valide la phase 1 sous une condition, levée ici.
- **I4, couture sous l'en-tête d'acte** : le halo ne peint plus la couleur de fond par-dessus la page. Il **efface la gravure** sous le texte (mode DstOut, dans la couche de la vignette), si bien que c'est la page elle-même qu'on voit sous les lettres. Mesure au bas de l'en-tête : 2 à 3 niveaux d'écart entre l'intérieur du cadre et la page, contre 16 auparavant.
- La mesure de l'en-tête de section suit aussi la taille de police (« Personnalisation » ajoutée au test, à 320 dp).
- **Reste consigné** :
  - « Ma » seul en tête de ligne à 320 dp et 200 % ;
  - les points ouverts de D-070.

**D-073 — Base du joueur et porte du contenu (phase 3a).**
- **Base `user.db`** (Room, schéma exporté) : trois tables, `revealed` (ce que le joueur a dévoilé), `name_correction` (ses noms corrigés) et `favorite`. Elle est séparée de la base de contenu, régénérée à chaque version : rien de ce que le joueur a saisi ne se perd à une mise à jour.
- **Révélations stockées par jeton** : la base ne garde que le jeton de la clé de révélation (12 premiers octets du SHA-256), jamais l'identifiant du bloc ni son titre. Le dépôt compare par jeton. La base ne contient ainsi aucun titre ni identifiant en clair, et l'écran ne manipule que des jetons.
- **Porte du contenu** (`ContentGateway`) : les écrans ne lisent le contenu que par elle. Elle combine en continu le profil de spoilers, le réglage « indiquer l'acte », les révélations et les corrections. Un changement de réglage se propage aussitôt à toutes les listes et fiches ouvertes. Les listes sont triées sur le nom replié (accents et casse ignorés).
- **Nom corrigé** : il prime partout (fiche, liste, liens) avec le statut « corrigé par vous », sans source. Une correction vide la supprime et rend le nom d'origine.
- Tests : dévoiler un bloc ne montre que ce bloc et ne stocke que son jeton ; une correction prime partout ; la liste suit l'acte ; les favoris s'ajoutent et se retirent.

**D-074 — Recherche globale (phase 3b, socle).**
- **Index des noms** (`core:model`, Kotlin pur) : un dictionnaire des mots des noms français, anglais et alias, chaque mot relié aux noms qui le contiennent. Chaque mot cherché est comparé par préfixe, puis par une distance de Damerau-Levenshtein bornée mesurée sur le début du mot : 1 faute jusqu'à 5 lettres, 2 au-delà, aucune sous 3 lettres (sinon une seule lettre trouverait tout). Les trigrammes prévus par le plan sont écartés : une transposition dans un mot court ne laisse aucun trigramme commun (« bolue » et « boule »).
- **Classement** : nom exact, début du nom, mots du nom dans n'importe quel ordre, puis fautes de frappe, puis texte intégral. À égalité : favoris, entrées de l'acte courant, nom français avant anglais, nom le plus court.
- **Texte intégral** : table FTS4 de la base, accents ignorés, à partir de 3 lettres, un passage par entrée et après les noms.
- **Verrou** : chaque passage est filtré sur le titre, la fiche et son bloc. Les résultats masqués ne sont ni donnés ni comptés. Sur demande (« chercher aussi dans le contenu masqué »), ils deviennent des voiles opaques, sans titre ni acte, que la révélation lève. Un nom corrigé par le joueur se cherche comme un nom français et reste soumis au verrou de sa fiche.
- **Base de contenu en version 3** : la source du nom français y est une colonne. Les listes et la recherche lisent des en-têtes sans décoder les fiches.
- **Mesures** : la recherche prend 2 à 8 ms (médiane, JVM, base complète de 2 552 entrées), pour un seuil de test de 50 ms. L'empreinte d'une clé de révélation n'est calculée que pour un contenu verrouillé.
- **Tests de fuite** :
  - chaque nom verrouillé, français et anglais, tapé tel quel, pour les trois profils de référence ;
  - chaque résultat de 36 recherches, pour les 18 profils ;
  - le plein texte sur la vraie base SQLite.
- **Un nom pour deux entrées** : « Amélioration de caractéristique » nomme le don et le sort. Le SRD 5.2.1 et le lexique AideDD l'emploient chacun ; la recherche les distingue par leur type.

**D-075 — Seconde vague d'effets et noms des origines.**
- **Effets rédigés** : 505 capacités et 114 états apportés par les classes, sous-classes et origines. Au total, 1 392 entrées portent désormais des effets rédigés. Aucune n'a été laissée de côté : quand une page se contredit ou ne chiffre pas un point, ce point est omis plutôt que deviné (formes sauvages, dés à haut niveau, portées sans mètres), et la section « bugs » fait foi quand elle décrit le comportement constaté (§4).
- **Une seule étiquette** : un bloc lié à l'intrigue de l'Acte II est étiqueté `LEGER` et reste vague.
- **Noms, troisième vague** : 176 noms (races, sous-races, historiques, dons, 80 capacités, 4 états), dont 34 `VF_DND` relevés dans le SRD 5.2.1 FR. Les 2 552 entrées ont désormais un nom français. Les noms anglais qui diffèrent du SRD restent `NON_OFFICIELLE` : « Half-Elf », « Half-Orc », « Gnomish Cunning », « Ability Improvement », « Magic Initiate: … ».
- **Formes à confirmer en jeu** (consignées dans `docs/TEST_SUR_TELEPHONE.md`) :
  - « Demi-elfe haut-elfe » ;
  - « Nain d'or » et « Nain d'écu » ;
  - « Halfelin pied-léger » et « Halfelin cœur-vaillant » ;
  - « Hanté », « Sauvageon » ;
  - les dons sans source (Charge, Endurant, Interprète, Ritualiste, Tireur d'élite magique, Maître des armes lourdes).
- **Statut `VF_DND` mince** : « Drow » (un lignage elfique dans le SRD) et « Duergar » (cité dans une description d'objet) gardent ce statut, car le terme anglais et son sens sont les mêmes.
- **Noms anglais retirés des textes** : les effets qui citaient un don par son nom anglais emploient désormais son nom français.

**D-076 — Écrans du Grimoire et de la recherche ; lieux d'obtention verrouillés comme leur provenance.**
- **Écrans** :
  - accueil du Grimoire (catégories groupées, nombre de fiches visibles) ;
  - liste d'une catégorie (filtre par nom français ou anglais, étiquettes d'école, de catégorie d'objet ou de nature, sous-classes et sous-races groupées par parent) ;
  - fiche (faits en bref, effets rédigés et structurés, voiles, provenances, liens, progression des classes, attribution et licence, favori, correction du nom) ;
  - recherche globale (résultats groupés par type, ligne fixe « chercher aussi dans le contenu masqué », voiles).
  Chaque écran ne lit que la porte du contenu ; l'état de liste est une fonction pure testée.
- **Vocabulaire de règles** (`core:ui`) : caractéristiques, compétences, dégâts, écoles, raretés, ressources, portées et zones. Les termes viennent du SRD 5.2.1 FR quand le terme anglais est le même : « Intuition », « Invocation » (Conjuration), raretés « Courante », « Peu courante ». Sinon, ce sont des propositions alignées sur les noms des fiches : « Point de ki », « Dé de supériorité ». Chaque `when` est exhaustif : une valeur nouvelle du modèle sans libellé ne compile pas.
- **Fuite corrigée** : la fiche d'un objet montrait le lieu d'une provenance voilée par le lien « Se trouve dans », dans 364 objets. Le lien vers un lieu d'obtention prend désormais au moins l'étiquette de la moins stricte des provenances qui y mènent. Un contrôle bloquant de la validation et un test de fuite le garantissent.
- **Classe sans sorts** : sa caractéristique d'incantation s'affiche comme « caractéristique des objets et parchemins ».
- **Ligne de fait** : la valeur passe sous le libellé quand les deux ne tiennent pas sur une ligne (grande police, valeur longue).
- **Vérification indépendante de la seconde vague** : 40 effets sur 40 exacts, 27 noms sur 27 corrects, aucune fuite. Trois corrections appliquées : « Invocation », « Intuition », une note reformulée. Deux paires de capacités distinctes au même nom français sont renommées : « Coup critique amélioré » (Champion), « Critique discret ».
- **Reste consigné** :
  - 31 groupes de fiches d'un même type au même nom (variantes que le jeu nomme pareil : souffle en cône ou en ligne, objets selon la rareté) : une précision par variante est à ajouter aux fichiers de noms ;
  - les pages secondaires citées par certains effets sont à ajouter à l'attribution des fiches (CC BY-NC-SA) ;
  - les sorts n'ont pas encore d'effets rédigés (leurs faits structurés sont affichés).

**D-077 — Précision des variantes au même nom.**
- **Le constat** : le jeu donne parfois un même nom à des variantes distinctes, par exemple un souffle en cône et un en ligne, un objet en deux raretés, ou un passif d'objet en « +1 » et « +2 ». Le nom français suit le jeu (même nom anglais, même nom français), mais deux lignes identiques dans une liste ne se distinguent pas.
- **La règle** : le nom porte désormais une **précision** facultative (`precision` dans les fichiers de noms), affichée à côté du nom et jamais dedans : « Souffle enflammé · cône ». Elle est tirée de la parenthèse du titre de la page source, traduite.
- **Contrôle bloquant** : deux entrées d'un même type au même nom français doivent avoir des précisions distinctes. Le contrôle a trouvé un groupe oublié lors de sa mise en place. 56 précisions couvrent les 32 groupes.
- **Base de contenu en version 4** : la précision y est une colonne. Une précision qui nomme un objet ou un sort suit l'étiquette du nom de sa fiche.

**D-078 — Attribution des pages qui ont servi aux effets.**
- La vérification de la seconde vague a montré que 7 effets sur 40 tirent un point de la page d'un état appliqué ou d'une capacité accordée, absente de l'attribution de la fiche. Or la licence CC BY-NC-SA demande de citer toute page dont une donnée dérive.
- La fiche attribue donc aussi la page de chaque état qu'elle applique et de chaque capacité qu'elle accorde. Le dépôt n'ajoute que celles des fiches liées visibles : le titre anglais d'une page ne doit jamais trahir une fiche masquée. Un test de fuite le vérifie sur toute la base.

**D-079 — Codex : douze fiches pilotes, codex exporté, deux règles de format.**
- **Pilotes** : une fiche par thème, de 544 à 889 mots, toutes référencées. Elles comptent 145 références, dont 61 pages de wiki consultées directement avec leur révision ; les livres sont cités en consultation secondaire avec leur `via`. Les versions contradictoires sont présentées en variantes dans neuf fiches. La validation passe sans problème.
- **Choix des sujets** : rencontrés au tout début du jeu, ou généraux dans D&D, sans lien avec un rebondissement. Les sections qui gâchent une autre œuvre portent `otherWorks` (`leger` ou `majeur`).
- **Nature `hors_univers`** : une date de sortie, un éditeur ou un chiffre de ventes n'est pas du canon de D&D. Les paragraphes concernés des fiches saga et « pour aller plus loin » sont reclassés, et l'appli les affichera avec le badge neutre.
- **Source `wikipedia-fr`** : elle est admise pour le seul titre VF d'une œuvre publiée (fiches saga et « pour aller plus loin »), comme le prévoit déjà le guide. La validation la refuse ailleurs. Les noms des Royaumes attestés seulement par un wiki (« Côte des Épées », « Averne », « Ménestrels ») restent en traduction non officielle, à confirmer.
- **Codex exporté** : `normalize` écrit le codex assemblé (relations étiquetées) dans `data/lore/codex.json`. Les tests de fuite et le test de la base le relisent avec les données de jeu, et vérifient les sections et les références. La validation refuse un export qui ne correspond plus aux fichiers de `content/`. La construction de la base suit désormais aussi `content/`.
- **Relecture indépendante des pilotes** : à faire avant les lots suivants (format, ton, références, étiquettes, exactitude d'un échantillon d'affirmations).

**D-080 — Relecture des pilotes du codex : réécriture avant les lots.**
- **Ce qui est acquis** : 36 affirmations tirées au hasard, 34 exactes et 2 approximatives, aucune fausse. Aucune phrase recopiée, aucune fuite de spoiler. Noms et typographie de bon niveau.
- **Ce qui bloque (droits)** : environ la moitié des paragraphes suivent phrase à phrase une seule section d'une page, dans le même ordre. C'est une paraphrase serrée, que la règle 5 interdit. Trois fiches mêlent en plus un dérivé du Forgotten Realms Wiki (CC BY-SA) et un dérivé de la prose de bg3.wiki (CC BY-NC-SA). Un paragraphe résume chant par chant un livre du jeu.
- **Décision** : les douze pilotes sont réécrits avant tout nouveau lot, puis relus de nouveau. `docs/LORE_FORMAT.md` gagne une méthode de rédaction (§1 bis) :
  - notes d'abord, page fermée ;
  - deux sources ou sections croisées par paragraphe ;
  - section `bg3` écrite d'après les faits structurés, sans soluce ;
  - pas de commentaire sur les sources ;
  - registre normalisé.
- **Contrôle ajouté** : les codes de licence des références sont normalisés (`cc-by-sa-3.0`, `cc-by-sa-4.0`, `cc-by-nc-sa-4.0`) et vérifiés par la validation ; une page de wiki doit avoir sa licence.

**D-081 — Écrans du Codex.**
- **Accueil du Codex** : les douze thèmes en deux groupes (« Le monde », « Histoires et œuvres »), avec le nombre de fiches visibles. Puis la **liste d'un thème**.
- **Fiche de lore**, au plus près de la maquette de la direction A :
  - titre, nom anglais et accroche ;
  - badges des natures présentes (Canon BG3, Canon D&D, Notre lecture, Hors univers) et temps de lecture ;
  - lettrine enluminée au premier paragraphe ;
  - nature et édition sous le titre d'une section ;
  - appels de référence en exposant, numérotés dans l'ordre du premier appel ;
  - **versions divergentes** dans un même cadre, chacune sous son édition ;
  - section masquée sous le voile ;
  - références en pied de fiche (pages et chapitres cités, source qui cite le livre), puis relations (« Pour tirer le fil ») ;
  - filet de progression de lecture et ancre de section (`#s3`) pour les liens profonds.
- **Registre exporté** : `normalize` écrit aussi `data/lore/references.json`, que les tests et les captures relisent. Le test de fuite vérifie désormais pour de bon que seules les références des sections visibles sont données.

**D-082 — Codex : rédaction en salle blanche.**
- **Seconde relecture des pilotes réécrits** :
  - 30 nouvelles affirmations : 29 exactes, 1 approximative, aucune fausse ; aucune fuite ;
  - mais 29 paragraphes sur 85 restent en paraphrase serrée, et six fiches mêlent encore des dérivés du Forgotten Realms Wiki (CC BY-SA) et de bg3.wiki (CC BY-NC-SA).
  La réécriture a changé les mots, rarement le plan.
- **Cause** : un rédacteur qui lit la source avant d'écrire en suit l'ordre et les images, même avec la consigne de s'en écarter.
- **Décision** : les fiches s'écrivent désormais en deux temps, par deux rédacteurs distincts (`docs/LORE_FORMAT.md` §1 ter) :
  - un extracteur relève des faits atomiques, télégraphiques et rangés par ordre alphabétique ;
  - un rédacteur qui n'a vu ni les sources ni l'ancienne fiche écrit à partir de ces seules notes et des données de `data/game`.
  Comme le texte final ne dérive d'aucune expression source, le mélange de licences disparaît aussi.
- **Pilotes** : onze fiches sont réécrites de cette façon. La fiche du *Guide des aventuriers de la Côte des Épées*, conforme à tous les critères, est gardée. Aucun nouveau lot du codex n'est lancé avant une relecture favorable.

**D-083 — Budget d'usage : Codex réduit pour la V1, agents plus légers, aperçu installable.**
- **Constat** : les douze fiches pilotes ont coûté environ quatre millions de jetons d'agents (rédaction, réécriture, deux relectures, extraction, salle blanche). À ce rythme, le socle de 220 fiches prévu au plan ne tient pas dans les limites d'usage.
- **Codex de la V1** : un socle réduit à une quarantaine de fiches essentielles, trois ou quatre par thème, toutes relues. Le glossaire et les anecdotes sont limités à ce que ces fiches nourrissent. Les objectifs du plan (§7) restent la cible des versions suivantes, par petits lots.
- **Agents** :
  - un modèle plus léger pour les agents de production (extraction des faits, premiers jets) ;
  - le modèle complet pour les relectures indépendantes, où l'exigence compte le plus ;
  - les phases de l'appli (builds, Ma partie, outils, finitions) sont menées sans agent de production, avec une seule vérification indépendante en fin de phase.
- **Aperçu** : un APK de débogage, installable hors Play Store, est fourni avant la V1 pour juger l'allure sur le téléphone. Il n'est pas signé avec la clé finale ; son contenu est partiel.

**D-084 — Moteur de règles, premier lot (phase 4).** Chaque règle de `core:rules` cite sa page de bg3.wiki et sa révision (`RuleSources`). Toutes ont leurs tests, sur les exemples chiffrés des pages et par recoupement avec les tableaux des classes de `data/game`.
- **Modificateur de caractéristique** : (valeur − 10) / 2 arrondi vers le bas, plafonné à +10. Source : *Abilities*, révision 439175.
- **Achat de points** : 27 points, valeurs de 8 à 15, un point par cran jusqu'à 13, deux pour 14 et pour 15, puis un +2 et un +1 (*Character creation*, révision 416995). La page donne 17 comme valeur de départ maximale : les deux bonus vont donc à deux caractéristiques différentes.
- **Bonus de maîtrise** : +2, puis +3 au niveau 5 et +4 au niveau 9, selon le niveau du personnage. DD des sorts : 8 + modificateur + maîtrise (*Proficiency*, révision 439299). Sorts préparés : modificateur + niveau de classe, au moins 1 (*Spells*, révision 433193).
- **Points de vie** (*Hit points*, révision 425339) :
  - valeur de départ de la première classe, puis gain par niveau de chaque classe prise ;
  - modificateur de Constitution à chaque niveau, rétroactif ;
  - Robustesse naine +1 par niveau ; Résilience draconique +1 par niveau d'ensorceleur.
- **Emplacements de sort** (*Spells*, révision 433193) :
  - niveau d'incantateur effectif : pour une seule classe, son niveau ou sa moitié ou son tiers arrondis au-dessus ; en multiclasse, la somme des parts arrondie au-dessous ; l'occultiste est exclu ;
  - une classe ne compte qu'à partir du niveau où elle obtient l'Incantation (2 pour le paladin et le rôdeur, 3 pour les tiers de lanceur, d'après leurs tableaux) ;
  - le recoupement retrouve exactement les tableaux des sept classes d'incantation et des deux tiers de lanceur.
- **Exception du rôdeur** : au niveau 12, il gagne un emplacement de niveau 3 de plus que la table. La page *Ranger* (révision 422145) le présente comme un écart voulu par BG3. Le moteur l'applique au rôdeur pur ; en multiclasse, la table générale s'applique, faute de source contraire.
- **Pas encore couvert** : classe d'armure, jets de d20 (avantage, critiques), probabilités, dons par niveau. C'est le lot suivant.

**D-085 — Moteur de règles, second lot : classe d'armure et jets de d20.**
- **Classe d'armure** (*Armour Class*, révision 439529) :
  - avec une armure : sa base plus la Dextérité, plafonnée à +2 en armure intermédiaire (+3 avec Maître des armures intermédiaires) et ignorée en armure lourde ;
  - quatre armures rares (« Exotic Material ») donnent la Dextérité entière ;
  - sans armure : 10 + Dextérité, ou la meilleure des formules d'Armure du mage (ou Résilience draconique), de Défense sans armure du barbare et de Défense sans armure du moine ; celle du moine cesse avec un bouclier.
- **Jets de d20** (*Dice rolls*, révision 439285) :
  - attaque et test de caractéristique : le 1 naturel échoue toujours, le 20 naturel réussit toujours ;
  - sauvegarde : ni l'un ni l'autre, sauf pour la concentration ou en dialogue ;
  - avantage et désavantage ne se cumulent pas et s'annulent ;
  - coup critique au seuil (abaissable, cumulable), qui touche toujours.
- **Valeur passive** : 10 + bonus, ±5 avec avantage ou désavantage (*Advantage*, révision 415831).
- **Probabilités exactes** : chaque face est énumérée, rien n'est simulé. Les tests les comparent à une énumération brute écrite à part, pour chaque sorte de jet, chaque mode, chaque modificateur et chaque cible.
- **Interprétation signalée** : avec avantage, la Chance du halfelin relance chaque dé tombé sur 1, comme dans les règles de D&D. La page ne précise que le cas d'un seul dé.

**D-086 — Fiche calculée d'un build.**
- **Modèle** : un build (`core:model`, paquet `builds`) ne stocke que les choix du joueur : origine, achat de points, bonus +2 et +1, puis un niveau par entrée (classe, sous-classe au niveau où elle se choisit, don). Tout le reste est recalculé. Le paquet ne s'appelle pas `build` : le `.gitignore` exclut les dossiers de ce nom.
- **Profils tirés des données** : points de vie, sauvegardes, caractéristique d'incantation, niveaux de don et de sous-classe viennent des fiches de `data/game`. La catégorie d'incantateur se déduit du tableau de classe : Magie de pacte à part, emplacements dès le niveau 1 pour un lanceur complet, au niveau 2 pour un demi-lanceur, sous-classe à emplacements pour un tiers de lanceur. Les capacités qui changent un calcul sont reconnues par leur identifiant stable (Défense sans armure, Résilience draconique, Robustesse naine, Robuste, Résilient).
- **Règles ajoutées** :
  - la multiclasse n'accorde jamais de maîtrise de jet de sauvegarde : seules comptent celles de la classe de départ (*Classes*, révision 417100) ;
  - Robuste : +2 points de vie par niveau du personnage, rétroactivement (*Tough*, révision 438958) ;
  - augmentations des dons plafonnées à 20 ; Résilient donne la maîtrise de la sauvegarde de la caractéristique augmentée (*Feats*, révision 439281) ;
  - un chevalier occulte ou un escroc arcanique lance ses sorts avec la caractéristique d'incantation de sa classe, l'Intelligence d'après les fiches.
- **Détail du calcul** : points de vie et classe d'armure sont rendus terme à terme (premier niveau, niveaux suivants par classe, bonus par niveau ; base de la formule et modificateurs). L'écran traduit ces termes en français ; les tests vérifient que leur somme égale le résultat des fonctions de règles prises une à une.
- **Problèmes signalés**, distingués entre invalides (le jeu ne le permettrait pas) et incomplets (un choix reste à faire) : budget ou bornes de l'achat de points, bonus manquants ou sur la même caractéristique, classe inconnue, plus de 12 niveaux, don absent ou mal placé, répartition contraire à la fiche du don, sous-classe manquante, d'une autre classe ou au mauvais niveau.
- **Hors du calcul pour l'instant** : armure et équipement, bonus de race aux compétences, sorts connus. La fiche montre la classe d'armure sans équipement.

