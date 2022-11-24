/*
 *
 * Copyright 2019 HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.rbac.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.configuration.entities.Permission;
import com.hivemq.extensions.rbac.configuration.entities.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigArchiverTest {

    private @NotNull File extensionFolder;
    private @NotNull ConfigArchiver configArchiver;

    @BeforeEach
    void setUp(@TempDir final @NotNull File extensionFolder) {
        this.extensionFolder = extensionFolder;
        configArchiver = new ConfigArchiver(extensionFolder, new XmlParser());
    }

    @Test
    void test_archive() throws Exception {
        final FileAuthConfig config = new FileAuthConfig();
        config.setRoles(List.of(new Role("id1", List.of(new Permission("topic1")))));
        configArchiver.archive(config);

        final File[] files = extensionFolder.listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);

        final File archiveFolder = files[0];
        assertEquals("credentials-archive", archiveFolder.getName());
        assertTrue(archiveFolder.isDirectory());

        final File[] archivedFiles = archiveFolder.listFiles();
        assertNotNull(archivedFiles);
        assertEquals(1, archivedFiles.length);
        System.out.println(archivedFiles[0].getName());
        assertTrue(archivedFiles[0].getName().startsWith("20"));
        assertTrue(archivedFiles[0].getName().endsWith("credentials.xml"));
    }
}
