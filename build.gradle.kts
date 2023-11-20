plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
}

group = "com.hivemq.extensions"
description = "HiveMQ File Role Based Access Control Extension"

hivemqExtension {
    name.set("HiveMQ File Role Based Access Control Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(10000)
    sdkVersion.set(libs.versions.hivemq.extensionSdk)

    resources {
        from("LICENSE")
    }
}

tasks.hivemqExtensionJar {
    manifest {
        attributes["Main-Class"] = "com.hivemq.extensions.rbac.generator.PasswordGenerator"
    }
}

dependencies {
    implementation(libs.commonsLang)
    implementation(libs.commonsText)
    implementation(libs.bouncycastle.prov)
    implementation(libs.caffeine)
    implementation(libs.jaxb.api)
    runtimeOnly(libs.jaxb.impl)
    implementation(libs.jcommander)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                implementation(libs.mockito)
            }
            targets.configureEach {
                testTask {
                    classpath += files("src/hivemq-extension")
                }
            }
        }
    }
}

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}
