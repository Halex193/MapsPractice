val ktor_version: String by project

plugins {
    kotlin("js")
}
group = "ro.halex.mapspractice"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlinx")
    }
}
dependencies {
    api(project(":library"))
    testImplementation(kotlin("test-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")
    implementation("io.ktor:ktor-client-js:$ktor_version") //include http&websockets
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.3.9")

    //ktor client js json
    implementation("io.ktor:ktor-client-json-js:$ktor_version")
    implementation("io.ktor:ktor-client-serialization-js:$ktor_version")
    implementation(npm("@googlemaps/google-maps-services-js", "3.1.6"))
    implementation(npm("@types/googlemaps", "3.39.13"))
}
kotlin {
    js {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
}