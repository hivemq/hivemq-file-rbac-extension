plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
    alias(libs.plugins.asciidoctor)
}

group = "com.hivemq.extensions"
description = "HiveMQ File Role Based Access Control Extension"

hivemqExtension {
    name.set("HiveMQ File Role Based Access Control Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(10000)
    sdkVersion.set(libs.versions.hivemq.extensionSdk)
}

tasks.hivemqExtensionJar {
    manifest {
        attributes["Main-Class"] = "com.hivemq.extensions.rbac.generator.PasswordGenerator"
    }
}

dependencies {
    hivemqProvided(libs.logback.classic)

    implementation(libs.commonsLang)
    implementation(libs.commonsText)
    implementation(libs.bouncycastle.prov)
    implementation(libs.caffeine)
    implementation(libs.jaxb.api)
    implementation(libs.jcommander)

    runtimeOnly(libs.jaxb.impl)
}

/* ******************** resources ******************** */

val prepareAsciidoc by tasks.registering(Sync::class) {
    from("README.adoc").into({ temporaryDir })
}

tasks.asciidoctor {
    dependsOn(prepareAsciidoc)
    sourceDir(prepareAsciidoc.map { it.destinationDir })
}

hivemqExtension.resources {
    from("LICENSE")
    from("README.adoc") { rename { "README.txt" } }
    from(tasks.asciidoctor)
}

/* ******************** test ******************** */

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.processTestResources {
    from("src/hivemq-extension/")
}

/* ******************** checks ******************** */

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    exclude("**/template-s3discovery.properties")
    exclude("**/logback-test.xml")
}

/* ******************** run ******************** */

tasks.prepareHivemqHome {
    hivemqHomeDirectory.set(file("/path/to/a/hivemq/folder"))
}
