plugins {
    id("com.android.application")
}

providers.environmentVariable("YIDONGTONG_BUILD_DIR").orNull?.let {
    layout.buildDirectory.set(file(it))
}

android {
    namespace = "com.yidongtong.guardian"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.yidongtong.guardian"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "1.3.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.all {
            it.useJUnit()
        }
    }

}

dependencies {
    implementation("androidx.work:work-runtime:2.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
