#!/usr/bin/env python3
"""Contrôle d'un fichier d'effets rédigés (data/effets/<lot>.yaml) contre son lot d'écriture.

Vérifie : YAML lisible, identifiants du lot (une fois chacun), révision de la page, typographie
française (espaces fines insécables, apostrophe, signe moins), longueur des lignes, absence de texte
anglais recopié du contexte du wiki (suite de 5 mots identiques).

Usage : python3 tools/lots/check_effets.py data/effets/<fichier>.yaml tools/cache/worklists/lots/<lot>.json [début fin]
        (lot `tout.json` : contrôle de n'importe quel fichier, sans liste des entrées manquantes)
"""
import json
import re
import sys

import yaml

LEVELS = {"aucun", "leger", "majeur"}
ACTS = {"prologue", "acte1", "acte2", "acte3", "epilogue"}


def words(text):
    return re.findall(r"[a-z']+", text.lower())


def main():
    out_path, lot_path = sys.argv[1], sys.argv[2]
    lot = json.load(open(lot_path, encoding="utf-8"))
    if len(sys.argv) == 5:
        lot = lot[int(sys.argv[3]):int(sys.argv[4])]
    by_id = {row["id"]: row for row in lot}
    data = yaml.safe_load(open(out_path, encoding="utf-8")) or {}
    problems = []
    for key, value in data.items():
        row = by_id.get(key)
        if row is None:
            problems.append(f"{key} : absent du lot")
            continue
        if not isinstance(value, dict) or set(value) - {"effets", "notes", "etiquette", "revision"}:
            problems.append(f"{key} : champs inattendus {sorted(set(value) - {'effets', 'notes', 'etiquette', 'revision'})}")
            continue
        if value.get("revision") != row["revision"]:
            problems.append(f"{key} : révision {value.get('revision')} au lieu de {row['revision']}")
        tag = value.get("etiquette")
        if tag is not None and (tag.get("act") not in ACTS or tag.get("level") not in LEVELS or tag.get("otherWorks") not in LEVELS):
            problems.append(f"{key} : étiquette invalide {tag}")
        lines = list(value.get("effets") or []) + list(value.get("notes") or [])
        if not value.get("effets"):
            problems.append(f"{key} : aucun effet")
        context = " ".join(str(v) for v in row.get("contexte_wiki_ne_pas_recopier", {}).values())
        grams = {tuple(w[i:i + 5]) for w in [words(context)] for i in range(len(w) - 4)}
        for line in lines:
            if not isinstance(line, str) or not line.strip():
                problems.append(f"{key} : ligne vide")
                continue
            if re.search(r"[ ][;:!?»]|«[ ]", line):
                problems.append(f"{key} : espace ordinaire autour d'une ponctuation haute : {line}")
            if re.search(r"(?<=\S)[;:!?]", line.replace(" ", "").replace("://", "")) and not re.search(r" [;:!?]", line):
                problems.append(f"{key} : espace fine insécable manquante : {line}")
            if "'" in line:
                problems.append(f"{key} : apostrophe droite : {line}")
            if re.search(r"(^|[\s(])-\s?\d", line):
                problems.append(f"{key} : trait d'union au lieu du signe moins (−) : {line}")
            if len(line) > 260:
                problems.append(f"{key} : ligne trop longue ({len(line)} signes)")
            w = words(line)
            if any(tuple(w[i:i + 5]) in grams for i in range(len(w) - 4)):
                problems.append(f"{key} : suite de mots reprise du contexte du wiki : {line}")
    # Avec le lot de référence `tout.json`, on ne contrôle que les entrées du fichier.
    missing = [] if lot_path.endswith("tout.json") else [i for i in by_id if i not in data]
    for p in problems:
        print("✗", p)
    print(f"{len(data)} entrées, {len(missing)} du lot sans effets, {len(problems)} problème(s)")
    if missing:
        print("Sans effets :", ", ".join(missing))
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
