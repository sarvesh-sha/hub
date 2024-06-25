/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.concurrency.Executors;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;

@JsonTypeName("SensorConfigForRawCANbus")
public class SensorConfigForRawCANbus extends SensorConfig
{
    public String  canPort;
    public int     canFrequency;
    public boolean canNoTermination;
    public boolean canInvert;

    //--//

    @Override
    public CompletableFuture<SensorResultForRawCANbus> exec(WaypointApplication app)
    {
        SensorResultForRawCANbus res = new SensorResultForRawCANbus();
        res.portDetected = true; // Assume CANbus present

        FirmwareHelper f = FirmwareHelper.get();
        if (f.mightBePresent(canPort, canInvert))
        {
            try (CanManager manager = new CanManager(BoxingUtils.get(canPort, "can0"), canFrequency, canNoTermination, canInvert)
            {
                @Override
                protected void notifyGoodMessage(CanObjectModel val) throws
                                                                     Exception
                {
                }

                @Override
                protected void notifyUnknownMessage(CanAccess.BaseFrame frame) throws
                                                                               Exception
                {
                }

                @Override
                protected boolean shouldProcessFrame(CanAccess.BaseFrame frame)
                {
                    CanAccess.ExtendedFrame frameExt = Reflection.as(frame, CanAccess.ExtendedFrame.class);
                    if (frameExt != null)
                    {
                        res.found.compute(frameExt.pgn, (key, old) -> BoxingUtils.get(old, 0) + 1);
                    }

                    CanAccess.StandardFrame frameStd = Reflection.as(frame, CanAccess.StandardFrame.class);
                    if (frameStd != null)
                    {
                        res.found.compute(frameStd.sourceAddress, (key, old) -> BoxingUtils.get(old, 0) + 1);
                    }

                    return false;
                }

                @Override
                protected void notifyTransport(String port,
                                               boolean opened,
                                               boolean closed)
                {
                    if (opened)
                    {
                        res.portDetected = true;
                    }
                }
            })
            {
                manager.start();

                await(sleep(seconds, TimeUnit.SECONDS));

                res.success = !res.found.isEmpty();
            }
            catch (Throwable t)
            {
                Throwable t2 = Exceptions.unwrapException(t);
                res.failure = t2.getMessage();
            }
        }

        return wrapAsync(res);
    }
}
