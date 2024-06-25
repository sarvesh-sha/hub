/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.niagaraDriver;

import com.optio3.product.importers.niagara.ModuleTypeAnnotation;
import com.optio3.product.importers.niagara.baja.driver.BDevice;
import com.optio3.product.importers.niagara.baja.driver.BPointDeviceExt;
import com.optio3.product.importers.niagara.baja.sys.BOrd;
import com.optio3.product.importers.niagara.baja.sys.BValue;

@ModuleTypeAnnotation(module = "niagaraDriver", type = "NiagaraStation")
public class BNiagaraStation extends BDevice
{
    public BOrd                   address;
    public BValue                 clientConnection; // BFoxClientConnection
    public BValue                 serverConnection; // BFoxServerConnection
    public BValue                 hostModel;
    public BValue                 version;
    public BValue                 worker; // BStationWorker
    public BNiagaraPointDeviceExt points;
    public BValue                 histories; // BNiagaraHistoryDeviceExt
    public BValue                 alarms; // BNiagaraAlarmDeviceExt
    public BValue                 schedules; // BNiagaraScheduleDeviceExt
    public BValue                 users; // BNiagaraUserDeviceExt
    public BValue                 sysDef; // BNiagaraSysDefDeviceExt
    public BValue                 virtual; // BNiagaraVirtualDeviceExt
    public BValue                 virtualsEnabled;
    public BValue                 files; // BNiagaraFileDeviceExt

    //--//

    @Override
    protected BPointDeviceExt getPoints()
    {
        return points;
    }
}
