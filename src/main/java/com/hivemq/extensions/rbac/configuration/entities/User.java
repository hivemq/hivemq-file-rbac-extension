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
import java.util.List;


@XmlRootElement
@XmlType(propOrder = {})
@XmlAccessorType(XmlAccessType.FIELD)
public class User {

    @Nullable
    @XmlElement(name = "name", required = true)
    private String name;

    @Nullable
    @XmlElement(name = "password", required = true)
    private String password;

    @Nullable
    @XmlElementWrapper(name = "roles")
    @XmlElement(name = "id")
    private List<String> roles;


    public User() {
    }

    public User(@NotNull final String name, @NotNull final String password, @NotNull final List<String> roles) {
        this.name = name;
        this.password = password;
        this.roles = roles;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@NotNull final String name) {
        this.name = name;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public void setPassword(@NotNull final String password) {
        this.password = password;
    }

    @Nullable
    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(@NotNull final List<String> roles) {
        this.roles = roles;
    }

    @NotNull
    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", roles=" + roles +
                '}';
    }

}
