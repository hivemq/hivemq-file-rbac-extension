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

    @Nullable
    @XmlElement(name = "topic", required = true)
    private String topic;

    @NotNull
    @XmlElement(name = "activity", defaultValue = "ALL")
    private TopicPermission.MqttActivity activity = TopicPermission.MqttActivity.ALL;

    @NotNull
    @XmlElement(name = "qos", defaultValue = "ALL")
    private TopicPermission.Qos qos = TopicPermission.Qos.ALL;

    @NotNull
    @XmlElement(name = "retain", defaultValue = "ALL")
    private TopicPermission.Retain retain = TopicPermission.Retain.ALL;

    @NotNull
    @XmlElement(name = "shared-subscription", defaultValue = "ALL")
    private TopicPermission.SharedSubscription sharedSubscription = TopicPermission.SharedSubscription.ALL;

    @NotNull
    @XmlElement(name = "shared-group", defaultValue = "#")
    private String sharedGroup = "#";

    public Permission() {
    }

    public Permission(@Nullable final String topic) {
        this.topic = topic;
    }

    public Permission(
            @NotNull final String topic,
            @NotNull final TopicPermission.MqttActivity activity,
            @NotNull final TopicPermission.Qos qos,
            @NotNull final TopicPermission.Retain retain,
            @NotNull final TopicPermission.SharedSubscription sharedSubscription,
            @NotNull final String sharedGroup) {
        this.topic = topic;
        this.activity = activity;
        this.qos = qos;
        this.retain = retain;
        this.sharedSubscription = sharedSubscription;
        this.sharedGroup = sharedGroup;
    }

    @Nullable
    public String getTopic() {
        return topic;
    }

    public void setTopic(@NotNull final String topic) {
        this.topic = topic;
    }

    @NotNull
    public TopicPermission.MqttActivity getActivity() {
        return activity;
    }

    public void setActivity(@NotNull final TopicPermission.MqttActivity activity) {
        this.activity = activity;
    }

    @NotNull
    public TopicPermission.Qos getQos() {
        return qos;
    }

    public void setQos(@NotNull final TopicPermission.Qos qos) {
        this.qos = qos;
    }

    @NotNull
    public TopicPermission.Retain getRetain() {
        return retain;
    }

    public void setRetain(@NotNull final TopicPermission.Retain retain) {
        this.retain = retain;
    }

    @NotNull
    public TopicPermission.SharedSubscription getSharedSubscription() {
        return sharedSubscription;
    }

    public void setSharedSubscription(@NotNull final TopicPermission.SharedSubscription sharedSubscription) {
        this.sharedSubscription = sharedSubscription;
    }

    @NotNull
    public String getSharedGroup() {
        return sharedGroup;
    }

    public void setSharedGroup(@NotNull final String sharedGroup) {
        this.sharedGroup = sharedGroup;
    }

    @NotNull
    @Override
    public String toString() {
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
