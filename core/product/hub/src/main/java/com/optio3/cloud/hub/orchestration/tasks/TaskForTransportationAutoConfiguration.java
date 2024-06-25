/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import static com.optio3.asyncawait.CompileTime.await;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.client.gateway.model.GatewayAutoDiscovery;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayDiscoveryApi;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForTransportationAutoConfiguration extends BaseGatewayTask
{
    private static final int TimeoutForAutoConfiguration = 12;

    enum State
    {
        TriggerDiscovery,
        WaitForTriggerDiscovery,
        AnalyzeResults,
        TriggerNetworkRefresh
    }

    public RecordLocator<NetworkAssetRecord> loc_network;

    public List<GatewayAutoDiscovery> results;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        GatewayAssetRecord rec_gateway,
                                                        NetworkAssetRecord rec_network) throws
                                                                                        Exception
    {
        return BaseGatewayTask.scheduleTask(sessionHolder, rec_gateway, 0, null, TaskForTransportationAutoConfiguration.class, (t) ->
        {
            t.initializeTimeout(TimeoutForAutoConfiguration, TimeUnit.HOURS);

            t.loc_network = sessionHolder.createLocator(rec_network);
        });
    }

    public static boolean alreadyRunning(SessionHolder sessionHolder,
                                         GatewayAssetRecord rec_gateway)
    {
        var lst = BackgroundActivityRecord.findHandlers(sessionHolder, false, true, TaskForTransportationAutoConfiguration.class, sessionHolder.createLocator(rec_gateway));
        return !lst.isEmpty();
    }

    //--//

    public String getTitle()
    {
        return String.format("Create data structures for transportation on gateway '%s'", name_gateway);
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_gateway;
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_TriggerDiscovery() throws
                                                            Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        loggerInstance.info("[%s] Starting autoconfiguration...", name_gateway);

        GatewayOperationToken token = await(proxy.performAutoDiscovery());
        prepareWaitOperation(30, TimeUnit.MINUTES, token);

        return continueAtState(State.WaitForTriggerDiscovery, 10, TimeUnit.SECONDS);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForTriggerDiscovery() throws
                                                                   Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        OpResult op = await(waitForOperations(true, (token) ->
        {
            results = await(proxy.getAutoDiscoveryResults(token));
            return AsyncRuntime.NullResult;
        }));

        switch (op)
        {
            case Success:
                loggerInstance.info("[%s] Autoconfiguration completed", name_gateway);

                return continueAtState(State.AnalyzeResults, 1, TimeUnit.SECONDS); // Delay a bit, to let staged results to be detected.

            case Failure:
                // BUGBUG: capture reason why it failed.
                loggerInstance.info("[%s] Autoconfiguration failed...", name_gateway);
                return markAsFailed("Autoconfiguration failed");

            default:
                return AsyncRuntime.NullResult;
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_AnalyzeResults() throws
                                                          Exception
    {
        boolean got = false;

        List<String> discovered = Lists.newArrayList();

        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        for (GatewayAutoDiscovery result : CollectionUtils.asEmptyCollectionIfNull(results))
        {
            ProtocolConfigForIpn cfgFound = Reflection.as(result.cfg, ProtocolConfigForIpn.class);

            if (cfgFound != null)
            {
                switch (result.flavor)
                {
                    case ArgoHytos:
                        cfg.argohytosPort = cfgFound.argohytosPort;

                        addEntry(discovered, name_gateway, "Found ArgoHytos at %s", cfg.argohytosPort);
                        got = true;
                        break;

                    case Bergstrom:
                        cfg.canPort = cfgFound.canPort;
                        cfg.canFrequency = cfgFound.canFrequency;
                        cfg.canNoTermination = cfgFound.canNoTermination;
                        cfg.canInvert = cfgFound.canInvert;

                        addEntry(discovered, name_gateway, "Found Bergstrom at %s", cfg.canPort);
                        got = true;
                        break;

                    case BlueSky:
                        cfg.ipnPort = cfgFound.ipnPort;
                        cfg.ipnBaudrate = cfgFound.ipnBaudrate;
                        cfg.ipnInvert = cfgFound.ipnInvert;
                        cfg.simulate = cfgFound.simulate;

                        addEntry(discovered, name_gateway, "Found BlueSky at %s", cfg.ipnPort);
                        got = true;
                        break;

                    case EpSolar:
                        cfg.epsolarPort = cfgFound.epsolarPort;
                        cfg.epsolarInvert = cfgFound.epsolarInvert;

                        addEntry(discovered, name_gateway, "Found EpSolar at %s", cfg.epsolarPort);
                        got = true;
                        break;

                    case GPS:
                        cfg.gpsPort = cfgFound.gpsPort;

                        addEntry(discovered, name_gateway, "Found GPS at %s", cfg.gpsPort);
                        got = true;
                        break;

                    case HendricksonWatchman:
                        cfg.canPort = cfgFound.canPort;
                        cfg.canFrequency = cfgFound.canFrequency;
                        cfg.canNoTermination = cfgFound.canNoTermination;
                        cfg.canInvert = cfgFound.canInvert;

                        addEntry(discovered, name_gateway, "Found Hendrickson Watchman at %s", cfg.canPort);
                        got = true;
                        break;

                    case Holykell:
                        cfg.holykellPort = cfgFound.holykellPort;
                        cfg.holykellInvert = cfgFound.holykellInvert;

                        addEntry(discovered, name_gateway, "Found Holykell at %s", cfg.holykellPort);
                        got = true;
                        break;

                    case OBDII:
                        cfg.obdiiPort = cfgFound.obdiiPort;
                        cfg.obdiiFrequency = cfgFound.obdiiFrequency;

                        addEntry(discovered, name_gateway, "Found OBD-II at %s/%d", cfg.obdiiPort, cfg.obdiiFrequency);
                        got = true;
                        break;

                    case J1939:
                        cfg.obdiiPort = cfgFound.obdiiPort;
                        cfg.obdiiFrequency = cfgFound.obdiiFrequency;

                        addEntry(discovered, name_gateway, "Found J1939 at %s/%d", cfg.obdiiPort, cfg.obdiiFrequency);
                        got = true;
                        break;

                    case MontageBluetoothGateway:
                        cfg.montageBluetoothGatewayPort = cfgFound.montageBluetoothGatewayPort;

                        addEntry(discovered, name_gateway, "Found Montage Bluetooth Gateway at %s", cfg.montageBluetoothGatewayPort);
                        got = true;
                        break;

                    case MorningStar:
                        cfg.tristarPort = cfgFound.tristarPort;

                        addEntry(discovered, name_gateway, "Found MorningStar at %s", cfg.tristarPort);
                        got = true;
                        break;

                    case Palfinger:
                        cfg.canPort = cfgFound.canPort;
                        cfg.canFrequency = cfgFound.canFrequency;
                        cfg.canNoTermination = cfgFound.canNoTermination;
                        cfg.canInvert = cfgFound.canInvert;

                        addEntry(discovered, name_gateway, "Found Palfinger at %s", cfg.canPort);
                        got = true;
                        break;

                    case StealthPower:
                        cfg.stealthpowerPort = cfgFound.stealthpowerPort;

                        addEntry(discovered, name_gateway, "Found StealthPower at %s", cfg.stealthpowerPort);
                        got = true;
                        break;

                    case Victron:
                        cfg.victronPort = cfgFound.victronPort;

                        addEntry(discovered, name_gateway, "Found Victron at %s", cfg.victronPort);
                        got = true;
                        break;
                }
            }
        }

        InstanceConfiguration instanceCfg = getServiceNonNull(InstanceConfiguration.class);
        got |= instanceCfg.fixupAutoConfig(cfg);

        if (!got)
        {
            loggerInstance.error("[%s] No sensors detected!!", name_gateway);

            return markAsFailed("No sensor detected on %s!", name_gateway);
        }

        callUnderTransaction(sessionHolder ->
                             {
                                 if (!discovered.isEmpty())
                                 {
                                     GatewayAssetRecord rec_gateway = getGatewayRecord(sessionHolder);
                                     rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.reportAutodiscovery, StringUtils.join(discovered, "\n"));
                                 }

                                 NetworkAssetRecord rec_network = sessionHolder.fromLocator(loc_network);
                                 rec_network.setProtocolsConfiguration(Lists.newArrayList(cfg));
                             });

        return continueAtState(State.TriggerNetworkRefresh);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true, autoRetry = true)
    public void state_TriggerNetworkRefresh(SessionHolder sessionHolder) throws
                                                                         Exception
    {
        TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
        settings.forceSamplingConfiguration = true;

        GatewayAssetRecord rec_gateway = getGatewayRecord(sessionHolder);
        TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, (t) ->
        {
            t.targetNetworks = Lists.newArrayList(loc_network);
        });

        markAsCompleted();
    }

    private void addEntry(List<String> discovered,
                          String name,
                          String fmt,
                          Object... args)
    {
        String text = String.format(fmt, args);

        discovered.add("   " + text);
        loggerInstance.info("[%s] %s", name, text);
    }
}
