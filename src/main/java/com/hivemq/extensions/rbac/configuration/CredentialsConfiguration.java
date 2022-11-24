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
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.configuration.entities.FileAuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Collections.unmodifiableList;

@ThreadSafe
public class CredentialsConfiguration {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(CredentialsConfiguration.class);
    static final @NotNull String CONFIG_NAME = "credentials.xml";
    private final @NotNull ReadWriteLock lock = new ReentrantReadWriteLock();

    //COWAL is perfect here because the callbacks are not expected to change regularly.
    //The callbacks are shared between this class and the reloadable config task.
    //Modifications are only possible via this class.
    private final @NotNull List<ReloadCallback> callbacks = new CopyOnWriteArrayList<>();

    private final @NotNull File extensionHomeFolder;
    private final @NotNull ConfigParser configParser;

    //guarded by lock
    private @Nullable FileAuthConfig config;


    public CredentialsConfiguration(
            final @NotNull File extensionHomeFolder,
            final @NotNull ScheduledExecutorService extensionExecutorService,
            final @NotNull ExtensionConfig extensionConfig) {
        configParser = new ConfigParser(extensionConfig);
        this.extensionHomeFolder = extensionHomeFolder;
        final ReloadConfigFileTask reloadableTask = new ReloadConfigFileTask(extensionHomeFolder,
                unmodifiableList(callbacks) /* We don't want the task to modify the callbacks!*/,
                configParser,
                new ConfigArchiver(extensionHomeFolder, new XmlParser()),
                this);
        extensionExecutorService.scheduleWithFixedDelay(reloadableTask,
                extensionConfig.getReloadInterval(),
                extensionConfig.getReloadInterval(),
                TimeUnit.SECONDS);
    }

    public void init() {
        config = configParser.read(getConfigFile(extensionHomeFolder));

        if (config == null) {
            LOG.warn("No credentials configuration file for file auth extension available, denying all connections.");
        }

        addReloadCallback((oldConfig, newConfig) -> {
            final Lock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                config = newConfig;
            } finally {
                writeLock.unlock();
            }
        });
    }

    public @Nullable FileAuthConfig getCurrentConfig() {
        final Lock readLock = lock.readLock();
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

    private static @NotNull File getConfigFile(final @NotNull File extensionHomeFolder) {
        return new File(extensionHomeFolder, CONFIG_NAME);
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
        private final @NotNull File configFile;
        private final @NotNull CredentialsConfiguration credentialsConfiguration;
        private final @NotNull List<ReloadCallback> callbacks;
        private @Nullable FileAuthConfig oldConfig;
        private long lastReadTimestamp;

        ReloadConfigFileTask(
                final @NotNull File extensionHomeFolder,
                final @NotNull List<ReloadCallback> callbacks,
                final @NotNull ConfigParser configParser,
                final @NotNull ConfigArchiver configArchiver,
                final @NotNull CredentialsConfiguration credentialsConfiguration) {
            this.callbacks = callbacks;
            this.configParser = configParser;
            this.configArchiver = configArchiver;
            configFile = getConfigFile(extensionHomeFolder);
            this.credentialsConfiguration = credentialsConfiguration;
            lastReadTimestamp = System.currentTimeMillis();
            oldConfig = configParser.read(configFile);
        }

        @Override
        public void run() {
            if (!configFile.exists()) {
                LOG.debug(
                        "No credentials file for file auth extension {} available, not reloading configuration for now",
                        configFile.getAbsolutePath());
                return;
            }

            final long lastModification = configFile.lastModified();
            if (credentialsConfiguration.getCurrentConfig() != null && lastReadTimestamp >= lastModification) {
                LOG.trace("Checked for changes for file {}. No changes since {}",
                        configFile.getAbsolutePath(),
                        lastModification);
                return;
            }

            LOG.debug("Credentials for file auth extension changed, checking new credentials file. {}",
                    configFile.getAbsolutePath());
            final FileAuthConfig newConfig = configParser.read(configFile);

            lastReadTimestamp = System.currentTimeMillis();

            if (newConfig == null) {
                //No changes or invalid new config
                return;
            }

            LOG.info("Credentials configuration for file auth extension changed, using new configuration.");
            try {
                configArchiver.archive(oldConfig);
            } catch (final IOException e) {
                LOG.warn("Archival of the old credentials config failed. Reason: {}", e.getMessage());
            }

            oldConfig = newConfig;
            for (final ReloadCallback callback : callbacks) {
                callback.onReload(oldConfig, newConfig);
            }
        }
    }
}
