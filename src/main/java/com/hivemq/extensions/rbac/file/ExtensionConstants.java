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
package com.hivemq.extensions.rbac.file;

import org.jetbrains.annotations.NotNull;

public class ExtensionConstants {

    public static final @NotNull String EXTENSION_NAME = "HiveMQ File RBAC Extension";

    public static final @NotNull String EXTENSION_CONFIG_LEGACY_LOCATION = "extension-config.xml";
    public static final @NotNull String EXTENSION_CONFIG_LOCATION = "conf/config.xml";

    public static final @NotNull String CREDENTIALS_LEGACY_LOCATION = "credentials.xml";
    public static final @NotNull String CREDENTIALS_LOCATION = "conf/credentials.xml";
}
