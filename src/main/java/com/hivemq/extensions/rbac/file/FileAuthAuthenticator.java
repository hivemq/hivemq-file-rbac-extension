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

import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.rbac.file.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.file.utils.CredentialsValidator;
import org.jetbrains.annotations.NotNull;

class FileAuthAuthenticator implements SimpleAuthenticator {

    private final @NotNull CredentialsValidator credentialsValidator;
    private final @NotNull ExtensionConfig extensionConfig;

    FileAuthAuthenticator(
            final @NotNull CredentialsValidator credentialsValidator,
            final @NotNull ExtensionConfig extensionConfig) {
        this.credentialsValidator = credentialsValidator;
        this.extensionConfig = extensionConfig;
    }

    @Override
    public void onConnect(
            final @NotNull SimpleAuthInput simpleAuthInput,
            final @NotNull SimpleAuthOutput simpleAuthOutput) {
        final var nextExtensionInsteadOfFail = extensionConfig.isNextExtensionInsteadOfFail();
        final var listenerNames = extensionConfig.getListenerNames();
        final var connectedListenerOptional = simpleAuthInput.getConnectionInformation().getListener();
        if (listenerNames != null && !listenerNames.isEmpty() && connectedListenerOptional.isPresent()) {
            final var connectedListenerName = connectedListenerOptional.get().getName();
            if (!listenerNames.contains(connectedListenerName)) {
                simpleAuthOutput.nextExtensionOrDefault();
                return;
            }
        }
        // check if username and password are present
        final var userNameOptional = simpleAuthInput.getConnectPacket().getUserName();
        final var passwordOptional = simpleAuthInput.getConnectPacket().getPassword();
        if (userNameOptional.isEmpty() || passwordOptional.isEmpty()) {
            // client is not authenticated
            if (nextExtensionInsteadOfFail) {
                simpleAuthOutput.nextExtensionOrDefault();
                return;
            }
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Authentication failed because username or password are missing");
            return;
        }
        final var userName = userNameOptional.get();
        // prevent clientIds with MQTT wildcard characters
        final var clientId = simpleAuthInput.getClientInformation().getClientId();
        if (clientId.contains("#") || clientId.contains("+")) {
            // client is not authenticated
            if (nextExtensionInsteadOfFail) {
                simpleAuthOutput.nextExtensionOrDefault();
                return;
            }
            simpleAuthOutput.failAuthentication(ConnackReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                    "The characters '#' and '+' are not allowed in the client identifier");
            return;
        }
        // prevent usernames with MQTT wildcard characters
        if (userName.contains("#") || userName.contains("+")) {
            // client is not authenticated
            if (nextExtensionInsteadOfFail) {
                simpleAuthOutput.nextExtensionOrDefault();
                return;
            }
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "The characters '#' and '+' are not allowed in the username");
            return;
        }
        // check if we have any roles for username/password combination
        final var roles = credentialsValidator.getRoles(userName, passwordOptional.get());
        if (roles == null || roles.isEmpty()) {
            //username/password combination is unknown or has invalid roles
            if (nextExtensionInsteadOfFail) {
                simpleAuthOutput.nextExtensionOrDefault();
                return;
            }
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED,
                    "Authentication failed because of invalid credentials");
            return;
        }
        // username/password combination is valid and has roles, so we set the default permissions for this client
        final var topicPermissions = credentialsValidator.getPermissions(clientId, userName, roles);
        simpleAuthOutput.getDefaultPermissions().addAll(topicPermissions);
        simpleAuthOutput.getDefaultPermissions().setDefaultBehaviour(DefaultAuthorizationBehaviour.DENY);
        simpleAuthOutput.authenticateSuccessfully();
    }
}
