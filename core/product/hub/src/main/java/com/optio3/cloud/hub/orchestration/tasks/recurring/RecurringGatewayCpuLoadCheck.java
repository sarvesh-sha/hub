/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wasComputationCancelled;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.model.ControlPointsSelection;
import com.optio3.cloud.hub.model.dashboard.AggregationRequest;
import com.optio3.cloud.hub.model.dashboard.AggregationResponse;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationTypeId;
import com.optio3.cloud.hub.model.schedule.FilterableTimeRange;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesLastValueRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesLastValueResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyRequest;
import com.optio3.cloud.hub.model.visualization.RangeSelection;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Optio3RecurringProcessor
public class RecurringGatewayCpuLoadCheck extends RecurringActivityHandler
{
    private static final String CPU_USAGE_USER      = "cpuUsageUser";
    private static final int    SAMPLING_PERIOD     = 1;
    private static final double INITIAL_WARNING     = 50.0;
    private static final double INCREMENTAL_WARNING = 10.0;

    enum ConfigVariable implements IConfigVariable
    {
        SiteUrl("SITE_URL"),
        GatewayId("GATEWAY_ID"),
        GatewaySysId("GATEWAY_SYSID"),
        CpuLoad("CPU_LOAD"),
        CpuLoadPrevious("CPU_LOAD_PREVIOUS"),
        CpuLoadThreshold("CPU_LOAD_THRESHOLD"),
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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator           = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_cpuLoad_normal   = s_configValidator.newTemplate(RecurringGatewayCpuLoadCheck.class,
                                                                                                                               "emails/gateway/cpuLoad_normal.txt",
                                                                                                                               "${",
                                                                                                                               "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_cpuLoad_elevated = s_configValidator.newTemplate(RecurringGatewayCpuLoadCheck.class,
                                                                                                                               "emails/gateway/cpuLoad_elevated.txt",
                                                                                                                               "${",
                                                                                                                               "}");

    //--//

    @Override
    public Duration startupDelay()
    {
        // Delay a bit to reduce load on system.
        return Duration.of(5, ChronoUnit.MINUTES);
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        SamplesCache samplesCache = sessionProvider.getServiceNonNull(SamplesCache.class);

        var lstGateways = sessionProvider.computeInReadOnlySession((sessionHolder) -> QueryHelperWithCommonFields.list(sessionHolder.createHelper(GatewayAssetRecord.class), null));
        for (RecordIdentity ri_gateway : lstGateways)
        {
            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                RecordHelper<GatewayAssetRecord>  helper_gateway       = sessionHolder.createHelper(GatewayAssetRecord.class);
                RecordHelper<DeviceElementRecord> helper_deviceElement = sessionHolder.createHelper(DeviceElementRecord.class);

                GatewayAssetRecord rec_gateway = helper_gateway.get(ri_gateway.sysId);

                DeviceElementRecord rec_deviceElement = DeviceElementRecord.findByIdentifierOrNull(helper_deviceElement, rec_gateway, "Global");
                if (rec_deviceElement != null)
                {
                    TimeSeriesLastValueRequest lastValueReq = new TimeSeriesLastValueRequest();
                    lastValueReq.spec       = new TimeSeriesPropertyRequest();
                    lastValueReq.spec.sysId = rec_deviceElement.getSysId();
                    lastValueReq.spec.prop  = CPU_USAGE_USER;

                    TimeSeriesLastValueResponse lastValueRes = lastValueReq.fetch(samplesCache);
                    if (lastValueRes == null)
                    {
                        continue;
                    }

                    ZonedDateTime samplingPeriod = lastValueRes.timestamp.truncatedTo(ChronoUnit.HOURS);

                    AggregationRequest req = new AggregationRequest();
                    req.selections = new ControlPointsSelection();
                    req.selections.identities.add(TypedRecordIdentity.newTypedInstance(rec_deviceElement));
                    req.aggregationType = AggregationTypeId.MEAN;

                    FilterableTimeRange currentPeriod = new FilterableTimeRange();
                    currentPeriod.range = RangeSelection.buildFixed(samplingPeriod.minus(SAMPLING_PERIOD, ChronoUnit.HOURS), samplingPeriod);

                    FilterableTimeRange previousPeriod = new FilterableTimeRange();
                    previousPeriod.range = new RangeSelection();
                    previousPeriod.range = RangeSelection.buildFixed(samplingPeriod.minus(2 * SAMPLING_PERIOD, ChronoUnit.HOURS), samplingPeriod.minus(SAMPLING_PERIOD, ChronoUnit.HOURS));

                    req.filterableRanges = Lists.newArrayList(previousPeriod, currentPeriod);
                    req.prop             = "cpuUsageUser";

                    AggregationResponse res = req.execute(sessionHolder);
                    if (res.resultsPerRange.size() == 2)
                    {
                        double previousValue = res.resultsPerRange.get(0)[0];
                        double currentValue  = res.resultsPerRange.get(1)[0];

                        if (!Double.isNaN(previousValue) && !Double.isNaN(currentValue))
                        {
                            boolean sendNormal   = false;
                            boolean sendElevated = false;

                            helper_gateway.optimisticallyUpgradeToLocked(rec_gateway, 30, TimeUnit.SECONDS);

                            rec_gateway.updateCpuLoad((int) currentValue, (int) previousValue);

                            MetadataMap metadata = rec_gateway.getMetadata();

                            double lastWarningLevel = GatewayAssetRecord.WellKnownMetadata.gatewayCpuLoadWarningLevel.getOrDefault(metadata, INITIAL_WARNING - INCREMENTAL_WARNING);
                            if (currentValue < INITIAL_WARNING)
                            {
                                ZonedDateTime lastEmail = GatewayAssetRecord.WellKnownMetadata.gatewayCpuLoadWarning.get(metadata);
                                if (lastEmail != null)
                                {
                                    GatewayAssetRecord.WellKnownMetadata.gatewayCpuLoadWarning.remove(metadata);
                                    GatewayAssetRecord.WellKnownMetadata.gatewayCpuLoadWarningLevel.remove(metadata);

                                    sendNormal = true;
                                }
                            }
                            else if (currentValue > lastWarningLevel + INCREMENTAL_WARNING)
                            {
                                ZonedDateTime lastEmail = GatewayAssetRecord.WellKnownMetadata.gatewayCpuLoadWarning.get(metadata);
                                if (lastEmail == null)
                                {
                                    GatewayAssetRecord.WellKnownMetadata.gatewayCpuLoadWarning.put(metadata, TimeUtils.now());
                                    GatewayAssetRecord.WellKnownMetadata.gatewayCpuLoadWarningLevel.put(metadata, currentValue);

                                    sendElevated = true;
                                }
                            }

                            rec_gateway.setMetadata(metadata);

                            if (sendNormal)
                            {
                                sendEmail(sessionHolder, rec_gateway, samplingPeriod, HubApplication.EmailFlavor.Info, "Gateway CPU Load Info", s_template_cpuLoad_normal);
                            }

                            if (sendElevated)
                            {
                                sendEmail(sessionHolder, rec_gateway, samplingPeriod, HubApplication.EmailFlavor.Warning, "Gateway CPU Load Warning", s_template_cpuLoad_elevated);
                            }

                            sessionHolder.commit();
                        }
                    }
                }
            }

            // Yield processor.
            await(sleep(1, TimeUnit.MILLISECONDS));

            if (wasComputationCancelled())
            {
                break;
            }
        }

        return wrapAsync(TimeUtils.future(30, TimeUnit.MINUTES));
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private void sendEmail(SessionHolder sessionHolder,
                           GatewayAssetRecord rec_gateway,
                           ZonedDateTime samplingPeriod,
                           HubApplication.EmailFlavor emailFlavor,
                           String subject,
                           ConfigVariables.Template<ConfigVariable> emailTemplate)
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
        parameters.setValue(ConfigVariable.CpuLoad, String.format("%d%%", rec_gateway.getCpuLoadLast4Hours()));
        parameters.setValue(ConfigVariable.CpuLoadPrevious, String.format("%d%%", rec_gateway.getCpuLoadPrevious4Hours()));
        parameters.setValue(ConfigVariable.CpuLoadThreshold, String.format("%d%%", (int) INITIAL_WARNING));
        parameters.setValue(ConfigVariable.Timestamp, samplingPeriod);

        app.sendEmailNotification(sessionHolder, false, emailFlavor, subject, true, parameters);
    }
}
