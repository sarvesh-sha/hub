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
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_MasterValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_UnitValues;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForBluesky")
public class SensorConfigForBluesky extends SensorConfig
{
    public String  ipnPort;
    public int     ipnBaudrate;
    public boolean ipnInvert;

    //--//

    @Override
    public CompletableFuture<SensorResultForBluesky> exec(WaypointApplication app) throws
                                                                                   Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.ipnPort = BoxingUtils.get(ipnPort, "/optio3-dev/optio3_RS485");
        cfg.ipnBaudrate = ipnBaudrate;
        cfg.ipnInvert = ipnInvert;

        SensorResultForBluesky res = new SensorResultForBluesky();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.ipnPort, cfg.ipnInvert))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final BlueSky_MasterValues obj_master = Reflection.as(obj, BlueSky_MasterValues.class);
                if (obj_master != null)
                {
                    res.inputCurrent = obj_master.totalInputCurrent;
                    res.batteryVoltage = obj_master.batteryVoltage;
                    res.batteryCurrent = obj_master.totalOutputCurrent;
                    res.totalChargeAH = obj_master.totalChargeAH;
                    return;
                }

                final BlueSky_UnitValues obj_unit = Reflection.as(obj, BlueSky_UnitValues.class);
                if (obj_unit != null)
                {
                    res.inputVoltage = obj_unit.inputVoltage;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.batteryVoltage, res.batteryCurrent, res.inputVoltage, res.inputCurrent, res.totalChargeAH), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
