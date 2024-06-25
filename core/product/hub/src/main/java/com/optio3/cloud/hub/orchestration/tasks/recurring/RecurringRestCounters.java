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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.RequestStatistics;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.persistence.HostAssetRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.protocol.model.RestDescriptor;
import com.optio3.protocol.model.RestPerfDescriptor;
import com.optio3.protocol.model.RestPerformanceCounters;
import com.optio3.util.BoxingUtils;
import com.optio3.util.TimeUtils;

@Optio3RecurringProcessor
public class RecurringRestCounters extends RecurringActivityHandler
{
    private final Map<String, RequestStatistics> m_stats               = Maps.newHashMap();
    private final Set<String>                    m_cumulativeFirstPass = Sets.newHashSet();
    private       ZonedDateTime                  m_nextSample;

    @Override
    public Duration startupDelay()
    {
        return null;
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        if (m_nextSample == null)
        {
            advance(sessionProvider);
        }

        if (TimeUtils.isTimeoutExpired(m_nextSample))
        {
            var    app       = sessionProvider.getServiceNonNull(HubApplication.class);
            var    stats     = app.getRequestStatistics();
            double timestamp = m_nextSample.toEpochSecond();

            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                HostAssetRecord rec_host = app.getCurrentHost(sessionHolder);

                GatewayDiscoveryEntity en_network  = InstanceConfiguration.newDiscoveryEntry(null, GatewayDiscoveryEntitySelector.Host, rec_host.getSysId());
                GatewayDiscoveryEntity en_protocol = InstanceConfiguration.newDiscoveryEntry(en_network, GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Perf);

                RestDescriptor deviceDesc = new RestDescriptor();
                deviceDesc.sysId = rec_host.getSysId();

                GatewayDiscoveryEntity en_device = InstanceConfiguration.newDiscoveryEntry(en_protocol, GatewayDiscoveryEntitySelector.Perf_Device, deviceDesc);

                Map<String, RestPerformanceCounters> cumulativePerf = Maps.newHashMap();

                for (RequestStatistics req : Lists.newArrayList(stats.values()))
                {
                    String path = req.path;

                    RequestStatistics reqPrevious = m_stats.get(path);
                    if (reqPrevious == null)
                    {
                        reqPrevious = new RequestStatistics(path);
                    }

                    RequestStatistics reqNext = req.copy();

                    RestPerformanceCounters obj = new RestPerformanceCounters();
                    obj.requests          = reqNext.count - reqPrevious.count;
                    obj.executionTime     = reqNext.executionTime - reqPrevious.executionTime;
                    obj.bytesRead         = reqNext.bytesRead - reqPrevious.bytesRead;
                    obj.bytesWritten      = reqNext.bytesWritten - reqPrevious.bytesWritten;
                    obj.statusSuccess     = reqNext.statusSuccess - reqPrevious.statusSuccess;
                    obj.statusClientError = reqNext.statusClientError - reqPrevious.statusClientError;
                    obj.statusServerError = reqNext.statusServerError - reqPrevious.statusServerError;

                    boolean firstPass = m_stats.put(path, reqNext) == null;

                    //--//

                    GatewayDiscoveryEntity en_object = prepareObject(en_device, path, timestamp, firstPass);
                    InstanceConfiguration.newDiscoverySample(en_object, GatewayDiscoveryEntitySelector.Perf_ObjectSample, timestamp, obj);

                    while (true)
                    {
                        int pos = path.lastIndexOf('/');
                        if (pos <= 0)
                        {
                            break;
                        }

                        String parentPath = path.substring(0, pos + 1);

                        RestPerformanceCounters cumulativeObj = cumulativePerf.get(parentPath);
                        if (cumulativeObj == null)
                        {
                            cumulativeObj = new RestPerformanceCounters();
                            cumulativePerf.put(parentPath, cumulativeObj);
                        }

                        cumulativeObj.requests += obj.requests;
                        cumulativeObj.executionTime += obj.executionTime;
                        cumulativeObj.bytesRead += obj.bytesRead;
                        cumulativeObj.bytesWritten += obj.bytesWritten;
                        cumulativeObj.statusSuccess += obj.statusSuccess;
                        cumulativeObj.statusClientError += obj.statusClientError;
                        cumulativeObj.statusServerError += obj.statusServerError;

                        path = parentPath.substring(0, parentPath.length() - 1);
                    }
                }

                for (String cumulativePath : cumulativePerf.keySet())
                {
                    RestPerformanceCounters cumulativeObj = cumulativePerf.get(cumulativePath);

                    GatewayDiscoveryEntity en_objectCumulative = prepareObject(en_device, cumulativePath, timestamp, m_cumulativeFirstPass.add(cumulativePath));
                    InstanceConfiguration.newDiscoverySample(en_objectCumulative, GatewayDiscoveryEntitySelector.Perf_ObjectSample, timestamp, cumulativeObj);
                }

                ResultStagingRecord.queue(sessionHolder.createHelper(ResultStagingRecord.class), Lists.newArrayList(en_network));

                sessionHolder.commit();
            }

            advance(sessionProvider);
        }

        return wrapAsync(m_nextSample);
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private GatewayDiscoveryEntity prepareObject(GatewayDiscoveryEntity en_device,
                                                 String path,
                                                 double timestamp,
                                                 boolean firstPass)
    {
        RestPerfDescriptor desc = new RestPerfDescriptor();
        desc.path = path;

        GatewayDiscoveryEntity en_object = InstanceConfiguration.newDiscoveryEntry(en_device, GatewayDiscoveryEntitySelector.Perf_Object, desc.toString());

        if (firstPass)
        {
            //
            // If this is the first time we see this counter,
            // queue some fake contents for the Hub.
            // That way the record for the counter will be created.
            //
            en_device.setTimestampEpoch(timestamp);
            en_device.contents = "<trigger>";

            en_object.setTimestampEpoch(timestamp);
            en_object.contents = "<trigger>";
        }

        return en_object;
    }

    private void advance(SessionProvider sessionProvider)
    {
        HubConfiguration cfg            = sessionProvider.getServiceNonNull(HubConfiguration.class);
        int              samplingPeriod = BoxingUtils.bound(cfg.developerSettings.restCounterFrequency, 10, 30 * 60);

        ZonedDateTime lastSample = m_nextSample;
        if (lastSample == null)
        {
            lastSample = TimeUtils.now();
        }

        lastSample = TimeUtils.truncateTimestampToMultipleOfPeriod(lastSample, samplingPeriod);

        m_nextSample = lastSample.plus(samplingPeriod, ChronoUnit.SECONDS);
    }
}
