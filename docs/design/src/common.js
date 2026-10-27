// Outils communs des maquettes : hasard déterministe, champs d'étoiles, barres système.
// Tout est généré en code, sans aucune image externe.

function rng(seed) {
  let s = seed >>> 0;
  return () => { s = (s * 1664525 + 1013904223) >>> 0; return s / 4294967296; };
}

// Étoile à quatre branches (scintillement), centrée en (x, y).
function sparkle(x, y, r, fill, opacity = 1) {
  const k = r * 0.22;
  return `<path d="M${x} ${y - r} Q${x + k} ${y - k} ${x + r} ${y} Q${x + k} ${y + k} ${x} ${y + r} Q${x - k} ${y + k} ${x - r} ${y} Q${x - k} ${y - k} ${x} ${y - r}Z" fill="${fill}" opacity="${opacity}"/>`;
}

// Champ d'étoiles procédural dans un rectangle.
function starField({ w, h, n, seed, color = "#fff", maxR = 1.3, minO = 0.25, sparkles = 0, sparkleColor }) {
  const r = rng(seed);
  let out = "";
  for (let i = 0; i < n; i++) {
    const x = (r() * w).toFixed(1), y = (r() * h).toFixed(1);
    const rad = (0.25 + r() * r() * maxR).toFixed(2);
    const o = (minO + r() * (1 - minO)).toFixed(2);
    out += `<circle cx="${x}" cy="${y}" r="${rad}" fill="${color}" opacity="${o}"/>`;
  }
  for (let i = 0; i < sparkles; i++) {
    out += sparkle(+(r() * w).toFixed(1), +(r() * h).toFixed(1), 2.5 + r() * 3.5, sparkleColor || color, 0.6 + r() * 0.4);
  }
  return out;
}

function statusBar(color) {
  return `<div class="statusbar" style="color:${color}">
    <span>21:47</span><span class="cam"></span>
    <span class="icons">
      <svg width="17" height="12" viewBox="0 0 17 12"><path d="M1 11h2V8H1zM5 11h2V6H5zM9 11h2V3H9zM13 11h2V0h-2z" fill="${color}"/></svg>
      <svg width="16" height="12" viewBox="0 0 16 12"><path d="M8 11.5 0.6 3.6a10.6 10.6 0 0 1 14.8 0z" fill="${color}"/></svg>
      <svg width="24" height="12" viewBox="0 0 24 12"><rect x=".5" y=".5" width="20" height="11" rx="3" fill="none" stroke="${color}" opacity=".5"/><rect x="2" y="2" width="14" height="8" rx="1.6" fill="${color}"/><rect x="21.6" y="4" width="1.6" height="4" rx=".8" fill="${color}" opacity=".5"/></svg>
    </span></div>`;
}

function gestureBar(color) { return `<div class="gesture" style="background:${color}"></div>`; }

// Remplit les emplacements [data-slot] d'un écran à partir d'un dictionnaire de fonctions.
function mount(slots) {
  document.querySelectorAll("[data-slot]").forEach(el => {
    const fn = slots[el.dataset.slot];
    if (fn) el.innerHTML = fn(el);
  });
}
