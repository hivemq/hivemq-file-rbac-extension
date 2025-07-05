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

import com.hivemq.extensions.rbac.file.ExtensionConstants;
import com.hivemq.extensions.rbac.file.configuration.entities.ExtensionConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigParserTest {

    @TempDir
    private @NotNull Path extensionFolder;

    private @NotNull ConfigParser configParser;

    @BeforeEach
    void setUp() {
        configParser = new ConfigParser(new ExtensionConfig());
    }

    @Test
    void test_valid_config() throws Exception {
        final var resource = getClass().getClassLoader().getResource(ExtensionConstants.CREDENTIALS_LOCATION);
        assertThat(resource).isNotNull();
        final var configPath = Path.of(resource.toURI());
        final var fileAuthConfig = configParser.read(configPath);
        assertThat(fileAuthConfig).isNotNull();
        assertThat(fileAuthConfig.getUsers()).hasSize(2);
        assertThat(fileAuthConfig.getRoles()).hasSize(2);
    }

    @Test
    void test_not_exising_file() {
        final var configPath = extensionFolder.resolve("not-existing.xml");
        final var fileAuthConfig = configParser.read(configPath);
        assertThat(fileAuthConfig).isNull();
    }

    @Test
    void test_invalid_file() throws Exception {
        final var configPath = extensionFolder.resolve("invalid.xml");
        Files.writeString(configPath, "<file-rbac></file-rbac>");
        final var fileAuthConfig = configParser.read(configPath);
        assertThat(fileAuthConfig).isNull();
    }
}
