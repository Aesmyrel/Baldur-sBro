# Sources — évaluation (phase 0)

> Évaluation faite le 27 octobre 2026. Chaque source sera revérifiée (licence, version) au moment de l'import, et la date de vérification sera stockée avec les données.
> Règle d'or : **les faits ne s'empruntent pas en texte**. On extrait des données structurées (chiffres, lieux, relations) et on écrit les résumés et le lore avec nos propres mots, en synthèse de plusieurs sources, avec références.

## 1. Vue d'ensemble

| Source | Usage prévu | Couverture | Fiabilité | Licence | Noms VF | Verdict |
|---|---|---|---|---|---|---|
| **bg3.wiki** (EN) | Données de jeu structurées (sorts, objets, classes, états, lieux, marchands, approbations, quêtes) | Très large, à jour du Patch 8 et des correctifs suivants | Élevée (communauté active, données issues des fichiers du jeu, pages versionnées) | CC BY-NC-SA 4.0 / CC BY-SA 4.0 (double licence depuis le 20/07/2024 ; NC-SA seule avant). **Traitée comme CC BY-NC-SA 4.0 partout** | Aucun | **Source principale des données de jeu** |
| **Forgotten Realms Wiki** (Fandom, EN) | Recherche de lore, repérage des références primaires (livres, pages, éditions) | Immense, toutes éditions | Bonne mais inégale : bien sourcée, mélange parfois les éditions | CC BY-SA 3.0 (licence Fandom standard, à reconfirmer à l'import : page inaccessible depuis l'outil de lecture, HTTP 402) | Aucun | **Référence secondaire, jamais de texte repris** |
| **Wikipédia** (FR et EN) | Faits « hors univers » : dates de sortie, éditeurs, auteurs, titres VF des jeux et romans | Bonne pour la saga, les éditions et les romans ; faible pour le lore interne | Bonne pour le monde réel | CC BY-SA 4.0 | Titres VF des œuvres | **Référence pour la saga et « Pour aller plus loin »** |
| **AideDD** (FR) | Terminologie française officielle de D&D 5e (2014 et 2024) : sorts, monstres, objets, états | Très large pour les règles ; partielle pour les Royaumes | Élevée pour le vocabulaire (reprend les traductions officielles) | Aucune licence ouverte : site de fan sous la politique de contenu de fans de Wizards. **Lexique uniquement, aucun texte** | Oui (D&D, pas BG3) | **Référence de vocabulaire** |
| **SRD 5.2.1 en français** (Wizards of the Coast, publié sur D&D Beyond) | Vocabulaire officiel FR des règles (états, sorts, créatures) | Règles de base 2024, pas de lore des Royaumes, pas de règles BG3 | Officielle | **CC BY 4.0** (attribution exigée) | Oui (D&D 2024) | **Meilleure source sous licence pour les termes de règles** |
| **Le Monde des Royaumes Oubliés** (Fandom, FR) | Contrôle croisé des noms propres FR des Royaumes | Moyenne | Moyenne (wiki de fans ; s'appuie sur les traductions publiées) | CC BY-SA 3.0 (Fandom, à reconfirmer) | Oui (Royaumes) | **Contrôle croisé de noms, pas de texte** |
| **Baldur's Gate Wiki FR** (Fandom) | Contrôle croisé des noms VF de BG3 (objets, sorts) | Partielle, mise à jour incertaine | Moyenne | CC BY-SA 3.0 (Fandom, à reconfirmer) | Oui (BG3, partiel) | **Indice de nom VF, jamais seul** |
| **Le jeu lui-même, en VF** (ta PS5) | Vérité terrain des noms VF et de la progression | Totale | Maximale | Propriété de Larian / WotC : on ne reprend que des noms courts, jamais de texte | Oui | **Arbitre final via la correction dans l'appli** |
| **Larian** (baldursgate3.game, support) | Notes de version, numéros de build | Correctifs | Officielle | © Larian : faits uniquement | Non | **Référence de version** |
| **Livres officiels D&D** (suppléments, aventures) | Références primaires du lore | — | Officielle (canon) | © WotC : références citées, jamais de texte | Titres VF quand ils existent | **Références citées** |
| **Romans, jeux précédents** | Références pour la saga et « Pour aller plus loin » | — | Officielle | © ayants droit : références citées | Titres VF | **Références citées** |
| Fextralife, jeuxvideo.com, Millenium, guides YouTube | — | Large | Variable | Tous droits réservés | Parfois | **Exclues** comme source de données ; au mieux un indice de nom VF signalé « à confirmer » |
| Reddit, forums, théories de fans | — | — | Non vérifiable | — | — | **Exclues** (les théories ne sont jamais présentées comme du canon) |

## 2. Fiches détaillées

### 2.1 bg3.wiki — données de jeu
- **Ce que c'est** : wiki communautaire indépendant consacré à BG3 (MediaWiki 1.43, ~13 000 articles, ~58 000 pages, ~48 contributeurs actifs au 27/10/2026). Il dispose de l'extension **Cargo**, qui expose des tables structurées interrogeables par l'API : `spells`, `equipment`, `weapons`, `weapon_types`, `conditions`, `condition_sources`, `status_groups`, `actions`, `passives`, `reactions`, `item_locations`, `locations`, `areas`, `creatures`, `camp_events`, `dialogue`… C'est idéal pour un pipeline reproductible, sans scraping du HTML.
- **Couverture** : classes, sous-classes (dont les 12 du Patch 8), races, historiques, dons, sorts, objets avec emplacement, acte et marchand, états, actions, compagnons (approbations détaillées), quêtes, lieux, créatures. Les pages de notes de version suivent chaque correctif.
- **Fiabilité** : élevée. Les valeurs proviennent en grande partie des fichiers du jeu, chaque page est versionnée (on stocke l'identifiant de révision `revid` de chaque page importée, ce qui rend chaque donnée vérifiable et datée). Points faibles : quelques pages en retard après un correctif, formulations parfois ambiguës sur les cas limites. On les signale dans `tools/` sous forme de liste de vérifications manuelles.
- **Licence** : « CC BY-NC-SA 4.0 or CC BY-SA 4.0 ». Le contenu publié avant le 20/07/2024 est sous CC BY-NC-SA 4.0 seule ; le contenu postérieur est en double licence ; le contenu dérivé d'un contenu antérieur reste NC-SA. **Décision (imposée par le brief) : tout est traité comme CC BY-NC-SA 4.0.** Conséquences :
  - attribution **par fiche** : lien vers la page source et sa révision, mention « contributeurs de bg3.wiki », licence affichée sur la fiche ;
  - usage non commercial (c'est le cas) ;
  - les données dérivées (JSON normalisés dans `data/`, base embarquée) sont redistribuées **sous la même licence CC BY-NC-SA 4.0**, ce que l'écran « Crédits et licences » et `data/LICENSE` indiqueront. Le code de l'appli peut avoir sa propre licence, distincte des données.
- **Exceptions à respecter** : le wiki précise que certains contenus ne sont pas sous licence Creative Commons, notamment ce qui est la propriété exclusive de Larian ou de WotC. **On n'importe donc ni images, ni icônes, ni textes du jeu** (descriptions d'objets, textes d'ambiance, dialogues, livres du jeu). On prend les données mécaniques (dés, portées, coûts, niveaux, conditions, emplacements, actes) et on rédige nos propres résumés.
- **Noms VF** : aucun. C'est le principal trou à combler (voir §4).
- **Méthode d'accès** : API `action=cargoquery` et `action=parse`/`revisions` hors de l'appli, avec un User-Agent identifié, un débit limité (1 requête/s), un cache local non versionné et des instantanés reproductibles. Jamais au moment de l'exécution de l'appli.

### 2.2 Forgotten Realms Wiki (forgottenrealms.fandom.com)
- **Couverture** : la plus large qui existe sur le lore des Royaumes (dieux, plans, lieux, factions, chronologie, toutes éditions de D&D, romans, jeux vidéo).
- **Fiabilité** : bonne. Les articles citent leurs sources avec titre et page, ce qui permet de **remonter à la référence primaire**. Limites : le mélange des éditions (2e, 3e, 4e, 5e) n'est pas toujours explicité, et certaines pages reprennent des contenus de jeux vidéo comme s'ils étaient du canon du jeu de rôle. On le compense en exigeant, pour chaque affirmation, l'édition et la nature du canon.
- **Licence** : CC BY-SA 3.0 (licence standard des wikis Fandom : « Community content is available under CC-BY-SA unless otherwise noted »). À reconfirmer en pied de page au moment du travail, la page de copyright n'ayant pas pu être lue par l'outil (HTTP 402).
- **Compatibilité** : CC BY-SA 3.0 et CC BY-NC-SA 4.0 sont **incompatibles** pour mélanger des textes. Cela ne pose pas de problème ici : **on ne reprend aucun texte** de ce wiki. Il sert à trouver les références et à recouper. Une affirmation dont la seule référence est ce wiki est marquée « référence secondaire ».
- **Traçabilité** : on stocke l'URL et l'identifiant de révision de la page consultée, plus la référence primaire qu'elle cite (avec la mention « citée par le Forgotten Realms Wiki, non consultée directement » quand on n'a pas le livre en main).

### 2.3 Wikipédia (FR et EN)
- **Couverture** : solide pour l'histoire éditoriale (jeux Baldur's Gate et extensions, dates, studios, éditions de D&D, romans et auteurs), faible pour le lore interne.
- **Licence** : CC BY-SA 4.0. Aucun texte repris ; faits et titres VF uniquement.
- **Usage** : saga Baldur's Gate (§15 du brief), « Pour aller plus loin » (§18), dates de sortie, titres VF (ex. *Guide des aventuriers de la Côte des Épées*). La version FR est souvent la meilleure source pour les titres français.

### 2.4 AideDD (aidedd.org)
- **Ce que c'est** : site français de fans, actif depuis 2000, centré sur D&D 5e (règles 2014 et 2024) : sorts, monstres, objets magiques, dons, et quelques contenus sur les Royaumes Oubliés.
- **Licence** : aucune licence ouverte. Le site se déclare « contenu de fan non officiel autorisé dans le cadre de la Politique des contenus de fans » de Wizards. **Usage : référence de vocabulaire uniquement** (correspondance VO → VF d'un terme), comme le demande le brief. Aucune description, aucun texte, aucune carte, aucun tableau repris.
- **Attention** : la VF de D&D a changé entre la traduction de 2014 et celle de 2024 pour certains termes, et BG3 (traduit en 2023) a fait ses propres choix. Un terme AideDD n'est donc **pas** automatiquement le terme de la VF de BG3 : il donne au mieux le statut « VF D&D officielle, VF BG3 à confirmer ».

### 2.5 SRD 5.2.1 en français (Wizards of the Coast)
- **Ce que c'est** : le document de référence officiel des règles D&D 2024, publié par Wizards en français (ainsi qu'en italien, allemand et espagnol), **sous licence CC BY 4.0**.
- **Usage** : la source la plus propre juridiquement pour le vocabulaire officiel des règles en français (noms d'états, de sorts, de créatures, termes de règles). Il ne contient ni règles propres à BG3 ni lore des Royaumes Oubliés.
- **Obligation** : la mention d'attribution exigée par le SRD figurera dans l'écran « Crédits et licences » si on l'utilise.

### 2.6 Wikis de fans francophones (Fandom)
- **Le Monde des Royaumes Oubliés** et **Baldur's Gate Wiki FR** : utiles pour recouper un nom propre ou un nom VF d'objet (« Côte des Épées », « Eauprofonde », « Château-Suif », « Porte de Baldur »…). Couverture partielle, fiabilité moyenne, licence Fandom CC BY-SA 3.0 à reconfirmer. Jamais utilisés seuls pour déclarer un nom « officiel ».

### 2.7 Le jeu (VF), sur ta PS5
- Seule source de vérité pour les noms VF de BG3. L'appli permet de corriger un nom en un appui depuis n'importe quelle fiche ; la correction est stockée localement, prioritaire sur la donnée embarquée, et exportée avec tes données. Je pourrai réintégrer tes corrections dans la base embarquée si tu me les transmets (fichier d'export).
- Alternative, si tu as un jour accès à une version PC : le fichier de localisation français du jeu (converti avec l'outil communautaire LSLib) donnerait **les noms** VF exacts. On n'en extrairait que les noms courts (sorts, objets, états, lieux), jamais les descriptions. C'est une question ouverte (voir PLAN.md), pas un prérequis.

### 2.8 Larian (notes de version)
- `baldursgate3.game/news` et le site d'assistance de Larian : notes de version officielles. Faits uniquement.

### 2.9 Références primaires du lore (livres officiels, romans, jeux)
Elles ne sont pas importées : elles sont **citées** (titre VO, titre VF s'il existe, édition de D&D, année, chapitre ou page quand on le connaît). Exemples de références à mobiliser :
- 5e édition : *Sword Coast Adventurer's Guide* (2015, VF *Guide des aventuriers de la Côte des Épées*), *Baldur's Gate: Descent into Avernus* (2019), *Mordenkainen's Tome of Foes* (2018), *Volo's Guide to Monsters* (2016), *Mordenkainen Presents: Monsters of the Multiverse* (2022) ;
- suppléments récents consacrés aux Royaumes : *Forgotten Realms: Heroes of Faerûn* et *Forgotten Realms: Adventures in Faerûn* (règles 2024, sortis en anglais le 11/11/2025 ; éditions françaises annoncées pour le 04/08/2026, sortie effective et titres VF à confirmer) ;
- éditions antérieures, quand le lore a évolué : *Forgotten Realms Campaign Setting* (3e, 2001), *Faiths and Pantheons* (3e, 2002), boîtes et suppléments de la 2e édition, *Fiend Folio* (AD&D 1re édition, 1981) pour l'origine des githyanki, etc. ;
- jeux : *Baldur's Gate* (1998) et ses suites, avec leurs extensions ; romans de la saga.
Chaque référence porte un statut : **consultée directement** ou **citée par une source secondaire** (FR Wiki, Wikipédia). L'appli affiche ce statut dans la note de bas de fiche.

## 3. Politiques de contenu de fans — contraintes pour l'appli

### 3.1 Larian — Fan Content Policy (mise à jour du 31 juillet 2025)
Ce que dit la politique générale de Larian :
- Elle couvre art, vidéos, récits, captures, cosplays, mods « ou toute autre chose ». **Elle renvoie, pour BG3, à des conditions séparées : les *Baldur's Gate 3 Fan Content Terms*** (voir 3.2).
- **Pas de vente** : pas de transactions commerciales, pas de vente d'objets reproduisant directement la propriété intellectuelle de Larian (exceptions limitées pour les commandes de petits créateurs et les dons volontaires).
- **Marques et logos** : interdits sans accord écrit.
- **Mentions légales** : ne jamais retirer les mentions de droits d'auteur ou de marques présentes dans les éléments de Larian.
- **Pas d'affiliation** : le contenu de fan est non officiel ; il doit indiquer clairement qu'il n'est **ni commandé ni sponsorisé par Larian Studios** et ne pas laisser croire à un soutien.
- **Contenu** : rien de discriminatoire, d'offensant, de diffamatoire ou de nuisible à la réputation de Larian ; aucun code malveillant, rien qui interfère avec le jeu.
- **Révocable** : Larian peut suspendre ou retirer la permission à tout moment.
- **IA générative** : ne pas « prompter » ni entraîner d'outils d'IA générative avec la propriété intellectuelle de Larian, ne pas tenter de la recréer avec eux ; ne pas imiter les voix des comédiens.

### 3.2 Larian — Baldur's Gate 3 Fan Content Terms (mise à jour du 31 juillet 2025)
- S'appliquent **exclusivement** au contenu de fans créé pour BG3 (créations, contenus audiovisuels, mods).
- **Intègrent entièrement la politique de contenu de fans de Wizards of the Coast** : tout contenu de fan BG3 doit aussi la respecter. Aucune compensation possible, sauf dans les cas permis par la politique de Wizards.
- Larian précise ne pas pouvoir accorder de permission au-delà de ces conditions ; pour le reste, il faut s'adresser à Wizards.
- Larian reçoit une licence d'utilisation sur le contenu publié ; le créateur reste responsable de son contenu.
- Les applis compagnons ne sont pas mentionnées explicitement : on applique donc l'esprit des deux textes et la politique de Wizards.

### 3.3 Wizards of the Coast — Fan Content Policy (mise à jour du 15 novembre 2017)
- Le contenu de fan doit être **gratuit** : pas de paiement, d'abonnement, de questionnaire ni d'inscription par e-mail pour y accéder ; pas de vente ni de licence à des tiers.
- **Pas de reproduction verbatim** de la propriété intellectuelle de Wizards (règles, livres).
- **Pas de logos ni de marques** de Wizards sans permission (D&D, Dungeons & Dragons… la liste n'est pas exhaustive).
- Pas d'utilisation des mécaniques dans un autre jeu (hors licence ouverte).
- **Mention obligatoire** (formulation imposée) : « *[Titre] is unofficial Fan Content permitted under the Fan Content Policy. Not approved/endorsed by Wizards. Portions of the materials used are property of Wizards of the Coast. ©Wizards of the Coast LLC.* »
- Wizards peut demander le retrait à tout moment.

### 3.4 Ce que cela impose concrètement à « Carnet de Faerûn »
1. **Gratuit, sans compte, sans pub, sans paiement** (déjà le cas).
2. **Aucun logo ni marque graphique** : pas de logo BG3, Larian, D&D ou Wizards, pas d'ampersand stylisé D&D. Les noms sont cités en texte, de façon descriptive (« compagnon non officiel pour Baldur's Gate 3 »).
3. **Aucun élément du jeu** : ni images, ni icônes, ni polices, ni musique, ni sons, ni textes (descriptions, dialogues, livres). Illustrations entièrement originales, sans imiter l'interface du jeu.
4. **Mentions obligatoires** dans l'écran « Crédits et licences » et dans « À propos » :
   - « Carnet de Faerûn est un contenu de fan non officiel. Il n'est ni commandé, ni sponsorisé, ni approuvé par Larian Studios ni par Wizards of the Coast. »
   - la mention de Wizards, en VO comme exigé, avec sa traduction : « Carnet de Faerûn is unofficial Fan Content permitted under the Fan Content Policy. Not approved/endorsed by Wizards. Portions of the materials used are property of Wizards of the Coast. ©Wizards of the Coast LLC. »
   - les marques citées : Baldur's Gate, Dungeons & Dragons, Forgotten Realms, Faerûn appartiennent à Wizards of the Coast ; Baldur's Gate 3 est développé et édité par Larian Studios.
5. **Licences par fiche** : chaque fiche affiche sa source et sa licence (CC BY-NC-SA 4.0 pour ce qui dérive de bg3.wiki, « texte original » pour le lore rédigé par nous). Jamais deux textes sous licences incompatibles dans la même fiche.
6. **Pas de reproduction verbatim** de règles ou de livres : les règles BG3 sont codées et testées, les résumés sont rédigés.
7. **Illustrations des personnages** : pas de portraits imitant les modèles du jeu. Les compagnons sont représentés par des **sceaux et emblèmes symboliques** originaux.
8. **Point de vigilance pour la V2 (« Demande à l'archiviste »)** : la clause IA de la politique générale de Larian interdit de « prompter » une IA générative avec sa propriété intellectuelle. Les conditions propres à BG3 ne reprennent pas cette clause mot pour mot, mais l'intention de Larian est claire. Avant la V2, il faudra trancher (voir PLAN.md, questions ouvertes). Aucune incidence sur la V1.
9. Le nom « Carnet de Faerûn » emploie un nom propre du monde de Wizards en texte, sans logo. Risque jugé faible pour un usage personnel non distribué ; noté dans DECISIONS.md.

## 4. Noms VF : stratégie et statuts

Chaque nom porte un **statut de traduction** visible sur la fiche :

| Statut | Signification | Affichage |
|---|---|---|
| `VF_BG3` | Nom vérifié dans la VF de BG3 (vu en jeu, ou deux sources concordantes dont une fiable) | Aucun badge |
| `VF_DND` | Traduction française officielle de D&D (SRD FR, AideDD, livres traduits) ; pour un terme du jeu, non vérifiée dans BG3 | Badge discret « VF à confirmer » pour un terme du jeu ; aucun badge pour un nom propre des Royaumes |
| `NON_OFFICIELLE` | Pas de source fiable : traduction proposée par nous | Badge « traduction non officielle » |
| `CORRIGEE` | Corrigée par toi dans l'appli | Badge « corrigé par vous » |

Priorité d'affichage : `CORRIGEE` > `VF_BG3` > `VF_DND` > `NON_OFFICIELLE`. Le nom anglais est toujours affiché en secondaire et reste cherchable.

## 5. Version du jeu visée

- **Patch 8** (« The Final Patch ») : 15 avril 2025, build 4.1.1.6758295. Douze sous-classes (une par classe), mode photo, cross-play.
- Correctifs suivants, d'après la page de notes de version de bg3.wiki : Hotfix #30 (30/04/2025), #31 (14/05/2025), #32 (20/05/2025), #33 (31/07/2025), #34 (23/09/2025), mise à jour sans nom (02/10/2025), #35 (20/11/2025), mise à jour sans nom (16/02/2026), **#36 (26/03/2026, dernier correctif numéroté annoncé sur le site officiel)**, mises à jour sans nom du 03/08/2026 (PC et PS5 : 4.1.1.7398727) et du **29/09/2026 (build 4.1.1.7631656, la plus récente au 27/10/2026)**.
- **Cible des données V1 : Patch 8 + tous les correctifs jusqu'à la mise à jour du 29/09/2026.** Le pipeline vérifie, avant chaque import, si une version plus récente est apparue, et l'appli affiche la version de données dans « À propos ».
- Les correctifs depuis le Patch 8 sont surtout des corrections de bugs ; le pipeline signalera toute modification de règle repérée dans leurs notes.

## 6. Sources consultées pour cette évaluation
- Larian, *Fan Content Policy* : https://larian.com/fan-content-policy
- Larian, *Baldur's Gate 3 Fan Content Terms* : https://baldursgate3.game/bg3-fan-content-terms/
- Wizards of the Coast, *Fan Content Policy* : https://company.wizards.com/en/legal/fancontentpolicy
- bg3.wiki, *Copyrights* : https://bg3.wiki/wiki/bg3wiki:Copyrights ; API : https://bg3.wiki/w/api.php ; notes de version : https://bg3.wiki/wiki/Patch_notes
- Larian, actualités BG3 : https://baldursgate3.game/news
- SRD 5.2.1 FR (CC BY 4.0) : https://media.dndbeyond.com/compendium-images/srd/5.2/FR_SRD_CC_v5.2.1.pdf
- AideDD : https://www.aidedd.org/
- Wizards, fiches produit *Heroes of Faerûn* et *Adventures in Faerûn* : https://wpn.wizards.com/en/products/forgotten-realms-heroes-of-faerun
