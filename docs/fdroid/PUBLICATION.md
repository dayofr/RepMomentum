# Publication sur F-Droid

## Ce que le dépôt fournit

- Licence GPL-3.0-or-later (`LICENSE`).
- Fiche du store dans `fastlane/metadata/android/` : textes fr-FR et en-US, icône et captures (dans en-US, utilisé par défaut pour les autres langues).
- Build release sans dépendance propriétaire ni bloc de dépendances Google (`dependenciesInfo` désactivé), sans permission.
- Sans `keystore.properties`, l'APK release est non signé : F-Droid le signe avec sa propre clé.
- Wrapper Gradle avec somme de contrôle.

## Étapes

1. Pousser `main` et le tag `v1.0.0` sur GitHub.
2. Forker https://gitlab.com/fdroid/fdroiddata, copier `docs/fdroid/fr.dayo.repmomentum.yml` dans `metadata/`.
3. Vérifier localement avec `fdroid lint fr.dayo.repmomentum` puis `fdroid build -v -l fr.dayo.repmomentum`.
4. Ouvrir une merge request sur fdroiddata.

## Nouvelle version

1. Incrémenter `versionCode` et `versionName` dans `app/build.gradle.kts`.
2. Ajouter `fastlane/metadata/android/<langue>/changelogs/<versionCode>.txt`.
3. Commiter, taguer `vX.Y.Z`, pousser le tag. F-Droid détecte le tag (`UpdateCheckMode: Tags`).

## Points d'attention

- `compileSdk = 37` : le serveur de build F-Droid doit disposer de cette plateforme.
- Toute migration Room doit conserver les données : F-Droid met à jour l'app sans la réinstaller.
- L'identifiant `fr.dayo.repmomentum` ne peut plus changer après publication.
