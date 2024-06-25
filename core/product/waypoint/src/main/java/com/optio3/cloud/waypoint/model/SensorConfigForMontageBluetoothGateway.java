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
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_PixelTagRaw;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_SmartLock;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_TemperatureHumiditySensor;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForMontageBluetoothGateway")
public class SensorConfigForMontageBluetoothGateway extends SensorConfig
{
    public String montageBluetoothGatewayPort;

    //--//

    @Override
    public CompletableFuture<SensorResultForMontageBluetoothGateway> exec(WaypointApplication app) throws
                                                                                                   Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.montageBluetoothGatewayPort = BoxingUtils.get(montageBluetoothGatewayPort, "/optio3-dev/optio3_RS232");

        SensorResultForMontageBluetoothGateway res = new SensorResultForMontageBluetoothGateway();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.montageBluetoothGatewayPort, false))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                if (obj instanceof BluetoothGateway_TemperatureHumiditySensor)
                {
                    res.detectedTRH = true;
                }
                if (obj instanceof BluetoothGateway_PixelTagRaw)
                {
                    res.detectedPixelTag = true;
                }
                if (obj instanceof BluetoothGateway_SmartLock)
                {
                    res.detectedSmartLock = true;
                }
            });

            await(exec(manager, () -> res.success = res.detectedHeartbeat || res.detectedTRH || res.detectedPixelTag || res.detectedSmartLock, (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
