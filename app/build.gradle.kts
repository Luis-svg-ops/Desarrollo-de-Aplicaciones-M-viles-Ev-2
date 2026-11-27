import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    // Kover: mide qué porcentaje de la lógica cubren las pruebas unitarias
    alias(libs.plugins.kover)
}

android {
    namespace = "cl.duoc.acis"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "cl.duoc.acis"
        // Android 8.0 (RNF-01 del Excel de requerimientos)
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ---- Firma del APK (EFT): la llave y su configuración están en la carpeta keystore/ ----
    val propiedadesFirma = Properties().apply {
        val archivo = rootProject.file("keystore/keystore.properties")
        if (archivo.exists()) archivo.inputStream().use { load(it) }
    }
    signingConfigs {
        create("release") {
            // Las variables de entorno tienen prioridad: así, en producción, las claves no van en el repo
            storeFile = rootProject.file("keystore/" + (System.getenv("ACIS_STORE_FILE") ?: propiedadesFirma.getProperty("storeFile", "acis-release.jks")))
            storePassword = System.getenv("ACIS_STORE_PASSWORD") ?: propiedadesFirma.getProperty("storePassword")
            keyAlias = System.getenv("ACIS_KEY_ALIAS") ?: propiedadesFirma.getProperty("keyAlias")
            keyPassword = System.getenv("ACIS_KEY_PASSWORD") ?: propiedadesFirma.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            // Compilación segura: R8 achica y ofusca el código (más difícil de copiar o modificar)
            optimization {
                enable = true
                keepRules {
                    files.add(file("proguard-rules.pro"))
                }
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.core)
    // Base de datos local (Room), igual que el ejemplo del curso
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Gson: convierte objetos Kotlin a JSON y de vuelta (informe de la OT)
    implementation(libs.gson)
    // Retrofit: consumo de los microservicios y de la API externa
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    // MockK: objetos falsos (mocks) para probar ViewModel y Repository sin base de datos ni red
    testImplementation(libs.mockk)
    // Para probar corrutinas (viewModelScope) en pruebas unitarias
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

/*
 * Cobertura de pruebas (EFT: "al menos el 80 % de la lógica, en ViewModel o Repository").
 * Se mide solo la lógica: ViewModel, Repository, reglas (model) y utilidades puras.
 * Se excluye lo que no es lógica o que se prueba en el emulador (androidTest):
 * pantallas de Compose, tema, navegación, Room (DAO y base) y acceso al hardware (GPS, cámara, archivos).
 * Reporte: ./gradlew koverHtmlReportDebug  →  app/build/reports/kover/htmlDebug/index.html
 */
kover {
    reports {
        filters {
            excludes {
                packages("cl.duoc.acis.view", "cl.duoc.acis.ui.theme", "cl.duoc.acis.navigation")
                classes(
                    "cl.duoc.acis.MainActivity*",
                    "*ComposableSingletons*",
                    // Room (se prueba en el emulador) y datos de ejemplo
                    "cl.duoc.acis.data.AppDatabase*", "cl.duoc.acis.data.*Dao*", "cl.duoc.acis.data.DatosEjemplo*",
                    // Solo forma del JSON o configuración de Retrofit (no tienen lógica)
                    "cl.duoc.acis.data.remote.*Dto", "cl.duoc.acis.data.remote.*Request", "cl.duoc.acis.data.remote.*Response",
                    "cl.duoc.acis.data.remote.ClienteApi*", "cl.duoc.acis.data.remote.Sesion*", "cl.duoc.acis.data.remote.ConfigServidor*",
                    // Hardware y archivos (se prueban en el emulador)
                    "cl.duoc.acis.util.ServicioUbicacion*", "cl.duoc.acis.util.FotosKt*", "cl.duoc.acis.util.GestorCheckIn*",
                    "*_Impl*", "*.BuildConfig"
                )
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
        variant("debug") {
            verify {
                rule("Cobertura mínima de la lógica (EFT)") {
                    minBound(80)
                }
            }
        }
    }
}

