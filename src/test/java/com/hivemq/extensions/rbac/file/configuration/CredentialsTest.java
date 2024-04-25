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
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialsTest {

    @TempDir
    private @NotNull Path extensionHome;

    private final @NotNull ExtensionConfig extensionConfig = new ExtensionConfig();

    @BeforeEach
    void setUp() {
        extensionConfig.setReloadInterval(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {ExtensionConstants.CREDENTIALS_LOCATION, ExtensionConstants.CREDENTIALS_LEGACY_LOCATION})
    void test_create_file_before_it_does_not_exit(
            final @NotNull String location) throws Exception {
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final CredentialsConfiguration credentialsConfiguration =
                new CredentialsConfiguration(extensionHome.toFile(), scheduledExecutorService, extensionConfig);
        credentialsConfiguration.init();
        final CountDownLatch latch = new CountDownLatch(1);
        credentialsConfiguration.addReloadCallback((oldConfig, newConfig) -> latch.countDown());
        //Create a new file
        createCredentialsConfig(extensionHome, location);
        //Check if reload was called
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertNotNull(credentialsConfiguration.getCurrentConfig());
        scheduledExecutorService.shutdown();
    }

    @ParameterizedTest
    @ValueSource(strings = {ExtensionConstants.CREDENTIALS_LOCATION, ExtensionConstants.CREDENTIALS_LEGACY_LOCATION})
    void test_reload_invalid_config(
            final @NotNull String location) throws Exception {
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final CredentialsConfiguration credentialsConfiguration =
                new CredentialsConfiguration(extensionHome.toFile(), scheduledExecutorService, extensionConfig);
        //Create a new file
        createCredentialsConfig(extensionHome, location);
        credentialsConfiguration.init();
        final Path resolve = extensionHome.resolve(location);
        assertTrue(resolve.toFile().delete());
        final CountDownLatch latch = new CountDownLatch(1);
        credentialsConfiguration.addReloadCallback((oldConfig, newConfig) -> latch.countDown());
        //Check if reload was called
        assertFalse(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(credentialsConfiguration.getCurrentConfig());
        scheduledExecutorService.shutdown();
    }

    @ParameterizedTest
    @ValueSource(strings = {ExtensionConstants.CREDENTIALS_LOCATION, ExtensionConstants.CREDENTIALS_LEGACY_LOCATION})
    void test_init(final @NotNull String location) throws Exception {
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        final CredentialsConfiguration credentialsConfiguration =
                new CredentialsConfiguration(extensionHome.toFile(), scheduledExecutorService, extensionConfig);
        createCredentialsConfig(extensionHome, location);
        credentialsConfiguration.init();
        assertNotNull(credentialsConfiguration.getCurrentConfig());
        scheduledExecutorService.shutdown();
    }

    private void createCredentialsConfig(final @NotNull Path extensionHome, final @NotNull String location)
            throws Exception {
        //Create a new file
        final Path configFile = extensionHome.resolve(location);

        //noinspection ResultOfMethodCallIgnored
        configFile.getParent().toFile().mkdir();

        //Copy config
        final URL resource = this.getClass().getClassLoader().getResource(ExtensionConstants.CREDENTIALS_LOCATION);
        assertNotNull(resource);
        final File file = new File(resource.toURI());
        Files.copy(file.toPath(), configFile, StandardCopyOption.REPLACE_EXISTING);
    }
}
