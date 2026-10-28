#!/usr/bin/env python3
"""Télécharge et allège les polices de la direction A, puis les place dans core:designsystem.

Polices : Almendra, Almendra SC, EB Garamond, Alegreya Sans, Alegreya Sans SC (toutes SIL OFL 1.1).
Sous-ensemble : latin, latin-1, latin étendu A, ponctuation générale, symboles utiles ; les fonctionnalités
OpenType (crénage, ligatures, chiffres, petites capitales) sont gardées.
Typographie française : espace fine insécable (U+202F, glyphe vide de 55 % de l'espace-mot), espace
insécable (U+00A0) et points de suspension (U+2026) ajoutés quand il le faut, pour que le texte ne passe
jamais par une police de repli.
Licence : Almendra déclare un nom de police réservé (OFL §3). Nos versions sous-ensemblées sont donc
renommées « Carnet Titres » / « Carnet Titres SC » ; EB Garamond et Alegreya Sans n'en déclarent pas.
Les licences OFL sont copiées dans les assets pour l'écran « Crédits et licences ».

Prérequis : pip install fonttools
Usage : python3 tools/design/fetch_app_fonts.py
"""
import io
import pathlib
import re
import urllib.request

from fontTools import subset
from fontTools.ttLib import TTFont

ROOT = pathlib.Path(__file__).resolve().parents[2]
FONT_DIR = ROOT / "core" / "designsystem" / "src" / "main" / "res" / "font"
LICENSE_DIR = ROOT / "core" / "designsystem" / "src" / "main" / "assets" / "licences" / "polices"

# famille Google Fonts -> (dossier OFL du dépôt google/fonts, [(style, poids, nom de ressource)])
FAMILIES = {
    "Almendra": ("almendra", [("normal", 400, "titres_regular"), ("normal", 700, "titres_bold"), ("italic", 400, "titres_italic")]),
    "Almendra SC": ("almendrasc", [("normal", 400, "titres_sc_regular")]),
    "EB Garamond": ("ebgaramond", [("normal", 400, "eb_garamond_regular"), ("normal", 500, "eb_garamond_medium"),
                                   ("normal", 600, "eb_garamond_semibold"), ("italic", 400, "eb_garamond_italic")]),
    "Alegreya Sans": ("alegreyasans", [("normal", 400, "alegreya_sans_regular"), ("normal", 500, "alegreya_sans_medium"),
                                       ("normal", 700, "alegreya_sans_bold")]),
    "Alegreya Sans SC": ("alegreyasanssc", [("normal", 500, "alegreya_sans_sc_medium"), ("normal", 700, "alegreya_sans_sc_bold")]),
}

UNICODES = (
    "U+0000-024F,"      # latin de base, latin-1, latin étendu A et B
    "U+02B0-02FF,"      # lettres modificatives (apostrophe typographique)
    "U+0300-036F,"      # diacritiques combinants
    "U+1E00-1EFF,"      # latin étendu additionnel
    "U+2000-206F,"      # ponctuation générale (dont U+202F espace fine insécable)
    "U+20AC,U+2116,U+2122,U+2190-2199,U+2212,U+2215,U+221E,U+2248,U+2260,U+2264,U+2265,U+25C6,U+2605,U+2726"
)
# Familles à nom réservé : nom d'origine → nom de notre version modifiée.
RENAMED = {"Almendra SC": "Carnet Titres SC", "Almendra": "Carnet Titres"}

FEATURES = ["kern", "liga", "clig", "dlig", "calt", "onum", "lnum", "tnum", "pnum", "smcp", "c2sc", "case", "frac", "sups", "subs", "ordn", "locl", "mark", "mkmk", "ccmp"]


def fetch(url: str, ua: str = "curl/8.5") -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": ua})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def ttf_urls(family: str) -> dict:
    q = family.replace(" ", "+")
    css = fetch(f"https://fonts.googleapis.com/css2?family={q}:ital,wght@0,400;0,500;0,600;0,700;1,400").decode()
    out = {}
    for block in re.findall(r"@font-face \{(.*?)\}", css, re.S):
        style = re.search(r"font-style: (\w+)", block).group(1)
        weight = int(re.search(r"font-weight: (\d+)", block).group(1))
        out[(style, weight)] = re.search(r"url\((https://[^)]+\.ttf)\)", block).group(1)
    return out


def add_glyph(font: TTFont, name: str, glyph, advance: int) -> None:
    order = font.getGlyphOrder()
    if name in order:
        return
    font.setGlyphOrder(order + [name])
    font["glyf"].glyphs[name] = glyph
    font["glyf"].glyphOrder = font.getGlyphOrder()
    font["hmtx"].metrics[name] = (advance, 0)


def ensure_spaces(font: TTFont) -> None:
    """Typographie française : U+00A0, U+202F et U+2026 si la police d'origine ne les a pas.

    L'espace fine insécable est un glyphe vide dédié de 55 % de l'espace-mot (environ 1/8 de cadratin) :
    l'espace fine (U+2009) des graisses grasses est souvent plus large qu'une espace-mot.
    Les points de suspension manquants sont composés de trois points.
    """
    from fontTools.ttLib.tables._g_l_y_f import Glyph, GlyphComponent
    cmap = font.getBestCmap()
    tables = [t for t in font["cmap"].tables if t.isUnicode()]
    def map_to(code: int, glyph: str) -> None:
        for t in tables:
            t.cmap[code] = glyph
    space_advance = font["hmtx"].metrics[cmap[0x0020]][0]
    if 0x00A0 not in cmap:
        map_to(0x00A0, cmap[0x0020])
    add_glyph(font, "uni202F.fr", Glyph(), round(space_advance * 0.55))
    map_to(0x202F, "uni202F.fr")
    if 0x2026 not in cmap and 0x002E in cmap:
        period = cmap[0x002E]
        advance = font["hmtx"].metrics[period][0]
        glyph = Glyph()
        glyph.numberOfContours = -1
        glyph.components = []
        for i in range(3):
            c = GlyphComponent()
            c.glyphName, c.x, c.y, c.flags = period, i * advance, 0, 0x0204 if i == 0 else 0x0004
            glyph.components.append(c)
        add_glyph(font, "ellipsis.fr", glyph, advance * 3)
        map_to(0x2026, "ellipsis.fr")


def rename(font: TTFont, family: str) -> None:
    """Remplace le nom réservé dans toutes les entrées de nom (famille, nom complet, PostScript, identifiant)."""
    new = RENAMED.get(family)
    if new is None:
        return
    # Les notices (copyright, licence et son adresse : nameID 0, 13, 14) restent celles de l'autrice.
    for record in font["name"].names:
        if record.nameID in (0, 13, 14):
            continue
        text = record.toUnicode()
        compact_old, compact_new = family.replace(" ", ""), new.replace(" ", "")
        if record.nameID == 6:  # nom PostScript : jamais d'espace
            record.string = text.replace(compact_old, compact_new).replace(family, compact_new)
        elif family in text or compact_old in text:
            record.string = text.replace(family, new).replace(compact_old, compact_new)


def main() -> None:
    FONT_DIR.mkdir(parents=True, exist_ok=True)
    LICENSE_DIR.mkdir(parents=True, exist_ok=True)
    opts = subset.Options()
    opts.layout_features = FEATURES
    opts.name_IDs = ["*"]
    opts.name_languages = ["*"]
    opts.notdef_outline = True
    opts.glyph_names = False
    unicodes = subset.parse_unicodes(UNICODES)
    for family, (ofl_dir, styles) in FAMILIES.items():
        urls = ttf_urls(family)
        for style, weight, res_name in styles:
            raw = fetch(urls[(style, weight)])
            font = TTFont(io.BytesIO(raw))
            sub = subset.Subsetter(opts)
            sub.populate(unicodes=unicodes)
            sub.subset(font)
            ensure_spaces(font)
            rename(font, family)
            target = FONT_DIR / f"{res_name}.ttf"
            font.save(target)
            print(f"{target.relative_to(ROOT)} : {len(raw) // 1024} Ko -> {target.stat().st_size // 1024} Ko")
        license_text = fetch(f"https://raw.githubusercontent.com/google/fonts/main/ofl/{ofl_dir}/OFL.txt")
        (LICENSE_DIR / f"{ofl_dir}-OFL.txt").write_bytes(license_text)


if __name__ == "__main__":
    main()
