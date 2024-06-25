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
import com.optio3.protocol.model.ipn.objects.epsolar.EpSolar_RealTimeData;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForEpSolar")
public class SensorConfigForEpSolar extends SensorConfig
{
    public String  epsolarPort;
    public boolean epsolarInvert;

    //--//

    @Override
    public CompletableFuture<SensorResultForEpSolar> exec(WaypointApplication app) throws
                                                                                   Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.epsolarPort = BoxingUtils.get(epsolarPort, "/optio3-dev/optio3_RS485");
        cfg.epsolarInvert = epsolarInvert;

        SensorResultForEpSolar res = new SensorResultForEpSolar();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.epsolarPort, cfg.epsolarInvert))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final EpSolar_RealTimeData obj2 = Reflection.as(obj, EpSolar_RealTimeData.class);
                if (obj2 != null)
                {
                    res.arrayVoltage = obj2.array_input_voltage;
                    res.arrayCurrent = obj2.array_input_voltage;
                    res.batteryVoltage = obj2.battery_voltage;
                    res.batteryCurrent = obj2.battery_current;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.arrayVoltage, res.arrayCurrent, res.batteryVoltage, res.batteryCurrent), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
