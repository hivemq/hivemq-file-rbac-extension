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
import com.hivemq.extensions.rbac.file.configuration.entities.FileAuthConfig;
import org.apache.commons.lang3.time.FastDateFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

@ThreadSafe
class ConfigArchiver {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(ConfigArchiver.class);

    private final @NotNull Path archiveFolder;
    private final @NotNull XmlParser xmlParser;

    ConfigArchiver(final @NotNull Path extensionHome, final @NotNull XmlParser xmlParser) {
        this.xmlParser = xmlParser;
        this.archiveFolder = extensionHome.resolve("credentials-archive").toAbsolutePath();
    }

    /**
     * A config to the archive folder as XML
     * <p>
     * If archiving was not successful, this method will throw an IOException.
     * <p>
     * This method is thread safe
     *
     * @param config the config to archive
     * @throws IOException if something bad happened and archival was not successful
     */
    synchronized void archive(final @Nullable FileAuthConfig config) throws IOException {
        if (config == null) {
            LOG.debug("Configuration is invalid, archiving is not possible");
            return;
        }
        // if someone's nasty and creates a file that looks like a folder
        if (Files.isRegularFile(archiveFolder)) {
            LOG.warn("The credentials archive folder is a file, trying to delete");
            if (Files.deleteIfExists(archiveFolder)) {
                throw new IOException("Could not delete file " + archiveFolder);
            }
        }
        // if someone deleted the folder in the meantime
        if (!Files.exists(archiveFolder)) {
            LOG.debug("Creating credentials archive Folder");
            Files.createDirectories(archiveFolder);
            LOG.info("Created credentials Archive folder {}.", archiveFolder);
        }
        final var formatter = FastDateFormat.getInstance("yyyyMMdd-hh-mm-ss");
        try {
            final var dateString = formatter.format(new Date());
            final var file = archiveFolder.resolve(dateString + "-credentials.xml").toAbsolutePath();
            xmlParser.marshal(config, file);
            LOG.info("Archived current credentials config to {}.", file);
        } catch (final NotMarshallableException e) {
            throw new IOException(e);
        }
    }
}
