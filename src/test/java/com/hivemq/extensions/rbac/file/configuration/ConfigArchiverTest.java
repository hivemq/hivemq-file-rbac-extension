/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.rbac.file.configuration;

import com.hivemq.extensions.rbac.file.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.file.configuration.entities.Permission;
import com.hivemq.extensions.rbac.file.configuration.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigArchiverTest {

    @TempDir
    private @NotNull Path extensionHome;

    private @NotNull ConfigArchiver configArchiver;

    @BeforeEach
    void setUp() {
        configArchiver = new ConfigArchiver(extensionHome, new XmlParser());
    }

    @Test
    void test_archive() throws Exception {
        final var config = new FileAuthConfig();
        config.setRoles(List.of(new Role("id1", List.of(new Permission("topic1")))));
        configArchiver.archive(config);

        final var files = extensionHome.toFile().listFiles();
        assertThat(files).hasSize(1);

        final var archiveFolder = files[0];
        assertThat(archiveFolder.getName()).isEqualTo("credentials-archive");
        assertThat(archiveFolder).isDirectory();

        final var archivedFiles = archiveFolder.listFiles();
        assertThat(archivedFiles).isNotNull();
        assertThat(archivedFiles[0].getName()).startsWith("20").endsWith("credentials.xml");
    }
}
