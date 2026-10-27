#!/usr/bin/env python3
"""Télécharge les polices libres (SIL OFL) utilisées par les maquettes de phase 0.

Les fichiers vont dans tools/design/.fonts/ (non versionné). Les polices viennent
de Google Fonts, toutes sous licence SIL Open Font License 1.1.
Usage : python3 tools/design/fetch_fonts.py
"""
import pathlib
import re
import urllib.request

FAMILIES = {
    # Direction A — Grimoire enluminé
    "Almendra": "ital,wght@0,400;0,700;1,400",
    "Almendra SC": "wght@400",
    "EB Garamond": "ital,wght@0,400;0,500;0,600;1,400",
    "Alegreya Sans": "wght@400;500;700",
    "Alegreya Sans SC": "wght@500;700",
    # Direction B — Atlas céleste
    "Bodoni Moda": "ital,opsz,wght@0,6..96,400;0,6..96,600;1,6..96,400",
    "Newsreader": "ital,opsz,wght@0,6..72,400;0,6..72,500;1,6..72,400",
    "Jost": "wght@300;400;500;600",
    # Direction C — Vitrail et sceaux
    "Grenze Gotisch": "wght@500;700;800",
    "Grenze": "wght@600;700;800",
    "Spectral": "ital,wght@0,400;0,500;0,600;1,400",
    "Manrope": "wght@400;500;600;700;800",
}

UA = ("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
      "Chrome/130.0 Safari/537.36")
OUT = pathlib.Path(__file__).resolve().parent / ".fonts"


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def main() -> None:
    OUT.mkdir(exist_ok=True)
    css_all = []
    for family, axes in FAMILIES.items():
        q = family.replace(" ", "+")
        css = fetch(f"https://fonts.googleapis.com/css2?family={q}:{axes}&display=block").decode()
        # Ne garder que les sous-ensembles latin et latin-ext (accents français).
        blocks = re.findall(r"/\* ([\w-]+) \*/\s*(@font-face \{.*?\})", css, re.S)
        for subset, block in blocks:
            if subset not in ("latin", "latin-ext"):
                continue
            url = re.search(r"url\((https://[^)]+)\)", block).group(1)
            name = url.rsplit("/", 1)[-1]
            path = OUT / name
            if not path.exists():
                path.write_bytes(fetch(url))
            css_all.append(block.replace(url, name))
        print("ok", family)
    (OUT / "fonts.css").write_text("\n".join(css_all))


if __name__ == "__main__":
    main()
