/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.infra.waypoint.KnownI2C;

@JsonTypeName("SensorConfigForI2CHub")
public class SensorConfigForI2CHub extends SensorConfig
{
    @Override
    public CompletableFuture<SensorResultForI2CHub> exec(WaypointApplication app) throws
                                                                                  Exception
    {
        SensorResultForI2CHub res = new SensorResultForI2CHub();

        FirmwareHelper f = FirmwareHelper.get();
        if (f.supportsI2C())
        {
            if (f.supportsI2Cmultiplex())
            {
                WaypointApplication.LoggerInstance.info("Found I2C multiplexer");

                res.portDetected = true;

                {
                    int[] scan = f.scanI2C(Integer.MAX_VALUE);
                    if (scan != null)
                    {
                        WaypointApplication.LoggerInstance.info("Found %d devices on main bus", scan.length);
                        for (int address : scan)
                        {
                            KnownI2C deviceModel = f.resolveI2C(Integer.MAX_VALUE, address, false);
                            WaypointApplication.LoggerInstance.info("Found %s / %s on main bus", deviceModel, address);
                            if (deviceModel != KnownI2C.NONE)
                            {
                                var scanResult = new SensorResultForI2CHub.Scan();
                                scanResult.bus     = -1; // Only report once. on the main bus
                                scanResult.address = address;
                                scanResult.device  = deviceModel.name;

                                res.success = true;
                                res.busScan.add(scanResult);
                            }
                        }
                    }
                }

                for (int bus = 0; bus < 8; bus++)
                {
                    int[] scan = f.scanI2C(bus);
                    if (scan != null)
                    {
                        WaypointApplication.LoggerInstance.info("Found %d devices on bus %d", scan.length, bus);
                        for (int address : scan)
                        {
                            KnownI2C deviceModel = f.resolveI2C(bus, address, false);
                            WaypointApplication.LoggerInstance.info("Found %s / %s on bus %d", deviceModel, address, bus);
                            switch (deviceModel)
                            {
                                case MCP3428:
                                    // Only report on main bus.
                                    break;

                                case NONE:
                                    break;

                                default:
                                    var scanDetails = new SensorResultForI2CHub.Scan();
                                    scanDetails.bus = bus;
                                    scanDetails.address = address;
                                    scanDetails.device = deviceModel.name;

                                    res.success = true;
                                    res.busScan.add(scanDetails);
                                    break;
                            }
                        }
                    }
                }
            }
        }

        return wrapAsync(res);
    }
}
