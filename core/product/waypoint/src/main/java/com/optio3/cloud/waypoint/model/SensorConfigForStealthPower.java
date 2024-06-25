/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.objects.stealthpower.BaseStealthPowerModel;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForStealthPower")
public class SensorConfigForStealthPower extends SensorConfig
{
    public String stealthpowerPort;

    //--//

    @Override
    public CompletableFuture<SensorResultForStealthPower> exec(WaypointApplication app) throws
                                                                                        Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.stealthpowerPort = BoxingUtils.get(stealthpowerPort, "/optio3-dev/optio3_RS232");

        SensorResultForStealthPower res = new SensorResultForStealthPower();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.stealthpowerPort, false))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final BaseStealthPowerModel obj_sp = Reflection.as(obj, BaseStealthPowerModel.class);
                if (obj_sp != null)
                {
                    Object voltage = obj_sp.getField("supply_voltage");
                    if (voltage == null)
                    {
                        voltage = obj_sp.getField("oem_voltage");
                    }
                    if (voltage instanceof Float)
                    {
                        res.supply_voltage = (float) voltage;
                    }
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.supply_voltage), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
