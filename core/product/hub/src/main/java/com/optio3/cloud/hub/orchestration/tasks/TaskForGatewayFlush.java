/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import static com.optio3.asyncawait.CompileTime.await;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayControlApi;
import com.optio3.cloud.client.gateway.proxy.GatewayDiscoveryApi;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;

public class TaskForGatewayFlush extends BaseGatewayTask
{
    enum State
    {
        FlushHeartbeat,
        FlushEntities,
        WaitForFlushEntities,
    }

    public State   step;
    public boolean flushEntities;
    public boolean flushHeartbeat;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        GatewayAssetRecord rec_gateway,
                                                        boolean flushEntities,
                                                        boolean flushHeartbeat) throws
                                                                                Exception
    {
        return BaseGatewayTask.scheduleTask(sessionHolder, rec_gateway, 0, null, TaskForGatewayFlush.class, (t) ->
        {
            t.initializeTimeout(30, TimeUnit.MINUTES);

            t.flushEntities  = flushEntities;
            t.flushHeartbeat = flushHeartbeat;
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return String.format("Flush entities buffered on gateway '%s'", name_gateway);
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_gateway;
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_FlushHeartbeat() throws
                                                          Exception
    {
        if (flushHeartbeat)
        {
            GatewayControlApi proxy = await(getControlProxy());
            if (proxy == null)
            {
                // Gateway not responding...
                return rescheduleDelayed(1, TimeUnit.MINUTES);
            }

            await(proxy.flushHeartbeat());
        }

        return continueAtState(State.FlushEntities);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_FlushEntities() throws
                                                         Exception
    {
        if (flushEntities)
        {
            GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
            if (proxy == null)
            {
                // Gateway not responding...
                return rescheduleDelayed(1, TimeUnit.MINUTES);
            }

            loggerInstance.info("[%s] Flushing entities...", name_gateway);

            GatewayOperationToken token = await(proxy.flushEntities());
            prepareWaitOperation(1, TimeUnit.MINUTES, token);

            return continueAtState(State.WaitForFlushEntities);
        }

        return markAsCompleted();
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForFlushEntities() throws
                                                                Exception
    {
        OpResult op = await(waitForOperations(true, null));
        switch (op)
        {
            case Success:
                loggerInstance.info("[%s] Completed flushing buffered entities!", name_gateway);

                return markAsCompleted();

            case Failure:
                loggerInstance.info("[%s] Flushing entities failed", name_gateway);

                // Retry.
                return continueAtState(State.FlushEntities, 10, TimeUnit.SECONDS);
        }

        return AsyncRuntime.NullResult;
    }
}