plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.oci)
    alias(libs.plugins.license)
}

group = "com.hivemq.extensions"
description = "HiveMQ File Role Based Access Control Extension"

hivemqExtension {
    name = "HiveMQ File Role Based Access Control Extension"
    author = "HiveMQ"
    priority = 1000
    startPriority = 10000
    sdkVersion = libs.versions.hivemq.extensionSdk

    resources {
        from("LICENSE")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

tasks.hivemqExtensionJar {
    manifest {
        attributes["Main-Class"] = "com.hivemq.extensions.rbac.file.generator.PasswordGenerator"
    }
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.commonsLang)
    implementation(libs.commonsText)
    implementation(libs.bouncycastle.prov)
    implementation(libs.caffeine)
    implementation(libs.jaxb.api)
    runtimeOnly(libs.jaxb.impl)
    implementation(libs.jcommander)
}

oci {
    registries {
        dockerHub {
            optionalCredentials()
        }
    }
    imageMapping {
        mapModule("com.hivemq", "hivemq-community-edition") {
            toImage("hivemq/hivemq-ce")
        }
    }
    imageDefinitions.register("main") {
        allPlatforms {
            dependencies {
                runtime("com.hivemq:hivemq-community-edition:latest") { isChanging = true }
            }
            layer("main") {
                contents {
                    permissions("opt/hivemq/", 0b111_111_101)
                    permissions("opt/hivemq/extensions/", 0b111_111_101)
                    into("opt/hivemq/extensions") {
                        from(zipTree(tasks.hivemqExtensionZip.flatMap { it.archiveFile }))
                    }
                }
            }
        }
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.assertj)
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

// configure reproducible builds
tasks.withType<AbstractArchiveTask>().configureEach {
    // normalize file permissions for reproducibility
    // files: 0644 (rw-r--r--), directories: 0755 (rwxr-xr-x)
    filePermissions {
        unix("0644")
    }
    dirPermissions {
        unix("0755")
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure consistent compilation across different JDK versions
    options.compilerArgs.addAll(listOf(
        // include parameter names for reflection (improves consistency)
        "-parameters"
    ))
}
