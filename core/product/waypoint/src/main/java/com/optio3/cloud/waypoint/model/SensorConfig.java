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
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.util.Exceptions;
import com.optio3.util.function.ConsumerWithException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = SensorConfigForArgoHytos.class),
                @JsonSubTypes.Type(value = SensorConfigForBergstrom.class),
                @JsonSubTypes.Type(value = SensorConfigForBluesky.class),
                @JsonSubTypes.Type(value = SensorConfigForEpSolar.class),
                @JsonSubTypes.Type(value = SensorConfigForGps.class),
                @JsonSubTypes.Type(value = SensorConfigForHendricksonWatchman.class),
                @JsonSubTypes.Type(value = SensorConfigForHolykell.class),
                @JsonSubTypes.Type(value = SensorConfigForI2CHub.class),
                @JsonSubTypes.Type(value = SensorConfigForJ1939.class),
                @JsonSubTypes.Type(value = SensorConfigForMontageBluetoothGateway.class),
                @JsonSubTypes.Type(value = SensorConfigForPalfinger.class),
                @JsonSubTypes.Type(value = SensorConfigForRawCANbus.class),
                @JsonSubTypes.Type(value = SensorConfigForStealthPower.class),
                @JsonSubTypes.Type(value = SensorConfigForTriStar.class),
                @JsonSubTypes.Type(value = SensorConfigForVictron.class) })
public abstract class SensorConfig
{
    public int seconds;

    //--//

    public abstract CompletableFuture<? extends SensorResult> exec(WaypointApplication app) throws
                                                                                            Exception;

    //--//

    protected static boolean checkValid(Object... values)
    {
        if (values == null)
        {
            return false;
        }

        for (Object value : values)
        {
            if (value instanceof Float && Float.isNaN((float) value))
            {
                return false;
            }

            if (value instanceof Double && Double.isNaN((double) value))
            {
                return false;
            }

            if (value instanceof Boolean && !((boolean) value))
            {
                return false;
            }
        }

        return true;
    }

    protected IpnManager prepare(ProtocolConfigForIpn cfg,
                                 SensorResult res,
                                 ConsumerWithException<IpnObjectModel> callback)
    {
        return new IpnManager(cfg)
        {
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

            @Override
            protected void streamSamples(IpnObjectModel obj) throws
                                                             Exception
            {
                callback.accept(obj);
            }

            @Override
            protected void notifySamples(IpnObjectModel obj,
                                         String field)
            {
            }

            @Override
            protected byte[] detectedStealthPowerBootloader(byte bootloadVersion,
                                                            byte hardwareVersion,
                                                            byte hardwareRevision)
            {
                return null;
            }

            @Override
            protected void completedStealthPowerBootloader(int statusCode)
            {
            }
        };
    }

    protected CompletableFuture<Void> exec(IpnManager manager,
                                           Runnable normal,
                                           Consumer<String> abnormal)
    {
        try
        {
            try
            {
                manager.start();

                try
                {
                    await(manager.getReadySignal(), 2, TimeUnit.MINUTES);

                    await(sleep(2, TimeUnit.SECONDS));
                }
                catch (Throwable e)
                {
                    // Ignore errors.
                }

                normal.run();
            }
            finally
            {
                manager.close();
            }
        }
        catch (Throwable t)
        {
            Throwable t2 = Exceptions.unwrapException(t);
            abnormal.accept(t2.getMessage());
        }

        return wrapAsync(null);
    }
}
