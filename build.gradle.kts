// Top-level build file. 모듈 공통 설정은 각 모듈의 build.gradle.kts에 둡니다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
