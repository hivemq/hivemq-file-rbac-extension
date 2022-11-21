/*
 *
 * Copyright 2019 dc-square GmbH
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
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.client.parameter.ListenerType;
import com.hivemq.extension.sdk.api.client.parameter.ProxyInformation;
import com.hivemq.extension.sdk.api.client.parameter.TlsInformation;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extensions.rbac.configuration.entities.ExtensionConfig;
import com.hivemq.extensions.rbac.utils.CredentialsValidator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("ConstantConditions")
public class FileAuthAuthenticatorTest {

    @Mock
    private CredentialsValidator credentialsValidator;
    private FileAuthAuthenticator fileAuthAuthenticator;

    @Mock
    private ExtensionConfig extensionConfig;

    @Mock
    private SimpleAuthOutput output;

    private ModifiableDefaultPermissions permissions;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        fileAuthAuthenticator = new FileAuthAuthenticator(credentialsValidator, extensionConfig);
        permissions = new TestDefaultPermissions();
        when(credentialsValidator.getPermissions(anyString(), anyString(), anyList())).thenReturn(List.of(mock(
                TopicPermission.class), mock(TopicPermission.class)));
        when(output.getDefaultPermissions()).thenReturn(permissions);
    }

    @Test
    public void test_wrong_listener_name() {
        when(extensionConfig.getListenerNames()).thenReturn(Set.of("listener-3", "listener-2"));
        fileAuthAuthenticator.onConnect(new TestInput("client1", "user1", "pass1", "listener-1"), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_empty_username() {
        when(extensionConfig.getListenerNames()).thenReturn(Set.of("listener-3", "listener-2"));
        fileAuthAuthenticator.onConnect(new TestInput("client1", null, "pass1", "listener-2"), output);
        verify(output).failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                "Authentication failed because username or password are missing");
    }

    @Test
    public void test_connect_with_empty_username_but_nextExtensionOrDefault() {
        when(extensionConfig.isNextExtensionInsteadOfFail()).thenReturn(true);
        when(extensionConfig.getListenerNames()).thenReturn(Set.of("listener-3", "listener-2"));
        fileAuthAuthenticator.onConnect(new TestInput("client1", null, "pass1", "listener-2"), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_empty_password() {
        fileAuthAuthenticator.onConnect(new TestInput("client1", "user1", null), output);
        verify(output).failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                "Authentication failed because username or password are missing");
    }

    @Test
    public void test_connect_with_empty_password_but_nextExtensionOrDefault() {
        when(extensionConfig.isNextExtensionInsteadOfFail()).thenReturn(true);
        fileAuthAuthenticator.onConnect(new TestInput("client1", "user1", null), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_wildcard_username() {
        fileAuthAuthenticator.onConnect(new TestInput("client1", "client/#", "pass1"), output);
        verify(output).failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                "The characters '#' and '+' are not allowed in the username");
    }

    @Test
    public void test_connect_with_wildcard_username_but_nextExtensionOrDefault() {
        when(extensionConfig.isNextExtensionInsteadOfFail()).thenReturn(true);
        fileAuthAuthenticator.onConnect(new TestInput("client1", "client/#", "pass1"), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_wildcard_plus_username() {
        fileAuthAuthenticator.onConnect(new TestInput("client1", "+/client", "pass1"), output);
        verify(output).failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                "The characters '#' and '+' are not allowed in the username");
    }

    @Test
    public void test_connect_with_wildcard_plus_username_but_nextExtensionOrDefault() {
        when(extensionConfig.isNextExtensionInsteadOfFail()).thenReturn(true);
        fileAuthAuthenticator.onConnect(new TestInput("client1", "+/client", "pass1"), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_wildcard_clientid() {
        fileAuthAuthenticator.onConnect(new TestInput("client/#", "user1", "pass1"), output);
        verify(output).failAuthentication(ConnackReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                "The characters '#' and '+' are not allowed in the client identifier");
    }

    @Test
    public void test_connect_with_wildcard_clientid_but_nextExtensionOrDefault() {
        when(extensionConfig.isNextExtensionInsteadOfFail()).thenReturn(true);
        fileAuthAuthenticator.onConnect(new TestInput("client/#", "user1", "pass1"), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_wildcard_plus_clientid() {
        fileAuthAuthenticator.onConnect(new TestInput("+/client", "user1", "pass1"), output);
        verify(output).failAuthentication(ConnackReasonCode.CLIENT_IDENTIFIER_NOT_VALID,
                "The characters '#' and '+' are not allowed in the client identifier");
    }

    @Test
    public void test_connect_with_wildcard_plus_clientid_but_nextExtensionOrDefault() {
        when(extensionConfig.isNextExtensionInsteadOfFail()).thenReturn(true);
        fileAuthAuthenticator.onConnect(new TestInput("+/client", "user1", "pass1"), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_invalid_credentials() {
        when(credentialsValidator.getRoles(anyString(), any(ByteBuffer.class))).thenReturn(null);
        when(extensionConfig.getListenerNames()).thenReturn(Set.of("listener-3", "listener-2"));
        fileAuthAuthenticator.onConnect(new TestInput("client1", "user1", "pass1", "listener-2"), output);
        verify(output).failAuthentication(ConnackReasonCode.NOT_AUTHORIZED,
                "Authentication failed because of invalid credentials");
    }

    @Test
    public void test_connect_with_invalid_credentials_but_nextExtensionOrDefault() {
        when(extensionConfig.isNextExtensionInsteadOfFail()).thenReturn(true);
        when(credentialsValidator.getRoles(anyString(), any(ByteBuffer.class))).thenReturn(null);
        when(extensionConfig.getListenerNames()).thenReturn(Set.of("listener-3", "listener-2"));
        fileAuthAuthenticator.onConnect(new TestInput("client1", "user1", "pass1", "listener-2"), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_valid_credentials_empty_roles() {
        when(credentialsValidator.getRoles(anyString(), any(ByteBuffer.class))).thenReturn(List.of());
        fileAuthAuthenticator.onConnect(new TestInput("client1", "user1", "pass1"), output);
        verify(output).failAuthentication(ConnackReasonCode.NOT_AUTHORIZED,
                "Authentication failed because of invalid credentials");
    }

    @Test
    public void test_connect_with_valid_credentials_empty_roles_but_nextExtensionOrDefault() {
        when(extensionConfig.isNextExtensionInsteadOfFail()).thenReturn(true);
        when(credentialsValidator.getRoles(anyString(), any(ByteBuffer.class))).thenReturn(List.of());
        fileAuthAuthenticator.onConnect(new TestInput("client1", "user1", "pass1"), output);
        verify(output).nextExtensionOrDefault();
    }

    @Test
    public void test_connect_with_valid_credentials() {
        when(credentialsValidator.getRoles(anyString(), any(ByteBuffer.class))).thenReturn(List.of("role1", "role2"));
        fileAuthAuthenticator.onConnect(new TestInput("client1", "user1", "pass1"), output);
        verify(output).authenticateSuccessfully();
        assertEquals(2, permissions.asList().size());
    }

    private static class TestDefaultPermissions implements ModifiableDefaultPermissions {

        private List<TopicPermission> permissions = new ArrayList<>();

        @Override
        public @NotNull List<TopicPermission> asList() {
            return permissions;
        }

        @Override
        public void add(@NotNull final TopicPermission permission) {
            permissions.add(permission);
        }

        @Override
        public void addAll(@NotNull final Collection<? extends TopicPermission> permissions) {
            this.permissions.addAll(permissions);
        }

        @Override
        public void remove(@NotNull final TopicPermission permission) {
            permissions.remove(permission);
        }

        @Override
        public void clear() {
            permissions.clear();
        }

        @Override
        public @NotNull DefaultAuthorizationBehaviour getDefaultBehaviour() {
            return DefaultAuthorizationBehaviour.DENY;
        }

        @Override
        public void setDefaultBehaviour(@NotNull final DefaultAuthorizationBehaviour defaultBehaviour) {

        }
    }

    @SuppressWarnings("ConstantConditions")
    private class TestInput implements SimpleAuthInput {

        private final @NotNull String clientId;
        private final @Nullable String userName;
        private final @Nullable String password;
        private final @Nullable String listenerName;

        private TestInput(
                final @NotNull String clientId, final @Nullable String userName, final @Nullable String password) {
            this.clientId = clientId;
            this.userName = userName;
            this.password = password;
            this.listenerName = null;
        }

        private TestInput(
                final @NotNull String clientId,
                final @Nullable String userName,
                final @Nullable String password,
                final @Nullable String listenerName) {
            this.clientId = clientId;
            this.userName = userName;
            this.password = password;
            this.listenerName = listenerName;
        }

        @Override
        public @NotNull ConnectPacket getConnectPacket() {
            return new TestConnectPacket(clientId, userName, password);
        }

        @Override
        public @NotNull ConnectionInformation getConnectionInformation() {
            return new TestConnectionInformation(listenerName);
        }

        @Override
        public @NotNull ClientInformation getClientInformation() {
            return () -> clientId;
        }
    }

    private class TestConnectionInformation implements ConnectionInformation {

        private final @Nullable String listenerName;

        TestConnectionInformation(String listenerName) {
            this.listenerName = listenerName;
        }

        @Override
        public @NotNull MqttVersion getMqttVersion() {
            return null;
        }

        @Override
        public @NotNull Optional<InetAddress> getInetAddress() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<Listener> getListener() {
            return Optional.of(new Listener() {
                @Override
                public int getPort() {
                    return 0;
                }

                @Override
                public @NotNull String getBindAddress() {
                    return null;
                }

                @Override
                public @NotNull ListenerType getListenerType() {
                    return null;
                }

                @Override
                public @NotNull String getName() {
                    return listenerName;
                }
            });
        }

        @Override
        public @NotNull Optional<ProxyInformation> getProxyInformation() {
            return Optional.empty();
        }

        @Override
        public @NotNull ConnectionAttributeStore getConnectionAttributeStore() {
            return null;
        }

        @Override
        public @NotNull Optional<TlsInformation> getTlsInformation() {
            return Optional.empty();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private class TestConnectPacket implements ConnectPacket {

        private final @NotNull String clientId;
        private final @Nullable String userName;
        private final @Nullable String password;

        private TestConnectPacket(
                final @NotNull String clientId, final @Nullable String userName, final @Nullable String password) {
            this.clientId = clientId;
            this.userName = userName;
            this.password = password;
        }

        @Override
        public @NotNull MqttVersion getMqttVersion() {
            return null;
        }

        @Override
        public @NotNull String getClientId() {
            return clientId;
        }

        @Override
        public boolean getCleanStart() {
            return false;
        }

        @Override
        public @NotNull Optional<WillPublishPacket> getWillPublish() {
            return Optional.empty();
        }

        @Override
        public long getSessionExpiryInterval() {
            return 0;
        }

        @Override
        public int getKeepAlive() {
            return 0;
        }

        @Override
        public int getReceiveMaximum() {
            return 0;
        }

        @Override
        public long getMaximumPacketSize() {
            return 0;
        }

        @Override
        public int getTopicAliasMaximum() {
            return 0;
        }

        @Override
        public boolean getRequestResponseInformation() {
            return false;
        }

        @Override
        public boolean getRequestProblemInformation() {
            return false;
        }

        @Override
        public @NotNull Optional<String> getAuthenticationMethod() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<ByteBuffer> getAuthenticationData() {
            return Optional.empty();
        }

        @Override
        public @NotNull UserProperties getUserProperties() {
            return null;
        }

        @Override
        public @NotNull Optional<String> getUserName() {
            return Optional.ofNullable(userName);
        }

        @Override
        public @NotNull Optional<ByteBuffer> getPassword() {
            if (password == null) {
                return Optional.empty();
            }
            return Optional.of(ByteBuffer.wrap(password.getBytes()));
        }
    }
}
