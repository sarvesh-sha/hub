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
import com.optio3.protocol.model.ipn.objects.hendrickson.Hendrickson_Watchman;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForHendricksonWatchman")
public class SensorConfigForHendricksonWatchman extends SensorConfig
{
    public String  canPort;
    public int     canFrequency;
    public boolean canNoTermination;
    public boolean canInvert;

    //--//

    @Override
    public CompletableFuture<SensorResultForHendricksonWatchman> exec(WaypointApplication app) throws
                                                                                               Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.canPort          = BoxingUtils.get(canPort, "can0");
        cfg.canFrequency     = canFrequency;
        cfg.canNoTermination = canNoTermination;
        cfg.canInvert        = canInvert;

        var res = new SensorResultForHendricksonWatchman();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.canPort, cfg.canInvert))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final Hendrickson_Watchman obj_wheel = Reflection.as(obj, Hendrickson_Watchman.class);
                if (obj_wheel != null)
                {
                    if (!Float.isNaN(obj_wheel.wheelTemperature))
                    {
                        res.temperature = obj_wheel.wheelTemperature;
                    }

                    if (!Float.isNaN(obj_wheel.wheelPressurePortA))
                    {
                        res.pressure = obj_wheel.wheelPressurePortA;
                    }

                    if (!Float.isNaN(obj_wheel.wheelPressurePortB))
                    {
                        res.pressure = obj_wheel.wheelPressurePortB;
                    }
                    return;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.temperature, res.pressure), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
