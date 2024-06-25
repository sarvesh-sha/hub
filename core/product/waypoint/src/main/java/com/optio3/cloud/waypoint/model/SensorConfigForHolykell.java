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
import com.optio3.protocol.model.ipn.objects.holykell.HolykellModel;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForHolykell")
public class SensorConfigForHolykell extends SensorConfig
{
    public String  holykellPort;
    public boolean holykellInvert;

    //--//

    @Override
    public CompletableFuture<SensorResultForHolykell> exec(WaypointApplication app) throws
                                                                                    Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.holykellPort = BoxingUtils.get(holykellPort, "/optio3-dev/optio3_RS485");
        cfg.holykellInvert = holykellInvert;

        SensorResultForHolykell res = new SensorResultForHolykell();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.holykellPort, cfg.holykellInvert))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final HolykellModel obj2 = Reflection.as(obj, HolykellModel.class);
                if (obj2 != null)
                {
                    res.level = obj2.level;
                    res.temperature = obj2.temperature;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.level, res.temperature), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
