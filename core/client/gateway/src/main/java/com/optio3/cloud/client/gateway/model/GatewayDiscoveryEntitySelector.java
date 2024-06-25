/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model;

public enum GatewayDiscoveryEntitySelector
{
    //@formatter:off
    Host               (false), // selectorValue: host identifier
    Gateway            (false), // selectorValue: gateway identifier
    Network            (false), // selectorValue: network identifier
    Protocol           (false), // selectorValue: protocol name
    //--//
    BACnet_Device      (false), // selectorValue: network number/instance number
    BACnet_Reachability(false), // selectorValue: none                                 , contents: instance of DeviceReachability
    BACnet_Object      (false), // selectorValue: object type/number                   , contents: instance of BACnetObjectModel, with only the sampled properties configured
    BACnet_ObjectSet   (false), // selectorValue: object type/number                   , contents: instance of BACnetObjectModel, with only the updated properties configured
    BACnet_ObjectConfig(false), // selectorValue: object type/number                   , contents: map of property name to integer period
    BACnet_ObjectSample(true ), // selectorValue: none                                 , contents: instance of BACnetObjectModel, with only the sampled properties configured
    //--//
    Ipn_Device         (false), // selectorValue: device identifier
    Ipn_Reachability   (false), // selectorValue: none                                 , contents: instance of DeviceReachability
    Ipn_Object         (false), // selectorValue: unit number                          , contents: instance of IpnObjectModel
    Ipn_ObjectConfig   (false), // selectorValue: object type/number                   , contents: map of property name to integer period
    Ipn_ObjectSample   (true ), // selectorValue: none                                 , contents: instance of IpnObjectModel
    //--//
    Perf_Device        (false), // selectorValue: device identifier
    Perf_Object        (false), // selectorValue: object name
    Perf_ObjectSample  (true ); // selectorValue: none                                 , contents: instance of one of the *PerformanceCounters classes
    //@formatter:on

    private final boolean m_isSample;

    GatewayDiscoveryEntitySelector(boolean isSample)
    {
        m_isSample = isSample;
    }

    public boolean isSample()
    {
        return m_isSample;
    }
}
