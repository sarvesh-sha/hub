/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationToken;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.prober.GatewayProberOperationRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Exceptions;

public class TaskForProberOperation extends BaseGatewayTask
{
    public enum State
    {
        StartingOp,
        CheckingOp,
        FetchingOpResults
    }

    public RecordLocator<GatewayProberOperationRecord> loc_op;
    public ProberOperationToken                        token;
    public int                                  nextCheck;

    //--//

    public static GatewayProberOperationRecord scheduleOperation(SessionHolder sessionHolder,
                                                                 GatewayAssetRecord targetGateway,
                                                                 ProberOperation op,
                                                                 Duration timeout) throws
                                                                            Exception
    {
        Exceptions.requireNotNull(targetGateway, InvalidArgumentException.class, "No gateway provided");

        GatewayProberOperationRecord               rec_op = GatewayProberOperationRecord.newInstance(targetGateway, op);
        RecordHelper<GatewayProberOperationRecord> helper = sessionHolder.createHelper(GatewayProberOperationRecord.class);
        helper.persist(rec_op);

        BackgroundActivityRecord rec_activity = BaseGatewayTask.scheduleTask(sessionHolder, targetGateway, 0, null, TaskForProberOperation.class, (t) ->
        {
            t.loggerInstance.info("Starting operation on gateway '%s'", t.name_gateway);

            t.initializeTimeout(timeout);

            t.loc_op = sessionHolder.createLocator(rec_op);
        });

        rec_op.setCurrentActivity(rec_activity);

        return rec_op;
    }

    //--//

    @Override
    public String getTitle()
    {
        return String.format("Operation on gateway '%s'", name_gateway);
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_gateway;
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, needsSession = true, autoRetry = true)
    public void state_StartingOp(SessionHolder sessionHolder) throws
                                                              Exception
    {
        GatewayProberOperationRecord rec_op = sessionHolder.fromLocatorOrNull(loc_op);
        if (rec_op == null)
        {
            markAsFailed("Operation on gateway '%s' got deleted...", name_gateway);
            return;
        }

        token = rec_op.startOperation(sessionHolder, rec_op.getInputDetails());
        if (token == null)
        {
            nextCheck = 1;
            rescheduleDelayed(1, TimeUnit.SECONDS);
            return;
        }
        else
        {
            continueAtState(State.CheckingOp);
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true, autoRetry = true)
    public void state_CheckingOp(SessionHolder sessionHolder) throws
                                                              Exception
    {
        GatewayProberOperationRecord rec_op = sessionHolder.fromLocatorOrNull(loc_op);
        if (rec_op == null)
        {
            markAsFailed("Operation on gateway '%s' got deleted...", name_gateway);
            return;
        }

        ProberOperationStatus execProcess = rec_op.checkOperation(sessionHolder, token);

        switch (execProcess)
        {
            case Executing:
                nextCheck = Math.min(10, nextCheck + 1);
                rescheduleDelayed(nextCheck, TimeUnit.SECONDS);
                return;

            case Completed:
                continueAtState(State.FetchingOpResults);
                return;
        }

        markAsFailed("Operation on gateway '%s' failed", name_gateway);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true, autoRetry = true)
    public void state_FetchingOpResults(SessionHolder sessionHolder) throws
                                                                     Exception
    {
        GatewayProberOperationRecord rec_op = sessionHolder.fromLocatorOrNull(loc_op);
        if (rec_op == null)
        {
            markAsFailed("Operation on gateway '%s' got deleted...", name_gateway);
            return;
        }

        if (!rec_op.getOperationResults(sessionHolder, token))
        {
            rescheduleDelayed(1, TimeUnit.SECONDS);
        }
        else
        {
            markAsCompleted();
        }
    }
}
