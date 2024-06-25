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
import com.optio3.protocol.model.ipn.objects.palfinger.Palfinger_Counters;
import com.optio3.protocol.model.ipn.objects.palfinger.Palfinger_SupplyVoltage;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForPalfinger")
public class SensorConfigForPalfinger extends SensorConfig
{
    public String  canPort;
    public int     canFrequency;
    public boolean canNoTermination;
    public boolean canInvert;

    //--//

    @Override
    public CompletableFuture<SensorResultForPalfinger> exec(WaypointApplication app) throws
                                                                                     Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.canPort = BoxingUtils.get(canPort, "can0");
        cfg.canFrequency = canFrequency;
        cfg.canNoTermination = canNoTermination;
        cfg.canInvert = canInvert;

        SensorResultForPalfinger res = new SensorResultForPalfinger();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.canPort, cfg.canInvert))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final Palfinger_Counters obj_counters = Reflection.as(obj, Palfinger_Counters.class);
                if (obj_counters != null)
                {
                    res.counterService = obj_counters.counterService;
                    return;
                }

                final Palfinger_SupplyVoltage obj_voltage = Reflection.as(obj, Palfinger_SupplyVoltage.class);
                if (obj_voltage != null)
                {
                    res.supplyVoltage = obj_voltage.supplyVoltage_V27;
                    res.plcTemperature = obj_voltage.plcTemperature;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.counterService, res.supplyVoltage, res.plcTemperature), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
