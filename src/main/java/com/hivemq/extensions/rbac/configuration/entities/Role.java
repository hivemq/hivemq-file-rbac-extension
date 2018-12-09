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


@XmlType(propOrder = {})
@XmlAccessorType(XmlAccessType.FIELD)
public class Role {

    @Nullable
    @XmlElement(name = "id")
    private String id;

    @Nullable
    @XmlElementWrapper(name = "permissions")
    @XmlElement(name = "permission")
    private List<Permission> permissions;

    public Role() {
    }

    public Role(@NotNull final String id, @NotNull final List<Permission> permissions) {
        this.id = id;
        this.permissions = permissions;
    }

    @Nullable
    public String getId() {
        return id;
    }

    public void setId(@NotNull final String id) {
        this.id = id;
    }

    @Nullable
    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(@NotNull final List<Permission> permissions) {
        this.permissions = permissions;
    }

    @NotNull
    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", permissions=" + permissions +
                '}';
    }
}
