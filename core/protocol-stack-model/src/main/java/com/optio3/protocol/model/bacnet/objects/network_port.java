/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.objects;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetAddressBinding;
import com.optio3.protocol.model.bacnet.constructed.BACnetBDTEntry;
import com.optio3.protocol.model.bacnet.constructed.BACnetFDTEntry;
import com.optio3.protocol.model.bacnet.constructed.BACnetHostNPort;
import com.optio3.protocol.model.bacnet.constructed.BACnetNameValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetRouterEntry;
import com.optio3.protocol.model.bacnet.constructed.BACnetVMACEntry;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.protocol.model.bacnet.enums.BACnetIPMode;
import com.optio3.protocol.model.bacnet.enums.BACnetNetworkNumberQuality;
import com.optio3.protocol.model.bacnet.enums.BACnetNetworkPortCommand;
import com.optio3.protocol.model.bacnet.enums.BACnetNetworkType;
import com.optio3.protocol.model.bacnet.enums.BACnetNotifyType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetProtocolLevel;
import com.optio3.protocol.model.bacnet.enums.BACnetReliability;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;

// NOTE: Generated automatically by regenerateObjectModels unit test!!
@JsonTypeName("BACnet:network_port")
public final class network_port extends BACnetObjectModel
{
    // @formatter:off
    @JsonIgnore // Avoid serializing the identity, it would be a duplicate in most cases.
    public BACnetObjectIdentifier              object_identifier;

    @JsonIgnore // Avoid serializing the type, we already know it.
    public BACnetObjectTypeOrUnknown           object_type;

    ///////////////////
    //
    // Required fields:
    //
    public long                                apdu_length;
    public boolean                             changes_pending;
    public float                               link_speed;
    public long                                network_number;
    public BACnetNetworkNumberQuality          network_number_quality;
    public BACnetNetworkType                   network_type;
    public String                              object_name;
    public boolean                             out_of_service;
    public BACnetPropertyIdentifierOrUnknown[] property_list;
    public BACnetProtocolLevel                 protocol_level;
    public BACnetReliability                   reliability;
    public BACnetStatusFlags                   status_flags;

    ///////////////////
    //
    // Optional fields:
    //
    public BACnetEventTransitionBits           acked_transitions;
    public boolean                             auto_slave_discovery;
    public BACnetHostNPort                     bacnet_ip_global_address;
    public BACnetIPMode                        bacnet_ip_mode;
    public byte[]                              bacnet_ip_multicast_address;
    public boolean                             bacnet_ip_nat_traversal;
    public long                                bacnet_ip_udp_port;
    public BACnetIPMode                        bacnet_ipv6_mode;
    public byte[]                              bacnet_ipv6_multicast_address;
    public long                                bacnet_ipv6_udp_port;
    public boolean                             bbmd_accept_fd_registrations;
    public List<BACnetBDTEntry>                bbmd_broadcast_distribution_table;
    public List<BACnetFDTEntry>                bbmd_foreign_device_table;
    public BACnetNetworkPortCommand            command;
    public String                              description;
    public boolean                             event_detection_enable;
    public BACnetEventTransitionBits           event_enable;
    public String[]                            event_message_texts;
    public String[]                            event_message_texts_config;
    public BACnetEventState                    event_state;
    public BACnetTimeStamp[]                   event_time_stamps;
    public BACnetHostNPort                     fd_bbmd_address;
    public long                                fd_subscription_lifetime;
    public byte[]                              ip_address;
    public byte[]                              ip_default_gateway;
    public boolean                             ip_dhcp_enable;
    public long                                ip_dhcp_lease_time;
    public long                                ip_dhcp_lease_time_remaining;
    public byte[]                              ip_dhcp_server;
    public byte[][]                            ip_dns_server;
    public byte[]                              ip_subnet_mask;
    public byte[]                              ipv6_address;
    public boolean                             ipv6_auto_addressing_enable;
    public byte[]                              ipv6_default_gateway;
    public long                                ipv6_dhcp_lease_time;
    public long                                ipv6_dhcp_lease_time_remaining;
    public byte[]                              ipv6_dhcp_server;
    public byte[][]                            ipv6_dns_server;
    public long                                ipv6_prefix_length;
    public String                              ipv6_zone_index;
    public boolean                             link_speed_autonegotiate;
    public float[]                             link_speeds;
    public byte[]                              mac_address;
    public List<BACnetAddressBinding>          manual_slave_address_binding;
    public long                                max_info_frames;
    public long                                max_master;
    public String                              network_interface_name;
    public long                                notification_class;
    public BACnetNotifyType                    notify_type;
    public String                              profile_location;
    public String                              profile_name;
    public long                                reference_port;
    public boolean                             reliability_evaluation_inhibit;
    public List<BACnetRouterEntry>             routing_table;
    public List<BACnetAddressBinding>          slave_address_binding;
    public boolean                             slave_proxy_enable;
    public BACnetNameValue[]                   tags;
    public List<BACnetVMACEntry>               virtual_mac_address_table;
    // @formatter:on

    public network_port()
    {
        super(BACnetObjectType.network_port);
        bbmd_broadcast_distribution_table = Lists.newArrayList();
        bbmd_foreign_device_table = Lists.newArrayList();
        event_message_texts = new String[3];
        event_message_texts_config = new String[3];
        event_time_stamps = new BACnetTimeStamp[3];
        event_time_stamps[0] = new BACnetTimeStamp();
        event_time_stamps[1] = new BACnetTimeStamp();
        event_time_stamps[2] = new BACnetTimeStamp();
        manual_slave_address_binding = Lists.newArrayList();
        routing_table = Lists.newArrayList();
        slave_address_binding = Lists.newArrayList();
        virtual_mac_address_table = Lists.newArrayList();
    }
}
