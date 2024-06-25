/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digineous;

import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.hub.model.asset.AssetState;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.customization.InstanceConfigurationWithWellKnownClasses;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousBlackBoxPayload;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousDeviceConfig;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousDeviceLibrary;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousMachineConfig;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousMachineLibrary;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousPointLibrary;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousVibrationMonitorDetails;
import com.optio3.cloud.hub.model.customization.digineous.model.DigineousVibrationPayload;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LazyRecordFlusher;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.concurrency.DebouncedAction;
import com.optio3.infra.integrations.infiniteimpulse.InfiniteImpulseHelper;
import com.optio3.infra.integrations.infiniteimpulse.model.MonitorsResponse;
import com.optio3.infra.integrations.infiniteimpulse.model.TrendEntry;
import com.optio3.infra.integrations.infiniteimpulse.model.TrendHistory;
import com.optio3.infra.integrations.infiniteimpulse.model.TrendValue;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownEquipmentClassOrCustom;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.objects.digineous.BaseDigineousModel;
import com.optio3.protocol.model.ipn.objects.digineous.DigineousDeviceFlavor;
import com.optio3.protocol.model.ipn.objects.digineous.Digineous_AnalogSignal;
import com.optio3.protocol.model.ipn.objects.digineous.Digineous_LogSignal;
import com.optio3.protocol.model.ipn.objects.digineous.Digineous_StatusSignal;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("InstanceConfigurationForDigineous")
public class InstanceConfigurationForDigineous extends InstanceConfigurationWithWellKnownClasses
{
    public static final String endpoint__DEVICE_TEMPLATE_NEW     = "device-template-new";
    public static final String endpoint__DEVICE_TEMPLATE_LIST    = "device-template-list";
    public static final String endpoint__DEVICE_TEMPLATE_GET     = "device-template-get";
    public static final String endpoint__DEVICE_TEMPLATE_DELETE  = "device-template-delete";
    public static final String endpoint__DEVICE_TEMPLATE_SET     = "device-template-set";
    public static final String endpoint__MACHINE_TEMPLATE_LIST   = "machine-template-list";
    public static final String endpoint__MACHINE_TEMPLATE_GET    = "machine-template-get";
    public static final String endpoint__MACHINE_TEMPLATE_DELETE = "machine-template-delete";
    public static final String endpoint__MACHINE_TEMPLATE_SET    = "machine-template-set";
    public static final String endpoint__VIBRATION_LIST          = "vibration-list";
    public static final String endpoint__MACHINE_LIST            = "machine-list";
    public static final String endpoint__MACHINE_CREATE          = "machine-create";
    public static final String endpoint__MACHINE_GET             = "machine-get";
    public static final String endpoint__MACHINE_DELETE          = "machine-delete";
    public static final String endpoint__MACHINE_SET             = "machine-set";
    public static final String endpoint__DEVICE_ACTIVE           = "device-active";
    public static final String endpoint__DATA                    = "data";

    public static class ForPull
    {
    }

    public static final Logger LoggerInstance        = new Logger(InstanceConfigurationForDigineous.class);
    public static final Logger LoggerInstanceForPull = new Logger(InstanceConfigurationForDigineous.ForPull.class);

    private static final String c_credentialKeyForVibrationServices          = "InfiniteImpulse";
    private static final String c_credentialKeyForVibrationServicesPlantId   = "InfiniteImpulsePlantId";
    private static final String c_credentialKeyForVibrationServicesFrequency = "InfiniteImpulseFrequency";

    //--//

    private final AtomicInteger m_counter = new AtomicInteger();

    private SessionProvider                  m_sessionProvider;
    private DatabaseActivity.LocalSubscriber m_regDbActivity;
    private Spooler                          m_spooler;

    private InfiniteImpulseHelper m_infiniteImpulseHelper;

    //--//

    public InstanceConfigurationForDigineous()
    {
    }

    synchronized Spooler ensureSpooler()
    {
        if (m_spooler == null)
        {
            m_spooler = new Spooler();
        }

        return m_spooler;
    }

    //--//

    @Override
    public CompletableFuture<Void> preprocessResults(List<GatewayDiscoveryEntity> entities)
    {
        // Nothing to do.
        return wrapAsync(null);
    }

    @Override
    public boolean shouldAutoConfig()
    {
        return false;
    }

    @Override
    public boolean fixupAutoConfig(ProtocolConfigForIpn cfg)
    {
        return false;
    }

    @Override
    public void reclassify()
    {
        //
        // Rerun classification, in case point classes have changed.
        //
        reclassifyAllNetworks();
    }

    @Override
    protected boolean shouldIncludeObjectInClassification(IpnObjectModel contents)
    {
        return true;
    }

    @Override
    protected boolean shouldBeSingletonInClassification(WellKnownPointClassOrCustom pointClass,
                                                        Set<String> pointTags)
    {
        return false;
    }

    @Override
    public boolean hasRoamingAssets()
    {
        return false;
    }

    @Override
    public boolean shouldReportWhenUnreachable(DeviceRecord rec,
                                               ZonedDateTime unresponsiveSince)
    {
        return true;
    }

    @Override
    public boolean prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                DeviceRecord rec_device,
                                                DeviceElementRecord rec_obj,
                                                boolean checkNonZeroValue,
                                                List<DeviceElementSampling> config)
    {
        DeviceElementSampling.add(config, DeviceElementRecord.DEFAULT_PROP_NAME, 30);
        return true;
    }

    @Override
    public void start()
    {
        stop();

        m_sessionProvider = new SessionProvider(m_app, null, Optio3DbRateLimiter.Normal);

        m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));

        m_regDbActivity.subscribeToTable(IpnDeviceRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                case DELETE:
                case UPDATE_DIRECT: // We don't invalidate for indirect updates.
                    Spooler spooler = ensureSpooler();
                    spooler.flushState();
                    break;
            }
        });
    }

    @Override
    public void stop()
    {
        if (m_regDbActivity != null)
        {
            m_regDbActivity.close();
            m_regDbActivity = null;
        }
    }

    @Override
    protected boolean shouldNotifyNewGateway(String instanceId)
    {
        return false;
    }

    @Override
    protected NetworkAssetRecord createInstanceId(SessionHolder sessionHolder,
                                                  String instanceId,
                                                  GatewayAssetRecord rec_gateway) throws
                                                                                  Exception
    {
        RecordHelper<GatewayAssetRecord> helper_gateway  = sessionHolder.createHelper(GatewayAssetRecord.class);
        RecordHelper<NetworkAssetRecord> helper_network  = sessionHolder.createHelper(NetworkAssetRecord.class);
        RecordHelper<LocationRecord>     helper_location = sessionHolder.createHelper(LocationRecord.class);

        LocationRecord rec_location = new LocationRecord();
        rec_location.setPhysicalName(String.format("Sensor #%s", instanceId));
        rec_location.setType(LocationType.BUILDING);
        helper_location.persist(rec_location);

        //--//

        NetworkAssetRecord rec_network = new NetworkAssetRecord();
        rec_network.setPhysicalName(String.format("Network #%s", instanceId));
        rec_network.setLocation(rec_location);
        rec_network.setSamplingPeriod(1800);
        helper_network.persist(rec_network);

        //--//

        rec_gateway.setPhysicalName(String.format("Gateway #%s", instanceId));
        rec_gateway.setLocation(rec_location);
        rec_gateway.setState(AssetState.passive);
        helper_gateway.persist(rec_gateway);

        rec_gateway.getBoundNetworks()
                   .add(rec_network);

        return rec_network;
    }

    @Override
    protected void afterNetworkCreation(SessionHolder sessionHolder,
                                        GatewayAssetRecord rec_gateway,
                                        NetworkAssetRecord rec_network) throws
                                                                        Exception
    {
        rec_gateway.setWarningThreshold(2 * 24 * 60);
        rec_gateway.setAlertThreshold(3 * 24 * 60);
    }

    @Override
    public boolean canAcceptNewSamples(CookiePrincipal principal,
                                       DeviceElementRecord rec_element,
                                       TimeSeriesPropertyType pt,
                                       BaseObjectModel model)
    {
        return true;
    }

    //--//

    public ZonedDateTime pullVibrationData() throws
                                             Exception
    {
        Spooler spooler = ensureSpooler();
        return spooler.pullVibrationData();
    }

    //--//

    @Path(endpoint__DEVICE_TEMPLATE_NEW)
    public DigineousDeviceLibrary deviceTemplateNew(SessionProvider sessionProvider,
                                                    CookiePrincipal principal,
                                                    @QueryParam("arg") String deviceFlavor) throws
                                                                                            Exception
    {
        principal.ensureAuthenticated();

        var deviceLibrary = new DigineousDeviceLibrary();
        deviceLibrary.deviceFlavor = Enum.valueOf(DigineousDeviceFlavor.class, deviceFlavor);
        deviceLibrary.id           = IdGenerator.newGuid();

        switch (deviceLibrary.deviceFlavor)
        {
            case BlackBox:
            {
                populateDeviceLibrary(deviceLibrary, null, DigineousBlackBoxPayload.getDescriptors());
                deviceLibrary.equipmentClass = WellKnownEquipmentClass.Sensor.asWrapped();
            }
            break;

            case InfiniteImpulse_Min:
            case InfiniteImpulse_Avg:
            case InfiniteImpulse_Max:
            {
                populateDeviceLibrary(deviceLibrary, null, DigineousVibrationPayload.getDescriptors());
                deviceLibrary.equipmentClass = WellKnownEquipmentClass.Vibration.asWrapped();

                for (DigineousPointLibrary point : deviceLibrary.points)
                {
                    point.enabled = true;
                }
            }
            break;
        }

        return deviceLibrary;
    }

    @Path(endpoint__DEVICE_TEMPLATE_LIST)
    public Set<String> deviceTemplateList(SessionProvider sessionProvider,
                                          CookiePrincipal principal) throws
                                                                     Exception
    {
        principal.ensureAuthenticated();

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);
            SystemPreferenceRecord.Tree node = tree.getNode(SystemPreferenceTypedValue.DigineousDeviceTemplate.getPath(), false);

            return node != null ? node.values.keySet() : Collections.emptySet();
        }
    }

    @Path(endpoint__DEVICE_TEMPLATE_GET)
    public DigineousDeviceLibrary deviceTemplateGet(SessionProvider sessionProvider,
                                                    CookiePrincipal principal,
                                                    @QueryParam("arg") String id) throws
                                                                                  Exception
    {
        principal.ensureAuthenticated();

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            DigineousDeviceLibrary res      = new DigineousDeviceLibrary();
            DigineousDeviceLibrary template = resolveDeviceTemplate(sessionHolder, id);

            populateDeviceLibrary(res, template, DigineousBlackBoxPayload.getDescriptors());

            return res;
        }
    }

    @Path(endpoint__DEVICE_TEMPLATE_DELETE)
    public boolean deviceTemplateDelete(SessionProvider sessionProvider,
                                        CookiePrincipal principal,
                                        @QueryParam("arg") String id) throws
                                                                      Exception
    {
        principal.ensureInAnyRole(WellKnownRole.Maintenance, WellKnownRole.Administrator);

        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            boolean res = SystemPreferenceRecord.removeTypedSubValue(sessionHolder, SystemPreferenceTypedValue.DigineousDeviceTemplate, id);

            sessionHolder.commit();

            return res;
        }
    }

    @Path(endpoint__DEVICE_TEMPLATE_SET)
    public DigineousDeviceLibrary deviceTemplateSet(SessionProvider sessionProvider,
                                                    CookiePrincipal principal,
                                                    DigineousDeviceLibrary library,
                                                    @QueryParam("arg") String id) throws
                                                                                  Exception
    {
        principal.ensureInAnyRole(WellKnownRole.Maintenance, WellKnownRole.Administrator);

        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            library.id = id;

            library.ensureInitialized();

            SystemPreferenceRecord.setTypedSubValue(sessionHolder, SystemPreferenceTypedValue.DigineousDeviceTemplate, id, library);

            sessionHolder.commitAndBeginNewTransaction();

            //
            // Refresh all machines using this template.
            //
            RecordHelper<NetworkAssetRecord> helper = sessionHolder.createHelper(NetworkAssetRecord.class);
            for (NetworkAssetRecord rec : helper.listAll())
            {
                DigineousMachineConfig machineConfig = rec.getMetadata(NetworkAssetRecord.WellKnownMetadata.digineous_machineConfig);
                if (machineConfig != null)
                {
                    DigineousMachineLibrary machineLibrary = resolveMachineTemplate(sessionHolder, machineConfig.machineTemplate);
                    if (machineLibrary != null && machineLibrary.deviceTemplates.contains(id))
                    {
                        try
                        {
                            syncMachine(sessionHolder, rec);
                        }
                        catch (Exception e)
                        {
                            LoggerInstance.error("Failed to sync machine %s, due to %s", machineConfig.machineName, e);
                        }
                    }
                }
            }

            sessionHolder.commit();
        }

        return library;
    }

    //--//

    @Path(endpoint__MACHINE_TEMPLATE_LIST)
    public Set<String> machineTemplateList(SessionProvider sessionProvider,
                                           CookiePrincipal principal) throws
                                                                      Exception
    {
        principal.ensureAuthenticated();

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);
            SystemPreferenceRecord.Tree node = tree.getNode(SystemPreferenceTypedValue.DigineousMachineTemplate.getPath(), false);

            return node != null ? node.values.keySet() : Collections.emptySet();
        }
    }

    @Path(endpoint__MACHINE_TEMPLATE_GET)
    public DigineousMachineLibrary machineTemplateGet(SessionProvider sessionProvider,
                                                      CookiePrincipal principal,
                                                      @QueryParam("arg") String id) throws
                                                                                    Exception
    {
        principal.ensureAuthenticated();

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            return SystemPreferenceRecord.getTypedSubValue(sessionHolder, SystemPreferenceTypedValue.DigineousMachineTemplate, id, DigineousMachineLibrary.class);
        }
    }

    @Path(endpoint__MACHINE_TEMPLATE_DELETE)
    public boolean machineTemplateDelete(SessionProvider sessionProvider,
                                         CookiePrincipal principal,
                                         @QueryParam("arg") String id) throws
                                                                       Exception
    {
        principal.ensureInAnyRole(WellKnownRole.Maintenance, WellKnownRole.Administrator);

        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            boolean res = SystemPreferenceRecord.removeTypedSubValue(sessionHolder, SystemPreferenceTypedValue.DigineousMachineTemplate, id);

            sessionHolder.commit();

            return res;
        }
    }

    @Path(endpoint__MACHINE_TEMPLATE_SET)
    public DigineousMachineLibrary machineTemplateSet(SessionProvider sessionProvider,
                                                      CookiePrincipal principal,
                                                      DigineousMachineLibrary library,
                                                      @QueryParam("arg") String id) throws
                                                                                    Exception
    {
        principal.ensureInAnyRole(WellKnownRole.Maintenance, WellKnownRole.Administrator);

        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            library.id = id;

            SystemPreferenceRecord.setTypedSubValue(sessionHolder, SystemPreferenceTypedValue.DigineousMachineTemplate, id, library);

            sessionHolder.commitAndBeginNewTransaction();

            //
            // Refresh all machines using this template.
            //
            RecordHelper<NetworkAssetRecord> helper = sessionHolder.createHelper(NetworkAssetRecord.class);
            for (NetworkAssetRecord rec : helper.listAll())
            {
                DigineousMachineConfig machineConfig = rec.getMetadata(NetworkAssetRecord.WellKnownMetadata.digineous_machineConfig);
                if (machineConfig != null && StringUtils.equals(machineConfig.machineTemplate, id))
                {
                    try
                    {
                        syncMachine(sessionHolder, rec);
                    }
                    catch (Exception e)
                    {
                        LoggerInstance.error("Failed to sync machine %s, due to %s", machineConfig.machineName, e);
                    }
                }
            }

            sessionHolder.commit();
        }

        return library;
    }

    //--//

    //--//

    @Path(endpoint__MACHINE_LIST)
    public List<String> machineList(SessionProvider sessionProvider,
                                    CookiePrincipal principal) throws
                                                               Exception
    {
        principal.ensureAuthenticated();

        List<String> lst = Lists.newArrayList();

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            RecordHelper<NetworkAssetRecord> helper = sessionHolder.createHelper(NetworkAssetRecord.class);
            for (NetworkAssetRecord rec : helper.listAll())
            {
                if (rec.getMetadata(NetworkAssetRecord.WellKnownMetadata.digineous_machineConfig) != null)
                {
                    lst.add(rec.getSysId());
                }
            }
        }

        return lst;
    }

    @Path(endpoint__MACHINE_CREATE)
    public TypedRecordIdentity<NetworkAssetRecord> machineCreate(SessionProvider sessionProvider,
                                                                 CookiePrincipal principal,
                                                                 DigineousMachineConfig details) throws
                                                                                                 Exception
    {
        principal.ensureInAnyRole(WellKnownRole.Maintenance, WellKnownRole.Administrator);

        RecordLocator<NetworkAssetRecord> loc_network = retryRecordCreation(sessionProvider, "network", details.machineId, (sessionHolder) ->
        {
            LocationRecord rec_location = new LocationRecord();
            rec_location.setPhysicalName(details.machineId);
            rec_location.setNormalizedName(details.machineName);
            rec_location.setType(LocationType.FACTORY);

            sessionHolder.persistEntity(rec_location);

            NetworkAssetRecord rec_network = new NetworkAssetRecord();
            rec_network.setPhysicalName(details.machineId);
            rec_network.setNormalizedName(details.machineName);
            rec_network.setLocation(rec_location);
            rec_network.setSamplingPeriod(1800);

            rec_network.putMetadata(NetworkAssetRecord.WellKnownMetadata.digineous_machineConfig, fixupMachineConfig(details));

            sessionHolder.persistEntity(rec_network);

            syncMachine(sessionHolder, rec_network);

            sessionHolder.commit();

            return sessionHolder.createLocator(rec_network);
        });

        return TypedRecordIdentity.newTypedInstance(loc_network);
    }

    @Path(endpoint__MACHINE_GET)
    public DigineousMachineConfig machineGet(SessionProvider sessionProvider,
                                             CookiePrincipal principal,
                                             @QueryParam("arg") String sysId) throws
                                                                              Exception
    {
        principal.ensureAuthenticated();

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            NetworkAssetRecord rec = sessionHolder.getEntityOrNull(NetworkAssetRecord.class, sysId);
            if (rec != null)
            {
                return rec.getMetadata(NetworkAssetRecord.WellKnownMetadata.digineous_machineConfig);
            }

            return null;
        }
    }

    @Path(endpoint__MACHINE_DELETE)
    public boolean machineDelete(SessionProvider sessionProvider,
                                 CookiePrincipal principal,
                                 @QueryParam("arg") String sysId) throws
                                                                  Exception
    {
        principal.ensureInAnyRole(WellKnownRole.Maintenance, WellKnownRole.Administrator);

        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            NetworkAssetRecord rec = sessionHolder.getEntityOrNull(NetworkAssetRecord.class, sysId);
            if (rec != null)
            {
                try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, false))
                {
                    RecordHelper<AssetRecord> helper_asset = sessionHolder.createHelper(AssetRecord.class);

                    rec.setState(AssetState.retired);

                    rec.remove(validation, helper_asset);
                }

                sessionHolder.commit();

                return true;
            }

            return false;
        }
    }

    @Path(endpoint__MACHINE_SET)
    public DigineousMachineConfig machineSet(SessionProvider sessionProvider,
                                             CookiePrincipal principal,
                                             @QueryParam("arg") String sysId,
                                             DigineousMachineConfig details) throws
                                                                             Exception
    {
        principal.ensureInAnyRole(WellKnownRole.Maintenance, WellKnownRole.Administrator);

        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            NetworkAssetRecord rec_network = sessionHolder.getEntityOrNull(NetworkAssetRecord.class, sysId);
            if (rec_network != null)
            {
                rec_network.putMetadata(NetworkAssetRecord.WellKnownMetadata.digineous_machineConfig, fixupMachineConfig(details));

                syncMachine(sessionHolder, rec_network);

                sessionHolder.commit();

                return details;
            }

            return null;
        }
    }

    //--//

    @Path(endpoint__VIBRATION_LIST)
    public List<DigineousVibrationMonitorDetails> vibrationList(SessionProvider sessionProvider,
                                                                CookiePrincipal principal) throws
                                                                                           Exception
    {
        principal.ensureInAnyRole(WellKnownRole.Maintenance, WellKnownRole.Administrator);

        return collectMonitorMacAddress();
    }

    private List<DigineousVibrationMonitorDetails> collectMonitorMacAddress() throws
                                                                              Exception
    {
        Integer targetPlantId = m_app.computeWithPrivateValue(c_credentialKeyForVibrationServicesPlantId, (plantId, passwd) -> Integer.parseInt(plantId));

        Spooler               spooler = ensureSpooler();
        InfiniteImpulseHelper helper  = spooler.buildInfiniteImpulseHelper();

        List<DigineousVibrationMonitorDetails> lst = Lists.newArrayList();

        Map<Integer, String> plants = helper.getPlants();
        for (Integer id : plants.keySet())
        {
            if (targetPlantId == null || Objects.equals(targetPlantId, id))
            {
                List<MonitorsResponse> monitors = helper.getMonitorsByPlantId(id, true);
                for (MonitorsResponse monitor : monitors)
                {
                    if (monitor.isActive)
                    {
                        DigineousVibrationMonitorDetails obj = new DigineousVibrationMonitorDetails();
                        obj.id         = monitor.id;
                        obj.plantId    = monitor.plantId;
                        obj.deviceName = monitor.deviceName;
                        obj.label      = monitor.label;
                        lst.add(obj);
                    }
                }
            }
        }

        lst.sort(Comparator.comparing(a -> a.id));
        return lst;
    }

    private void resolveMonitorMacAddress(int id) throws
                                                  Exception
    {
        for (DigineousVibrationMonitorDetails obj : collectMonitorMacAddress())
        {
            if (obj.id == id)
            {
                return;
            }
        }

        throw Exceptions.newGenericException(InvalidArgumentException.class, "Unknown Vibration Monitor '%d'", id);
    }

    //--//

    @Path(endpoint__DEVICE_ACTIVE)
    public Map<Integer, ZonedDateTime> deviceActive(SessionProvider sessionProvider,
                                                    CookiePrincipal principal) throws
                                                                               Exception
    {
        Spooler spooler = ensureSpooler();

        return spooler.getActiveDevices();
    }

    @Path(endpoint__DATA)
    public DigineousBlackBoxPayload handlePoints(SessionProvider sessionProvider,
                                                 JsonNode json) throws
                                                                Exception
    {
        if (json == null)
        {
            return null;
        }

        ObjectMapper mapper = DigineousBlackBoxPayload.getFixupObjectMapper(ObjectMappers.SkipNulls);

        Spooler spooler = ensureSpooler();

        List<JsonNode> elements = Lists.newArrayList();

        if (json.isArray())
        {
            for (JsonNode jsonNode : json)
            {
                elements.add(jsonNode);
            }
        }
        else
        {
            elements.add(json);
        }

        FieldModel[] fieldModels  = DigineousBlackBoxPayload.getDescriptors();
        LookupPoints lookupPoints = spooler.ensurePointsLookup();
        PointLocator pl           = null;

        for (JsonNode element : elements)
        {
            DigineousBlackBoxPayload payload = mapper.treeToValue(element, DigineousBlackBoxPayload.class);

            pl = spooler.dispatch(lookupPoints, fieldModels, DigineousDeviceFlavor.BlackBox, payload.deviceID, ZonedDateTime.of(payload.ReceivedDateTime, ZoneOffset.UTC), payload);
        }

        DigineousBlackBoxPayload reply = new DigineousBlackBoxPayload();
        reply.ID               = m_counter.incrementAndGet();
        reply.ReceivedDateTime = TimeUtils.nowUtc()
                                          .toLocalDateTime();

        if (pl != null)
        {
            pl.populateReply(sessionProvider, reply);
        }

        return reply;
    }

    //--//

    static BaseDigineousModel extractFlavor(DeviceRecord rec)
    {
        // Any concrete subclass of BaseDigineousModel would do.
        Digineous_AnalogSignal obj = new Digineous_AnalogSignal();

        IpnDeviceDescriptor desc = rec.getIdentityDescriptor(IpnDeviceDescriptor.class);
        if (desc != null && obj.parseId(desc.name))
        {
            return obj;
        }

        return null;
    }

    private FieldModel[] getFieldModels(DigineousDeviceFlavor deviceFlavor)
    {
        switch (deviceFlavor)
        {
            default:
            case BlackBox:
                return DigineousBlackBoxPayload.getDescriptors();

            case InfiniteImpulse_Min:
            case InfiniteImpulse_Avg:
            case InfiniteImpulse_Max:
                return DigineousVibrationPayload.getDescriptors();
        }
    }

    private void populateDeviceLibrary(DigineousDeviceLibrary res,
                                       DigineousDeviceLibrary template,
                                       FieldModel[] fieldModels)
    {
        if (template != null)
        {
            res.id             = template.id;
            res.name           = template.name;
            res.deviceFlavor   = template.deviceFlavor;
            res.equipmentClass = template.equipmentClass;
        }

        res.ensureInitialized();

        for (FieldModel fieldModel : fieldModels)
        {
            DigineousPointLibrary point = template != null ? template.locatePoint(fieldModel.name) : null;
            if (point == null)
            {
                point             = new DigineousPointLibrary();
                point.identifier  = fieldModel.name;
                point.description = fieldModel.getDescription(null);
                point.units       = fieldModel.getUnits(null);
                point.pointClass  = fieldModel.getPointClass(null);
                point.tags        = fieldModel.getPointTags(null);
            }

            res.points.add(point);
        }
    }

    //--//

    static class PointLocator
    {
        RecordLocator<NetworkAssetRecord> network;

        DigineousDeviceFlavor deviceFlavor;
        int                   deviceId;

        RecordLocator<IpnDeviceRecord>                  device;
        BaseAssetDescriptor                             deviceDesc;
        DigineousDeviceLibrary                          deviceLibrary;
        Map<String, RecordLocator<DeviceElementRecord>> points;

        public ZonedDateTime getLastPull(SessionProvider sessionProvider)
        {
            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                IpnDeviceRecord rec_device = sessionHolder.fromLocatorOrNull(device);
                return rec_device != null ? rec_device.getMetadata(IpnDeviceRecord.WellKnownMetadata.infiniteImpulseLastPull) : null;
            }
        }

        public void setLastPull(SessionProvider sessionProvider,
                                ZonedDateTime lastTimestamp)
        {
            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                IpnDeviceRecord rec_device = sessionHolder.fromLocatorOrNull(device);
                if (rec_device != null)
                {
                    rec_device.putMetadata(IpnDeviceRecord.WellKnownMetadata.infiniteImpulseLastPull, lastTimestamp);

                    sessionHolder.commit();
                }
            }
        }

        public void populateReply(SessionProvider sessionProvider,
                                  DigineousBlackBoxPayload reply) throws
                                                                  Exception
        {
            reply.deviceID = deviceId;

            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                RecordHelper<DeviceElementRecord> helper_element = sessionHolder.createHelper(DeviceElementRecord.class);

                IpnDeviceRecord rec_device = sessionHolder.fromLocatorOrNull(device);
                if (rec_device != null)
                {
                    var fields = Reflection.collectFields(DigineousBlackBoxPayload.class);
                    for (String fieldName : fields.keySet())
                    {
                        DigineousPointLibrary point = deviceLibrary.locatePoint(fieldName);
                        if (point != null && point.enabled)
                        {
                            DeviceElementRecord rec_element = locatePoint(sessionHolder, helper_element, rec_device, fieldName);
                            if (rec_element != null)
                            {
                                Digineous_StatusSignal obj = Reflection.as(rec_element.getTypedDesiredContents(IpnObjectModel.getObjectMapper(), IpnObjectModel.class), Digineous_StatusSignal.class);
                                if (obj != null && obj.isAbleToUpdateState(point.identifier))
                                {
                                    Field f = fields.get(fieldName);
                                    f.set(reply, Reflection.coerceNumber(obj.active, f.getType()));
                                }
                            }
                        }
                    }
                }
            }
        }

        private DeviceElementRecord locatePoint(SessionHolder sessionHolder,
                                                RecordHelper<DeviceElementRecord> helper_element,
                                                IpnDeviceRecord rec_device,
                                                String fieldName)
        {
            if (points == null)
            {
                points = Maps.newHashMap();
            }

            RecordLocator<DeviceElementRecord> loc = points.get(fieldName);
            DeviceElementRecord                rec = loc != null ? sessionHolder.fromLocatorOrNull(loc) : null;

            if (rec == null)
            {
                rec = DeviceElementRecord.findByIdentifierOrNull(helper_element, rec_device, fieldName);
                points.put(fieldName, sessionHolder.createLocator(rec));
            }

            return rec;
        }
    }

    static class ElementPending
    {
        final String                                     elementIdentifier;
        final TreeMap<ZonedDateTime, BaseDigineousModel> samples = new TreeMap<>();

        ElementPending(String elementIdentifier)
        {
            this.elementIdentifier = elementIdentifier;
        }
    }

    static class DevicePending
    {
        final BaseAssetDescriptor         deviceDesc;
        final DigineousDeviceLibrary      deviceLibrary;
        final Map<String, ElementPending> elements = Maps.newHashMap();

        int pending;

        DevicePending(BaseAssetDescriptor deviceDesc,
                      DigineousDeviceLibrary deviceLibrary)
        {
            this.deviceDesc    = deviceDesc;
            this.deviceLibrary = deviceLibrary;
        }
    }

    static class NetworkPending
    {
        final RecordLocator<NetworkAssetRecord> network;
        final Map<String, DevicePending>        devices = Maps.newHashMap();

        int pending;

        NetworkPending(RecordLocator<NetworkAssetRecord> network)
        {
            this.network = network;
        }
    }

    static class LookupPoints
    {
        private final Map<DigineousDeviceFlavor, Map<Integer, PointLocator>> map = Maps.newHashMap();

        static LookupPoints build(SessionProvider sessionProvider) throws
                                                                   Exception
        {
            LookupPoints lookupPoints = new LookupPoints();

            try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
            {
                IpnDeviceRecord.enumerateNoNesting(sessionHolder.createHelper(IpnDeviceRecord.class), -1, null, (rec_device) ->
                {
                    BaseDigineousModel obj = extractFlavor(rec_device);
                    if (obj != null)
                    {
                        PointLocator pl = new PointLocator();

                        pl.deviceLibrary = resolveDeviceLibrary(sessionHolder, rec_device);
                        if (pl.deviceLibrary != null)
                        {
                            pl.network = sessionHolder.createLocator(rec_device.getParentAsset(NetworkAssetRecord.class));

                            pl.deviceFlavor = obj.deviceFlavor;
                            pl.deviceId     = obj.deviceId;

                            pl.device     = sessionHolder.createLocator(rec_device);
                            pl.deviceDesc = rec_device.getIdentityDescriptor();

                            Map<Integer, PointLocator> mapSub = lookupPoints.map.computeIfAbsent(pl.deviceFlavor, (k) -> Maps.newHashMap());
                            mapSub.put(pl.deviceId, pl);
                        }
                    }

                    return StreamHelperNextAction.Continue_Evict;
                });
            }

            return lookupPoints;
        }

        PointLocator get(DigineousDeviceFlavor flavor,
                         int deviceId)
        {
            var mapSub = map.get(flavor);
            return mapSub != null ? mapSub.get(deviceId) : null;
        }

        void collectIds(Set<Integer> ids,
                        DigineousDeviceFlavor flavor)
        {
            var mapSub = map.get(flavor);
            if (mapSub != null)
            {
                ids.addAll(mapSub.keySet());
            }
        }
    }

    //--//

    static class PullStats
    {
        int notFound;
        int hits;
        int misses;
        int skippedSamples;
        int errors;

        static void dump(Severity level,
                         Map<Integer, PullStats> monitorStats)
        {
            LoggerInstanceForPull.log(null, level, null, null, "Statistics: %,d monitor(s)", monitorStats.size());

            monitorStats.forEach((k, v) ->
                                 {
                                     LoggerInstanceForPull.log(null,
                                                               level,
                                                               null,
                                                               null,
                                                               "   %d => hits(%d) misses(%d) skipped(%d) errors(%d) notFound(%d)",
                                                               k,
                                                               v.hits,
                                                               v.misses,
                                                               v.skippedSamples,
                                                               v.errors,
                                                               v.notFound);
                                 });
        }
    }

    class Spooler
    {
        private final DebouncedAction<Void> m_flusher = new DebouncedAction<>(this::flushNewSamplesInBackground);

        private final Map<Integer, PullStats> m_monitorStatistics = Maps.newHashMap();
        private       MonotonousTime          m_nextMonitorsReport;

        private LookupPoints m_lookupPoints;

        private final Map<String, NetworkPending> m_rootsForSamples = Maps.newHashMap();
        private final Map<Integer, ZonedDateTime> m_activeDevices   = Maps.newHashMap();
        private       int                         m_pendingSamples;

        private void flushState()
        {
            m_lookupPoints = null;
        }

        private LookupPoints ensurePointsLookup() throws
                                                  Exception
        {
            synchronized (m_rootsForSamples)
            {
                if (m_lookupPoints == null)
                {
                    m_lookupPoints = LookupPoints.build(m_sessionProvider);
                }

                return m_lookupPoints;
            }
        }

        ZonedDateTime pullVibrationData() throws
                                          Exception
        {
            InfiniteImpulseHelper helper = buildInfiniteImpulseHelper();
            if (helper == null)
            {
                // No credentials, exiting.
                return null;
            }

            LookupPoints lookupPoints = ensurePointsLookup();

            Set<Integer> monitors = Sets.newHashSet();
            lookupPoints.collectIds(monitors, DigineousDeviceFlavor.InfiniteImpulse_Min);
            lookupPoints.collectIds(monitors, DigineousDeviceFlavor.InfiniteImpulse_Avg);
            lookupPoints.collectIds(monitors, DigineousDeviceFlavor.InfiniteImpulse_Max);

            if (!monitors.isEmpty())
            {
                FieldModel[] fieldModels = DigineousVibrationPayload.getDescriptors();

                for (Integer monitorId : monitors)
                {
                    PullStats ps = m_monitorStatistics.computeIfAbsent(monitorId, (k) -> new PullStats());

                    try
                    {
                        PointLocator plMin = lookupPoints.get(DigineousDeviceFlavor.InfiniteImpulse_Min, monitorId);
                        PointLocator plAvg = lookupPoints.get(DigineousDeviceFlavor.InfiniteImpulse_Avg, monitorId);
                        PointLocator plMax = lookupPoints.get(DigineousDeviceFlavor.InfiniteImpulse_Max, monitorId);

                        ZonedDateTime start = null;
                        boolean       first;

                        if (plMin != null)
                        {
                            start = TimeUtils.min(start, plMin.getLastPull(m_sessionProvider));
                        }

                        if (plAvg != null)
                        {
                            start = TimeUtils.min(start, plAvg.getLastPull(m_sessionProvider));
                        }

                        if (plMax != null)
                        {
                            start = TimeUtils.min(start, plMax.getLastPull(m_sessionProvider));
                        }

                        first = start == null;

                        if (first)
                        {
                            start = TimeUtils.past(1, TimeUnit.HOURS);
                        }
                        else
                        {
                            // Don't fetch more than a day of backlog.
                            start = TimeUtils.max(start, TimeUtils.past(1, TimeUnit.DAYS));
                        }

                        ZonedDateTime lastTimestamp = start;

                        // Back up a bit, in case there's some overlap we are missing.
                        start = start.minus(10, ChronoUnit.MINUTES);

                        ZonedDateTime now = TimeUtils.nowUtc();

                        while (true)
                        {
                            ZonedDateTime end = start.plus(30, ChronoUnit.MINUTES);

                            LoggerInstanceForPull.debugObnoxious("Fetch: %s # %s", lastTimestamp, start);

                            TrendHistory res = helper.getTrend(start, end, true, monitorId);
                            if (res == null)
                            {
                                ps.notFound++;
                                break;
                            }

                            // Sort, just in case...
                            res.basic_features.sort((a, b) -> TimeUtils.compare(a.time, b.time));

                            boolean madeProgress = false;

                            for (TrendEntry entry : res.basic_features)
                            {
                                long diff = Duration.between(lastTimestamp, entry.time)
                                                    .toSeconds();

                                if (diff <= 0)
                                {
                                    // Already seen this sample, skip.
                                    LoggerInstanceForPull.debugObnoxious("Seen: %s # %s", lastTimestamp, entry.time);
                                    continue;
                                }

                                if (!first && diff > 10)
                                {
                                    long skippedSamples = (diff - 10) / 10;

                                    LoggerInstanceForPull.debugObnoxious("Skip (%d): %s => %s", skippedSamples, lastTimestamp, entry.time);
                                    ps.skippedSamples += skippedSamples;
                                    ps.misses++;
                                }
                                else
                                {
                                    ps.hits++;
                                }

                                dispatchVibration(fieldModels, lookupPoints, plMin, DigineousDeviceFlavor.InfiniteImpulse_Min, monitorId, entry.time, entry.min);
                                dispatchVibration(fieldModels, lookupPoints, plAvg, DigineousDeviceFlavor.InfiniteImpulse_Avg, monitorId, entry.time, entry.avg);
                                dispatchVibration(fieldModels, lookupPoints, plMax, DigineousDeviceFlavor.InfiniteImpulse_Max, monitorId, entry.time, entry.max);

                                lastTimestamp = entry.time;
                                madeProgress  = true;
                                first         = false;
                            }

                            if (!madeProgress)
                            {
                                if (end.isBefore(now))
                                {
                                    start = end;
                                    continue;
                                }

                                break;
                            }

                            start = lastTimestamp;
                        }

                        if (plMin != null)
                        {
                            plMin.setLastPull(m_sessionProvider, lastTimestamp);
                        }

                        if (plAvg != null)
                        {
                            plAvg.setLastPull(m_sessionProvider, lastTimestamp);
                        }

                        if (plMax != null)
                        {
                            plMax.setLastPull(m_sessionProvider, lastTimestamp);
                        }
                    }
                    catch (Throwable t)
                    {
                        LoggerInstanceForPull.debug("Failed to fetch data for Infinite Impulse monitor %d, due to %s", monitorId, t);
                        ps.errors++;
                    }
                }

                if (LoggerInstanceForPull.isEnabled(Severity.DebugVerbose))
                {
                    PullStats.dump(Severity.DebugVerbose, m_monitorStatistics);
                }
                else if (LoggerInstanceForPull.isEnabled(Severity.Debug) && TimeUtils.isTimeoutExpired(m_nextMonitorsReport))
                {
                    m_nextMonitorsReport = TimeUtils.computeTimeoutExpiration(1, TimeUnit.HOURS);

                    PullStats.dump(Severity.Debug, m_monitorStatistics);
                }
            }

            Integer frequency = m_app.computeWithPrivateValue(c_credentialKeyForVibrationServicesFrequency, (plantId, passwd) -> Integer.parseInt(plantId));
            return TimeUtils.future(BoxingUtils.get(frequency, 5 * 60), TimeUnit.SECONDS);
        }

        private InfiniteImpulseHelper buildInfiniteImpulseHelper()
        {
            try
            {
                InfiniteImpulseHelper res = m_infiniteImpulseHelper;
                if (res == null || res.isStale())
                {
                    res                     = m_app.computeWithPrivateValue(c_credentialKeyForVibrationServices, (email, passwd) -> new InfiniteImpulseHelper(null, email, passwd));
                    m_infiniteImpulseHelper = res;
                }

                return res;
            }
            catch (Throwable t)
            {
                LoggerInstanceForPull.error("Failed to fetch secrets, due to %s", t);
            }

            return null;
        }

        private void dispatchVibration(FieldModel[] fieldModels,
                                       LookupPoints lookupPoints,
                                       PointLocator pl,
                                       DigineousDeviceFlavor flavor,
                                       int monitorId,
                                       ZonedDateTime time,
                                       TrendValue val) throws
                                                       Exception
        {
            if (pl != null && val != null)
            {
                synchronized (m_activeDevices)
                {
                    m_activeDevices.put(monitorId, TimeUtils.now());
                }

                DigineousVibrationPayload payload = new DigineousVibrationPayload();
                payload.totalAcceleration = limitPrecision(val.totalAcceleration);
                payload.velocityX         = limitPrecision(val.velocityX);
                payload.velocityY         = limitPrecision(val.velocityY);
                payload.velocityZ         = limitPrecision(val.velocityZ);
                payload.temperature       = limitPrecision(val.temperature);
                payload.audio             = val.audio;

                dispatch(lookupPoints, fieldModels, flavor, monitorId, time, payload);
            }
        }

        //--//

        Map<Integer, ZonedDateTime> getActiveDevices()
        {
            synchronized (m_activeDevices)
            {
                return Maps.newHashMap(m_activeDevices);
            }
        }

        PointLocator dispatch(LookupPoints lookupPoints,
                              FieldModel[] fieldModels,
                              DigineousDeviceFlavor flavor,
                              int deviceId,
                              ZonedDateTime timestamp,
                              Object payload) throws
                                              Exception
        {
            synchronized (m_activeDevices)
            {
                m_activeDevices.put(deviceId, TimeUtils.now());
            }

            PointLocator pl = lookupPoints.get(flavor, deviceId);
            if (pl == null || pl.network == null)
            {
                // Unknown point.
                return null;
            }

            synchronized (m_rootsForSamples)
            {
                final PointLocator finalPl = pl;

                NetworkPending network = m_rootsForSamples.computeIfAbsent(pl.network.getIdRaw(), (key) -> new NetworkPending(finalPl.network));
                DevicePending  device  = network.devices.computeIfAbsent(pl.device.getIdRaw(), (key) -> new DevicePending(finalPl.deviceDesc, finalPl.deviceLibrary));

                for (FieldModel fieldModel : fieldModels)
                {
                    BaseDigineousModel model = null;

                    DigineousPointLibrary point = pl.deviceLibrary.locatePoint(fieldModel.name);
                    if (point != null && point.enabled)
                    {
                        Field  f   = Reflection.findField(payload.getClass(), fieldModel.name);
                        Object val = f.get(payload);

                        if (fieldModel.type == Float.class && val instanceof Float)
                        {
                            model = point.buildAnalog((float) val);
                        }
                        else if (fieldModel.type == Double.class && val instanceof Double)
                        {
                            model = point.buildAnalog((float) (double) val);
                        }
                        else if (fieldModel.type == Boolean.class && val instanceof Boolean)
                        {
                            model = point.buildDigital((boolean) val);
                        }
                        else if (fieldModel.type == Integer.class && val instanceof Integer)
                        {
                            model = point.buildDigital(((int) val) != 0);
                        }
                    }

                    if (model != null)
                    {
                        model.deviceFlavor = flavor;
                        model.deviceId     = deviceId;
                        model.units        = point.units;

                        ElementPending element = device.elements.computeIfAbsent(fieldModel.name, (key) -> new ElementPending(fieldModel.name));

                        element.samples.put(timestamp, model);
                        m_pendingSamples++;
                    }
                }

                network.pending++;
                device.pending++;
            }

            flushNewSamples(1000);

            return pl;
        }

        private void flushNewSamples(int threshold)
        {
            synchronized (m_rootsForSamples)
            {
                if (m_pendingSamples > threshold)
                {
                    m_pendingSamples = 0;

                    m_flusher.cancel();
                    m_flusher.schedule(0, TimeUnit.MILLISECONDS);
                }
                else
                {
                    m_flusher.schedule(1, TimeUnit.SECONDS);
                }
            }
        }

        private CompletableFuture<Void> flushNewSamplesInBackground() throws
                                                                      Exception
        {
            while (m_sessionProvider != null)
            {
                List<GatewayDiscoveryEntity> batch = extractBatch();
                if (batch.isEmpty())
                {
                    break;
                }

                if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
                {
                    LoggerInstance.debugObnoxious(ObjectMappers.prettyPrintAsJson(batch));
                }

                try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
                {
                    ResultStagingRecord.queue(sessionHolder.createHelper(ResultStagingRecord.class), batch);

                    sessionHolder.commit();
                }
            }

            return AsyncRuntime.NullResult;
        }

        private List<GatewayDiscoveryEntity> extractBatch() throws
                                                            Exception
        {
            List<GatewayDiscoveryEntity> batch     = Lists.newArrayList();
            int                          batchSize = 0;

            synchronized (m_rootsForSamples)
            {
                for (NetworkPending np : m_rootsForSamples.values())
                {
                    if (np.pending > 0)
                    {
                        GatewayDiscoveryEntity en_network  = newDiscoveryEntry(null, GatewayDiscoveryEntitySelector.Network, np.network.getIdRaw());
                        GatewayDiscoveryEntity en_protocol = newDiscoveryEntry(en_network, GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Ipn);

                        batch.add(en_network);

                        for (DevicePending dp : np.devices.values())
                        {
                            if (dp.pending > 0)
                            {
                                GatewayDiscoveryEntity en_device = newDiscoveryEntry(en_protocol, GatewayDiscoveryEntitySelector.Ipn_Device, dp.deviceDesc);

                                for (ElementPending ep : dp.elements.values())
                                {
                                    for (Map.Entry<ZonedDateTime, BaseDigineousModel> entry : ep.samples.entrySet())
                                    {
                                        ZonedDateTime      timestamp = entry.getKey();
                                        BaseDigineousModel obj       = entry.getValue();

                                        for (FieldModel fieldModel : obj.getDescriptors())
                                        {
                                            if (obj.shouldIncludeProperty(fieldModel.name))
                                            {
                                                double timestampEpochSeconds = TimeUtils.fromUtcTimeToTimestamp(timestamp);

                                                GatewayDiscoveryEntity en_object = newDiscoveryEntry(en_device, GatewayDiscoveryEntitySelector.Ipn_Object, ep.elementIdentifier);
                                                newDiscoverySample(en_object, timestampEpochSeconds, obj);

                                                batchSize++;
                                            }
                                        }
                                    }

                                    ep.samples.clear();

                                    if (batchSize > 10_000)
                                    {
                                        // Limit max size of a single batch.
                                        return batch;
                                    }
                                }

                                dp.pending = 0;
                            }
                        }

                        np.pending = 0;
                    }
                }
            }

            return batch;
        }

        private double limitPrecision(double val)
        {
            final double precision = 100;

            long valTruncated = (long) (val * precision);
            return valTruncated * (1.0 / precision);
        }
    }

    //--//

    private void syncMachine(SessionHolder sessionHolder,
                             NetworkAssetRecord rec_network) throws
                                                             Exception
    {
        DigineousMachineConfig machineConfig = rec_network.getMetadata(NetworkAssetRecord.WellKnownMetadata.digineous_machineConfig);
        if (machineConfig == null)
        {
            throw Exceptions.newIllegalArgumentException("No Machine Config");
        }

        DigineousMachineLibrary machineLibrary = resolveMachineTemplate(sessionHolder, machineConfig.machineTemplate);
        if (machineLibrary == null)
        {
            throw Exceptions.newIllegalArgumentException("Unknown Machine Library template '%s'", machineConfig.machineTemplate);
        }

        List<String> validDevices = Lists.newArrayList();

        for (DigineousDeviceConfig deviceConfig : machineConfig.devices)
        {
            String deviceTemplateId = CollectionUtils.getNthElement(machineLibrary.deviceTemplates, deviceConfig.deviceIndex);
            if (deviceTemplateId == null)
            {
                throw Exceptions.newIllegalArgumentException("Unknown Device '%d' at index %d", deviceConfig.deviceId, deviceConfig.deviceIndex);
            }

            DigineousDeviceLibrary deviceLibrary = resolveDeviceTemplate(sessionHolder, deviceTemplateId);
            if (deviceLibrary == null)
            {
                throw Exceptions.newIllegalArgumentException("Unknown Device Library template '%s'", deviceTemplateId);
            }

            IpnDeviceRecord rec_device = createDevice(sessionHolder, rec_network, machineConfig, deviceConfig, deviceLibrary);
            validDevices.add(rec_device.getSysId());
        }

        //--//

        RecordHelper<AssetRecord>     helper_asset  = sessionHolder.createHelper(AssetRecord.class);
        RecordHelper<IpnDeviceRecord> helper_device = sessionHolder.createHelper(IpnDeviceRecord.class);

        for (IpnDeviceRecord rec_device : rec_network.getChildren(helper_device))
        {
            if (!validDevices.contains(rec_device.getSysId()))
            {
                try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, false, true))
                {
                    rec_device.remove(validation, helper_asset);
                }
            }
        }

        scheduleClassification(sessionHolder, rec_network);
    }

    private IpnDeviceRecord createDevice(SessionHolder sessionHolder,
                                         NetworkAssetRecord rec_network,
                                         DigineousMachineConfig machineConfig,
                                         DigineousDeviceConfig deviceConfig,
                                         DigineousDeviceLibrary deviceLibrary) throws
                                                                               Exception
    {
        if (deviceLibrary == null)
        {
            // Invalid.
            throw Exceptions.newIllegalArgumentException("No device template!");
        }

        LazyRecordFlusher<IpnDeviceRecord> lazy_device = IpnDeviceRecord.ensureIdentifier(sessionHolder.createHelper(IpnDeviceRecord.class),
                                                                                          rec_network,
                                                                                          BaseDigineousModel.buildId(deviceLibrary.deviceFlavor, deviceConfig.deviceId));
        IpnDeviceRecord rec_device = lazy_device.get();

        switch (deviceLibrary.deviceFlavor)
        {
            case InfiniteImpulse_Min:
            case InfiniteImpulse_Avg:
            case InfiniteImpulse_Max:
                DigineousDeviceConfig deviceConfigOld = rec_device.getMetadata(DeviceRecord.WellKnownMetadata.digineous_deviceConfig);
                if (deviceConfigOld == null || deviceConfigOld.deviceId != deviceConfig.deviceId)
                {
                    // Resolve only if new or changed.
                    resolveMonitorMacAddress(deviceConfig.deviceId);
                }
                break;
        }

        switch (deviceLibrary.deviceFlavor)
        {
            case InfiniteImpulse_Min:
                rec_device.setPhysicalName(String.format("Vibration %d / Min", deviceConfig.deviceId));
                break;

            case InfiniteImpulse_Avg:
                rec_device.setPhysicalName(String.format("Vibration %d / Average", deviceConfig.deviceId));
                break;

            case InfiniteImpulse_Max:
                rec_device.setPhysicalName(String.format("Vibration %d / Max", deviceConfig.deviceId));
                break;

            default:
                rec_device.setPhysicalName(String.format("BB #%d", deviceConfig.deviceId));
                break;
        }

        rec_device.putMetadata(DeviceRecord.WellKnownMetadata.digineous_deviceConfig, deviceConfig);
        rec_device.putMetadata(DeviceRecord.WellKnownMetadata.digineous_machineConfig, machineConfig);

        lazy_device.persistIfNeeded();

        syncDevice(sessionHolder, machineConfig, rec_device, deviceLibrary);

        return rec_device;
    }

    private void syncDevice(SessionHolder sessionHolder,
                            DigineousMachineConfig machineConfig,
                            DeviceRecord rec_device,
                            DigineousDeviceLibrary deviceLibrary) throws
                                                                  Exception
    {
        RecordHelper<DeviceElementRecord> helper_element = sessionHolder.createHelper(DeviceElementRecord.class);

        for (FieldModel fieldModel : getFieldModels(deviceLibrary.deviceFlavor))
        {
            BaseDigineousModel obj = null;

            DigineousPointLibrary point = deviceLibrary.locatePoint(fieldModel.name);
            if (point != null && point.enabled)
            {
                if (fieldModel.type == Float.class || fieldModel.type == Double.class)
                {
                    obj = new Digineous_AnalogSignal();
                }
                else if (fieldModel.type == Boolean.class)
                {
                    obj = new Digineous_StatusSignal();
                }
                else if (fieldModel.type == Integer.class)
                {
                    obj = new Digineous_StatusSignal();
                }
                else if (fieldModel.type == String.class)
                {
                    obj = new Digineous_LogSignal();
                }
            }

            syncDevicePoint(helper_element, rec_device, fieldModel, machineConfig, deviceLibrary.equipmentClass, point, obj);
        }
    }

    private void syncDevicePoint(RecordHelper<DeviceElementRecord> helper_element,
                                 DeviceRecord rec_device,
                                 FieldModel fieldModel,
                                 DigineousMachineConfig machineConfig,
                                 WellKnownEquipmentClassOrCustom deviceEquipmentClass,
                                 DigineousPointLibrary point,
                                 BaseDigineousModel obj) throws
                                                         Exception
    {
        if (obj != null)
        {
            SessionHolder           sessionHolder  = helper_element.currentSessionHolder();
            DigineousMachineLibrary machineLibrary = resolveMachineTemplate(sessionHolder, machineConfig.machineTemplate);

            obj.machineId             = machineConfig.machineId;
            obj.machineName           = machineConfig.machineName;
            obj.machineEquipmentClass = machineLibrary.equipmentClass;

            obj.deviceEquipmentClass = deviceEquipmentClass;

            obj.pointClass  = point.pointClass;
            obj.tags        = point.tags;
            obj.description = point.description;
            obj.units       = point.units;

            LazyRecordFlusher<DeviceElementRecord> lazy_object = DeviceElementRecord.ensureIdentifier(helper_element, rec_device, fieldModel.name);
            DeviceElementRecord                    rec_object  = lazy_object.get();

            ObjectMapper om = IpnObjectModel.getObjectMapper();
            rec_object.setContents(om, obj);
            rec_object.setDesiredContents(sessionHolder, om, obj);

            rec_object.setPhysicalName(fieldModel.getDescription(obj));

            List<DeviceElementSampling> config = Lists.newArrayList();
            DeviceElementSampling.add(config, DeviceElementRecord.DEFAULT_PROP_NAME, 30);
            rec_object.setSamplingSettings(config);

            lazy_object.persistIfNeeded();
        }
        else
        {
            DeviceElementRecord rec_element = DeviceElementRecord.findByIdentifierOrNull(helper_element, rec_device, fieldModel.name);
            if (rec_element != null)
            {
                helper_element.delete(rec_element);
            }
        }
    }

    //--//

    private static DigineousMachineConfig fixupMachineConfig(DigineousMachineConfig machineConfig)
    {
        int index = 0;

        for (DigineousDeviceConfig deviceConfig : machineConfig.devices)
        {
            deviceConfig.deviceIndex = index++;
        }

        return machineConfig;
    }

    private static DigineousMachineLibrary resolveMachineTemplate(SessionHolder sessionHolder,
                                                                  String id) throws
                                                                             IOException
    {
        return SystemPreferenceRecord.getTypedSubValue(sessionHolder, SystemPreferenceTypedValue.DigineousMachineTemplate, id, DigineousMachineLibrary.class);
    }

    private static DigineousDeviceLibrary resolveDeviceTemplate(SessionHolder sessionHolder,
                                                                String id) throws
                                                                           IOException
    {
        DigineousDeviceLibrary obj = SystemPreferenceRecord.getTypedSubValue(sessionHolder, SystemPreferenceTypedValue.DigineousDeviceTemplate, id, DigineousDeviceLibrary.class);
        if (obj != null)
        {
            obj.ensureInitialized();
        }
        return obj;
    }

    private static DigineousDeviceLibrary resolveDeviceLibrary(SessionHolder sessionHolder,
                                                               IpnDeviceRecord rec_device) throws
                                                                                           IOException
    {
        DigineousMachineConfig machineConfig = rec_device.getMetadata(DeviceRecord.WellKnownMetadata.digineous_machineConfig);
        if (machineConfig != null)
        {
            DigineousMachineLibrary machineLibrary = resolveMachineTemplate(sessionHolder, machineConfig.machineTemplate);
            if (machineLibrary != null)
            {
                DigineousDeviceConfig deviceConfig = rec_device.getMetadata(DeviceRecord.WellKnownMetadata.digineous_deviceConfig);
                if (deviceConfig != null)
                {
                    String deviceTemplateId = CollectionUtils.getNthElement(machineLibrary.deviceTemplates, deviceConfig.deviceIndex);
                    if (deviceTemplateId != null)
                    {
                        return resolveDeviceTemplate(sessionHolder, deviceTemplateId);
                    }
                }
            }
        }

        return null;
    }
}
