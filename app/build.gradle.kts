import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Local distribution settings are deliberately separate from the public Demo.
val storeConfigPath = providers.gradleProperty("storeConfigFile").orElse("store.properties")
val storeProperties = Properties().apply {
    val config = rootProject.file(storeConfigPath.get())
    if (config.isFile) config.inputStream().use { load(it) }
}
val storeGomokuUrl = storeProperties.getProperty("gomokuUrl", "").trim()
val storeBlokusUrl = storeProperties.getProperty("blokusUrl", "").trim()
fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""

val validateStoreConfig = tasks.register("validateStoreConfig") {
    inputs.property("gomokuUrl", storeGomokuUrl)
    inputs.property("blokusUrl", storeBlokusUrl)
    doLast {
        mapOf("gomokuUrl" to storeGomokuUrl, "blokusUrl" to storeBlokusUrl).forEach { (key, value) ->
            val uri = runCatching { URI(value) }.getOrNull()
            check(uri != null && uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
                uri.rawQuery == null && uri.rawFragment == null &&
                (uri.port == -1 || uri.port in 1..65535) && value.endsWith("/")) {
                "Store build requires $key in store.properties (or -PstoreConfigFile=...). " +
                    "Use an HTTPS directory URL ending in /, without credentials, query or fragment."
            }
        }
    }
}
// Validate only store builds; a fresh checkout can build Demo without local settings.
tasks.configureEach {
    if (name == "preStoreDebugBuild" || name == "preStoreReleaseBuild") {
        dependsOn(validateStoreConfig)
    }
}

android {
    namespace = "com.icego.gamecenter"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.icego.gamecenter"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }
    flavorDimensions += "distribution"
    productFlavors {
        create("demo") {
            dimension = "distribution"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            buildConfigField("boolean", "IS_DEMO", "true")
            buildConfigField("String", "DEFAULT_GOMOKU_URL", quoted(""))
            buildConfigField("String", "DEFAULT_BLOKUS_URL", quoted(""))
        }
        create("store") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_DEMO", "false")
            buildConfigField("String", "DEFAULT_GOMOKU_URL", quoted(storeGomokuUrl))
            buildConfigField("String", "DEFAULT_BLOKUS_URL", quoted(storeBlokusUrl))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
