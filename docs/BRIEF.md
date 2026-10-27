# Brief : « Carnet de Faerûn », appli Android compagnon de Baldur's Gate 3

## Mission
Conçois et développe de bout en bout une application Android native qui m'accompagne dans Baldur's Gate 3. Elle repose sur trois piliers d'égale importance :
1. un compagnon de jeu pratique (encyclopédie, builds, progression) ;
2. une plongée dans le lore de Donjons & Dragons et des Royaumes Oubliés, derrière tout ce que je croise dans BG3 ;
3. une vraie direction artistique : l'appli doit être belle et donner envie d'y passer du temps.
Appareil de référence : Google Pixel 10 Pro XL sous Android 17. Niveau visé : une appli premium, fluide et fiable.
Usage personnel et non commercial, sans affiliation avec Larian Studios ni Wizards of the Coast (mention à faire figurer dans l'appli). Si le dépôt contient déjà du code, analyse-le d'abord.

## Contexte d'utilisation
- Je joue sur PS5 Pro, en français. En pleine partie, le téléphone est un second écran posé à côté de la manette : je le prends d'une main pour une info précise, puis je le repose. Il faut donc : actions principales à portée du pouce, une info trouvée en deux ou trois appuis, reprise exacte là où j'en étais, option « écran toujours allumé » pendant une session.
- Hors partie, j'adore le lore : je veux le lire longuement, confortablement, et passer d'une fiche à l'autre en tirant le fil.
- Aucun accès aux sauvegardes PS5 : toute la progression se saisit à la main, en un appui, sans formulaire.
- Je veux un APK installable directement et des instructions d'installation simples.

## Principes non négociables
1. Zéro spoiler par défaut.
   - L'appli connaît mon acte en cours (Prologue, Acte 1, Acte 2, Acte 3, Épilogue) et ma tolérance aux spoilers (aucun, léger, complet).
   - Chaque contenu est étiqueté par acte et par niveau de spoiler. Ce qui dépasse ma progression est masqué partout (fiches, listes, recherche, widget) et révélable seulement si je le demande.
   - Le lore aussi : une fiche sur un dieu, une faction ou un événement ancien peut trahir un rebondissement de BG3. Découpe chaque fiche de lore en sections étiquetées séparément (général, dans BG3 par acte, spoilers d'autres œuvres). Une info qui laisse deviner un rebondissement compte comme un spoiler.
   - Les spoilers des autres œuvres (jeux précédents, romans, aventures) ont leur propre réglage.
   - Des tests automatisés garantissent qu'aucun contenu verrouillé ne fuit.
2. Hors ligne d'abord. Toutes les données sont embarquées. Pas de pub, pas de tracker, pas d'analytics, pas de permission INTERNET en V1 (si une bibliothèque l'exige, justifie-le dans docs/DECISIONS.md).
3. Les règles de BG3, pas celles du jeu de rôle papier.
   - BG3 s'écarte de D&D 5e sur de nombreux points. Chaque règle de calcul (achat de points, bonus de caractéristiques, emplacements de sorts en multiclasse, dons, critiques…) vient de sources BG3 et est couverte par des tests.
   - Le contenu est à jour du Patch 8 (dernier patch majeur, 12 nouvelles sous-classes) et des correctifs parus depuis : vérifie la version la plus récente.
4. Exactitude avant quantité.
   - Un périmètre réduit mais exact vaut mieux que des milliers d'entrées approximatives.
   - N'invente jamais rien : si une info n'est pas fiable, laisse le champ vide et signale-le.
   - Pour le lore, chaque affirmation est rattachée à au moins une référence vérifiable. Elle est étiquetée selon sa nature : canon de BG3, canon officiel de D&D (en précisant l'édition quand le lore a changé d'une édition à l'autre) ou interprétation.
   - Les théories de fans sont exclues ou clairement signalées. Quand des sources ou des éditions se contredisent, présente les versions sans trancher.
5. Respect des droits.
   - Textes : uniquement des sources sous licence compatible. Pour bg3.wiki, traite tout le contenu comme CC BY-NC-SA 4.0 (attribution par fiche, usage non commercial, contenu dérivé sous la même licence). Vérifie la licence de toute autre source avant import. Garde la licence fiche par fiche et ne fusionne jamais dans une même fiche des textes sous licences incompatibles.
   - Le lore est écrit avec tes mots, en synthèse de plusieurs sources : jamais de copie ni de paraphrase serrée d'un seul texte (suppléments, romans, wikis, dialogues ou livres du jeu).
   - Aucune image, icône, police, musique ni logo issus du jeu, de ses sites, des wikis ou des livres de D&D (propriété de Larian ou de WotC). Toutes les illustrations sont originales ; polices sous licence libre.
   - Lis la politique de Larian sur le contenu de fans (Fan Content Policy) et résume ses contraintes dans docs/SOURCES.md.
6. Français partout.
   - Interface en français.
   - Les termes du jeu (sorts, objets, compétences, états…) apparaissent comme dans la VF de BG3. Les noms des Royaumes Oubliés suivent les traductions françaises officielles de D&D. Le nom anglais reste en secondaire.
   - Recherche dans les deux langues, insensible aux accents et tolérante aux fautes de frappe.
   - Sans source fiable pour un nom VF : marque-le « traduction non officielle » et laisse-moi le corriger dans l'appli. La correction est stockée localement et prioritaire sur la donnée embarquée.

## Fonctionnalités

### V1 : le compagnon de jeu
1. Ma partie (accueil) : acte en cours, difficulté, groupe actif, prochains objectifs, et alertes « point de non-retour » avant chaque changement d'acte (ce qui deviendra manquable).
2. Encyclopédie :
   - contenu : classes et sous-classes, races et sous-races, historiques, dons, sorts, objets (où, quel acte, quel marchand, manquable ou non), états, actions ;
   - filtres utiles (classe, niveau de sort, école, concentration, rareté, acte, type de dégâts…) et fiches reliées entre elles.
3. Planificateur de builds :
   - niveaux 1 à 12, multiclasse, achat de points, dons, sorts, sous-classes ;
   - statistiques dérivées (PV, CA estimée, bonus de maîtrise, DD de sauvegarde des sorts, bonus d'attaque, emplacements de sorts), avec le détail du calcul au toucher ;
   - plusieurs builds, et une vue « groupe de 4 » (rôles, compétences et types de dégâts couverts ou manquants) ;
   - partage d'un build par QR code ou code texte, et import inverse (Google Code Scanner de ML Kit, sans permission caméra).
4. Compagnons : recrutement (où, quand, pièges à éviter), ce qu'ils approuvent ou désapprouvent, moments clés d'approbation par acte, étapes de romance et de quête personnelle.
5. Progression : quêtes, lieux, objets manquables et trophées par acte, cochables en un appui, avec avancement en pourcentage.
6. Outils de table :
   - probabilité de réussir un jet (DD, modificateur, avantage ou désavantage, bonus comme Assistance ou Inspiration bardique, règles de BG3 sur les réussites et échecs critiques) ;
   - lanceur de dés avec retour haptique.
7. Recherche globale instantanée (jeu et lore), accessible depuis n'importe quel écran.
8. Notes personnelles rattachables à un acte, une quête, un compagnon ou une fiche de lore.
9. Sauvegarde de mes données (progression, builds, notes, favoris, corrections) : export et import en fichier, compatibles avec la sauvegarde Android.

### V1 : le grand codex du lore (un pilier, pas un bonus)
10. Volet « Dans l'univers de D&D » sur chaque fiche qui s'y prête (personnage, dieu, faction, lieu, créature, objet légendaire) : origines, histoire, place dans les Royaumes Oubliés, ce que BG3 en fait et en quoi il s'écarte du canon.
11. Panthéon : les divinités présentes ou évoquées dans BG3 (domaines, fidèles, alliances et rivalités, histoire).
12. Cosmologie : les plans d'existence liés au jeu, expliqués simplement, avec un schéma illustré original.
13. Peuples et bestiaire : origines, culture et écologie selon D&D, avec en regard les conseils de combat propres à BG3.
14. Chronologie des Royaumes : des empires disparus jusqu'à BG3, datée selon le calendrier des Royaumes (CV), filtrable, chaque événement relié à ses fiches.
15. La saga Baldur's Gate : les jeux précédents et leurs extensions résumés, et leurs échos dans BG3.
16. Carte illustrée originale de la région de la Côte des Épées où se déroule le jeu, dessinée de zéro (aucune carte existante copiée), avec ses lieux reliés aux fiches.
17. Livres et notes du jeu : index (où les trouver, quel acte), résumé de quelques lignes avec tes mots et lien vers le lore concerné ; jamais le texte original.
18. Pour aller plus loin : suppléments officiels (y compris les plus récents consacrés aux Royaumes Oubliés), aventures, romans et jeux qui développent chaque sujet, avec le titre VF quand il existe et ce que l'œuvre apporte.
19. Le saviez-vous ? : une anecdote de lore par jour sur l'accueil, choisie selon ma progression.
20. Glossaire D&D : termes et noms propres, VF et VO.
21. Mode lecture : typographie soignée, taille réglable, favoris, « reprendre ma lecture », temps de lecture estimé.

### V2 (seulement après ma validation de la V1)
22. Demande à l'archiviste : questions en langage naturel (jeu ou lore), traitées sur l'appareil par Gemini Nano via l'API Prompt de ML Kit GenAI (vérifie son statut actuel).
   - Réponses fondées uniquement sur les fiches locales déjà débloquées par ma progression, en distinguant canon BG3 et canon D&D, avec liens vers les fiches citées.
   - Fonction masquée si le modèle n'est pas disponible. Si la base ne contient pas la réponse, l'archiviste le dit.
23. Toile du lore : graphe interactif et animé des liens entre dieux, factions, personnages, lieux et événements, filtré par ma progression.
24. Widget d'écran d'accueil (Jetpack Glance) : acte en cours, prochains objectifs et anecdote du jour, sans spoiler.
25. Pliables et tablettes : affichage liste + détail côte à côte.

## Direction artistique (aussi importante que les fonctionnalités)
- Ambition : chaque écran doit pouvoir figurer dans un artbook. Rien ne doit ressembler à une appli Material par défaut, ni imiter l'interface du jeu. Material 3 sert de socle technique (accessibilité, composants) ; l'identité visuelle est entièrement sur mesure.
- Piste de départ, à challenger en phase 0 : un grimoire enluminé vu à travers un écran moderne. Encre profonde, parchemin, feuille d'or, lueur de bougie ; ornements fins, lettrines enluminées en tête des fiches de lore, cadres et séparateurs dessinés.
- Typographie : une police de titre à caractère, une serif pensée pour la lecture longue (le lore), une sans-serif nette pour les données ; toutes sous licence libre et intégrées.
- Illustrations : originales, dans un style unique et cohérent réalisable en vectoriel et en code (par exemple gravure au trait, sceaux, vitrail, constellations). À produire :
   - emblèmes des classes, glyphes des écoles de magie, icônes des types de dégâts ;
   - en-têtes illustrés par section et par acte, sans rien dévoiler de l'intrigue ;
   - carte et schéma des plans ;
   - icône d'appli adaptative avec version monochrome.
- Matière et lumière : textures subtiles générées en code, effets de shader AGSL (reflets d'or, halos, poussière magique) dans un budget de performance strict.
- Mouvement : des transitions qui racontent quelque chose (encre qui se répand, page qui se tourne, éléments partagés entre liste et fiche). Un spoiler se révèle comme une brume qui se dissipe sous le doigt, avec retour haptique ; les dés sont lancés avec élan. Tout reste fluide à 120 Hz et se simplifie quand « supprimer les animations » ou l'économiseur de batterie est actif.
- Thèmes : sombre par défaut (option noir pur OLED) et thème parchemin clair ; chaque section a sa couleur d'accent.
- La beauté ne passe jamais avant la lisibilité : contrastes AA, hiérarchie claire, textes longs confortables.

## Exigences techniques
- Socle : Kotlin, Jetpack Compose, Material 3 (Material 3 Expressive si stable). Dernières versions stables de toute la chaîne au moment où tu commences (vérifie-les ; pas d'alpha sans justification). minSdk 33 (shaders AGSL sans version dégradée), targetSdk 37 (Android 17).
- Architecture : modules Gradle (app, core:model, core:data, core:database, core:designsystem, core:testing, feature:*), flux de données unidirectionnel (ViewModel + StateFlow), Hilt, navigation Compose avec liens profonds.
- Données :
   - pipeline d'import versionné dans tools/, exécuté hors de l'appli → JSON normalisés et validés → base SQLite pré-remplie (Room + recherche plein texte) livrée dans l'APK ;
   - jamais de scraping à l'exécution ;
   - chaque entrée porte : id stable, noms FR et EN, acte, niveau de spoiler, source, licence, date d'import ;
   - les fiches de lore ont en plus des sections étiquetées, leurs références, la nature du canon et l'édition, et des relations typées vers les autres fiches (base de la future Toile du lore) ;
   - les données utilisateur vivent dans une base séparée et survivent aux mises à jour de la base embarquée.
- Pixel 10 Pro XL : défilement et animations sans saccade à 120 Hz, edge-to-edge, retour prédictif, haptique soignée qui respecte les réglages système.
- Mise en page adaptative dès la V1 : une appli qui cible Android 17 ne peut plus bloquer l'orientation ni le redimensionnement sur grand écran.
- Performance (build release) : objectifs démarrage à froid sous 1 s et recherche sous 50 ms ; R8 et Baseline Profiles ; mesures Macrobenchmark si un appareil ou un émulateur est disponible.
- Accessibilité : TalkBack, cibles tactiles de 48 dp minimum, police jusqu'à 200 %.

## Méthode

### Phase 0 : plan et direction artistique, puis STOP
1. Copie ce brief mot pour mot dans docs/BRIEF.md. Il fait foi, en particulier après une compaction du contexte.
2. Évalue les sources (jeu et lore) : couverture, fiabilité, licence, noms VF → docs/SOURCES.md. À évaluer entre autres : bg3.wiki, le Forgotten Realms Wiki, Wikipédia, et AideDD pour la terminologie française officielle (référence de vocabulaire, pas de texte à reprendre).
3. Rédige docs/PLAN.md :
   - architecture, schéma de données, écrans ;
   - phases avec critères de fin, lots parallélisables, risques, questions ouvertes ;
   - arborescence du codex de lore (thèmes et nombre de fiches visé par thème) ;
   - jusqu'à 5 idées de fonctionnalités supplémentaires, classées par valeur et effort.
4. Rédige docs/DIRECTION_ARTISTIQUE.md avec trois directions nettement différentes (palette, typographies, ornements, style d'illustration, principes de mouvement). Maquette chacune sur trois écrans clés (Ma partie, une fiche de lore, le planificateur de builds), en images dans docs/design/.
5. Crée CLAUDE.md : conventions, commandes de build et de test, règles du projet (spoilers, licences, règles BG3, exactitude du lore, charte artistique).
6. Arrête-toi : attends que je choisisse une direction artistique et que je valide le plan avant d'écrire le code de l'appli.

### Ensuite
- Ordre conseillé : socle et design system de la direction choisie → pipelines de données du jeu et du lore, en parallèle → encyclopédie, codex du lore et recherche → builds → progression et compagnons → outils → finitions → V2.
- Le lore est un chantier à part entière, parallélisable par thème (panthéon, plans, peuples, chronologie, saga, livres du jeu…). Le schéma de données et le design system n'ont qu'une seule source de vérité.
- Chaque phase se termine par une vérification indépendante, menée par des agents qui n'ont pas produit le travail :
   - build OK, tests verts, lint propre ;
   - échantillon de données de jeu et d'affirmations de lore comparé à leurs références ;
   - fuites de spoilers, licences, accessibilité ;
   - revue artistique des captures selon docs/DIRECTION_ARTISTIQUE.md (hiérarchie, rythme, cohérence, finitions). Itère jusqu'à ce que ce soit vraiment beau, pas seulement correct.
- Un commit Git par étape cohérente, jamais de réécriture d'historique. Ne modifie rien en dehors du dossier du projet.
- Tiens à jour docs/PROGRESS.md (fait, en cours, reste à faire, problèmes connus) et docs/DECISIONS.md (choix et raisons).
- Ne me pose une question que pour une décision difficile à défaire ou qui change le produit. Sinon, tranche, note-le dans DECISIONS.md et avance.

### Vérifier sans mon téléphone
- SDK Android absent : installe les outils en ligne de commande. Si c'est impossible, dis-le-moi tout de suite plutôt que de coder à l'aveugle.
- Tests à écrire :
   - tests unitaires : règles de calcul, verrouillage des spoilers, recherche ;
   - tests du pipeline et de la base : schéma, comptes, références croisées, champs obligatoires, chaque affirmation de lore référencée ;
   - captures d'écran automatiques (Roborazzi ou équivalent) des écrans clés en thèmes sombre et parchemin, en grande police et en largeur de pliable. Examine toi-même ces captures et corrige ce qui cloche.
- Téléphone branché en adb : installe et teste dessus. Sinon, émulateur si possible. Sinon, liste ce qui reste à tester sur appareil réel dans docs/TEST_SUR_TELEPHONE.md.

## Livrables de la V1
- Code propre et README.md en français.
- APK release signé avec une clé locale jamais committée. Explique-moi comment sauvegarder cette clé : sans elle, impossible d'installer une mise à jour sans désinstaller l'appli.
- docs/INSTALLER.md : installation pas à pas sur le Pixel.
- Écran « Crédits et licences » complet dans l'appli.
- Une planche des écrans principaux dans docs/design/vitrine.png.
- Rapport final : ce qui fonctionne, couverture des données de jeu et du lore par catégorie (en %), limites connues, suite proposée.
