plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license")
    id("io.github.sgtsilvio.gradle.defaults")
    id("org.asciidoctor.jvm.convert")
}

group = "com.hivemq.extensions"
description = "HiveMQ File Role Based Access Control Extension"

hivemqExtension {
    name.set("HiveMQ File Role Based Access Control Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(10000)
    sdkVersion.set("${property("hivemq-extension-sdk.version")}")
}

dependencies {
    hivemqProvided("ch.qos.logback:logback-classic:${property("logback.version")}")

    implementation("org.apache.commons:commons-lang3:${property("commons-lang.version")}")
    implementation("org.apache.commons:commons-text:${property("commons-text.version")}")
    implementation("org.bouncycastle:bcprov-jdk15on:${property("bouncycastle.version")}")
    implementation("com.github.ben-manes.caffeine:caffeine:${property("caffeine.version")}")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:${property("jakarta-xml-bind.version")}")
    implementation("com.beust:jcommander:${property("jcommander.version")}")

    runtimeOnly("com.sun.xml.bind:jaxb-impl:${property("jaxb.version")}")
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
    testImplementation("junit:junit:${property("junit.version")}")
    testImplementation("org.powermock:powermock-api-mockito2:${property("powermock.version")}")
    testImplementation("org.powermock:powermock-module-junit4:${property("powermock.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnit()
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
