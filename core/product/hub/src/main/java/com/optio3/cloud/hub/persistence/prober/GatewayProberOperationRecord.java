/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.prober;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.client.gateway.model.LogEntry;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayProberControlApi;
import com.optio3.cloud.hub.model.asset.GatewayProberOperation;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithHeartbeat;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.ObjectMappers;
import com.optio3.service.IServiceProvider;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "GATEWAY_PROBER_OPERATION")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "GatewayProberOperation", model = GatewayProberOperation.class, metamodel = GatewayProberOperationRecord_.class)
public class GatewayProberOperationRecord extends RecordWithHeartbeat implements ModelMapperTarget<GatewayProberOperation, GatewayProberOperationRecord_>,
                                                                                 LogHandler.ILogHost<GatewayProberOperationLogRecord>
{
    /**
     * The deployment this task is controlled by.
     */
    @Optio3ControlNotifications(reason = "Only notify host of gateway's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getGateway")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getGateway")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "gateway", nullable = false, foreignKey = @ForeignKey(name = "PROBER_OPERATION__GATEWAY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private GatewayAssetRecord gateway;

    //--//

    @Lob
    @Column(name = "input_details")
    private String inputDetails;

    @Transient
    private final PersistAsJsonHelper<String, ProberOperation> m_inputDetailsHelper = new PersistAsJsonHelper<>(() -> inputDetails,
                                                                                                                (val) -> inputDetails = val,
                                                                                                                String.class,
                                                                                                                ProberOperation.class,
                                                                                                                ObjectMappers.SkipNulls,
                                                                                                                true);

    //--//

    @Lob
    @Column(name = "output_details")
    private String outputDetails;

    @Transient
    private final PersistAsJsonHelper<String, ProberOperation.BaseResults> m_outputDetailsHelper = new PersistAsJsonHelper<>(() -> outputDetails,
                                                                                                                             (val) -> outputDetails = val,
                                                                                                                             String.class,
                                                                                                                             ProberOperation.BaseResults.class,
                                                                                                                             ObjectMappers.SkipNulls,
                                                                                                                             true);

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    @Optio3ControlNotifications(reason = "Notify prober of activity changes", direct = Notify.NEVER, reverse = Notify.ALWAYS)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getCurrentActivity", setter = "setCurrentActivity")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "current_activity", foreignKey = @ForeignKey(name = "PROBER__CURRENT_ACTIVITY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private BackgroundActivityRecord currentActivity;

    //--//

    public GatewayProberOperationRecord()
    {
    }

    public static GatewayProberOperationRecord newInstance(GatewayAssetRecord gateway,
                                                           ProberOperation inputDetails)
    {
        requireNonNull(gateway);

        GatewayProberOperationRecord res = new GatewayProberOperationRecord();
        res.gateway = gateway;
        res.setInputDetails(inputDetails);
        return res;
    }

    //--//

    public ProberOperation getInputDetails()
    {
        return m_inputDetailsHelper.get();
    }

    public boolean setInputDetails(ProberOperation details)
    {
        return m_inputDetailsHelper.set(details);
    }

    //--//

    public ProberOperation.BaseResults getOutputDetails()
    {
        return m_outputDetailsHelper.get();
    }

    public boolean setOutputDetails(ProberOperation.BaseResults details)
    {
        return m_outputDetailsHelper.set(details);
    }

    //--//

    public ZonedDateTime getLastOutput()
    {
        return lastOutput;
    }

    public int getLastOffset()
    {
        return lastOffset;
    }

    @Override
    public byte[] getLogRanges()
    {
        return logRanges;
    }

    @Override
    public void setLogRanges(byte[] logRanges,
                             ZonedDateTime lastOutput,
                             int lastOffset)
    {
        if (!Arrays.equals(this.logRanges, logRanges))
        {
            this.logRanges  = logRanges;
            this.lastOutput = lastOutput;
            this.lastOffset = lastOffset;
        }
    }

    @Override
    public void refineLogQuery(LogHandler.JoinHelper<?, GatewayProberOperationLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, GatewayProberOperationLogRecord_.owningOp, this);
    }

    @Override
    public GatewayProberOperationLogRecord allocateNewLogInstance()
    {
        return GatewayProberOperationLogRecord.newInstance(this);
    }

    public static LogHandler<GatewayProberOperationRecord, GatewayProberOperationLogRecord> allocateLogHandler(RecordLocked<GatewayProberOperationRecord> lock)
    {
        return new LogHandler<>(lock, GatewayProberOperationLogRecord.class);
    }

    public static LogHandler<GatewayProberOperationRecord, GatewayProberOperationLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                                               GatewayProberOperationRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, GatewayProberOperationLogRecord.class);
    }

    //--//

    public GatewayAssetRecord getGateway()
    {
        return gateway;
    }

    public BackgroundActivityRecord getCurrentActivity()
    {
        return currentActivity;
    }

    public void setCurrentActivity(BackgroundActivityRecord currentActivity)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.currentActivity != currentActivity)
        {
            this.currentActivity = currentActivity;
        }
    }

    //--//

    public static List<GatewayProberOperationRecord> getBatch(RecordHelper<GatewayProberOperationRecord> helper,
                                                              List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        if (!gotHeartbeatRecently(10, TimeUnit.MINUTES))
        {
            // The task hasn't been updated in ten minutes, it's okay to remove it.
            return;
        }

        if (currentActivity != null && !currentActivity.getStatus()
                                                       .isDone())
        {
            validation.addFailure("currentActivity", "Operation '%s' is running", currentActivity.getSysId());
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<GatewayProberOperationRecord> taskHelper)
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            taskHelper.delete(this);
        }
    }

    //--//

    public ProberOperationToken startOperation(IServiceProvider serviceProvider,
                                               ProberOperation input)
    {
        try
        {
            GatewayAssetRecord rec_gateway = getGateway();

            GatewayProberControlApi proxy = rec_gateway.getProxy(serviceProvider, GatewayProberControlApi.class);

            return getAndUnwrapException(proxy.executeOperation(input));
        }
        catch (Throwable t)
        {
            // In case of connection error, assume executing.
            return null;
        }
    }

    public ProberOperationStatus checkOperation(SessionHolder sessionHolder,
                                                ProberOperationToken token)
    {
        List<LogEntry>        logs = Lists.newArrayList();
        ProberOperationStatus status;

        try
        {
            GatewayAssetRecord rec_gateway = getGateway();

            GatewayProberControlApi proxy = rec_gateway.getProxy(sessionHolder, GatewayProberControlApi.class);

            status = getAndUnwrapException(proxy.checkOperation(token, (lst) ->
            {
                logs.addAll(lst);
                return AsyncRuntime.NullResult;
            }));
        }
        catch (Throwable t)
        {
            // In case of connection error, assume executing.
            status = ProberOperationStatus.Executing;
        }

        if (!logs.isEmpty())
        {
            RecordLocked<GatewayProberOperationRecord> lock = sessionHolder.optimisticallyUpgradeToLocked(this, 20, TimeUnit.SECONDS);

            try (var logHandler = allocateLogHandler(lock))
            {
                try (LogHolder log = logHandler.newLogHolder())
                {
                    for (LogEntry entry : logs)
                    {
                        log.addLineSync(1, entry.timestamp, null, entry.thread, entry.selector, entry.level, entry.line);
                    }
                }
            }
        }

        return status;
    }

    public boolean cancelOperation(IServiceProvider serviceProvider,
                                   ProberOperationToken token)
    {
        try
        {
            GatewayAssetRecord rec_gateway = getGateway();

            GatewayProberControlApi proxy = rec_gateway.getProxy(serviceProvider, GatewayProberControlApi.class);

            getAndUnwrapException(proxy.cancelOperation(token));
            return true;
        }
        catch (Throwable t)
        {
            // In case of connection error, swallow exception.
            return false;
        }
    }

    public boolean getOperationResults(IServiceProvider serviceProvider,
                                       ProberOperationToken token)
    {
        try
        {
            GatewayAssetRecord rec_gateway = getGateway();

            GatewayProberControlApi proxy = rec_gateway.getProxy(serviceProvider, GatewayProberControlApi.class);

            ProberOperation.BaseResults res = getAndUnwrapException(proxy.getOperationResults(token));
            setOutputDetails(res);
            return true;
        }
        catch (Throwable t)
        {
            // In case of connection error, swallow exception.
            return false;
        }
    }
}
