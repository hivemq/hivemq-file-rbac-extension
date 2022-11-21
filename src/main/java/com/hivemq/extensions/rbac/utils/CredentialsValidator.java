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
package com.hivemq.extensions.rbac.utils;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.annotations.ThreadSafe;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extensions.rbac.configuration.Configuration;
import com.hivemq.extensions.rbac.configuration.entities.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@ThreadSafe
@SuppressWarnings("ConstantConditions")
public class CredentialsValidator {

    private final @NotNull Configuration configuration;

    private final ReadWriteLock usersLock = new ReentrantReadWriteLock();
    private final ReadWriteLock rolesLock = new ReentrantReadWriteLock();
    private final @NotNull
    ExtensionConfig extensionConfig;
    private final @NotNull CredentialsHasher credentialsHasher;

    private @NotNull Map<String, User> users = new ConcurrentHashMap<>();
    private @NotNull Map<String, Role> roles = new ConcurrentHashMap<>();

    public CredentialsValidator(@NotNull final Configuration configuration, @NotNull final ExtensionConfig extensionConfig, @NotNull final MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.extensionConfig = extensionConfig;
        this.credentialsHasher = new CredentialsHasher(metricRegistry);
    }

    public void init() {

        final @Nullable FileAuthConfig currentConfig = configuration.getCurrentConfig();
        if (currentConfig != null) {
            updateUsersMap(currentConfig);
            updateRolesMap(currentConfig);
        }

        configuration.addReloadCallback(new Configuration.ReloadCallback() {
            @Override
            public void onReload(@Nullable final FileAuthConfig oldConfig, @NotNull final FileAuthConfig newConfig) {
                updateUsersMap(newConfig);
                updateRolesMap(newConfig);
            }
        });
    }

    /**
     * @param userName the userName
     * @param password the password
     * @return a list of the users roles or null if the credentials are not valid
     */
    @Nullable
    public List<String> getRoles(@NotNull final String userName, @NotNull final ByteBuffer password) {

        //If Config is invalid do not allow clients to connect
        if (users.isEmpty() || roles.isEmpty()) {
            return null;
        }

        final Lock readLock = usersLock.readLock();
        readLock.lock();
        final User user;
        try {
            user = users.get(userName);
        } finally {
            readLock.unlock();
        }

        if (user == null) {
            return null;
        }

        if (extensionConfig.getPasswordType() == PasswordType.HASHED) {

            final String base64Password = encodePassword(password);

            final boolean passwordsEqual = credentialsHasher.checkCredentials(base64Password, user.getPassword());

            if (!passwordsEqual) {
                return null;
            }

        } else {
            if (!user.getPassword().equals(StandardCharsets.UTF_8.decode(password).toString())) {
                return null;
            }
        }

        return user.getRoles();
    }

    @NotNull
    public List<TopicPermission> getPermissions(final @NotNull String clientId, final @NotNull String userName, final @NotNull List<String> clientRoles) {

        if (clientRoles.isEmpty()) {
            return Collections.emptyList();
        }

        final ArrayList<TopicPermission> topicPermissions = new ArrayList<>();
        for (String clientRole : clientRoles) {
            final Role role = roles.get(clientRole);
            for (Permission permission : role.getPermissions()) {
                topicPermissions.add(toTopicPermission(clientId, userName, permission));
            }
        }

        return topicPermissions;
    }

    @NotNull
    private String encodePassword(final @NotNull ByteBuffer password) {
        byte[] passwordBytes = new byte[password.remaining()];
        password.get(passwordBytes);
        return Base64.getEncoder().encodeToString(passwordBytes);
    }

    private void updateUsersMap(final @NotNull FileAuthConfig config) {
        final List<User> newUsers = config.getUsers();

        final ConcurrentHashMap<String, User> newUsersMap = new ConcurrentHashMap<>(newUsers.size());
        for (User newUser : newUsers) {
            newUsersMap.put(newUser.getName(), newUser);
        }

        final Lock writeLock = usersLock.writeLock();
        writeLock.lock();
        try {
            users = newUsersMap;
        } finally {
            writeLock.unlock();
        }
    }

    private void updateRolesMap(final @NotNull FileAuthConfig config) {
        final List<Role> newRoles = config.getRoles();

        final ConcurrentHashMap<String, Role> newRolesMap = new ConcurrentHashMap<>(newRoles.size());
        for (Role newRole : newRoles) {
            newRolesMap.put(newRole.getId(), newRole);
        }

        final Lock writeLock = rolesLock.writeLock();
        writeLock.lock();
        try {
            roles = newRolesMap;
        } finally {
            writeLock.unlock();
        }
    }

    private TopicPermission toTopicPermission(@NotNull final String clientId, @NotNull final String userName, @NotNull final Permission permission) {
        return Builders.topicPermission()
                .topicFilter(getTopicFilter(clientId, userName, permission))
                .activity(permission.getActivity())
                .type(TopicPermission.PermissionType.ALLOW)
                .retain(permission.getRetain())
                .qos(permission.getQos())
                .sharedSubscription(permission.getSharedSubscription())
                .sharedGroup(permission.getSharedGroup())
                .build();
    }

    private String getTopicFilter(@NotNull final String clientId, @NotNull final String userName, final Permission permission) {
        final String configTopic = permission.getTopic();
        return Substitution.substitute(configTopic, clientId, userName);
    }

}
