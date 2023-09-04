package com.hivemq.extensions.rbac.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.rbac.ExtensionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author Yannick Weber
 */
public class ConfigResolver implements Supplier<Path> {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(ConfigResolver.class);

    private final @NotNull AtomicBoolean legacyWarningAlreadyLogged = new AtomicBoolean();
    private final @NotNull Path extensionHome;
    private final @NotNull String location;
    private final @NotNull String legacyLocation;

    public ConfigResolver(
            final @NotNull Path extensionHome, final @NotNull String location, final @NotNull String legacyLocation) {
        this.extensionHome = extensionHome;
        this.location = location;
        this.legacyLocation = legacyLocation;
    }

    @Override
    public @NotNull Path get() {
        final Path extensionXmlPath = extensionHome.resolve(location);
        final Path extensionXmlLegacyPath = extensionHome.resolve(legacyLocation);
        // If the config is present at the legacy location we chose this in any case.
        // The only way the config could be present at the legacy location is when it was deliberately placed there.
        if (extensionXmlLegacyPath.toFile().exists()) {
            if (!legacyWarningAlreadyLogged.getAndSet(true)) {
                LOG.warn("{}: The configuration file is placed at the legacy location '{}'. " +
                                "Please move the configuration file to the new location '{}'. " +
                                "Support for the legacy location will be removed in a future release.",
                        ExtensionConstants.EXTENSION_NAME,
                        extensionXmlLegacyPath,
                        extensionXmlPath);
            }
            return extensionXmlLegacyPath;
        }
        return extensionXmlPath;
    }
}
