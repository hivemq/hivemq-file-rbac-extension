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
import java.util.List;

@XmlRootElement
@XmlType(propOrder = {})
@XmlAccessorType(XmlAccessType.FIELD)
public class User {

    @XmlElement(name = "name", required = true)
    private @Nullable String name;

    @XmlElement(name = "password", required = true)
    private @Nullable String password;

    @XmlElementWrapper(name = "roles")
    @XmlElement(name = "id")
    private @Nullable List<String> roles;

    public User() {
    }

    public User(final @Nullable String name, final @Nullable String password, final @Nullable List<String> roles) {
        this.name = name;
        this.password = password;
        this.roles = roles;
    }

    public @Nullable String getName() {
        return name;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public @Nullable List<String> getRoles() {
        return roles;
    }

    @Override
    public @NotNull String toString() {
        return "User{" + "name='" + name + '\'' + ", password='" + password + '\'' + ", roles=" + roles + '}';
    }
}
