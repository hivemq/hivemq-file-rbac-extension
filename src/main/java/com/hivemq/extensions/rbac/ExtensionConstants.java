package com.hivemq.extensions.rbac;

import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Yannick Weber
 */
public class ExtensionConstants {

    public static final @NotNull String EXTENSION_NAME = "HiveMQ File RBAC Extension";
    public static final @NotNull String EXTENSION_ID = "hivemq-file-rbac-extension";

    public static final @NotNull String EXTENSION_CONFIG_LEGACY_LOCATION = "extension-config.xml";
    public static final @NotNull String EXTENSION_CONFIG_LOCATION = "conf/config.xml";

    public static final @NotNull String CREDENTIALS_LEGACY_LOCATION = "credentials.xml";
    public static final @NotNull String CREDENTIALS_LOCATION = "conf/credentials.xml";
}
