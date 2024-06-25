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
import com.optio3.protocol.model.ipn.objects.argohytos.stealthpower.ArgoHytos_LubCos;
import com.optio3.protocol.model.ipn.objects.argohytos.stealthpower.ArgoHytos_OPComII;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForArgoHytos")
public class SensorConfigForArgoHytos extends SensorConfig
{
    public String argohytosPort;

    //--//

    @Override
    public CompletableFuture<SensorResultForArgoHytos> exec(WaypointApplication app) throws
                                                                                     Exception
    {
        var cfg = new ProtocolConfigForIpn();
        cfg.argohytosPort = BoxingUtils.get(argohytosPort, "/optio3-dev/optio3_RS232");

        var res = new SensorResultForArgoHytos();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.argohytosPort, false))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final var obj_opcom = Reflection.as(obj, ArgoHytos_OPComII.class);
                if (obj_opcom != null)
                {
                    res.ISO4um = 1 + obj_opcom.ISO4um;
                    res.temperature = 1;
                }

                final var obj_lubcos = Reflection.as(obj, ArgoHytos_LubCos.class);
                if (obj_lubcos != null)
                {
                    res.ISO4um = 1;
                    res.temperature = 1 + obj_lubcos.temperature;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.ISO4um, res.temperature), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
