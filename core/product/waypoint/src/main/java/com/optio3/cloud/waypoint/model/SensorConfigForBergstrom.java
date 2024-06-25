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
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters;
import com.optio3.protocol.model.ipn.objects.nitephoenix.NitePhoenix_HVAC_Unit;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForBergstrom")
public class SensorConfigForBergstrom extends SensorConfig
{
    public String  canPort;
    public int     canFrequency;
    public boolean canNoTermination;
    public boolean canInvert;

    //--//

    @Override
    public CompletableFuture<SensorResultForBergstrom> exec(WaypointApplication app) throws
                                                                                     Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.canPort = BoxingUtils.get(canPort, "can0");
        cfg.canFrequency = canFrequency;
        cfg.canNoTermination = canNoTermination;
        cfg.canInvert = canInvert;

        SensorResultForBergstrom res = new SensorResultForBergstrom();

        IpnManager manager = prepare(cfg, res, (obj) ->
        {
            final NitePhoenix_HVAC_Unit obj_hvac = Reflection.as(obj, NitePhoenix_HVAC_Unit.class);
            if (obj_hvac != null)
            {
                res.compressorSpeed = obj_hvac.compressorSpeed;
                return;
            }

            final NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs obj_state = Reflection.as(obj, NitePhoenix_BatteryMonitoringSystem_AlgorithmOutputs.class);
            if (obj_state != null)
            {
                res.stateOfCharge = obj_state.stateOfCharge;
            }

            final NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters obj_voltage = Reflection.as(obj, NitePhoenix_BatteryMonitoringSystem_AuxiliaryBatteryParameters.class);
            if (obj_voltage != null)
            {
                res.voltage = obj_voltage.voltage;
            }
        });

        await(exec(manager, () -> res.success = checkValid(res.compressorSpeed, res.stateOfCharge), (error) -> res.failure = error));

        return wrapAsync(res);
    }
}
