:hivemq-link: http://www.hivemq.com
:hivemq-extension-docs-link: http://www.hivemq.com/docs/extensions/latest/
:hivemq-extension-docs-archetype-link: http://www.hivemq.com/docs/extensions/latest/#maven-archetype-chapter
:hivemq-blog-tools: http://www.hivemq.com/mqtt-toolbox
:maven-documentation-profile-link: http://maven.apache.org/guides/introduction/introduction-to-profiles.html
:hivemq-support: http://www.hivemq.com/support/
:hivemq-listener: https://www.hivemq.com/docs/hivemq/4.4/user-guide/listeners.html#tcp-listener

== HiveMQ File Role based Access Control Extension

*Extension Type*: Security

*Version*: 4.4.0

*License*: Apache License 2.0

=== Purpose

The File Authentication and Authorization Extension implements Access Control based on a configuration file.

This extension implements the configuration for authentication and authorization rules via XML-file.
These mechanism are important to protect a MQTT deployment, and the data which is exchanged, from unwanted access.

The extension provides fine grained control on a topic level to limit clients to specific topics and specific actions (publish or subscribe). Substitution rules for clientId and username allow for dynamic roles to be applied to multiple clients, while still limiting each client to "their own" topics.

=== Features

* Username and password based authentication for MQTT Clients
* Fine grained access control on a topic-filter level
* Role based permission management
* Automatic Substitution of client identifier and username
* Runtime reload for Credentials and Roles
* Support for Hashed or Plain-text passwords
* Tooling to generate salted password hashes
* Option to define a set of listeners the extension is used for

=== Installation

. Unzip the file: `hivemq-file-rbac-extension-<version>-distribution.zip` to the directory: `<HIVEMQ_HOME>/extensions`
. Configure the extension (`extension-config.xml`) and the credentials (`credentials.xml`).
. Start HiveMQ

=== First Steps

Install the extension. The users and roles from the example configuration files are now applied to all new MQTT connections.

=== Next Steps

Setup your custom Users and Roles in the credentials configuration and configure the extension for your specific use case.

CAUTION: Because client identifier and user names can be used for <<substitution,substitution>> in the permissions, MQTT wildcard characters `#` and  `+` are prohibited for client identifier and user names when this extension is used. MQTT connections which include these characters are denied by this extension.

[#configuration]
=== Configuration

This extension has two configuration files. The <<extensions-config,extension configuration>> file (`extension-config.xml`) that includes the general configuration for the extension itself.
And the <<credentials-config,credentials configuration>> file (`credentials.xml`) that includes the configuration of Users, Roles and Permissions.

The credentials configuration file is watched for changes and reloaded at runtime if necessary. If the credentials configuration file has changed and contains a valid configuration, then the previous configuration is automatically archived to an archive folder `credentials-archive` inside the extension folder. So that changes can be tracked and rolled-back if needed.
If the new credentials configuration is invalid the current configuration is maintained.

NOTE: The permissions for connected clients are not changed, only new connecting clients are affected.

[#credentials-config]
==== Credentials configuration

The credentials configuration includes the following settings.

.Example credentials.xml
[source,xml]
----
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<file-rbac>
    <users>
        <user>
            <name>user1</name>
            <!--- password hash for "pass1" -->
            <password>WFNQUVB0UkxjM04xa0hSR1BQNGhuOTJKVzdlbXA4bjk=:100:FY12nwpUEbBK9EKQ/Aw/rQKSoA7jXsC0HKELwU2mLCVU39bJVK0zf4NemuFeDOHPO4BW1nOjxi6NporkC6rUog==</password>
            <roles>
                <id>role1</id>
            </roles>
        </user>
        <user>
            <name>admin-user</name>
            <!-- password hash for "admin-password" -->
            <password>Vjc1a0lxQ3Nvb0ljNFVHNE9WRnM3RG1IZmdNUFcwVGY=:100:PL2FLqfpdhONG7qXjAMmdVn4wlMiXnypdXiFW09zqorFhKgoiixFQw2EVJJfE9Zn79q45V7Xpc6JeKLp0ntmYA==</password>
            <roles>
                <id>role1</id>
                <id>superuser</id>
            </roles>
        </user>
    </users>
    <roles>
        <role>
            <id>role1</id>
            <permissions>
                <permission>
                    <!-- PUBLISH and SUBSCRIBE to all topics below "data/<clientid>/" -->
                    <topic>data/${{clientid}}/#</topic>
                </permission>
                <permission>
                    <!-- PUBLISH to topic "outgoing/<clientid>", retained only-->
                    <topic>outgoing/${{clientid}}</topic>
                    <activity>PUBLISH</activity>
                    <retain>RETAINED</retain>
                </permission>
                <permission>
                    <!-- SUBSCRIBE to topic "incoming/<username>"-->
                    <topic>incoming/${{username}}/actions</topic>
                    <activity>SUBSCRIBE</activity>
                </permission>
            </permissions>
        </role>
        <role>
            <id>superuser</id>
            <permissions>
                <permission>
                    <!-- Allow everything -->
                    <topic>#</topic>
                </permission>
            </permissions>
        </role>
    </roles>
</file-rbac>
----

===== User configuration


|===
|Configuration |Description
|`name` |Username that is presented by the client in the MQTT CONNECT packet.
|`password` |Password that is presented by the client in the MQTT CONNECT packet. Plain text or hashed passwords are supported.
|`roles` |List of IDs of a role which is defined in the same configuration file. The permissions of these roles are applied to the user.
|===

Hashed password strings for the credentials configuration can be generated by running the included password generator tool with the following command, from inside the extension folder.

.Example Usage
[source,bash]
----
java -jar hivemq-file-rbac-extension-4.0.0.jar -p mypassword
----

This tool utilizes the configuration from the extension configuration file (`extension-config.xml`) to generate salted password hashes with the same settings as the extension. By default the tool searches for the configuration file in the working directory. A custom location can be specified with the `-c` parameter.

A custom salt can be passed with the `-s` parameter, by default a random salt is generated.

The amount of hashing iteration can be specified with the `-i` parameter.


.Example with hashed password
[source,xml]
----
<user>
    <name>user1</name>
    <!--- password hash for "pass1" -->
    <password>TUh5SWZlWmRNNzJQeXU0UkF2QmVKZXBBWFl6VU1Jc28=:gDR4bZ8kABBEL0WBflf09IMJahRlb1KGL2wJydlyWElfIu1F65SSU+RZZpjzy+vT4dDPJxiBSHM07wr56+bKsA==</password>
    <roles>
        <id>role1</id>
    </roles>
</user>
----

.Example with plain text password
[source,xml]
----
<user>
    <name>user1</name>
    <password>pass1</password>
    <roles>
        <id>role1</id>
    </roles>
</user>
----

===== Role configuration

|===
|Configuration |Description
|`id` |The ID for this role.
|`permissions` |A list of permissions which are applied for this role. Permissions are applied and checked by HiveMQ in the order the appear in the configuration file.
|===

===== Permisssion configuration

|===
|Configuration |Default |Description
|`topic` |-|The topic on which this permission should apply. Can contain standard MQTT wildcards `#` and `+`. Also special substitution with `${{clientid}}` and `${{username}}` is supported.
|`activity` |`ALL` |The activity which this client can perform on this topic. Can be `PUBLISH`, `SUBSCRIBE` or `ALL`.
|`qos` |`ALL` |The MQTT QoS which this client can publish/subscribe with on this topic. The value can be `ZERO`, `ONE`, `TWO`, `ZERO_ONE`, `ONE_TWO`, `ZERO_TWO` or `ALL`.
|`retained` |`ALL` |If a message published on this topic can/must be retained. Values are `NOT_RETAINED`, `RETAINED` or `ALL`. This setting is only relevant for PUBLISH messages.
|`shared-subscription` |`ALL` |If a subscription on this topic can/must be a shared subscription. Values are `SHARED`, `NOT_SHARED` or `ALL`. This setting is only relevant for SUBSCRIBE messages.
|`shared-group` |`#` |Limits the Shared Subscription group name for a subscription. Values are `#` to match all or a specific string value. This setting is only relevant for SUBSCRIBE messages that include a Shared Subscription.
|===

[#substitution]
===== Substitution
The special markers `${{clientid}}` and `${{username}}` in the topic filter for a permission are automatically replaced by the extension with the client identifier and username of the client for which authorization is performed. This allows to configure a permission that applies to multiple clients, but always contains their specific client identifier or username in the topic. Limiting each client to "their own" topics.

[#extensions-config]
==== Extension configuration

The credentials configuration includes the following settings.

.Example extension-config.xml
[source,xml]
----
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<extension-configuration>

    <!-- Reload interval for credentials in seconds -->
    <credentials-reload-interval>60</credentials-reload-interval>

    <!-- Optional list of names of listeners this extension is used for
    <listener-names>
        <listener-name>my-listener</listener-name>
        <listener-name>my-listener-2</listener-name>
    </listener-names> -->

    <!-- If the credentials file is using HASHED or PLAIN passwords -->
    <password-type>HASHED</password-type>

</extension-configuration>

----


|===
|Configuration |Default |Description
|`credentials-reload-interval` |`60` |Regular interval in seconds, in which the `credentials.xml` configuration file is checked for changes and reloaded.
|`listener-names` |`null` |List of names of listeners, this extension will be used for. See {hivemq-listener}[HiveMQ config details^].
|`password-type` |`HASHED` |How passwords are stored in the `credentials.xml` configuration file. Can either bei `PLAIN` for plain text passwords, or `HASHED` for a salted password hash.
|===

NOTE: The `listener-names` feature requires the use of at least HiveMQ 4.1 / HiveMQ CE 2020.1

==== Need help?

If you encounter any problems, we are happy to help. The best place to get in contact is our {hivemq-support}[support^].


= Contributing

If you want to contribute to HiveMQ File RBAC Extension, see the link:CONTRIBUTING.md[contribution guidelines].

= License

HiveMQ File RBAC Extension is licensed under the `APACHE LICENSE, VERSION 2.0`. A copy of the license can be found link:LICENSE.txt[here].

