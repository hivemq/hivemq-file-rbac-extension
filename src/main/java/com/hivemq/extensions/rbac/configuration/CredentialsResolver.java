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
public class CredentialsResolver implements Supplier<Path> {

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(CredentialsResolver.class);

    private final @NotNull AtomicBoolean legacyWarningAlreadyLogged = new AtomicBoolean();
    private final @NotNull Path extensionHome;

    public CredentialsResolver(final @NotNull Path extensionHome) {
        this.extensionHome = extensionHome;
    }

    @Override
    public @NotNull Path get() {
        final Path credentialsXmlPath = extensionHome.resolve(ExtensionConstants.CREDENTIALS_LOCATION);
        final Path credentialsXmlLegacyPath = extensionHome.resolve(ExtensionConstants.CREDENTIALS_LEGACY_LOCATION);
        // If the config is present at the legacy location we chose this in any case.
        // The only way the config could be present at the legacy location is when it was deliberately placed there.
        if (credentialsXmlLegacyPath.toFile().exists()) {
            if (!legacyWarningAlreadyLogged.getAndSet(true)) {
                LOG.warn("{}: The credentials file is placed at the legacy location '{}'. " +
                                "Please move the configuration file to the new location '{}'. " +
                                "Support for the legacy location will be removed in a future release.",
                        ExtensionConstants.EXTENSION_NAME,
                        credentialsXmlLegacyPath,
                        credentialsXmlPath);
            }
            return credentialsXmlLegacyPath;
        }
        return credentialsXmlPath;
    }
}
