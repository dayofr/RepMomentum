# RepMomentum

Journal de musculation pour séries réparties dans la journée. Chaque série est une entrée indépendante : pas de séance à démarrer ni à terminer. Application Android hors ligne, sans compte ni télémétrie.

## Prérequis

- JDK 17 (`brew install openjdk@17`, puis `JAVA_HOME` dessus).
- SDK Android avec la plateforme 37.

## Commandes

```bash
./gradlew :app:installDebug                 # installer sur l'appareil ou l'émulateur
./gradlew :app:testDebugUnitTest            # tests JVM
./gradlew :app:connectedDebugAndroidTest    # tests instrumentés (émulateur lancé)
./gradlew :app:assembleRelease              # APK release (R8)
```

## Signature release

1. Créer la clé (hors dépôt) :
   `keytool -genkeypair -v -keystore repmomentum-release.jks -alias repmomentum -keyalg RSA -keysize 4096 -validity 10000`
2. Copier `keystore.properties.example` en `keystore.properties` et le remplir.
3. `./gradlew :app:assembleRelease` produit alors un APK signé.

Sans `keystore.properties`, l'APK release reste non signé. Ne jamais versionner la clé ni ses mots de passe : une clé perdue empêche toute mise à jour de l'app installée.

## Architecture

- `data/` : Room (`Exercise`, `SetEntry`), dépôts, préférences (DataStore).
- `domain/` : calculs (`StatsCalculator`), validation des saisies, dates locales.
- `ui/` : écrans Compose et ViewModels (MVVM), navigation typée.
- Injection manuelle via `AppContainer`.

Le volume (poids × reps) ne compte que les exercices à charge externe et sépare kg et lb. Les exercices au poids du corps notent le lest ajouté ; ceux en durée stockent des secondes.

## Polices

Space Grotesk (titres), Hanken Grotesk (texte) et JetBrains Mono (chiffres), en polices variables dans `app/src/main/res/font/`. Licence SIL OFL 1.1 : textes dans `licenses/`.
