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
import com.optio3.protocol.model.ipn.objects.victron.Victron_RealTimeData;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForVictron")
public class SensorConfigForVictron extends SensorConfig
{
    public String victronPort;

    //--//

    @Override
    public CompletableFuture<SensorResultForVictron> exec(WaypointApplication app) throws
                                                                                   Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.victronPort = BoxingUtils.get(victronPort, "/optio3-dev/optio3_RS485");

        SensorResultForVictron res = new SensorResultForVictron();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.victronPort, false))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final Victron_RealTimeData obj2 = Reflection.as(obj, Victron_RealTimeData.class);
                if (obj2 != null)
                {
                    res.panelVoltage = obj2.panel_voltage;
                    res.panelPower = obj2.panel_power;
                    res.batteryVoltage = obj2.battery_voltage;
                    res.batteryCurrent = obj2.battery_current;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.panelVoltage, res.panelPower, res.batteryVoltage, res.batteryCurrent), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
