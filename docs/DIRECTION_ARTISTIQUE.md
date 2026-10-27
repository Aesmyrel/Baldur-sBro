# Direction artistique — trois pistes à choisir

> Phase 0. Trois directions nettement différentes, chacune maquettée sur trois écrans clés (Ma partie, fiche de lore, planificateur de builds), plus la fiche de lore en thème clair.
> **Planches** : `docs/design/a-grimoire.png`, `docs/design/b-atlas.png`, `docs/design/c-vitrail.png` ; **comparatif** 3 × 3 : `docs/design/comparatif.png` ; écrans en haute définition dans `docs/design/<direction>/`.
> Tout ce que montrent les maquettes (illustrations, ornements, textures, emblèmes) est **généré en code** (SVG), sans aucune image externe : c'est la preuve que chaque style est reproductible en vectoriel et en Compose. Les sources sont dans `docs/design/src/` ; régénération : `python3 tools/design/fetch_fonts.py && node tools/design/render_mockups.js`.

## 0. Ce qui vaut pour les trois directions

- **La lisibilité d'abord** : texte courant ≥ 4,5:1 sur son fond, grands titres ≥ 3:1, mesurés pour chaque jeton de couleur (tableaux ci-dessous). Texte de lecture à 18 sp par défaut, interligne ~1,5, colonne de lecture plafonnée à ~38 em sur grand écran, taille réglable, police système jusqu'à 200 % sans troncature.
- **Material 3 comme socle technique** (accessibilité, gestes, composants, retour prédictif), **identité entièrement sur mesure** : nos propres jetons de couleur, de forme, de typographie et de mouvement ; aucun composant laissé avec son style par défaut.
- **Ni imitation du jeu, ni élément du jeu** : aucune image, icône, police, couleur signature, cadre ou disposition de l'interface de BG3 ; aucun portrait de personnage (les compagnons sont des emblèmes et des sceaux symboliques) ; aucun logo Larian, Wizards ou D&D.
- **Ergonomie en partie** : actions principales dans la zone du pouce (barre de navigation et recherche en bas à droite, cases à cocher dans la moitié basse), cibles ≥ 48 dp, une info en deux ou trois appuis.
- **Thèmes** : sombre par défaut, **noir pur OLED** en option (le fond passe à `#000000`, les surfaces s'éclaircissent légèrement pour garder la hiérarchie), et un thème clair propre à chaque direction. **Une couleur d'accent par section** (Partie, Grimoire, Codex, Builds, Compagnons, Progression, Outils).
- **Matière et lumière sous budget** : textures générées une fois puis mises en cache ; au plus **un shader AGSL animé visible à la fois**, objectif < 2 ms de GPU par image au Pixel 10 Pro XL, mis en pause hors écran ; tous les effets ont un rendu statique utilisé quand « supprimer les animations » ou l'économiseur de batterie est actif.
- **Mouvement qui raconte** : chaque direction définit sa transition de liste vers fiche (éléments partagés), sa page qui se tourne, son voile de spoiler qui se dissipe sous le doigt avec retour haptique, son lancer de dés avec élan. Durées courtes (150–400 ms), interruptibles, à 120 Hz ; en mouvement réduit, fondus de 100 ms.
- **Typographie française** : espaces fines insécables avant « ; : ! ? » et dans les guillemets, apostrophe typographique, chiffres tabulaires pour les données, petites capitales réelles quand la police en a.
- **Polices** : toutes sous **SIL Open Font License 1.1**, intégrées à l'APK (sous-ensembles latin et latin étendu), licences reproduites dans l'écran Crédits.

## 1. Remise en question de la piste de départ

Le brief propose « un grimoire enluminé vu à travers un écran moderne ». C'est une piste forte, mais elle a trois écueils :
1. **le cliché** : parchemin, cuir et dorures sont le langage visuel de la plupart des applis « fantasy », et risquent de faire penser à une interface de jeu ;
2. **le skeuomorphisme** : des textures lourdes nuisent à la lecture longue et à la fluidité ;
3. **l'or sur fond sombre**, superbe en filet, devient vite illisible en texte courant.

La direction **A** garde la piste en la resserrant (aplats d'encre, grain à peine perceptible, or réservé aux filets, aux lettrines et aux chiffres clés, pigments médiévaux pour les accents). Les directions **B** et **C** proposent deux vraies alternatives, l'une précise et aérienne, l'autre graphique et lumineuse.

---

## 2. Direction A — « Grimoire enluminé »

**Intention.** Un manuscrit enluminé vu à travers un écran moderne : encre profonde, vélin à peine grenu, feuille d'or posée en filets fins, pigments médiévaux (lapis, vermillon, vert-de-gris, pourpre, orpiment) pour les sections. Chaleureux, littéraire, fait pour lire longtemps à la lueur d'une bougie.

**Palette**

| Rôle | Sombre | Parchemin (clair) | Contraste (sombre / clair) |
|---|---|---|---|
| Fond / surface | Encre `#120F0C` / `#1A1511` | Vélin `#EFE5CC` / `#F6EEDA` | — |
| Texte | `#EEE4CD` | `#2B2016` | 14,3 / 12,7 |
| Texte secondaire | `#BCAD8F` | `#5B4A35` | 8,2 / 6,8 |
| Or (filets, chiffres, libellés) | `#D8B260` | `#7A5814` | 9,0 / 5,2 |
| Lapis — Codex | `#7E9BE6` | `#2F51A8` | 6,7 / 5,8 |
| Vermillon — Grimoire, alertes | `#E66A47` | `#B03A1C` | 5,6 / 4,8 |
| Vert-de-gris — Builds | `#5CB89D` | `#256A57` | 7,6 / 5,1 |
| Pourpre — Compagnons | `#CE72A6` | `#8E2E66` | 5,7 / 6,1 |
| Orpiment — Progression ; or pâle — Partie ; sépia — Outils | | | |

Feuille d'or : dégradé métallique `#F6E3A6 → #D8B260 → #9C7A2E → #E9CF86`, réservé aux surfaces ≥ 24 dp (lettrines, médaillons, bouton de recherche).

**Typographie.** Titres **Almendra** (calligraphie humaniste, caractère médiéval sans être gothique) et **Almendra SC** ; lecture **EB Garamond** (Garamond de référence, ligatures et petites capitales) ; données **Alegreya Sans** / **Alegreya Sans SC** (humane, chiffres tabulaires).

**Ornements.** Doubles filets d'or et d'ombre, fleurons en losange, coins à rinceaux, pieds-de-mouche en vermillon, rubriques (première lettre en vermillon), **lettrines enluminées** (initiale d'or sur fond lapis, rinceaux, points d'or) en tête de chaque fiche de lore.

**Illustration.** **Gravure au trait** : contours fins et hachures parallèles ou croisées dont la densité fait la valeur (ciel en hachures horizontales, reliefs en diagonales, ombres en croisillons). Emblèmes de classe, glyphes d'école et icônes de dégâts au trait ; en-têtes d'acte en paysages gravés sans élément d'intrigue ; carte gravée façon portulan ; schéma des plans en cercles concentriques gravés. Icône d'appli : médaillon d'or avec plume et étoile ; version monochrome au trait.

**Matière et lumière.** Grain de vélin (bruit fractal à 4–8 % en lumière douce), halo de bougie très lent (shader, ±2 % de luminance), **reflet d'or** qui balaie une lettrine ou un médaillon une seule fois à l'ouverture, poussière dorée dans le voile de spoiler.

**Mouvement.** L'**encre se répand** (masque radial à bord bruité) pour ouvrir une fiche depuis la liste ; la **page se tourne** entre fiches de lore (et au retour prédictif) ; le **voile de spoiler est une brume d'encre** qui se dissipe sous le doigt en laissant flotter des paillettes d'or ; cocher un objectif **appose un sceau de cire** (léger choc haptique) ; les dés roulent sur un tapis de vélin.

**Forces.** Le meilleur confort de lecture longue ; cohérence immédiate avec le lore ; chaleur. **Risques.** Proximité avec le vocabulaire « fantasy » courant, à tenir par la retenue ; l'or demande de la discipline (jamais en texte courant).

---

## 3. Direction B — « Atlas céleste »

**Intention.** Le carnet comme un instrument d'astronome : nuit profonde, filets de laiton et d'argent, cartes du ciel, anneaux gradués d'astrolabe. **Chaque emblème est une constellation, chaque progression une orbite.** Précis, aérien, d'une élégance d'atlas du XVIIIe siècle. Très cohérent avec la cosmologie, les plans et les dieux, qui sont au cœur du lore de BG3.

**Palette**

| Rôle | Sombre | Clair « carte imprimée » | Contraste (sombre / clair) |
|---|---|---|---|
| Fond / surface | Nuit `#070B18` / `#0D1428` | Papier `#ECE9E1` / `#F4F2EC` | — |
| Texte | `#E8EDF7` | Bleu de Prusse `#15213E` | 15,6 / 13,1 |
| Texte secondaire | `#A7B3CF` | `#45506A` | 8,7 / 6,6 |
| Laiton — Partie, libellés | `#D9B877` | `#7A5D1F` | 9,6 / 5,1 |
| Aurore — Grimoire | `#66D5C3` | `#116B5F` | 10,3 / 5,3 |
| Nébuleuse — Codex | `#A797F5` | `#5643BE` | 7,3 / 5,9 |
| Soleil — Builds, alertes | `#F3A96A` | `#964E15` | 9,3 / 5,1 |
| Rose — Compagnons | `#F093B4` | `#A33A63` | 8,3 / 5,2 |
| Comète — Progression `#7FC8F4` ; argent — Outils `#C8D2E6` | | | |

**Typographie.** Titres **Bodoni Moda** (didone à tailles optiques : l'esthétique des atlas gravés ; italique pour les grands titres) ; lecture **Newsreader** (tailles optiques, pensée pour l'écran) ; données **Jost** (géométrique, capitales espacées comme des graduations).

**Ornements.** Anneaux gradués (graduations à 5° et 15°, chiffres romains), méridiens en pointillé, étoiles à quatre branches, barre de navigation bordée d'une règle graduée, initiales serties dans un cadran.

**Illustration.** **Constellations** (points et traits) pour tout ce qui est emblème : classes, écoles de magie, types de dégâts, compagnons (signes), dieux (sceaux stellaires). En-têtes d'acte : ciel nocturne avec une constellation propre à chaque acte et une ligne d'horizon au trait, sans élément d'intrigue. Carte : planisphère terrestre au trait avec rose des vents ; **schéma des plans en sphères emboîtées**, naturel dans ce style. Icône : astrolabe stylisé ; monochrome au trait. C'est le style le plus simple à produire de façon homogène (règles géométriques).

**Matière et lumière.** Champ d'étoiles procédural, scintillement (shader très peu coûteux), nébuleuses en bruit fractal coloré, halo de laiton autour de l'élément actif.

**Mouvement.** Les **anneaux tournent et s'alignent** à l'ouverture d'un écran (comme un astrolabe qu'on règle) ; transition liste → fiche par **zoom dans une lentille** (révélation circulaire) ; la **nébuleuse se dissipe** sous le doigt pour révéler un spoiler ; les progressions se remplissent en orbites ; les dés tournent sur eux-mêmes comme une planète avant de se poser.

**Forces.** La plus originale ; clarté des données (cadrans, lectures d'instrument) ; production d'illustrations cohérente et économique. **Risques.** Ambiance plus froide, moins « médiévale » ; la didone demande des tailles généreuses (titres ≥ 28 sp) ; à éviter en texte courant.

---

## 4. Direction C — « Vitrail et sceaux »

**Intention.** Le carnet comme une verrière contemporaine : **plombs d'un noir absolu** (parfaits sur OLED), verres de joaillerie qui laissent passer la lumière, réseaux gothiques, rosaces, **sceaux de cire**. Graphique, lumineux, coloré ; chaque progression s'allume comme une rosace.

**Palette**

| Rôle | Sombre | Clair « pierre et lumière » | Contraste (sombre / clair) |
|---|---|---|---|
| Fond / surface | Plomb `#050406` / `#0F0D12` | Pierre `#ECE6DB` / `#F5F1EA` | — |
| Texte | `#F3EEE6` | `#1E1A22` | 16,7 / 13,8 |
| Texte secondaire | `#BAB1A7` | `#4E4652` | 9,1 / 7,3 |
| Ambre — Partie | `#F2B04C` | `#8A5508` | 10,2 / 5,0 |
| Rubis — Grimoire, manquables | `#F0607A` | `#B3243A` | 6,1 / 5,2 |
| Saphir — Codex | `#7FA2FF` | `#2A57C4` | 7,8 / 5,2 |
| Émeraude — Builds | `#4CCB91` | `#17704A` | 9,4 / 4,9 |
| Améthyste — Outils | `#B88AF7` | `#6E3BBC` | 7,4 / 5,6 |
| Turquoise — Progression ; rose — Compagnons | | | |

Les verres eux-mêmes sont des dégradés radiaux (clair → saturé → profond) avec une texture de verre soufflé ; le texte posé sur un verre est blanc avec un liseré noir.

**Typographie.** Titres **Grenze Gotisch** (gothique lisible, à fort caractère), chiffres et lettrines en **Grenze** romaine de la même famille (les capitales gothiques I, J, P sont ambiguës : on ne les utilise pas seules) ; lecture **Spectral** (serif dessinée pour l'écran) ; données **Manrope** (grotesque nette, chiffres tabulaires).

**Ornements.** Plombs de 2–3 dp avec un filet de lumière, arcs brisés, lancettes, quatre-feuilles pour les cases à cocher, rosace pour la progression, losanges de verre pour les sections, bouton de recherche en gemme.

**Illustration.** **Vitrail en aplats** : chaque illustration est un assemblage de pièces de verre cernées de plomb. Emblèmes de classe en sceaux de cire colorés, glyphes d'école en médaillons de verre, icônes de dégâts en losanges de couleur ; en-têtes d'acte en verrières paysagères ; carte en grande verrière ; schéma des plans en rosace. Icône : rosace à huit pétales ; monochrome en réseau de plomb. Le plus spectaculaire, mais les illustrations complexes sont plus longues à dessiner.

**Matière et lumière.** **Lumière qui traverse le verre** (shader : rayon lent qui balaie la verrière, caustiques douces), grain de verre, halos de couleur sur OLED, givre procédural.

**Mouvement.** Les pièces de verre **s'assemblent** en cascade à l'ouverture ; transition liste → fiche par **coulée de lumière** ; le **spoiler est un verre givré qui dégèle** sous le doigt ; cocher un objectif **frappe un sceau de cire** (choc haptique franc) et allume un pétale de la rosace ; les dés sont des gemmes qui ricochent.

**Forces.** Impact visuel maximal, lisibilité « à bout de bras » excellente en partie (formes franches, très fort contraste), OLED idéal. **Risques.** Plus chargé pour la lecture longue (à compenser par des pages de lecture très sobres) ; police gothique à doser ; coût de production des grandes illustrations.

---

## 5. Comparaison

| Critère | A · Grimoire | B · Atlas | C · Vitrail |
|---|---|---|---|
| Lecture longue du lore | ★★★ | ★★☆ | ★★☆ |
| Coup d'œil en pleine partie | ★★☆ | ★★☆ | ★★★ |
| Originalité (loin des codes « fantasy ») | ★☆☆ | ★★★ | ★★☆ |
| Lien avec le lore (plans, dieux, magie) | ★★★ | ★★★ | ★★☆ |
| Rendu sur OLED | ★★☆ | ★★★ | ★★★ |
| Coût de production des illustrations | moyen | faible | élevé |
| Risque principal | cliché | froideur | lourdeur |

**Ma recommandation : A**, qui sert le mieux la lecture longue (le pilier lore) tout en restant claire en partie, à condition de tenir la retenue décrite plus haut. **B** est l'option la plus originale et la plus économique à produire ; **C** la plus spectaculaire sur l'écran OLED du Pixel. Un mélange cohérent est possible : **A avec le système de constellations de B** pour la cosmologie, le panthéon et les emblèmes. Le choix t'appartient.

## 6. Ce que la phase 1 produira pour la direction choisie
- Jetons Compose (couleurs sombre, OLED et clair, accents par section, typographie, formes, espacements, élévations, durées et courbes de mouvement) dans `core:designsystem`, source unique.
- Polices intégrées (sous-ensembles), ornements et emblèmes en `ImageVector` ou en dessin Compose, shaders AGSL avec repli statique.
- Composants signature : en-tête d'acte, sélecteur d'acte, carte d'alerte, ligne d'objectif cochable, médaillon/sceau, lettrine, titre de section, voile de spoiler, pastille « Tirer le fil », feuille de détail de calcul, barre de navigation avec recherche intégrée.
- Captures Roborazzi du catalogue (sombre, OLED, clair, police 200 %, largeur pliable) et revue artistique indépendante avant toute fonctionnalité.
