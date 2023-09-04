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
import com.hivemq.extensions.rbac.ExtensionConstants;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.configuration.entities.PasswordType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigurationTest {

    @TempDir
    private @NotNull Path extensionHome;

    @ParameterizedTest
    @ValueSource(strings = {
            ExtensionConstants.EXTENSION_CONFIG_LOCATION,
            ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION})
    void test_read_extension_configuration(final @NotNull String location) throws Exception {
        final Path configFile = getTempConfig(location);
        Files.writeString(configFile,
                "<extension-configuration><credentials-reload-interval>999</credentials-reload-interval></extension-configuration>");
        final ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration(extensionHome.toFile());
        final ExtensionConfig extensionConfig = extensionConfiguration.getExtensionConfig();
        assertNotNull(extensionConfig);
        assertEquals(999, extensionConfig.getReloadInterval());
    }

    @Test
    void test_read_extension_file_not_present() {
        final ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration(extensionHome.toFile());
        final ExtensionConfig extensionConfig = extensionConfiguration.getExtensionConfig();
        assertNotNull(extensionConfig);
        //check that default values are used
        assertEquals(60, extensionConfig.getReloadInterval());
        assertEquals(PasswordType.HASHED, extensionConfig.getPasswordType());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ExtensionConstants.EXTENSION_CONFIG_LOCATION,
            ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION})
    void test_read_extension_configuration_invalid_reload_interval(final @NotNull String location) throws Exception {
        final Path configFile = getTempConfig(location);
        Files.writeString(configFile,
                "<extension-configuration><credentials-reload-interval>-1</credentials-reload-interval></extension-configuration>");
        final ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration(extensionHome.toFile());
        final ExtensionConfig extensionConfig = extensionConfiguration.getExtensionConfig();
        assertNotNull(extensionConfig);
        assertEquals(60, extensionConfig.getReloadInterval());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ExtensionConstants.EXTENSION_CONFIG_LOCATION,
            ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION})
    void test_read_extension_configuration_invalid_pw_type(final @NotNull String location) throws Exception {
        final Path configFile = getTempConfig(location);
        Files.writeString(configFile,
                "<extension-configuration><password-type>ABC</password-type></extension-configuration>");
        final ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration(extensionHome.toFile());
        final ExtensionConfig extensionConfig = extensionConfiguration.getExtensionConfig();
        assertNotNull(extensionConfig);
        assertEquals(PasswordType.HASHED, extensionConfig.getPasswordType());
        assertNull(extensionConfig.getListenerNames());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ExtensionConstants.EXTENSION_CONFIG_LOCATION,
            ExtensionConstants.EXTENSION_CONFIG_LEGACY_LOCATION})
    void test_read_extension_configuration_existing_listener_names(final @NotNull String location) throws Exception {
        final Path configFile = getTempConfig(location);
        Files.writeString(configFile,
                "<extension-configuration><listener-names><listener-name>listener-1</listener-name><listener-name>listener-2</listener-name></listener-names></extension-configuration>");
        final ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration(extensionHome.toFile());
        final ExtensionConfig extensionConfig = extensionConfiguration.getExtensionConfig();
        assertNotNull(extensionConfig);
        assertNotNull(extensionConfig.getListenerNames());
        assertEquals(2, extensionConfig.getListenerNames().size());
    }

    private @NotNull Path getTempConfig(final @NotNull String location) {
        final Path configFile = extensionHome.resolve(location);
        //noinspection ResultOfMethodCallIgnored
        configFile.getParent().toFile().mkdir();
        return configFile;
    }
}
