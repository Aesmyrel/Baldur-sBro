#!/usr/bin/env python3
"""Prépare les lots d'écriture (noms français, effets rédigés) à partir des données normalisées.

Entrées : data/game/*.json (versionné), tools/cache/bg3wiki/*.json et tools/cache/lexiques/ (non versionnés).
Sorties : tools/cache/worklists/lots/<lot>.json (non versionné : contient, pour contexte seulement,
le texte du wiki, qui ne doit jamais être recopié). Un rédacteur produit ensuite data/i18n/noms/<lot>.yaml
ou data/effets/<lot>.yaml (versionnés), relus par le pipeline.

Usage : python3 tools/lots/export_lots.py
"""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
CACHE = ROOT / "tools" / "cache"
OUT = CACHE / "worklists" / "lots"

COLLECTION = {"spell": "feature", "feature": None, "item": None, "condition": "condition", "place": "location"}


def load_entries():
    entries = []
    for f in sorted((ROOT / "data" / "game").glob("*.json")):
        entries += json.loads(f.read_text(encoding="utf-8"))
    return entries


def load_pages():
    pages = {}
    for f in sorted((CACHE / "bg3wiki").glob("*.json")):
        for title, page in json.loads(f.read_text(encoding="utf-8")).items():
            pages[title] = page
    return pages


def template_params(text):
    """Paramètres de premier niveau du premier modèle de page (lecture sommaire, pour contexte)."""
    m = re.search(r"\{\{\s*(Feature page|Passive feature page|Equipment ?page|WeaponPage|Condition page|Location page)", text, re.I)
    if not m:
        return {}
    i, depth = m.start() + 2, 1
    j = i
    while j < len(text) and depth:
        if text.startswith("{{", j): depth += 1; j += 2
        elif text.startswith("}}", j): depth -= 1; j += 2
        else: j += 1
    body = text[i:j - 2]
    parts, cur, d, k = [], "", 0, 0
    while k < len(body):
        two = body[k:k + 2]
        if two in ("{{", "[["): d += 1; cur += two; k += 2; continue
        if two in ("}}", "]]") and d: d -= 1; cur += two; k += 2; continue
        if body[k] == "|" and d == 0: parts.append(cur); cur = ""; k += 1; continue
        cur += body[k]; k += 1
    parts.append(cur)
    out = {}
    for p in parts[1:]:
        if "=" in p:
            key, val = p.split("=", 1)
            out[key.strip()] = re.sub(r"<!--.*?-->", "", val, flags=re.S).strip()
    return out


CONTEXT_KEYS = ["description", "summary", "effects", "special", "condition", "condition duration", "condition save", "condition dc", "type", "rarity", "region", "act"]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    entries = load_entries()
    pages = load_pages()
    lots = {}
    for e in entries:
        title = e["attribution"][0]["title"]
        params = template_params(pages.get(title, {}).get("text", ""))
        context = {k: params[k][:600] for k in CONTEXT_KEYS if params.get(k)}
        item = {
            "id": e["id"], "type": e["type"], "en": e["name"]["en"], "fr_actuel": e["name"]["fr"], "statut": e["name"]["status"],
            "page": title, "revision": e["attribution"][0]["revision"],
            "etiquette": e["tag"], "faits": e["facts"], "contexte_wiki_ne_pas_recopier": context,
        }
        if e["name"]["status"] == "UNOFFICIAL":
            lots.setdefault(f"noms-{e['type']}", []).append(item)
        if "effect_text" in e.get("unknown", []) or "effects" in e.get("unknown", []):
            lots.setdefault(f"effets-{e['type']}", []).append(item)
    for name, items in sorted(lots.items()):
        (OUT / f"{name}.json").write_text(json.dumps(items, ensure_ascii=False, indent=1), encoding="utf-8")
        print(f"{name} : {len(items)}")
    # Lexique officiel D&D (vocabulaire seulement), pour des traductions cohérentes avec la VF de D&D.
    lexique = {}
    for f in sorted((CACHE / "lexiques").glob("aidedd-*.html")):
        html = f.read_text(encoding="utf-8")
        for row in re.findall(r"<tr>(.*?)</tr>", html, re.S):
            vf = re.search(r'<td class="item"><a [^>]*>(.*?)</a>', row)
            vo = re.search(r'<td class="colVO">(.*?)</td>', row)
            if vf and vo and vo.group(1).strip():
                lexique[re.sub(r"<[^>]+>", "", vo.group(1)).strip()] = re.sub(r"<[^>]+>", "", vf.group(1)).strip()
    (OUT / "lexique-dnd-vo-vf.json").write_text(json.dumps(dict(sorted(lexique.items())), ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"lexique : {len(lexique)} paires")


if __name__ == "__main__":
    main()
