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
    - `interpretation` : notre lecture, clairement signalée, et jamais une théorie de fan.
- **Versions contradictoires** (le lore a varié d'une édition à l'autre) : deux paragraphes ou plus dans la même section, avec la même valeur de `variantes`, présentés côte à côte, sans trancher.
- **Théories de fans exclues.**

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
- Titres VF des livres et romans : seulement s'ils sont vérifiés (Wikipédia FR, éditeur).
- Un identifiant décrit dans deux fichiers doit l'être à l'identique.

## 5. Typographie et ton

- Français soigné, sobre, au présent de narration ; vouvoiement dans les phrases adressées au lecteur (rare).
- Noms des Royaumes : traductions officielles de D&D quand elles sont vérifiées, nom anglais entre parenthèses à la première occurrence si utile.
- Espace fine insécable (U+202F) avant `; : ! ?` et dans les guillemets `« »` ; apostrophe ’ ; années en « 1492 CV ».

## 6. Contrôle avant de rendre

`./gradlew :tools:pipeline:run --args="validate"` doit passer, sans erreur sur vos fiches.
