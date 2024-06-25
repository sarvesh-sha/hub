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
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForJ1939")
public class SensorConfigForJ1939 extends SensorConfig
{
    public String  obdiiPort;
    public int     obdiiFrequency;
    public boolean obdiiInvert;

    //--//

    @Override
    public CompletableFuture<SensorResultForJ1939> exec(WaypointApplication app) throws
                                                                                 Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.obdiiPort = BoxingUtils.get(obdiiPort, "can0");
        cfg.obdiiFrequency = obdiiFrequency;
        cfg.obdiiInvert = obdiiInvert;

        SensorResultForJ1939 res = new SensorResultForJ1939();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.obdiiPort, cfg.obdiiInvert))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final ObdiiObjectModel obj_obdii = Reflection.as(obj, ObdiiObjectModel.class);
                if (obj_obdii != null)
                {
                    res.found.add(obj_obdii.extractUnitId());
                }
            });

            await(exec(manager, () -> res.success = !res.found.isEmpty(), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
