# À vérifier sur le téléphone

> Ce conteneur n'a ni émulateur ni appareil : tout ce qui suit a été vérifié par les tests JVM, Robolectric
> et les captures Roborazzi, mais demande une confirmation sur le Pixel. Liste tenue à jour à chaque phase ;
> le rapport final reprendra ce qui reste.

## Installation et permissions
- [ ] L'APK signé s'installe (`docs/INSTALLER.md`, à venir en phase 7) et s'ouvre hors ligne, en mode avion.
- [ ] Paramètres → Applis → Carnet de Faerûn → Autorisations : seule la vibration est déclarée ; aucune autorisation réseau (D-021).
- [ ] Le scanner de codes (Google Code Scanner, phase 6) fonctionne sans autorisation réseau (D-021).

## Thèmes et démarrage
- [ ] Écran de démarrage en vélin en parchemin, en encre en sombre et OLED, sans éclair d'une autre teinte (D-052).
- [ ] Changer de thème (parchemin ↔ sombre) recrée l'activité : on revient au même écran, même onglet, même position (D-052).
- [ ] Au premier lancement sur un téléphone en mode clair, le passage à l'encre (thème par défaut) est bref et sans clignotement gênant.
- [ ] OLED : fond réellement noir (pixels éteints), aucun halo ni grain.

## Mouvement, shaders, haptique
- [ ] Brume du voile animée (mouvement non réduit) : fluide à 120 Hz, une seule animation à la fois, arrêt hors écran et après la révélation.
- [ ] Mouvement réduit (réglage système ou réglage de l'appli) et économiseur de batterie : rendu statique, fondus courts.
- [ ] Budget GPU des shaders sous 2 ms par image (profilage GPU des options pour les développeurs).
- [ ] Vibrations discrètes, et absentes quand le retour haptique est coupé dans les réglages système.

## Lisibilité
- [ ] Police système à 200 % : aucun mot coupé, aucun libellé tronqué hors barre de navigation (libellés lus en entier par TalkBack).
- [ ] TalkBack : le voile s'annonce « contenu masqué » et se révèle par l'action « Révéler » ; la valeur du curseur de taille de lecture est lue.
- [ ] Mode session : l'écran reste allumé tant qu'il est actif, et s'éteint normalement une fois désactivé.

## Navigation
- [ ] Changer d'onglet puis revenir retrouve l'écran et sa position.
- [ ] Un lien `carnetdefaerun://` ouvre la bonne fiche ; relancer l'appli depuis les tâches récentes ne rejoue pas le lien.
- [ ] Geste de retour prédictif : aperçu de la page précédente, retour vers « Ma partie » depuis la racine d'un autre onglet.
