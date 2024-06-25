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
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_FilteredADC;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForTriStar")
public class SensorConfigForTriStar extends SensorConfig
{
    public String tristarPort;

    //--//

    @Override
    public CompletableFuture<SensorResultForTriStar> exec(WaypointApplication app) throws
                                                                                   Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.tristarPort = BoxingUtils.get(tristarPort, "/optio3-dev/optio3_RS485");

        SensorResultForTriStar res = new SensorResultForTriStar();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.tristarPort, false))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final TriStar_FilteredADC obj_state = Reflection.as(obj, TriStar_FilteredADC.class);
                if (obj_state != null)
                {
                    res.arrayVoltage = obj_state.adc_va_f;
                    res.arrayCurrent = obj_state.adc_ia_f_shadow;
                    res.batteryVoltage = obj_state.adc_vb_f_med;
                    res.batteryCurrent = obj_state.adc_ib_f_shadow;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.arrayVoltage, res.arrayCurrent, res.batteryVoltage, res.batteryCurrent), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
