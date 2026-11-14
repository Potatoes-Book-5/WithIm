import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Pleos Playground 키는 git에 올라가지 않는 local.properties에서 읽습니다.
// 항목 이름은 local.properties.example 참고.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String): String = localProperties.getProperty(key, "").trim()

// Public API key supplied outside the repository. Never log the value.
val molitApiKeyLiteral = providers.gradleProperty("MOLIT_API_KEY").orNull?.trim().orEmpty()
    .ifBlank { providers.environmentVariable("MOLIT_API_KEY").orNull?.trim().orEmpty() }
    .let { value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r") + "\"" }
val vworldApiKeyLiteral = providers.gradleProperty("VWORLD_API_KEY").orNull?.trim().orEmpty()
    .ifBlank { providers.environmentVariable("VWORLD_API_KEY").orNull?.trim().orEmpty() }
    .let { value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r") + "\"" }

android {
    namespace = "com.potatoes.book5.realestate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.potatoes.book5.realestate"
        // Pleos 요구사항: 차량 사이버 보안(사이닝 키 로테이션·리니지) 때문에 API 28 이상
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "MOLIT_API_KEY", molitApiKeyLiteral)
        buildConfigField("String", "VWORLD_API_KEY", vworldApiKeyLiteral)

        buildConfigField("String", "PLEOS_PROJECT_ID", "\"${localProp("pleos.projectId")}\"")
        buildConfigField("String", "PLEOS_CLIENT_ID", "\"${localProp("pleos.clientId")}\"")
        buildConfigField("String", "PLEOS_CLIENT_SECRET", "\"${localProp("pleos.clientSecret")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // Room 마이그레이션 테스트가 내보낸 스키마를 읽는다.
    sourceSets {
        getByName("androidTest").assets.srcDir(rootProject.file("core/data/schemas"))
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":feature:yeongtong"))
    // Pleos Connect SDK — 요구사항 확정 후 주석 해제
    // implementation(libs.pleos.vehicle)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
