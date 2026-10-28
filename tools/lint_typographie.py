#!/usr/bin/env python3
"""Contrôle (et corrige avec --fix) la typographie française des chaînes d'interface.

Règles : espace fine insécable (U+202F) avant « ; : ! ? » et à l'intérieur des guillemets « » ;
apostrophe typographique (’) entre deux lettres. Les corrections sont idempotentes.
Portée : chaînes, tableaux et pluriels de tous les fichiers res/values*/strings.xml du projet.
Usage : python3 tools/lint_typographie.py [--fix]   (code de sortie 1 s'il reste des fautes)
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
NNBSP = "\u202f"
STRING = re.compile(r"(<(?:string|item)\b[^>]*>)(.*?)(</(?:string|item)>)", re.S)
RULES = [
    (re.compile(r"[ \u00a0]([;:!?])"), NNBSP + r"\1"),   # espace ordinaire avant ponctuation haute
    (re.compile(r"(?<=[\w»)])([;:!?])"), NNBSP + r"\1"),  # ponctuation collée au mot
    (re.compile(r"«[ \u00a0\u202f]?"), "«" + NNBSP),
    (re.compile(r"[ \u00a0\u202f]?»"), NNBSP + "»"),
    (re.compile(r"(?<=[^\W\d_])\\?'(?=[^\W\d_])"), "’"),  # l\'acte, l'acte → l’acte
]


def fix_text(text: str) -> str:
    # On ne touche ni aux URL, ni aux formats (%1$s), ni aux heures (21:47), ni aux schémas (x://).
    # Les entités XML (&amp; …) sont mises de côté pendant la correction.
    entities = re.findall(r"&#?\w+;", text)
    out = re.sub(r"&#?\w+;", "\x00", text)
    for pattern, repl in RULES:
        out = pattern.sub(repl, out)
    for e in entities:
        out = out.replace("\x00", e, 1)
    out = re.sub(NNBSP + r":(//|\d)", r":\1", out)
    out = re.sub(r"(%\d\$[a-z])" + NNBSP + ":", r"\1:", out)  # « %1$s: » d'un format technique
    return out


def main() -> int:
    fix = "--fix" in sys.argv
    faults = 0
    for f in ROOT.glob("**/src/main/res/values*/strings.xml"):
        if ".android-sdk" in f.parts:
            continue
        src = f.read_text(encoding="utf-8")
        def repl(m):
            nonlocal faults
            new = fix_text(m.group(2))
            if new != m.group(2):
                faults += 1
                print(f"{f.relative_to(ROOT)} : {m.group(2)[:70]!r}")
            return m.group(1) + (new if fix else m.group(2)) + m.group(3)
        out = STRING.sub(repl, src)
        if fix and out != src:
            f.write_text(out, encoding="utf-8")
    print(("Corrigées : " if fix else "Fautes : ") + str(faults))
    return 0 if (fix or faults == 0) else 1


if __name__ == "__main__":
    sys.exit(main())
