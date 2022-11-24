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
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
@XmlAccessorType(XmlAccessType.FIELD)
public class Permission {

    @XmlElement(name = "topic", required = true)
    private @Nullable String topic;

    @XmlElement(name = "activity", defaultValue = "ALL")
    private @Nullable TopicPermission.MqttActivity activity = TopicPermission.MqttActivity.ALL;

    @XmlElement(name = "qos", defaultValue = "ALL")
    private @Nullable TopicPermission.Qos qos = TopicPermission.Qos.ALL;

    @XmlElement(name = "retain", defaultValue = "ALL")
    private @Nullable TopicPermission.Retain retain = TopicPermission.Retain.ALL;

    @XmlElement(name = "shared-subscription", defaultValue = "ALL")
    private @Nullable TopicPermission.SharedSubscription sharedSubscription = TopicPermission.SharedSubscription.ALL;

    @XmlElement(name = "shared-group", defaultValue = "#")
    private @Nullable String sharedGroup = "#";

    @SuppressWarnings("unused")
    public Permission() {
    }

    public Permission(final @Nullable String topic) {
        this.topic = topic;
    }

    public @Nullable String getTopic() {
        return topic;
    }

    public @Nullable TopicPermission.MqttActivity getActivity() {
        return activity;
    }

    public void setActivity(final @Nullable TopicPermission.MqttActivity activity) {
        this.activity = activity;
    }

    public @Nullable TopicPermission.Qos getQos() {
        return qos;
    }

    public void setQos(final @Nullable TopicPermission.Qos qos) {
        this.qos = qos;
    }

    public @Nullable TopicPermission.Retain getRetain() {
        return retain;
    }

    public void setRetain(final @Nullable TopicPermission.Retain retain) {
        this.retain = retain;
    }

    public @Nullable TopicPermission.SharedSubscription getSharedSubscription() {
        return sharedSubscription;
    }

    public void setSharedSubscription(final @Nullable TopicPermission.SharedSubscription sharedSubscription) {
        this.sharedSubscription = sharedSubscription;
    }

    public @Nullable String getSharedGroup() {
        return sharedGroup;
    }

    public void setSharedGroup(final @Nullable String sharedGroup) {
        this.sharedGroup = sharedGroup;
    }

    @Override
    public @NotNull String toString() {
        return "Permission{" +
                "topic='" +
                topic +
                '\'' +
                ", activity=" +
                activity +
                ", qos=" +
                qos +
                ", retain=" +
                retain +
                ", sharedSubscription=" +
                sharedSubscription +
                ", sharedGroup='" +
                sharedGroup +
                '\'' +
                '}';
    }
}
