/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

public class DiscoveryState
{
    public boolean doneDiscovery;
    public boolean doneDeviceFetch;
    public boolean doneObjectsListing;
    public boolean donePropertiesFetch;
    public boolean doneAutoConfigureSampling;

    public boolean readyForDataCollection;

    //--//

    public DiscoveryRequest requestDiscovery;
    public DiscoveryRequest requestDeviceFetch;
    public DiscoveryRequest requestObjectsListing;
    public DiscoveryRequest requestPropertiesFetch;
    public DiscoveryRequest requestAutoConfigureSampling;
}
