// Rend les maquettes de phase 0 en PNG avec le Chromium de Playwright.
// Prérequis : python3 tools/design/fetch_fonts.py (polices OFL dans tools/design/.fonts/).
// Usage : node tools/design/render_mockups.js [a-grimoire b-atlas c-vitrail]
const path = require("path");
const fs = require("fs");
const { chromium } = require("playwright");

const ROOT = path.resolve(__dirname, "..", "..");
const SRC = path.join(ROOT, "docs", "design", "src");
const OUT = path.join(ROOT, "docs", "design");
const ALL = ["a-grimoire", "b-atlas", "c-vitrail"];

(async () => {
  const names = process.argv.slice(2).length ? process.argv.slice(2) : ALL;
  const browser = await chromium.launch();
  for (const name of names) {
    const file = path.join(SRC, `${name}.html`);
    // Planche complète, échelle modérée pour rester légère dans le dépôt.
    const board = await browser.newPage({ viewport: { width: 1200, height: 800 }, deviceScaleFactor: 1.25 });
    await board.goto("file://" + file);
    await board.evaluate(() => document.fonts.ready);
    await board.waitForTimeout(300);
    await board.screenshot({ path: path.join(OUT, `${name}.png`), fullPage: true });
    await board.close();

    // Écrans individuels, en haute définition.
    const page = await browser.newPage({ viewport: { width: 1200, height: 800 }, deviceScaleFactor: 2 });
    await page.goto("file://" + file);
    await page.evaluate(() => document.fonts.ready);
    await page.waitForTimeout(300);
    const dir = path.join(OUT, name);
    fs.mkdirSync(dir, { recursive: true });
    for (const el of await page.$$(".screen[data-name]")) {
      const screenName = await el.getAttribute("data-name");
      await el.screenshot({ path: path.join(dir, `${screenName}.png`), omitBackground: true });
    }
    await page.close();
    console.log("rendu", name);
  }
  // Comparatif des trois directions, à partir des écrans individuels.
  if (names.length === ALL.length) {
    const cmp = await browser.newPage({ viewport: { width: 1400, height: 900 }, deviceScaleFactor: 1 });
    await cmp.goto("file://" + path.join(SRC, "comparatif.html"));
    await cmp.evaluate(() => document.fonts.ready);
    await cmp.waitForTimeout(500);
    await cmp.screenshot({ path: path.join(OUT, "comparatif.png"), fullPage: true });
    await cmp.close();
    console.log("rendu comparatif");
  }
  await browser.close();
})();
