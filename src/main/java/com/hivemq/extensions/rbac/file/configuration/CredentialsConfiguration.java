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

import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extensions.rbac.file.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.file.configuration.entities.FileAuthConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.hivemq.extensions.rbac.file.ExtensionConstants.CREDENTIALS_LEGACY_LOCATION;
import static com.hivemq.extensions.rbac.file.ExtensionConstants.CREDENTIALS_LOCATION;
import static java.util.Collections.unmodifiableList;

@ThreadSafe
public class CredentialsConfiguration {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(CredentialsConfiguration.class);

    private final @NotNull ReadWriteLock lock = new ReentrantReadWriteLock();

    // COWAL is perfect here because the callbacks are not expected to change regularly.
    // The callbacks are shared between this class and the reloadable config task.
    // Modifications are only possible via this class.
    private final @NotNull List<ReloadCallback> callbacks = new CopyOnWriteArrayList<>();

    private final @NotNull ConfigParser configParser;

    private final @NotNull ConfigResolver credentialsResolver;

    // guarded by lock
    private @Nullable FileAuthConfig config;

    public CredentialsConfiguration(
            final @NotNull Path extensionHome,
            final @NotNull ScheduledExecutorService extensionExecutorService,
            final @NotNull ExtensionConfig extensionConfig) {
        this.credentialsResolver = new ConfigResolver(extensionHome, CREDENTIALS_LOCATION, CREDENTIALS_LEGACY_LOCATION);
        this.configParser = new ConfigParser(extensionConfig);
        final var reloadableTask = new ReloadConfigFileTask(//
                unmodifiableList(callbacks) /* We don't want the task to modify the callbacks!*/,
                configParser,
                new ConfigArchiver(extensionHome, new XmlParser()),
                this,
                credentialsResolver);
        extensionExecutorService.scheduleWithFixedDelay(reloadableTask,
                extensionConfig.getReloadInterval(),
                extensionConfig.getReloadInterval(),
                TimeUnit.SECONDS);
    }

    public void init() {
        config = configParser.read(credentialsResolver.get());
        if (config == null) {
            LOG.warn("No credentials configuration file for file auth extension available, denying all connections.");
        }
        addReloadCallback((oldConfig, newConfig) -> {
            final var writeLock = lock.writeLock();
            writeLock.lock();
            try {
                config = newConfig;
            } finally {
                writeLock.unlock();
            }
        });
    }

    public @Nullable FileAuthConfig getCurrentConfig() {
        final var readLock = lock.readLock();
        readLock.lock();
        try {
            return config;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Adds a reload callback.
     */
    public void addReloadCallback(final @NotNull ReloadCallback callback) {
        callbacks.add(callback);
    }

    /**
     * A callback that gets triggered every time the config file changes.
     * <p>
     * A callback is guaranteed to not get executed concurrently.
     * <p>
     * All callbacks are executed in a dedicated executor. Callbacks are not executed concurrently,
     * so make sure they don't block for too long
     */
    public interface ReloadCallback {

        /**
         * A callback that is called when the config changes
         *
         * @param oldConfig the old config
         * @param newConfig the new config
         */
        void onReload(@Nullable FileAuthConfig oldConfig, @NotNull FileAuthConfig newConfig);
    }

    private static class ReloadConfigFileTask implements Runnable {

        private final @NotNull ConfigArchiver configArchiver;
        private final @NotNull ConfigParser configParser;
        private final @NotNull CredentialsConfiguration credentialsConfiguration;
        private final @NotNull ConfigResolver configResolver;
        private final @NotNull List<ReloadCallback> callbacks;
        private @Nullable FileAuthConfig oldConfig;
        private long lastReadTimestamp;

        ReloadConfigFileTask(
                final @NotNull List<ReloadCallback> callbacks,
                final @NotNull ConfigParser configParser,
                final @NotNull ConfigArchiver configArchiver,
                final @NotNull CredentialsConfiguration credentialsConfiguration,
                final @NotNull ConfigResolver configResolver) {
            this.callbacks = callbacks;
            this.configParser = configParser;
            this.configArchiver = configArchiver;
            this.credentialsConfiguration = credentialsConfiguration;
            this.configResolver = configResolver;
            this.lastReadTimestamp = System.currentTimeMillis();
            this.oldConfig = configParser.read(configResolver.get());
        }

        @Override
        public void run() {
            final var configFile = configResolver.get();
            if (!Files.exists(configFile)) {
                LOG.debug(
                        "No credentials file for file auth extension {} available, not reloading configuration for now",
                        configFile);
                return;
            }
            final var lastModification = configFile.toFile().lastModified();
            if (credentialsConfiguration.getCurrentConfig() != null && lastReadTimestamp >= lastModification) {
                LOG.trace("Checked for changes for file {}. No changes since {}", configFile, lastModification);
                return;
            }
            LOG.debug("Credentials for file auth extension changed, checking new credentials file. {}", configFile);
            final var newConfig = configParser.read(configFile);
            lastReadTimestamp = System.currentTimeMillis();
            if (newConfig == null) {
                // no changes or invalid new config
                return;
            }
            LOG.info("Credentials configuration for file auth extension changed, using new configuration.");
            try {
                configArchiver.archive(oldConfig);
            } catch (final IOException e) {
                LOG.warn("Archival of the old credentials config failed. Reason: {}", e.getMessage());
            }
            oldConfig = newConfig;
            for (final var callback : callbacks) {
                callback.onReload(oldConfig, newConfig);
            }
        }
    }
}
