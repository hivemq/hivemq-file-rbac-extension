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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialsTest {

    @TempDir
    private @NotNull Path extensionHome;

    private final @NotNull ExtensionConfig extensionConfig = new ExtensionConfig();
    private final @NotNull ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @BeforeEach
    void setUp() {
        extensionConfig.setReloadInterval(1);
    }

    @AfterEach
    void tearDown() {
        executorService.shutdown();
    }

    @ParameterizedTest
    @ValueSource(strings = {ExtensionConstants.CREDENTIALS_LOCATION, ExtensionConstants.CREDENTIALS_LEGACY_LOCATION})
    void test_create_file_before_it_does_not_exit(
            final @NotNull String location) throws Exception {
        final var credentialsConfiguration =
                new CredentialsConfiguration(extensionHome, executorService, extensionConfig);
        credentialsConfiguration.init();
        final var latch = new CountDownLatch(1);
        credentialsConfiguration.addReloadCallback((oldConfig, newConfig) -> latch.countDown());
        // create a new file
        createCredentialsConfig(extensionHome, location);
        // check if reload was called
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(credentialsConfiguration.getCurrentConfig()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {ExtensionConstants.CREDENTIALS_LOCATION, ExtensionConstants.CREDENTIALS_LEGACY_LOCATION})
    void test_reload_invalid_config(final @NotNull String location) throws Exception {
        final var credentialsConfiguration =
                new CredentialsConfiguration(extensionHome, executorService, extensionConfig);
        // create a new file
        createCredentialsConfig(extensionHome, location);
        credentialsConfiguration.init();
        final var resolve = extensionHome.resolve(location);
        Files.delete(resolve);
        final var latch = new CountDownLatch(1);
        credentialsConfiguration.addReloadCallback((oldConfig, newConfig) -> latch.countDown());
        // check if reload was called
        assertThat(latch.await(5, TimeUnit.SECONDS)).isFalse();
        assertThat(credentialsConfiguration.getCurrentConfig()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {ExtensionConstants.CREDENTIALS_LOCATION, ExtensionConstants.CREDENTIALS_LEGACY_LOCATION})
    void test_init(final @NotNull String location) throws Exception {
        final var credentialsConfiguration =
                new CredentialsConfiguration(extensionHome, executorService, extensionConfig);
        createCredentialsConfig(extensionHome, location);
        credentialsConfiguration.init();
        assertThat(credentialsConfiguration.getCurrentConfig()).isNotNull();
    }

    private void createCredentialsConfig(final @NotNull Path extensionHome, final @NotNull String location)
            throws Exception {
        // create a new file
        final var targetPath = extensionHome.resolve(location);
        Files.createDirectories(targetPath.getParent());

        // copy config
        final var resource = getClass().getClassLoader().getResource(ExtensionConstants.CREDENTIALS_LOCATION);
        assertThat(resource).isNotNull();
        final var configPath = Path.of(resource.toURI()).toAbsolutePath();
        Files.copy(configPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
