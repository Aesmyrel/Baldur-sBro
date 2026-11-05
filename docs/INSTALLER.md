# Installer Carnet de Faerûn sur votre téléphone

Carnet de Faerûn s'installe en dehors du Play Store, à partir d'un fichier APK. L'appli fonctionne entièrement hors ligne et ne demande qu'une seule permission : la vibration, pour le lanceur de dés.

## 1. Avant de commencer

- **Téléphone visé** : Android 13 ou plus récent (version minimale de l'appli), processeur arm64 (le Pixel 10 Pro XL convient).
- **Une seule clé pour toutes les versions.** Android n'accepte de mettre à jour une appli que si la nouvelle version est signée avec la **même clé** que l'ancienne. Gardez précieusement la clé (fichier `.jks`) et ses mots de passe : si vous la perdez, il faudra désinstaller l'appli pour installer la version suivante, ce qui efface ses données. Exportez-les d'abord (voir § 5).
- La clé et le fichier `keystore.properties` ne doivent **jamais** entrer dans le dépôt Git : ils sont exclus par `.gitignore`.

## 2. Créer votre clé de signature (une seule fois)

Sur votre ordinateur, avec un JDK 21 installé, à la racine du projet :

```sh
mkdir -p signing
keytool -genkeypair -v \
  -keystore signing/carnet.jks \
  -alias carnet \
  -keyalg RSA -keysize 4096 -validity 36500 \
  -dname "CN=Carnet de Faerun (usage personnel)"
```

`keytool` demande un mot de passe pour le magasin de clés, puis pour la clé. Notez-les dans votre gestionnaire de mots de passe.

Créez ensuite, à la racine du projet, le fichier `keystore.properties` (non versionné) :

```properties
storeFile=signing/carnet.jks
storePassword=VOTRE_MOT_DE_PASSE_DU_MAGASIN
keyAlias=carnet
keyPassword=VOTRE_MOT_DE_PASSE_DE_LA_CLE
```

Sauvegardez `signing/carnet.jks` et ces mots de passe hors de l'ordinateur (clé USB chiffrée, coffre de mots de passe).

## 3. Construire l'APK signé

```sh
bash tools/setup_android_sdk.sh          # la première fois seulement : installe le SDK Android dans .android-sdk/
export ANDROID_HOME=$PWD/.android-sdk LC_ALL=C.UTF-8 LANG=C.UTF-8
./gradlew check                          # facultatif mais conseillé : lint, tests, captures, contrôle des permissions
./gradlew :app:assembleRelease
```

L'APK se trouve dans `app/build/outputs/apk/release/app-release.apk`. Sans `keystore.properties`, Gradle produit un APK **non signé** (`app-release-unsigned.apk`), qu'Android refusera d'installer.

Pour vérifier la signature et les permissions :

```sh
.android-sdk/build-tools/37.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
.android-sdk/build-tools/37.0.0/aapt2 dump permissions app/build/outputs/apk/release/app-release.apk
```

La seconde commande ne doit lister que `android.permission.VIBRATE` (et une permission interne propre à l'appli, `fr.carnetdefaerun.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, ajoutée automatiquement par Android pour ses récepteurs internes).

## 4. Installer sur le téléphone

**Méthode A — par câble (adb)**, si le débogage USB est activé (Paramètres → À propos du téléphone → appuyer sept fois sur « Numéro de build », puis Paramètres → Système → Options pour les développeurs → Débogage USB) :

```sh
.android-sdk/platform-tools/adb install -r app/build/outputs/apk/release/app-release.apk
```

**Méthode B — sans câble** :

1. Copiez l'APK sur le téléphone (Google Drive, câble en mode transfert de fichiers, ou tout autre moyen).
2. Ouvrez-le depuis l'appli Fichiers.
3. Android demande d'autoriser l'installation d'applis depuis cette source : acceptez pour l'appli Fichiers seulement. Vous pourrez retirer cette autorisation ensuite (Paramètres → Applis → Accès spéciaux des applis → Installer des applis inconnues).
4. Play Protect peut proposer d'analyser l'appli, puisqu'elle ne vient pas du Play Store : laissez-le faire, puis installez.

Au premier lancement, réglez votre progression (acte en cours) et votre tolérance aux spoilers dans Ma partie → Réglages.

## 5. Mettre à jour, sauvegarder, réinstaller

- **Mise à jour** : construisez la nouvelle version avec la même clé et installez-la par-dessus (méthode A avec `-r`, ou méthode B). Vos données sont conservées.
- **Sauvegarde manuelle** : Réglages → Données → Exporter crée un fichier `.carnet` : réglages, contenus dévoilés, noms corrigés, favoris, builds, groupe et notes. Gardez-le avant toute désinstallation.
- **Restauration** : Réglages → Données → Importer remplace toutes les données par celles du fichier.
- **Sauvegarde Android** : si elle est activée sur votre compte Google (Paramètres → Système → Sauvegarde), les données du joueur y sont incluses automatiquement ; la base de contenu ne l'est pas, elle revient avec l'APK.

## 6. Vérifier que l'appli reste hors ligne

Paramètres → Applis → Carnet de Faerûn → Autorisations : seule la vibration doit apparaître, aucune autorisation réseau. Vous pouvez aussi utiliser l'appli en mode avion : tout fonctionne. La liste complète des vérifications à faire sur le téléphone est dans `docs/TEST_SUR_TELEPHONE.md`.

## 7. Désinstaller

Paramètres → Applis → Carnet de Faerûn → Désinstaller. Toutes les données de l'appli sont effacées : exportez-les d'abord (§ 5).
