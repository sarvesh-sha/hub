/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertEventType;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.config.UserGroupRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.concurrency.Executors;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Optio3RecurringProcessor
public class RecurringUnreachableBACnetDeviceCheck extends RecurringActivityHandler implements RecurringActivityHandler.ForTable
{
    enum ConfigVariable implements IConfigVariable
    {
        SiteUrl("SITE_URL"),
        ExtraInfo("EXTRA"),
        Details("DETAILS");

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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator      = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_reachable   = s_configValidator.newTemplate(RecurringUnreachableBACnetDeviceCheck.class,
                                                                                                                          "emails/bacnet-device/reachable.txt",
                                                                                                                          "${",
                                                                                                                          "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_unreachable = s_configValidator.newTemplate(RecurringUnreachableBACnetDeviceCheck.class,
                                                                                                                          "emails/bacnet-device/unreachable.txt",
                                                                                                                          "${",
                                                                                                                          "}");

    private MonotonousTime m_nextBatch;

    //--//

    @Override
    public Class<?> getEntityClass()
    {
        return BACnetDeviceRecord.class;
    }

    @Override
    public boolean shouldTrigger(DbEvent event)
    {
        trackPending(event.context.sysId);

        return true;
    }

    //--//

    @Override
    public Duration startupDelay()
    {
        return null;
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        AtomicInteger unreachableCount = new AtomicInteger();
        Set<String>   needProcessing   = Sets.newHashSet();

        //
        // Quick check to see if we need to process all the devices.
        //
        Set<String> pending = flushPending();
        if (pending != null)
        {
            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                final RecordHelper<BACnetDeviceRecord> helper_device = sessionHolder.createHelper(BACnetDeviceRecord.class);

                for (String sysId : pending)
                {
                    BACnetDeviceRecord rec_device = helper_device.getOrNull(sysId);
                    if (rec_device != null)
                    {
                        if (shouldProcess(needProcessing, unreachableCount, rec_device))
                        {
                            m_nextBatch = null;
                            break;
                        }
                    }
                }
            }

            unreachableCount.set(0);
            needProcessing.clear();
        }

        if (TimeUtils.isTimeoutExpired(m_nextBatch))
        {
            m_nextBatch = MonotonousTime.computeTimeoutExpiration(Duration.of(1, ChronoUnit.HOURS));

            //
            // First pass without updates, to collect the records that need to be modified.
            //
            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                final RecordHelper<BACnetDeviceRecord> helper_device = sessionHolder.createHelper(BACnetDeviceRecord.class);

                AssetRecord.enumerate(helper_device, true, -1, null, (rec_device) ->
                {
                    shouldProcess(needProcessing, unreachableCount, rec_device);

                    return StreamHelperNextAction.Continue_Evict;
                });
            }

            if (!needProcessing.isEmpty())
            {
                final List<Details> deltaUnreachable = Lists.newArrayList();
                final List<Details> deltaReachable   = Lists.newArrayList();

                // Let it settle for a bit.
                Executors.safeSleep(5_000);

                //
                // Second pass to actually modify the records.
                //
                for (String sysId : needProcessing)
                {
                    try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
                    {
                        RecordLocked<BACnetDeviceRecord> lock_device = sessionHolder.getEntityWithLockOrNull(BACnetDeviceRecord.class, sysId, 3, TimeUnit.MINUTES);
                        if (lock_device != null)
                        {
                            BACnetDeviceRecord rec_device = lock_device.get();

                            MetadataMap metadata = rec_device.getMetadata();

                            var state = BACnetDeviceRecord.WellKnownMetadata.bacnetReachability.get(metadata);

                            if (state.notifiedUnreachable == null)
                            {
                                if (state.notifiedReachable == null)
                                {
                                    state.notifiedReachable = TimeUtils.now(); // Assume reachable.
                                }

                                if (state.unreachable != null)
                                {
                                    ZonedDateTime warningThreshold = TimeUtils.past(rec_device.getMinutesBeforeTransitionToUnreachable(), TimeUnit.MINUTES);
                                    if (state.unreachable.isBefore(warningThreshold))
                                    {
                                        deltaUnreachable.add(new Details(sessionHolder, rec_device, false, state.unreachable));
                                        state.notifiedUnreachable = state.unreachable;
                                        state.notifiedReachable   = null;
                                    }

                                    state.warningDebounce = null;
                                }
                            }

                            if (state.notifiedReachable == null)
                            {
                                if (state.reachable != null)
                                {
                                    if (state.warningDebounce == null)
                                    {
                                        state.warningDebounce = TimeUtils.now();
                                    }

                                    var warningDebounce = state.warningDebounce.plus(rec_device.getMinutesBeforeTransitionToReachable(), ChronoUnit.MINUTES);
                                    if (TimeUtils.isTimeoutExpired(warningDebounce))
                                    {
                                        deltaReachable.add(new Details(sessionHolder, rec_device, true, state.notifiedUnreachable));

                                        state.notifiedReachable   = state.reachable;
                                        state.notifiedUnreachable = null;
                                        state.warningDebounce     = null;
                                    }
                                }
                            }

                            BACnetDeviceRecord.WellKnownMetadata.bacnetReachability.put(metadata, state);

                            rec_device.setMetadata(metadata);
                        }

                        sessionHolder.commit();
                    }
                }

                try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
                {
                    if (!deltaUnreachable.isEmpty())
                    {
                        handleWarning(sessionHolder, unreachableCount.get(), deltaUnreachable);
                    }

                    if (!deltaReachable.isEmpty())
                    {
                        handleSuccess(sessionHolder, unreachableCount.get(), deltaReachable);
                    }

                    sessionHolder.commit();
                }
            }
        }

        return wrapAsync(TimeUtils.future(15, TimeUnit.MINUTES));
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private boolean shouldProcess(Set<String> needProcessing,
                                  AtomicInteger unreachableCount,
                                  BACnetDeviceRecord rec_device)
    {
        String      sysId    = rec_device.getSysId();
        MetadataMap metadata = rec_device.getMetadata();

        var state = BACnetDeviceRecord.WellKnownMetadata.bacnetReachability.get(metadata);
        if (state.unreachable != null)
        {
            if (rec_device.getState() == AssetState.operational)
            {
                ZonedDateTime warningThreshold = TimeUtils.past(rec_device.getMinutesBeforeTransitionToUnreachable(), TimeUnit.MINUTES);
                if (state.unreachable.isBefore(warningThreshold))
                {
                    unreachableCount.incrementAndGet();

                    if (state.notifiedUnreachable == null)
                    {
                        needProcessing.add(sysId);
                        return true;
                    }
                }
            }
        }
        else
        {
            if (state.notifiedReachable == null)
            {
                needProcessing.add(sysId);
                return true;
            }
        }

        return false;
    }

    //--//

    private static class Details
    {
        String displayName;
        String details;

        Details(SessionHolder sessionHolder,
                BACnetDeviceRecord rec_device,
                boolean reachable,
                ZonedDateTime timestamp)
        {
            AssetRecord rec_parent = rec_device.getParentAsset();

            BaseAssetDescriptor id        = rec_device.getIdentityDescriptor();
            String              name      = rec_device.getName();
            boolean             nameBlank = StringUtils.isBlank(name);

            if (id != null)
            {
                if (nameBlank)
                {
                    this.displayName = id.toString();
                }
                else
                {
                    this.displayName = String.format("%s - %s", name, id);
                }
            }
            else
            {
                this.displayName = name;
            }

            boolean found = false;

            for (AlertRecord rec_alert : rec_device.getAlerts())
            {
                if (rec_alert.getType() == AlertType.COMMUNICATION_PROBLEM)
                {
                    switch (rec_alert.getStatus())
                    {
                        case active:
                        case muted:
                            if (reachable)
                            {
                                rec_alert.addHistoryEntry(sessionHolder, null, AlertEventLevel.success, AlertEventType.closed, "Device re-established communication");
                            }
                            found = true;
                            break;
                    }
                }
            }

            String timestampText = TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(timestamp.withZoneSameInstant(ZoneId.of("America/Los_Angeles")));

            if (reachable)
            {
                details = String.format("   %s ($$SITE_URL$$/#/devices/device/%s) - back online after %s", displayName, rec_device.getSysId(), timestampText);
            }
            else
            {
                details = String.format("   %s ($$SITE_URL$$/#/devices/device/%s) - unresponsive since %s", displayName, rec_device.getSysId(), timestampText);
            }

            if (!found && !reachable)
            {
                AlertHistoryRecord rec_event = rec_device.createNewAlert(sessionHolder,
                                                                         null,
                                                                         null,
                                                                         AlertType.COMMUNICATION_PROBLEM,
                                                                         AlertEventLevel.failure,
                                                                         "Device '%s' cannot be contacted",
                                                                         displayName);

                AlertRecord rec_alert = rec_event.getAlert();
                rec_alert.setSeverity(AlertSeverity.SIGNIFICANT);
                rec_alert.setDescription("Device did not reply to any requests");
            }
        }
    }

    private void handleSuccess(SessionHolder sessionHolder,
                               int unreachableCount,
                               List<Details> deltaReachable)
    {
        List<String> details = Lists.newArrayList();

        deltaReachable.sort((a, b) -> StringUtils.compareIgnoreCase(a.displayName, b.displayName));

        for (Details delta : deltaReachable)
        {
            details.add(delta.details);
        }

        sendEmail(sessionHolder, unreachableCount, details, HubApplication.EmailFlavor.Info, "BACnet Info", s_template_reachable);
    }

    private void handleWarning(SessionHolder sessionHolder,
                               int unreachableCount,
                               List<Details> deltaUnreachable)
    {
        List<String> details = Lists.newArrayList();

        deltaUnreachable.sort((a, b) -> StringUtils.compareIgnoreCase(a.displayName, b.displayName));

        for (Details delta : deltaUnreachable)
        {
            details.add(delta.details);
        }

        sendEmail(sessionHolder, unreachableCount, details, HubApplication.EmailFlavor.Warning, "BACnet Warning", s_template_unreachable);
    }

    private void sendEmail(SessionHolder sessionHolder,
                           int unreachableCount,
                           List<String> details,
                           HubApplication.EmailFlavor emailFlavor,
                           String subject,
                           ConfigVariables.Template<ConfigVariable> emailTemplate)
    {
        HubApplication   app = sessionHolder.getServiceNonNull(HubApplication.class);
        HubConfiguration cfg = sessionHolder.getServiceNonNull(HubConfiguration.class);

        details = CollectionUtils.transformToList(details, (detail) -> StringUtils.replace(detail, "$$SITE_URL$$", cfg.cloudConnectionUrl));

        //--//

        ConfigVariables<ConfigVariable> parameters = emailTemplate.allocate();

        parameters.setValue(ConfigVariable.SiteUrl, cfg.cloudConnectionUrl);
        parameters.setValue(ConfigVariable.ExtraInfo, unreachableCount > 0 ? String.format(" (%d unreachable in total)", unreachableCount) : "");
        parameters.setValue(ConfigVariable.Details, String.join("\n", details));

        app.sendEmailNotification(sessionHolder, false, emailFlavor, subject, true, parameters);

        UserGroupRecord rec_group = UserGroupRecord.findByMetadata(sessionHolder, UserGroupRecord.WellKnownMetadata.reachabilityGroup);
        if (rec_group != null)
        {
            Set<UserRecord> users = Sets.newHashSet();
            for (UserGroupRecord rec_groupClosure : rec_group.getGroupsClosure())
            {
                users.addAll(rec_groupClosure.getMembers());
            }

            for (UserRecord rec_user : users)
            {
                app.sendEmailNotification(sessionHolder, false, rec_user.getEmailAddress(), subject, true, parameters);
            }
        }
    }
}
