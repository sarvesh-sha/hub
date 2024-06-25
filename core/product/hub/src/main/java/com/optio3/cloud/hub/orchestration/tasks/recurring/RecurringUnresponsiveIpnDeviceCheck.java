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
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.config.UserGroupRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Optio3RecurringProcessor
public class RecurringUnresponsiveIpnDeviceCheck extends RecurringActivityHandler implements RecurringActivityHandler.ForTable
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
    private static final ConfigVariables.Template<ConfigVariable>  s_template_reachable   = s_configValidator.newTemplate(RecurringUnresponsiveIpnDeviceCheck.class,
                                                                                                                          "emails/ipn-device/responsive.txt",
                                                                                                                          "${",
                                                                                                                          "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_unreachable = s_configValidator.newTemplate(RecurringUnresponsiveIpnDeviceCheck.class,
                                                                                                                          "emails/ipn-device/unresponsive.txt",
                                                                                                                          "${",
                                                                                                                          "}");

    private MonotonousTime m_nextBatch;

    //--//

    @Override
    public Class<?> getEntityClass()
    {
        return IpnDeviceRecord.class;
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
        final InstanceConfiguration cfg = sessionProvider.getServiceNonNull(InstanceConfiguration.class);

        AtomicInteger                            unresponsiveCount = new AtomicInteger();
        TypedRecordIdentityList<IpnDeviceRecord> lstUnresponsive   = new TypedRecordIdentityList<>();
        TypedRecordIdentityList<IpnDeviceRecord> lstResponsive     = new TypedRecordIdentityList<>();

        Set<String> pending = flushPending();
        if (pending != null)
        {
            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                final RecordHelper<IpnDeviceRecord> helper_device = sessionHolder.createHelper(IpnDeviceRecord.class);

                for (String sysId : pending)
                {
                    IpnDeviceRecord rec_device = helper_device.getOrNull(sysId);
                    if (rec_device != null)
                    {
                        if (shouldProcess(cfg, unresponsiveCount, lstUnresponsive, lstResponsive, rec_device))
                        {
                            m_nextBatch = null;
                            break;
                        }
                    }
                }
            }

            unresponsiveCount.set(0);
            lstUnresponsive.clear();
            lstResponsive.clear();
        }

        if (TimeUtils.isTimeoutExpired(m_nextBatch))
        {
            m_nextBatch = MonotonousTime.computeTimeoutExpiration(Duration.of(1, ChronoUnit.HOURS));

            //
            // First pass without updates, to collect the records that need to be modified.
            //
            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                final RecordHelper<IpnDeviceRecord> helper_device = sessionHolder.createHelper(IpnDeviceRecord.class);

                AssetRecord.enumerate(helper_device, true, -1, null, (rec_device) ->
                {
                    shouldProcess(cfg, unresponsiveCount, lstUnresponsive, lstResponsive, rec_device);

                    return StreamHelperNextAction.Continue_Evict;
                });
            }

            if (!lstUnresponsive.isEmpty() || !lstResponsive.isEmpty())
            {
                final List<Details> deltaUnresponsive = Lists.newArrayList();
                final List<Details> deltaResponsive   = Lists.newArrayList();

                //
                // Second pass to actually modify the records.
                //
                try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
                {
                    for (TypedRecordIdentity<IpnDeviceRecord> ri : lstUnresponsive)
                    {
                        RecordLocked<IpnDeviceRecord> lock_device = sessionHolder.fromIdentityWithLockOrNull(ri, 10, TimeUnit.SECONDS);
                        if (lock_device != null)
                        {
                            IpnDeviceRecord rec_device = lock_device.get();

                            MetadataMap   metadata     = rec_device.getMetadata();
                            ZonedDateTime unresponsive = IpnDeviceRecord.WellKnownMetadata.ipnUnresponsive.get(metadata);
                            if (unresponsive != null)
                            {
                                deltaUnresponsive.add(new Details(sessionHolder, rec_device, false, unresponsive));

                                IpnDeviceRecord.WellKnownMetadata.ipnWarning.put(metadata, unresponsive);
                                rec_device.setMetadata(metadata);
                            }
                        }
                    }

                    for (TypedRecordIdentity<IpnDeviceRecord> ri : lstResponsive)
                    {
                        RecordLocked<IpnDeviceRecord> lock_device = sessionHolder.fromIdentityWithLockOrNull(ri, 10, TimeUnit.SECONDS);
                        if (lock_device != null)
                        {
                            IpnDeviceRecord rec_device = lock_device.get();

                            MetadataMap   metadata = rec_device.getMetadata();
                            ZonedDateTime warning  = IpnDeviceRecord.WellKnownMetadata.ipnWarning.get(metadata);
                            if (warning != null)
                            {
                                deltaResponsive.add(new Details(sessionHolder, rec_device, true, warning));

                                IpnDeviceRecord.WellKnownMetadata.ipnWarning.remove(metadata);

                                rec_device.setMetadata(metadata);
                            }
                        }
                    }

                    if (!deltaUnresponsive.isEmpty())
                    {
                        handleWarning(sessionHolder, unresponsiveCount.get(), deltaUnresponsive);
                    }

                    if (!deltaResponsive.isEmpty())
                    {
                        handleSuccess(sessionHolder, unresponsiveCount.get(), deltaResponsive);
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

    private boolean shouldProcess(InstanceConfiguration cfg,
                                  AtomicInteger unresponsiveCount,
                                  TypedRecordIdentityList<IpnDeviceRecord> lstUnresponsive,
                                  TypedRecordIdentityList<IpnDeviceRecord> lstResponsive,
                                  IpnDeviceRecord rec_device)
    {
        MetadataMap   metadata     = rec_device.getMetadata();
        ZonedDateTime unresponsive = IpnDeviceRecord.WellKnownMetadata.ipnUnresponsive.get(metadata);
        ZonedDateTime warning      = IpnDeviceRecord.WellKnownMetadata.ipnWarning.get(metadata);

        if (unresponsive != null)
        {
            if (rec_device.getState() == AssetState.operational)
            {
                ZonedDateTime now                  = TimeUtils.now();
                ZonedDateTime unresponsiveDetected = IpnDeviceRecord.WellKnownMetadata.ipnUnresponsiveDebounce.getOrDefault(metadata, now);
                if (unresponsiveDetected.isBefore(now))
                {
                    if (cfg.shouldReportWhenUnreachable(rec_device, unresponsive))
                    {
                        unresponsiveCount.incrementAndGet();

                        if (warning == null)
                        {
                            lstUnresponsive.add(RecordIdentity.newTypedInstance(rec_device));
                            return true;
                        }
                    }
                }
            }
        }
        else
        {
            if (warning != null)
            {
                ZonedDateTime now                = TimeUtils.now();
                ZonedDateTime responsiveDetected = IpnDeviceRecord.WellKnownMetadata.ipnResponsiveDebounce.getOrDefault(metadata, now);
                if (responsiveDetected.isBefore(now))
                {
                    lstResponsive.add(RecordIdentity.newTypedInstance(rec_device));
                    return true;
                }
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
                IpnDeviceRecord rec_device,
                boolean responsive,
                ZonedDateTime timestamp)
        {
            AssetRecord rec_parent = rec_device.getParentAsset();

            this.displayName = String.format("%s (%s)", rec_device.getIdentityDescriptor(), rec_parent.getName());

            boolean found = false;

            for (AlertRecord rec_alert : rec_device.getAlerts())
            {
                if (rec_alert.getType() == AlertType.COMMUNICATION_PROBLEM)
                {
                    switch (rec_alert.getStatus())
                    {
                        case active:
                        case muted:
                            if (responsive)
                            {
                                rec_alert.addHistoryEntry(sessionHolder, null, AlertEventLevel.success, AlertEventType.closed, "Sensor re-established communication");
                            }
                            found = true;
                            break;
                    }
                }
            }

            String timestampText = TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(timestamp.withZoneSameInstant(ZoneId.of("America/Los_Angeles")));

            if (responsive)
            {
                details = String.format("   %s ($$SITE_URL$$/#/devices/device/%s) - back online after %s", displayName, rec_device.getSysId(), timestampText);
            }
            else
            {
                details = String.format("   %s ($$SITE_URL$$/#/devices/device/%s) - unresponsive since %s", displayName, rec_device.getSysId(), timestampText);
            }

            if (!found && !responsive)
            {
                AlertHistoryRecord rec_event = rec_device.createNewAlert(sessionHolder,
                                                                         null,
                                                                         null,
                                                                         AlertType.COMMUNICATION_PROBLEM,
                                                                         AlertEventLevel.failure,
                                                                         "Sensor '%s' stopped sending data",
                                                                         displayName);

                AlertRecord rec_alert = rec_event.getAlert();
                rec_alert.setSeverity(AlertSeverity.SIGNIFICANT);
                rec_alert.setDescription("Sensor stopped sending data");
            }
        }
    }

    private void handleSuccess(SessionHolder sessionHolder,
                               int unresponsiveCount,
                               List<Details> deltaResponsive)
    {
        List<String> details = Lists.newArrayList();

        deltaResponsive.sort((a, b) -> StringUtils.compareIgnoreCase(a.displayName, b.displayName));

        for (Details delta : deltaResponsive)
        {
            details.add(delta.details);
        }

        sendEmail(sessionHolder, unresponsiveCount, details, HubApplication.EmailFlavor.Info, "Ipn Info", s_template_reachable);
    }

    private void handleWarning(SessionHolder sessionHolder,
                               int unresponsiveCount,
                               List<Details> deltaUnresponsive)
    {
        List<String> details = Lists.newArrayList();

        deltaUnresponsive.sort((a, b) -> StringUtils.compareIgnoreCase(a.displayName, b.displayName));

        for (Details delta : deltaUnresponsive)
        {
            details.add(delta.details);
        }

        sendEmail(sessionHolder, unresponsiveCount, details, HubApplication.EmailFlavor.Warning, "Ipn Warning", s_template_unreachable);
    }

    private void sendEmail(SessionHolder sessionHolder,
                           int unresponsiveCount,
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
        parameters.setValue(ConfigVariable.ExtraInfo, unresponsiveCount > 0 ? String.format(" (%d unresponsive in total)", unresponsiveCount) : "");
        parameters.setValue(ConfigVariable.Details, String.join("\n", details));

        app.sendEmailNotification(sessionHolder, false, emailFlavor, subject, true, parameters);

        UserGroupRecord rec_group = UserGroupRecord.findByMetadata(sessionHolder, UserGroupRecord.WellKnownMetadata.responsivenessGroup);
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
