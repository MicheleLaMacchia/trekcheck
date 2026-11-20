# trekcheck
applicazione per la gestione dei trekking

## Sviluppo Android

Il progetto usa Kotlin 2.2.20, Android Gradle Plugin 8.13.2, Gradle 8.14.8 tramite Wrapper e Jetpack Compose con BOM 2025.10.00. Compila e targetizza Android API 37 con Build Tools 37.0.0.

In Codex Cloud inizializza il terminale con l'ambiente predisposto:

```bash
export JAVA_HOME=/workspace/.jdk21/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/workspace/.android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export ANDROID_USER_HOME=/workspace/.android-user
export GRADLE_USER_HOME=/workspace/.gradle-user
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/16.0/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/build-tools/37.0.0:$PATH"

proxy_url="${HTTPS_PROXY:-${https_proxy:-}}"
if [ -n "$proxy_url" ]; then
  proxy_address="${proxy_url#*://}"
  proxy_address="${proxy_address##*@}"
  proxy_host="${proxy_address%%:*}"
  proxy_port="${proxy_address##*:}"
  export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:+$JAVA_TOOL_OPTIONS }-Dhttps.proxyHost=$proxy_host -Dhttps.proxyPort=$proxy_port -Dhttp.proxyHost=$proxy_host -Dhttp.proxyPort=$proxy_port"
fi
```

Poi esegui i test JVM e crea l'APK debug con il Wrapper, senza installare Gradle globalmente:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Coverage e CI

La coverage usa JaCoCo integrato in Gradle. Il report HTML è generato in `app/build/reports/jacoco/debugUnitTest/html/index.html` e il report XML in `app/build/reports/jacoco/debugUnitTest/report.xml`. Il task di verifica impone almeno il 5% di copertura delle righe, escludendo classi Android generate come `R`, `BuildConfig` e `Manifest`. Il task `check` include questa soglia.

La GitHub Action in `.github/workflows/android.yml` esegue unit test JVM, Android Lint, coverage con soglia e build debug sulle push a `main` e sulle pull request dirette a `main`. Pubblica l'APK debug solo se tutti i passaggi sono riusciti. La retention predefinita è 7 giorni. Puoi cambiarla creando la variabile GitHub Actions `DEBUG_APK_RETENTION_DAYS` con il numero di giorni desiderato, entro il limite configurato nel repository.

Per scaricare l'APK dal telefono, apri il repository su GitHub, vai in **Actions**, seleziona una run riuscita e scarica l'artifact **trekcheck-debug-apk**. Apri lo ZIP nell'app File, estrai `app-debug.apk` e toccalo per installarlo. Android potrebbe chiedere di autorizzare temporaneamente l'installazione da browser o File. Questo APK debug non è destinato alla pubblicazione sul Play Store.
