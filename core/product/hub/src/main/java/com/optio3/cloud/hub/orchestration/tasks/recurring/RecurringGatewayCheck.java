/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertEventType;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementSampleRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.prober.GatewayProberOperationRecord;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.concurrency.AsyncGate;
import com.optio3.logging.Logger;
import com.optio3.util.BoxingUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Optio3RecurringProcessor
public class RecurringGatewayCheck extends RecurringActivityHandler implements RecurringActivityHandler.ForTable
{
    public static final Logger LoggerInstance = new Logger(RecurringGatewayCheck.class);

    enum ConfigVariable implements IConfigVariable
    {
        SiteUrl("SITE_URL"),
        GatewayId("GATEWAY_ID"),
        GatewaySysId("GATEWAY_SYSID"),
        LastHeartbeat("LAST_HEARTBEAT"),
        ExtraDetails("EXTRA_DETAILS"),
        Timestamp("TIME");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator        = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_newUnit       = s_configValidator.newTemplate(RecurringGatewayCheck.class, "emails/gateway/new_unit.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_autoDiscovery = s_configValidator.newTemplate(RecurringGatewayCheck.class, "emails/gateway/auto_config.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_gotHeartbeat  = s_configValidator.newTemplate(RecurringGatewayCheck.class, "emails/gateway/got_heartbeat.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_noHeartbeat   = s_configValidator.newTemplate(RecurringGatewayCheck.class, "emails/gateway/no_heartbeat.txt", "${", "}");

    private MonotonousTime m_nextPerfCounterPruning;
    private MonotonousTime m_nextBatch = MonotonousTime.computeTimeoutExpiration(Duration.of(1, ChronoUnit.HOURS));

    //--//

    @Override
    public Class<?> getEntityClass()
    {
        return GatewayAssetRecord.class;
    }

    @Override
    public synchronized boolean shouldTrigger(DbEvent event)
    {
        trackPending(event.context.sysId);

        return true;
    }

    //--//

    @Override
    public Duration startupDelay()
    {
        return Duration.of(10, ChronoUnit.MINUTES);
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        ZonedDateTime now = TimeUtils.now();

        if (TimeUtils.isTimeoutExpired(m_nextBatch))
        {
            var lstGateways = sessionProvider.computeInReadOnlySession((sessionHolder) -> QueryHelperWithCommonFields.list(sessionHolder.createHelper(GatewayAssetRecord.class), null));
            callInParallel(lstGateways, (ri) ->
            {
                processSingle(sessionProvider, ri);
            });

            flushPending();

            if (TimeUtils.isTimeoutExpired(m_nextPerfCounterPruning))
            {
                //
                // Purge empty performance trends older than a month.
                //
                ZonedDateTime purgeThreshold = now.minus(30, ChronoUnit.DAYS);

                int            totalDeleted = 0;
                HubApplication app          = sessionProvider.getServiceNonNull(HubApplication.class);
                try (AsyncGate.Holder ignored = app.closeGate(HibernateSearch.Gate.class))
                {
                    for (TypedRecordIdentity<GatewayAssetRecord> ri : lstGateways)
                    {
                        totalDeleted += pruneOldPerfCounters(sessionProvider, ri, purgeThreshold);
                    }

                    var lstNetworks = sessionProvider.computeInReadOnlySession((sessionHolder) -> QueryHelperWithCommonFields.list(sessionHolder.createHelper(NetworkAssetRecord.class), null));
                    for (TypedRecordIdentity<NetworkAssetRecord> ri : lstNetworks)
                    {
                        totalDeleted += pruneOldPerfCounters(sessionProvider, ri, purgeThreshold);
                    }
                }

                if (totalDeleted > 0)
                {
                    LoggerInstance.info("Purged %d old performance counters in total.", totalDeleted);
                }

                // Rerun after a while.
                m_nextPerfCounterPruning = TimeUtils.computeTimeoutExpiration(13, TimeUnit.HOURS);
            }

            m_nextBatch = MonotonousTime.computeTimeoutExpiration(Duration.of(1, ChronoUnit.HOURS));
        }
        else
        {
            Set<String> pending = flushPending();
            if (pending != null)
            {
                for (String sysId : pending)
                {
                    processSingle(sessionProvider, RecordIdentity.newTypedInstance(GatewayAssetRecord.class, sysId));
                }
            }
        }

        return wrapAsync(TimeUtils.future(10, TimeUnit.MINUTES));
    }

    private void processSingle(SessionProvider sessionProvider,
                               TypedRecordIdentity<GatewayAssetRecord> ri_gateway)
    {
        try (var sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            RecordLocked<GatewayAssetRecord> lock_gateway = sessionHolder.fromIdentityWithLockOrNull(ri_gateway, 30, TimeUnit.SECONDS);
            if (lock_gateway != null)
            {
                GatewayAssetRecord rec_gateway = lock_gateway.get();

                if (rec_gateway.getMetadata(GatewayAssetRecord.WellKnownMetadata.reportAsNew))
                {
                    rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.reportAsNew, null);

                    sendEmail(sessionHolder, rec_gateway, HubApplication.EmailFlavor.Info, "New Gateway Info", s_template_newUnit, null);
                }

                String report = rec_gateway.getMetadata(GatewayAssetRecord.WellKnownMetadata.reportAutodiscovery);
                if (report != null)
                {
                    rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.reportAutodiscovery, null);

                    sendEmail(sessionHolder, rec_gateway, HubApplication.EmailFlavor.Info, "Auto Discovery", s_template_autoDiscovery, report);
                }

                ZonedDateTime lastHeartbeat = rec_gateway.getLastUpdatedDate();
                if (lastHeartbeat != null)
                {
                    ZonedDateTime now = TimeUtils.now();

                    ZonedDateTime warningThreshold = now.minus(rec_gateway.getWarningThreshold(), ChronoUnit.MINUTES);
                    ZonedDateTime alertThreshold   = now.minus(rec_gateway.getAlertThreshold(), ChronoUnit.MINUTES);
                    ZonedDateTime successThreshold = now.minus(30, ChronoUnit.MINUTES);

                    if (lastHeartbeat.isAfter(successThreshold))
                    {
                        handleSuccess(sessionHolder, rec_gateway);
                    }
                    else
                    {
                        if (lastHeartbeat.isBefore(warningThreshold))
                        {
                            handleWarning(sessionHolder, rec_gateway, lastHeartbeat);
                        }

                        if (lastHeartbeat.isBefore(alertThreshold))
                        {
                            handleFailure(sessionHolder, rec_gateway);
                        }
                    }
                }

                //
                // Prune stale prober operations.
                //
                for (GatewayProberOperationRecord rec_operation : Lists.newArrayList(rec_gateway.getOperations()))
                {
                    if (!TimeUtils.wasUpdatedRecently(rec_operation.getCreatedOn(), 1, TimeUnit.DAYS))
                    {
                        sessionHolder.deleteEntity(rec_operation);
                    }
                }

                sessionHolder.commit();
            }
        }
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private <T extends AssetRecord> int pruneOldPerfCounters(SessionProvider sessionProvider,
                                                             TypedRecordIdentity<T> ri,
                                                             ZonedDateTime purgeThreshold) throws
                                                                                           Exception
    {
        try (SessionHolder holder = sessionProvider.newSessionWithTransaction())
        {
            AssetRecord rec = holder.fromIdentity(ri);

            final RecordHelper<DeviceElementRecord>       helper_element = holder.createHelper(DeviceElementRecord.class);
            final RecordHelper<DeviceElementSampleRecord> helper_sample  = holder.createHelper(DeviceElementSampleRecord.class);

            AtomicInteger kept         = new AtomicInteger();
            AtomicInteger deleted      = new AtomicInteger();
            int           totalDeleted = 0;

            rec.enumerateChildren(helper_element, true, -1, null, (rec_perf) ->
            {
                AtomicBoolean hasSamples = new AtomicBoolean();

                if (StringUtils.startsWith(rec_perf.getIdentifier(), "IP::"))
                {
                    // TODO: UPGRADE PATCH: Legacy fixup to remove old elements.
                }
                else
                {
                    final ZonedDateTime lastUpdate = rec_perf.getUpdatedOn();

                    rec_perf.deleteSamplesOlderThan(helper_sample, purgeThreshold);

                    if (lastUpdate.isAfter(purgeThreshold))
                    {
                        hasSamples.set(true);
                    }
                    else
                    {
                        rec_perf.filterArchives(helper_sample, null, null, false, (desc) ->
                        {
                            hasSamples.set(true);
                            return SamplesCache.StreamNextAction.Done;
                        }, null);
                    }
                }

                if (!hasSamples.get())
                {
                    helper_element.delete(rec_perf);

                    deleted.incrementAndGet();
                    return StreamHelperNextAction.Continue;
                }
                else
                {
                    kept.incrementAndGet();
                    return StreamHelperNextAction.Continue_Flush_Evict;
                }
            });

            int deletedCount = deleted.get();
            if (deletedCount > 0)
            {
                totalDeleted += deletedCount;

                LoggerInstance.info("Purged %d old performance counters from '%s', %d left.", deletedCount, rec.getName(), kept.get());
            }
            else
            {
                LoggerInstance.debug("Keeping %d performance counters from '%s'", kept.get(), rec.getName());
            }

            holder.commit();

            return totalDeleted;
        }
    }

    //--//

    private void handleSuccess(SessionHolder sessionHolder,
                               GatewayAssetRecord rec_gateway)
    {
        MetadataMap metadata = rec_gateway.getMetadata();

        ZonedDateTime lastEmail = GatewayAssetRecord.WellKnownMetadata.gatewayWarning.get(metadata);
        if (lastEmail != null)
        {
            GatewayAssetRecord.WellKnownMetadata.gatewayWarning.remove(metadata);
            rec_gateway.setMetadata(metadata);

            Duration d = Duration.between(lastEmail, TimeUtils.now());
            String   extraDetails;

            long val = d.toDays();
            if (val > 0)
            {
                if (val >= 14)
                {
                    extraDetails = String.format("%d weeks", val / 7);
                }
                else
                {
                    extraDetails = String.format("%d days", val);
                }
            }
            else
            {
                val = d.toHours();
                if (val > 0)
                {
                    extraDetails = String.format("%d hours", val);
                }
                else
                {
                    val = d.toMinutes();
                    extraDetails = String.format("%d minutes", val);
                }
            }

            sendEmail(sessionHolder, rec_gateway, HubApplication.EmailFlavor.Info, "Gateway Info", s_template_gotHeartbeat, extraDetails);
        }

        AlertRecord rec_alert = findOpenAlert(rec_gateway);
        if (rec_alert != null)
        {
            rec_alert.addHistoryEntry(sessionHolder, null, AlertEventLevel.success, AlertEventType.closed, "Gateway '%s' re-established communication", rec_gateway.getInstanceId());
        }
    }

    private void handleWarning(SessionHolder sessionHolder,
                               GatewayAssetRecord rec_gateway,
                               ZonedDateTime lastHeartbeat)
    {
        switch (rec_gateway.getState())
        {
            case operational:
                MetadataMap metadata = rec_gateway.getMetadata();

                ZonedDateTime lastEmail = GatewayAssetRecord.WellKnownMetadata.gatewayWarning.get(metadata);
                if (lastEmail == null)
                {
                    GatewayAssetRecord.WellKnownMetadata.gatewayWarning.put(metadata, lastHeartbeat);
                    rec_gateway.setMetadata(metadata);

                    sendEmail(sessionHolder, rec_gateway, HubApplication.EmailFlavor.Warning, "Gateway Warning", s_template_noHeartbeat, null);
                }
                break;
        }
    }

    private void handleFailure(SessionHolder sessionHolder,
                               GatewayAssetRecord rec_gateway)
    {
        switch (rec_gateway.getState())
        {
            case operational:
                AlertRecord rec_alert = findOpenAlert(rec_gateway);
                if (rec_alert == null)
                {
                    AlertHistoryRecord rec_event = rec_gateway.createNewAlert(sessionHolder,
                                                                              null,
                                                                              null,
                                                                              AlertType.COMMUNICATION_PROBLEM,
                                                                              AlertEventLevel.failure,
                                                                              "Gateway '%s' stopped sending updates",
                                                                              rec_gateway.getInstanceId());

                    rec_alert = rec_event.getAlert();
                    rec_alert.setSeverity(AlertSeverity.SIGNIFICANT);
                    rec_alert.setDescription(String.format("No check-in from Gateway '%s'", rec_gateway.getInstanceId()));
                }
                break;
        }
    }

    private void sendEmail(SessionHolder sessionHolder,
                           GatewayAssetRecord rec_gateway,
                           HubApplication.EmailFlavor emailFlavor,
                           String subject,
                           ConfigVariables.Template<ConfigVariable> emailTemplate,
                           String extraDetails)
    {
        HubApplication   app = sessionHolder.getServiceNonNull(HubApplication.class);
        HubConfiguration cfg = sessionHolder.getServiceNonNull(HubConfiguration.class);

        ConfigVariables<ConfigVariable> parameters = emailTemplate.allocate();

        String instanceId = rec_gateway.getInstanceId();

        String name = rec_gateway.getName();
        if (StringUtils.isBlank(name))
        {
            name = instanceId;
        }
        else
        {
            name = String.format("%s [%s]", name, instanceId);
        }

        parameters.setValue(ConfigVariable.SiteUrl, cfg.cloudConnectionUrl);
        parameters.setValue(ConfigVariable.GatewayId, name);
        parameters.setValue(ConfigVariable.GatewaySysId, rec_gateway.getSysId());
        parameters.setValue(ConfigVariable.ExtraDetails, extraDetails);
        parameters.setValue(ConfigVariable.LastHeartbeat, BoxingUtils.get(rec_gateway.getLastUpdatedDate(), rec_gateway.getCreatedOn()));
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

        app.sendEmailNotification(sessionHolder, false, emailFlavor, subject, true, parameters);
    }

    private AlertRecord findOpenAlert(GatewayAssetRecord rec_gateway)
    {
        for (AlertRecord rec_alert : rec_gateway.getAlerts())
        {
            if (rec_alert.getType() == AlertType.COMMUNICATION_PROBLEM)
            {
                switch (rec_alert.getStatus())
                {
                    case active:
                    case muted:
                        return rec_alert;
                }
            }
        }

        return null;
    }
}
