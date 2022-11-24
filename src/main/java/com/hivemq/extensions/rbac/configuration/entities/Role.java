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
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlType(propOrder = {})
@XmlAccessorType(XmlAccessType.FIELD)
public class Role {

    @XmlElement(name = "id")
    private @Nullable String id;

    @XmlElementWrapper(name = "permissions")
    @XmlElement(name = "permission")
    private @Nullable List<Permission> permissions;

    public Role() {
    }

    public Role(final @Nullable String id, final @Nullable List<Permission> permissions) {
        this.id = id;
        this.permissions = permissions;
    }

    public @Nullable String getId() {
        return id;
    }

    public @Nullable List<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public @NotNull String toString() {
        return "Role{" + "id='" + id + '\'' + ", permissions=" + permissions + '}';
    }
}
