/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.bacnet;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.driver.BDevice;
import com.optio3.product.importers.niagara.baja.driver.BPointDeviceExt;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "bacnet", type = "BacnetDevice")
public class BBacnetDevice extends BDevice
{
    public BBacnetAddress         address;
    public BBacnetPointDeviceExt  points;
    public BValue                 virtual;
    public BValue                 alarms;
    public BValue                 schedules;
    public BValue                 trendLogs;
    public BBacnetConfigDeviceExt config;
    public BValue                 enumerationList;
    public BValue                 useCov;
    public BValue                 maxCovSubscriptions;
    public BValue                 covSubscriptions;
    public BValue                 pollFrequency;
    public BValue                 characterSet;
    public BValue                 maxPollTimeouts;

    //--//

    @Override
    protected BPointDeviceExt getPoints()
    {
        return points;
    }
}

