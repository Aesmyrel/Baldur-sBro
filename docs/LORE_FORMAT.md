# Codex — guide de rédaction des fiches de lore

> Pour les rédacteurs (agents ou personnes) du codex. Le brief (`docs/BRIEF.md`) et `CLAUDE.md` font foi ;
> sources et licences : `docs/SOURCES.md` ; arborescence et objectifs : `docs/PLAN.md` §7.

## 1. Principes

- **Exactitude avant quantité.** On n'écrit que ce qu'une source dit. Champ inconnu → on ne l'écrit pas.
- **Nos mots, en synthèse.** Chaque paragraphe est rédigé par nous à partir d'au moins une source, idéalement plusieurs ; jamais une copie, jamais une paraphrase serrée d'un seul texte (structure, ordre des idées et formulations doivent être les nôtres).
- **Chaque paragraphe** porte :
  - au moins une référence ;
  - une **nature** :
    - `canon_bg3` : ce que montre le jeu ;
    - `canon_dnd` : ce que disent les publications de D&D, avec l'`edition` d'où vient l'affirmation (`add1`, `add2`, `3e`, `3.5`, `4e`, `5e-2014`, `5e-2024`, `multi`) ;
    - `interpretation` : notre lecture, clairement signalée, et jamais une théorie de fan ;
    - `hors_univers` : un fait du monde réel (parution, éditeur, auteurs, accueil d'une œuvre), jamais du lore.
- **Versions contradictoires** (le lore a varié d'une édition à l'autre) : deux paragraphes ou plus dans la même section, avec la même valeur de `variantes`, présentés côte à côte, sans trancher.
- **Théories de fans exclues.**

### 1 bis. Méthode de rédaction (D-080, après la relecture des pilotes)

1. **Notes d'abord, page fermée.** Relever les faits dans une liste, de plusieurs sources ou sections, puis fermer les pages et rédiger depuis la liste, avec un plan propre à la fiche. Chaque paragraphe croise au moins deux sections ou deux pages. On ne suit jamais l'ordre des phrases d'une source, et on ne traduit jamais une expression marquante.
2. **Une fiche, une licence de travail.** Les sections générales s'appuient sur le Forgotten Realms Wiki et les livres qu'il cite ; la section `bg3` s'écrit à partir des faits structurés de `data/game` et de ce qu'on constate en jeu, jamais à partir de la prose de bg3.wiki (CC BY-NC-SA), qu'on ne cite qu'en référence.
3. **Section `bg3` sans soluce.** Pas de DD, de points de vie ni de nombre de tours : une relation vers la fiche de jeu suffit. Une même scène n'est racontée que dans une seule fiche.
4. **Livres du jeu.** Décrire l'objet, où on le trouve et son thème en une ou deux phrases ; jamais son contenu chant par chant ou chapitre par chapitre.
5. **Pas de commentaire sur les sources dans le texte** (« le wiki », « cette fiche », « les pages consultées ») : une version s'attribue à l'ouvrage primaire qui la porte, et les wikis restent dans les références.
6. **`ecarts` pour les variations du canon seulement.** Un désaccord entre sources secondaires sur un fait réel se règle en une phrase avec une fourchette. La conviction d'un personnage n'est pas une variante.
7. **Une liste d'apparitions est `hors_univers`.**
8. **Une causalité ou une étymologie** n'est écrite que si la source la donne.
9. **Au moins une relation par fiche** ; les alias sont seulement d'autres noms du même sujet.
10. **Registre** : une entrée par page qui cite le livre (`<page>/<livre>`) ; éditeur et année repris du modèle de citation de la page ; `edition` toujours renseignée, y compris pour un roman ou un jeu (l'édition de son époque) ; licences notées `cc-by-sa-3.0` (Fandom), `cc-by-sa-4.0` (Wikipédia), `cc-by-nc-sa-4.0` (bg3.wiki) ; `titre_vf` seulement s'il est vérifié sur Wikipédia FR ou chez l'éditeur.
11. **Typographie** : « Acte I » ; « n° » ; titres anglais avec deux-points sans espace (`Forgotten Realms: Heroes of Faerûn`), comme sur la couverture ; noms des Royaumes en VF attestée, la VO entre parenthèses à la première occurrence.

### 1 ter. Rédaction en salle blanche (D-082)

La méthode du §1 bis ne suffit pas quand un même rédacteur lit la source puis écrit : la seconde relecture a encore trouvé un tiers de paragraphes en paraphrase serrée. Le codex s'écrit donc en deux temps, par deux rédacteurs distincts :

1. **Extraction** (`tools/cache/lore-notes/<fiche>.yaml`, non versionné). Un premier rédacteur lit les sources et relève des **faits atomiques**, en français télégraphique : au plus 20 mots, aucune phrase ni image de la source, aucune expression marquante. Chaque fait porte sa référence, sa page éventuelle, sa nature, son édition et, s'il appartient à une version divergente, son groupe de variantes. Les faits sont rangés par ordre alphabétique, jamais dans l'ordre de la source. Pour la section `bg3`, seuls les faits structurés de `data/game` sont repris, avec l'identifiant de la fiche de jeu. Les métadonnées de la fiche (identifiant, nom, alias, étiquettes, relations) sont recopiées telles quelles.
2. **Rédaction**. Un second rédacteur écrit la fiche **à partir des seules notes** et de `data/game`. Il n'ouvre ni les sources (`tools/cache/lore/`, `tools/cache/bg3wiki/`, sites), ni l'ancienne version de la fiche. Il choisit son plan, regroupe les faits par idée et les relie avec ses mots.
3. **Relecture** indépendante, comme avant (§1 bis et droits).

## 2. Spoilers : étiquettes

Chaque fiche a une étiquette de **titre** (ce que révèle le nom dans une liste), une étiquette de **fiche** (l'accroche) et une étiquette par **section**. Une section ne protège jamais moins que sa fiche, une fiche jamais moins que son titre.

- `act` : le moment de la partie où le joueur peut l'apprendre (`prologue`, `acte1`, `acte2`, `acte3`, `epilogue`).
- `level` (spoilers de BG3) :
  - `aucun` : descriptif ;
  - `leger` : laisse deviner, donne un indice ;
  - `majeur` : révèle un rebondissement ou une issue.
- `otherWorks` : même échelle, pour les spoilers des autres œuvres (jeux précédents, romans, aventures).
- **Une information qui laisse deviner un rebondissement est au moins `leger`.** Dans le doute, plus strict.
- La section `general` (le sujet dans D&D) est en général `aucun` pour BG3, mais peut gâcher un roman ou un jeu précédent (`otherWorks`).
- La section `bg3` porte l'acte où l'on rencontre ce qu'elle raconte.
- Le **nom** d'une fiche peut être un indice : si c'est le cas, l'étiquette de titre le dit.

## 3. Format (`content/lore/<thème>/<fiche>.yaml`)

```yaml
id: lore.peuples.exemple          # « lore. », puis le thème, puis le nom anglais en minuscules et tirets
theme: peuples                    # pantheon, cosmologie, peuples, bestiaire, factions, lieux, personnages,
                                  # objets, chronologie, saga, livres, plus-loin
nom: { fr: Exemple, en: Example, statut: VF_DND, source: aidedd }   # statuts : docs/LOTS_ECRITURE.md §3
alias: [Autre nom]
etiquette_titre: { act: prologue, level: aucun, otherWorks: aucun }
etiquette: { act: prologue, level: aucun, otherWorks: aucun }
resume: Une phrase d’accroche, avec nos mots.
sections:
  - type: general                 # general, bg3, autres_oeuvres, ecarts, plus_loin
    titre: Origines
    etiquette: { act: prologue, level: aucun, otherWorks: aucun }
    paragraphes:
      - texte: Un paragraphe rédigé par nous…
        nature: canon_dnd
        edition: multi
        refs: [ { ref: frwiki-exemple }, { ref: mtof, page: "88" } ]
relations:
  - { cible: lore.peuples.autre, type: rival_of }   # worships, ally_of, rival_of, member_of, leads,
                                                     # located_in, parent_of, created_by, appears_in,
                                                     # mentioned_in, see_also
```

- Une relation peut viser une fiche de jeu (`item.…`, `spell.…`, `place.…`) si elle existe dans `data/game`.
- Sections : 2 à 5 par fiche, paragraphes de 60 à 160 mots ; une fiche « socle » fait 300 à 900 mots.

## 4. Références (`content/references/<lot>.yaml`)

```yaml
frwiki-exemple:
  type: wiki                      # livre, aventure, roman, jeu, magazine, wiki, site
  titre_vo: "Example (Forgotten Realms Wiki)"
  url: https://forgottenrealms.fandom.com/wiki/Example
  revision: 123456                # identifiant de révision de la page consultée (obligatoire pour un wiki)
  licence: cc-by-sa
  consultation: directe
mtof:
  type: livre
  titre_vo: Mordenkainen's Tome of Foes
  editeur: Wizards of the Coast
  annee: 2018
  edition: 5e-2014
  consultation: secondaire        # connue par une source secondaire, pas consultée nous-mêmes
  via: frwiki-exemple
```

- Un livre ou une aventure qu'on n'a pas lus nous-mêmes : `consultation: secondaire` et `via` vers la page qui le cite (avec sa page ou son chapitre si elle le donne).
- **Révision d'une page de wiki** : avec l'API MediaWiki, par exemple
  `https://forgottenrealms.fandom.com/api.php?action=query&prop=revisions&rvprop=ids&titles=<Titre>&format=json`.
- Titres VF des livres et romans : seulement s'ils sont vérifiés (Wikipédia FR, éditeur). Le nom d'une fiche `saga` ou `plus-loin` dont le titre VF est vérifié sur Wikipédia FR prend `statut: VF_DND, source: wikipedia-fr`. Pour les autres thèmes, un nom des Royaumes attesté seulement par un wiki reste `NON_OFFICIELLE`.
- Un identifiant décrit dans deux fichiers doit l'être à l'identique.

## 5. Typographie et ton

- Français soigné, sobre, au présent de narration ; vouvoiement dans les phrases adressées au lecteur (rare).
- Noms des Royaumes : traductions officielles de D&D quand elles sont vérifiées, nom anglais entre parenthèses à la première occurrence si utile.
- Espace fine insécable (U+202F) avant `; : ! ?` et dans les guillemets `« »` ; apostrophe ’ ; années en « 1492 CV ».

## 6. Contrôle avant de rendre

`./gradlew :tools:pipeline:run --args="validate"` doit passer, sans erreur sur vos fiches.
