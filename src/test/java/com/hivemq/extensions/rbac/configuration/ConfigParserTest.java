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
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigParserTest {

    private @NotNull File extensionFolder;
    private @NotNull ConfigParser configParser;

    @BeforeEach
    void setUp(@TempDir final @NotNull File extensionFolder) {
        this.extensionFolder = extensionFolder;
        configParser = new ConfigParser(new ExtensionConfig());
    }

    @Test
    void test_valid_config() throws Exception {
        final URL resource = this.getClass().getClassLoader().getResource("credentials.xml");
        assertNotNull(resource);
        final File file = new File(resource.toURI());
        final FileAuthConfig fileAuthConfig = configParser.read(file);
        assertNotNull(fileAuthConfig);
        assertNotNull(fileAuthConfig.getRoles());
        assertEquals(2, fileAuthConfig.getRoles().size());
        assertNotNull(fileAuthConfig.getUsers());
        assertEquals(2, fileAuthConfig.getUsers().size());
    }

    @Test
    void test_not_exising_file() {
        final File file = new File(extensionFolder, "not-existing.xml");
        final FileAuthConfig fileAuthConfig = configParser.read(file);
        assertNull(fileAuthConfig);
    }

    @Test
    void test_invalid_file() throws Exception {
        final File file = new File(extensionFolder, "invalid.xml");
        Files.writeString(file.toPath(), "<file-rbac></file-rbac>");
        final FileAuthConfig fileAuthConfig = configParser.read(file);
        assertNull(fileAuthConfig);
    }
}
