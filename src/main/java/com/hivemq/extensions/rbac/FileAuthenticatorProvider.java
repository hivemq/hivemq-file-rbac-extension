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
package com.hivemq.extensions.rbac;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.utils.CredentialsValidator;

class FileAuthenticatorProvider implements AuthenticatorProvider {

    private final @NotNull FileAuthAuthenticator authenticator;

    FileAuthenticatorProvider(
            final @NotNull CredentialsValidator credentialsValidator, final @NotNull ExtensionConfig extensionConfig) {
        this.authenticator = new FileAuthAuthenticator(credentialsValidator, extensionConfig);
    }

    @Override
    public @Nullable Authenticator getAuthenticator(final @NotNull AuthenticatorProviderInput authenticatorProviderInput) {
        //Always return the same authenticator, because it is thread-safe and can be shared between multiple clients
        return authenticator;
    }
}
