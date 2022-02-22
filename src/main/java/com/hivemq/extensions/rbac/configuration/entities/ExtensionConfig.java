/*
 * Copyright 2018 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hivemq.extensions.rbac.configuration.entities;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.xml.bind.annotation.*;
import java.util.Set;


@XmlRootElement(name = "extension-configuration")
@XmlType(propOrder = {})
@XmlAccessorType(XmlAccessType.FIELD)
public class ExtensionConfig {

    @XmlElement(name = "credentials-reload-interval", defaultValue = "60")
    private int reloadInterval = 60;

    @Nullable
    @XmlElementWrapper(name = "listener-names")
    @XmlElement(name = "listener-name")
    private Set<String> listenerNames;

    @NotNull
    @XmlElement(name = "password-type", defaultValue = "HASHED")
    private PasswordType passwordType = PasswordType.HASHED;

    @XmlElement(name = "next-extension-instead-of-fail", defaultValue = "false")
    private boolean nextExtensionInsteadOfFail = false;

    public ExtensionConfig() {
    }

    public ExtensionConfig(final int reloadInterval,
                           final Set<String> listenerNames,
                           final @NotNull PasswordType passwordType,
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

    public Set<String> getListenerNames() {
        return listenerNames;
    }

    public void setListenerNames(Set<String> listenerNames) {
        this.listenerNames = listenerNames;
    }

    @NotNull
    public PasswordType getPasswordType() {
        return passwordType;
    }

    public void setPasswordType(final @NotNull PasswordType passwordType) {
        this.passwordType = passwordType;
    }

    public boolean isNextExtensionInsteadOfFail() {
        return nextExtensionInsteadOfFail;
    }

    public void setNextExtensionInsteadOfFail(boolean nextExtensionInsteadOfFail) {
        this.nextExtensionInsteadOfFail = nextExtensionInsteadOfFail;
    }

    @Override
    public String toString() {
        return "ExtensionConfig{" +
                "reloadInterval=" + reloadInterval +
                ", listenerNames=" + listenerNames +
                ", passwordType=" + passwordType +
                ", nextExtensionInsteadOfFail=" + nextExtensionInsteadOfFail +
                '}';
    }
}
