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
import com.hivemq.extensions.rbac.file.configuration.entities.PasswordType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationTest {

    @TempDir
    private @NotNull Path extensionHome;

    @ParameterizedTest
    @ValueSource(strings = {
            ExtensionConstants.EXTENSION_CONFIG_LOCATION, ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION})
    void test_read_extension_configuration(final @NotNull String location) throws Exception {
        final var configFile = getTempConfig(location);
        Files.writeString(configFile,
                "<extension-configuration><credentials-reload-interval>999</credentials-reload-interval></extension-configuration>");
        final var extensionConfiguration = new ExtensionConfiguration(extensionHome);
        final var extensionConfig = extensionConfiguration.getExtensionConfig();
        assertThat(extensionConfig).isNotNull();
        assertThat(extensionConfig.getReloadInterval()).isEqualTo(999);
    }

    @Test
    void test_read_extension_file_not_present() {
        final var extensionConfiguration = new ExtensionConfiguration(extensionHome);
        final var extensionConfig = extensionConfiguration.getExtensionConfig();
        assertThat(extensionConfig).isNotNull();
        // check that default values are used
        assertThat(extensionConfig.getReloadInterval()).isEqualTo(60);
        assertThat(extensionConfig.getPasswordType()).isEqualTo(PasswordType.HASHED);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ExtensionConstants.EXTENSION_CONFIG_LOCATION, ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION})
    void test_read_extension_configuration_invalid_reload_interval(final @NotNull String location) throws Exception {
        final var configFile = getTempConfig(location);
        Files.writeString(configFile,
                "<extension-configuration><credentials-reload-interval>-1</credentials-reload-interval></extension-configuration>");
        final var extensionConfiguration = new ExtensionConfiguration(extensionHome);
        final var extensionConfig = extensionConfiguration.getExtensionConfig();
        assertThat(extensionConfig).isNotNull();
        assertThat(extensionConfig.getReloadInterval()).isEqualTo(60);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ExtensionConstants.EXTENSION_CONFIG_LOCATION, ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION})
    void test_read_extension_configuration_invalid_pw_type(final @NotNull String location) throws Exception {
        final var configFile = getTempConfig(location);
        Files.writeString(configFile,
                "<extension-configuration><password-type>ABC</password-type></extension-configuration>");
        final ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration(extensionHome);
        final ExtensionConfig extensionConfig = extensionConfiguration.getExtensionConfig();
        assertThat(extensionConfig).isNotNull();
        assertThat(extensionConfig.getPasswordType()).isEqualTo(PasswordType.HASHED);
        assertThat(extensionConfig.getListenerNames()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ExtensionConstants.EXTENSION_CONFIG_LOCATION, ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION})
    void test_read_extension_configuration_existing_listener_names(final @NotNull String location) throws Exception {
        final var configFile = getTempConfig(location);
        Files.writeString(configFile,
                "<extension-configuration><listener-names><listener-name>listener-1</listener-name><listener-name>listener-2</listener-name></listener-names></extension-configuration>");
        final var extensionConfiguration = new ExtensionConfiguration(extensionHome);
        final var extensionConfig = extensionConfiguration.getExtensionConfig();
        assertThat(extensionConfig).isNotNull();
        assertThat(extensionConfig.getListenerNames()).hasSize(2);
    }

    private @NotNull Path getTempConfig(final @NotNull String location) throws Exception {
        final var configFile = extensionHome.resolve(location);
        Files.createDirectories(configFile.getParent());
        return configFile;
    }
}
