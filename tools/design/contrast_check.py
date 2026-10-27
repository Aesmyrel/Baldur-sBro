#!/usr/bin/env python3
"""Mesure le contraste WCAG de chaque jeton de texte des maquettes contre chaque fond.

Lit les palettes `PAL = { dark: {...}, light: {...} }` des pages docs/design/src/*.html
et écrit docs/design/contrastes.md. Seuil : 4,5:1 pour tout jeton susceptible de porter du texte.
Usage : python3 tools/design/contrast_check.py
"""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[2]
SRC = ROOT / "docs" / "design" / "src"
OUT = ROOT / "docs" / "design" / "contrastes.md"
BACKGROUNDS = ("bg", "surface", "raised", "navbg")
NOT_TEXT = {"line", "line2", "lead", "lead2", "leadHi", "gold2", "brass2", "star", "candle", "grain", "onGlass", "onGlass2", "frostText", "light"}


def luminance(hex_color: str) -> float:
    h = hex_color.lstrip("#")
    def channel(c: float) -> float:
        return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    r, g, b = (channel(int(h[i:i + 2], 16) / 255) for i in (0, 2, 4))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a: str, b: str) -> float:
    la, lb = sorted((luminance(a), luminance(b)), reverse=True)
    return (la + 0.05) / (lb + 0.05)


def palettes(html: str) -> dict:
    block = html[html.index("const PAL"):]
    out = {}
    for theme in ("dark", "light"):
        body = re.search(theme + r"\s*:\s*\{(.*?)\}", block, re.S).group(1)
        out[theme] = dict(re.findall(r'(\w+):"(#[0-9A-Fa-f]{6})"', body))
    return out


def main() -> None:
    lines = ["# Contrastes mesurés (WCAG 2.x)", "",
             "Généré par `tools/design/contrast_check.py` à partir des palettes des maquettes.",
             "Valeur = contraste **minimal** du jeton contre tous les fonds de son thème (bg, surface, raised, navbg). Seuil 4,5:1.", ""]
    failures = 0
    for page in sorted(SRC.glob("[abc]-*.html")):
        for theme, pal in palettes(page.read_text()).items():
            bgs = [pal[k] for k in BACKGROUNDS if k in pal]
            rows = []
            for name, color in pal.items():
                if name in BACKGROUNDS or name in NOT_TEXT:
                    continue
                worst = min(contrast(color, bg) for bg in bgs)
                ok = worst >= 4.5
                failures += not ok
                rows.append(f"| `{name}` | `{color}` | {worst:.2f} | {'oui' if ok else '**NON**'} |")
            lines += [f"## {page.stem} — {theme}", "", "| Jeton | Couleur | Contraste minimal | ≥ 4,5 |", "|---|---|---|---|", *rows, ""]
    # Texte posé sur un verre ou sur une pastille colorée (direction C) : chaque paire réellement utilisée.
    pairs = [
        ("Bouclier « Int » : chiffre blanc sur verre saphir profond (teinte claire)", "#FFFFFF", "#4166C8"),
        ("Bouclier « Int » : chiffre blanc sur verre saphir profond (teinte moyenne)", "#FFFFFF", "#2A4CB0"),
        ("Boucliers : chiffre blanc sur verre nuit (teinte claire)", "#FFFFFF", "#3E4C8C"),
        ("Rosace : « 42 » sombre sur verre ambre", "#2A1A04", "#F2AB3C"),
        ("Pastille « Acte I » : texte sombre sur ambre (sombre)", "#120E14", "#F2B04C"),
        ("Pastille « Manquable » : texte sombre sur rubis (sombre)", "#120E14", "#F0607A"),
        ("Pastille « Lieu » : texte sombre sur texte secondaire (sombre)", "#120E14", "#BAB1A7"),
        ("Givre : texte sombre sur verre givré", "#1C1922", "#C9D3E6"),
    ]
    lines += ["## Texte sur verre et pastilles (direction C)", "", "| Usage | Texte | Fond | Contraste | ≥ 4,5 |", "|---|---|---|---|---|"]
    for label, fg, bg in pairs:
        c = contrast(fg, bg)
        failures += c < 4.5
        lines.append(f"| {label} | `{fg}` | `{bg}` | {c:.2f} | {'oui' if c >= 4.5 else '**NON**'} |")
    lines += ["", "En thème clair, les pastilles de C prennent une version claire du verre avec un texte sombre, ou le verre profond avec un texte blanc (à mesurer en phase 1).", ""]
    lines.append(f"Échecs : {failures}.")
    OUT.write_text("\n".join(lines) + "\n")
    print(f"{OUT.relative_to(ROOT)} écrit — échecs : {failures}")


if __name__ == "__main__":
    main()
