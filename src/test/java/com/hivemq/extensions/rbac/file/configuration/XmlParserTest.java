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
import com.hivemq.extensions.rbac.file.configuration.entities.FileAuthConfig;
import com.hivemq.extensions.rbac.file.configuration.entities.PasswordType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XmlParserTest {

    @TempDir
    private @NotNull Path extensionHome;

    @Test
    void test_marshal_directory() {
        final var xmlParser = new XmlParser();
        assertThatThrownBy(() -> xmlParser.marshal(new FileAuthConfig(), extensionHome)).isInstanceOf(
                NotMarshallableException.class);
    }

    @Test
    void test_marshal_file_exists() {
        final var xmlParser = new XmlParser();
        assertThatThrownBy(() -> xmlParser.marshal(new FileAuthConfig(), extensionHome)).isInstanceOf(
                NotMarshallableException.class);
    }

    @Test
    void test_marshal_unmarshal_credentials() throws Exception {
        final var xmlParser = new XmlParser();
        final var resource = getClass().getClassLoader().getResource(ExtensionConstants.CREDENTIALS_LOCATION);
        assertThat(resource).isNotNull();
        final var configPath = Path.of(resource.toURI()).toAbsolutePath();
        final var config = xmlParser.unmarshalFileAuthConfig(configPath);
        final var testFile = extensionHome.resolve("test.xml");
        xmlParser.marshal(config, testFile);
        final var marshalledConfig = xmlParser.unmarshalFileAuthConfig(testFile);
        assertThat(marshalledConfig.toString().trim()).isEqualTo(config.toString().trim());
    }

    @Test
    void test_unmarshal_extension_config() throws Exception {
        final var xmlParser = new XmlParser();
        final var resource = getClass().getClassLoader().getResource("test-extension-config.xml");
        assertThat(resource).isNotNull();
        final var configPath = Path.of(resource.toURI());
        final var config = xmlParser.unmarshalExtensionConfig(configPath);
        assertThat(config.getPasswordType()).isEqualTo(PasswordType.HASHED);
        assertThat(config.getReloadInterval()).isEqualTo(120);
    }
}
