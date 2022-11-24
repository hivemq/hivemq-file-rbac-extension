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
package com.hivemq.extensions.rbac.configuration.entities;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Set;

@XmlRootElement(name = "extension-configuration")
@XmlType(propOrder = {})
@XmlAccessorType(XmlAccessType.FIELD)
public class ExtensionConfig {

    @XmlElement(name = "credentials-reload-interval", defaultValue = "60")
    private int reloadInterval = 60;

    @XmlElementWrapper(name = "listener-names")
    @XmlElement(name = "listener-name")
    private @Nullable Set<String> listenerNames;

    @XmlElement(name = "password-type", defaultValue = "HASHED")
    private @Nullable PasswordType passwordType = PasswordType.HASHED;

    @XmlElement(name = "next-extension-instead-of-fail", defaultValue = "false")
    private boolean nextExtensionInsteadOfFail = false;

    public ExtensionConfig() {
    }

    public ExtensionConfig(
            final int reloadInterval,
            final @Nullable Set<String> listenerNames,
            final @Nullable PasswordType passwordType,
            final boolean nextExtensionInsteadOfFail) {
        this.reloadInterval = reloadInterval;
        this.listenerNames = listenerNames;
        this.passwordType = passwordType;
        this.nextExtensionInsteadOfFail = nextExtensionInsteadOfFail;
    }

    public int getReloadInterval() {
        return reloadInterval;
    }

    public void setReloadInterval(final int reloadInterval) {
        this.reloadInterval = reloadInterval;
    }

    public @Nullable Set<String> getListenerNames() {
        return listenerNames;
    }

    public @Nullable PasswordType getPasswordType() {
        return passwordType;
    }

    public void setPasswordType(final @Nullable PasswordType passwordType) {
        this.passwordType = passwordType;
    }

    public boolean isNextExtensionInsteadOfFail() {
        return nextExtensionInsteadOfFail;
    }

    @Override
    public @NotNull String toString() {
        return "ExtensionConfig{" +
                "reloadInterval=" +
                reloadInterval +
                ", listenerNames=" +
                listenerNames +
                ", passwordType=" +
                passwordType +
                ", nextExtensionInsteadOfFail=" +
                nextExtensionInsteadOfFail +
                '}';
    }
}
