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
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.IpnSatellite;
import com.optio3.protocol.model.ipn.objects.IpnSatelliteFix;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;

@JsonTypeName("SensorConfigForGps")
public class SensorConfigForGps extends SensorConfig
{
    public String gpsPort;

    //--//

    @Override
    public CompletableFuture<SensorResultForGps> exec(WaypointApplication app) throws
                                                                               Exception
    {
        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.gpsPort = BoxingUtils.get(gpsPort, "/optio3-dev/optio3_gps");

        SensorResultForGps res = new SensorResultForGps();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(cfg.gpsPort, false))
        {
            IpnManager manager = prepare(cfg, res, (obj) ->
            {
                final IpnLocation objLocation = Reflection.as(obj, IpnLocation.class);
                if (objLocation != null)
                {
                    res.longitude = objLocation.longitude;
                    res.latitude  = objLocation.latitude;
                    res.altitude  = objLocation.altitude;
                    res.speed     = objLocation.speed;
                }

                final IpnSatellite objSatellite = Reflection.as(obj, IpnSatellite.class);
                if (objSatellite != null && objSatellite.isTracked())
                {
                    res.satellitesInView.add(objSatellite.satelliteId);
                }

                final IpnSatelliteFix objSatelliteFix = Reflection.as(obj, IpnSatelliteFix.class);
                if (objSatelliteFix != null)
                {
                    res.hasFix          = objSatelliteFix.fixMode != IpnSatelliteFix.FixMode.Unavailable;
                    res.satellitesInFix = objSatelliteFix.fixSet;
                }
            });

            await(exec(manager, () -> res.success = checkValid(res.longitude, res.latitude, res.altitude, res.speed), (error) -> res.failure = error));
        }

        return wrapAsync(res);
    }
}
