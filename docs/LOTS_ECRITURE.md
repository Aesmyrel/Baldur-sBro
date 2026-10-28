# Lots d'écriture — guide des rédacteurs

> Pour les agents (ou personnes) qui proposent des noms français et rédigent les effets des fiches de jeu.
> Règles du projet : `CLAUDE.md` (non négociables), `docs/SOURCES.md`, `docs/DECISIONS.md` (D-055, D-057, D-058, D-062).

## 1. Ce qu'on reçoit, ce qu'on rend

- **Entrée** : `tools/cache/worklists/lots/<lot>.json` (non versionné). Chaque ligne donne l'identifiant, le nom anglais, la page et la révision de bg3.wiki, l'étiquette de spoiler calculée, les faits structurés et, **pour contexte seulement**, quelques paramètres de la page (`contexte_wiki_ne_pas_recopier`).
- **Lexique** : `tools/cache/worklists/lots/lexique-dnd-vo-vf.json` (800 paires VO → VF de la 5e édition de D&D, vocabulaire officiel) et `tools/cache/lexiques/srd-5.2.1-fr.txt` (texte du SRD 5.2.1 en français, pour vérifier un terme de règles).
- **Sortie** :
  - noms : `data/i18n/noms/<lot>.yaml`
  - effets : `data/effets/<lot>.yaml`
  - un fichier par lot, en UTF-8, au format YAML décrit plus bas.
- **Ne jamais modifier** autre chose que son propre fichier de sortie.

## 2. Droits : ce qui est interdit

- **Aucun texte du jeu ni du wiki recopié**, ni traduit phrase à phrase. Les descriptions du contexte servent à comprendre la mécanique, puis on écrit autrement : on part des faits structurés (chiffres, durées, sauvegardes, états) et on les présente dans notre propre forme.
- Aucune citation, aucune réplique, aucun texte d'ambiance (`quote`), aucune note ou anecdote du wiki.
- Pas de théorie, pas d'invention : une mécanique qu'on ne comprend pas avec certitude ne s'écrit pas. On laisse l'entrée de côté et on la liste en fin de rapport.

## 3. Noms français

```yaml
item.amulet-of-misty-step:
  fr: Amulette de foulée brumeuse
  statut: NON_OFFICIELLE
spell.mage-armour:
  fr: Armure de mage
  statut: VF_DND
  source: aidedd
condition.blinded:
  fr: Aveuglé
  statut: VF_DND
  source: srd-5.2.1-fr
```

- `statut` :
  - `VF_DND` : seulement si le terme exact figure dans le lexique officiel (`source: aidedd`) ou dans le SRD 5.2.1 français (`source: srd-5.2.1-fr`), avec le même sens ; on vérifie dans le fichier.
  - `NON_OFFICIELLE` : tout le reste. **Jamais `VF_BG3`** : seule la VF du jeu, vue en jeu, peut le justifier.
- **Cohérence avec la VF de D&D** : un nom composé reprend le terme officiel de chaque élément (« Amulet of Misty Step » → « Amulette de foulée brumeuse », puisque le lexique donne « Misty Step » = « Foulée brumeuse »). Ordre et accords du français (« Gants de force », pas « Force gants »).
- **Noms propres** : un nom propre de personnage ou une création de BG3 sans équivalent officiel reste tel quel (« Phalar Aluve »). Un lieu des Royaumes Oubliés qui a un nom officiel en français ne prend ce statut que s'il est vérifiable dans le lexique ; sinon `NON_OFFICIELLE`.
- **Typographie** : majuscule au premier mot seulement (« Anneau de protection »), sauf nom propre ; apostrophe typographique ’ ; aucun guillemet.
- **Spoilers** : si le nom lui-même laisse deviner un rebondissement (personnage dont l'identité est une surprise, issue d'une quête), ajouter une étiquette plus stricte que celle de la ligne :
  ```yaml
  item.exemple:
    fr: …
    statut: NON_OFFICIELLE
    etiquette: { act: acte2, level: leger, otherWorks: aucun }
  ```
  Valeurs : `act` parmi `prologue`, `acte1`, `acte2`, `acte3`, `epilogue` ; `level` et `otherWorks` parmi `aucun`, `leger`, `majeur`.
- **En cas de doute** sur un nom révélateur : étiquette au moins `leger`.

## 4. Effets rédigés

```yaml
feature.absolutes-bane:
  effets:
    - "Coup d’arme réussi : la cible fait un jet de sauvegarde de Charisme (DD 11) ou subit l’état Fléau pendant 2 tours."
  notes:
    - "Ne se cumule pas avec le sort Fléau."
  revision: 439185
```

- `revision` : la révision de la page indiquée dans le lot (obligatoire).
- **Style télégraphique et factuel** :
  - une ligne par effet, au présent ;
  - on commence par le déclencheur (« Coup d’arme réussi : », « Une fois par repos long : », « Tant que l’objet est porté : ») ;
  - les chiffres sont ceux des faits structurés ;
  - termes de règles de la VF de D&D (lexique, SRD) : « jet de sauvegarde de Dextérité », « avantage », « désavantage », « classe d’armure », « dégâts de feu », « action bonus », « réaction », « repos court », « repos long », « tour ».
- On **décrit la mécanique, sans l'ambiance** : pas d'adjectif décoratif, pas de « vous sentez… ».
- **Notes** : seulement une précision mécanique utile et sûre (non-cumul, limite). Aucune anecdote, aucun bogue, aucun lieu ni personnage.
- **États** : ce que l'état fait à la créature qui le subit, et sa fin si les faits la donnent.
- **Spoilers** : un effet qui nomme un lieu, un personnage ou une quête est déconseillé ; s'il le faut, ajouter `etiquette` (au moins `leger`, à l'acte concerné).
- **Typographie française** : espace fine insécable (U+202F) avant `; : ! ?` et à l'intérieur des guillemets `« »` ; apostrophe ’ ; nombres en chiffres ; signe moins « − » (U+2212) pour un malus (−1d4).

## 5. Contrôle avant de rendre

- Le YAML se relit (`python3 -c "import yaml,sys; yaml.safe_load(open(sys.argv[1]))" fichier`).
- Chaque identifiant vient du lot, une seule fois.
- Aucune phrase de `contexte_wiki_ne_pas_recopier` n'apparaît, même traduite mot à mot.
- Fin de rapport :
  - ce qui a été laissé de côté et pourquoi ;
  - les noms jugés révélateurs ;
  - les doutes de terminologie.
