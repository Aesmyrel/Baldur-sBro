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
