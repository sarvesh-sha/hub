/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.exception.DetailedApplicationException;
import com.optio3.cloud.exception.NotImplementedException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.model.asset.DeviceElementSampling;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyType;
import com.optio3.cloud.hub.orchestration.tasks.TaskForSamplingConfiguration;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.location.LocationRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.function.FunctionWithException;

@Optio3IncludeInApiDefinitions
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = InstanceConfigurationDoNothing.class),
                @JsonSubTypes.Type(value = InstanceConfigurationForCRE.class),
                @JsonSubTypes.Type(value = InstanceConfigurationWithWellKnownClasses.class) })
public abstract class InstanceConfiguration
{
    public static final Logger LoggerInstance = new Logger(InstanceConfiguration.class);

    //--//

    private enum EndpointArgument
    {
        SessionProvider,
        ContainerRequestContext,
        CookiePrincipal,
        Query,
        Json,
        Stream,
    }

    private static class EndpointDescriptor
    {
        Method             method;
        String             id;
        EndpointArgument[] argsMapping;
    }

    //--//

    protected HubApplication m_app;

    public void setApp(HubApplication app)
    {
        m_app = app;
    }

    public abstract void start() throws
                                 SocketException;

    public abstract void stop();

    public abstract CompletableFuture<Void> preprocessResults(List<GatewayDiscoveryEntity> entities) throws
                                                                                                     Exception;

    public abstract boolean hasRoamingAssets();

    public abstract boolean shouldAutoConfig();

    public abstract boolean fixupAutoConfig(ProtocolConfigForIpn cfg);

    public abstract boolean shouldReportWhenUnreachable(DeviceRecord rec,
                                                        ZonedDateTime unresponsiveSince);

    public abstract boolean prepareSamplingConfiguration(SessionHolder sessionHolder,
                                                         DeviceRecord rec_device,
                                                         DeviceElementRecord rec_obj,
                                                         boolean checkNonZeroValue,
                                                         List<DeviceElementSampling> config) throws
                                                                                             IOException;

    public void handleSamplingReconfiguration(SessionHolder sessionHolder,
                                              NetworkAssetRecord rec_network) throws
                                                                              Exception
    {
        BackgroundActivityRecord rec_classification = scheduleClassification(sessionHolder, rec_network);

        //
        // Start task with a delay, to make sure we pick up as many changes as possible.
        //
        BackgroundActivityRecord rec_samplingConfig = TaskForSamplingConfiguration.scheduleTaskIfNotRunning(sessionHolder, rec_network);
        if (rec_samplingConfig != null && rec_classification != null)
        {
            rec_samplingConfig.transitionToWaiting(rec_classification, null);
        }
    }

    public final boolean updateNormalizationRules(SessionHolder sessionHolder) throws
                                                                               Exception
    {
        RecordHelper<NormalizationRecord> helper = sessionHolder.createHelper(NormalizationRecord.class);

        boolean updated = false;

        // Roundtrip all the rules, in case we have fixups to apply.
        for (NormalizationRecord rec : helper.listAll())
        {
            updated |= rec.setRules(rec.getRules());
        }

        NormalizationRecord rec = NormalizationRecord.findActive(helper);

        NormalizationRules rules = updateNormalizationRules(sessionHolder, rec != null ? rec.getRules() : null);
        if (rules != null)
        {
            if (rec != null)
            {
                updated |= rec.setRules(rules);
            }
            else
            {
                rec = NormalizationRecord.newInstance(helper, rules, null);

                helper.persist(rec);

                rec.makeActive(helper);
                updated = true;
            }
        }

        return updated;
    }

    public abstract NormalizationRules updateNormalizationRules(SessionHolder sessionHolder,
                                                                NormalizationRules rules) throws
                                                                                          Exception;

    public abstract void reclassify();

    public abstract BackgroundActivityRecord scheduleClassification(SessionHolder sessionHolder,
                                                                    NetworkAssetRecord rec_network) throws
                                                                                                    Exception;

    public abstract void executeClassification(SessionProvider sessionProvider,
                                               RecordLocator<NetworkAssetRecord> loc_network) throws
                                                                                              Exception;

    public abstract void handleWorkflowCreated(SessionHolder sessionHolder,
                                               WorkflowRecord rec,
                                               UserRecord rec_user);

    public abstract void handleWorkflowUpdated(SessionHolder sessionHolder,
                                               WorkflowRecord rec,
                                               UserRecord rec_user);

    public final JsonNode handleEndpoint(SessionProvider sessionProvider,
                                         ContainerRequestContext requestContext,
                                         String endpointId,
                                         String endpointArg,
                                         JsonNode json,
                                         InputStream stream) throws
                                                             Throwable
    {
        if (LoggerInstance.isEnabled(Severity.Debug))
        {
            LoggerInstance.debug("Got %s message from endpoint '%s':", stream != null ? "stream" : "json", endpointId);
            LoggerInstance.debugObnoxious(ObjectMappers.prettyPrintAsJson(json));
        }

        NextEndpoint:
        for (EndpointDescriptor ed : getEndpointDescriptors())
        {
            if (!ed.id.equals(endpointId))
            {
                continue;
            }

            Parameter[] parameters = ed.method.getParameters();
            Object[]    args       = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++)
            {
                Parameter parameter = parameters[i];
                Type      argType   = parameter.getType();

                switch (ed.argsMapping[i])
                {
                    case SessionProvider:
                        args[i] = sessionProvider;
                        break;

                    case ContainerRequestContext:
                        args[i] = requestContext;
                        break;

                    case CookiePrincipal:
                        args[i] = CookiePrincipal.getFromContext(requestContext);
                        break;

                    case Stream:
                        if (argType != InputStream.class)
                        {
                            continue NextEndpoint;
                        }

                        args[i] = stream;
                        break;

                    case Json:
                        if (argType == InputStream.class)
                        {
                            continue NextEndpoint;
                        }

                        try
                        {
                            args[i] = ObjectMappers.SkipNulls.treeToValue(json, Reflection.getRawType(argType));
                            ModelMapper.trimModel(args[i]);
                        }
                        catch (Throwable t)
                        {
                            LoggerInstance.debugVerbose("Failed to decode payload as '%s', due to %s", argType, t);
                            continue NextEndpoint;
                        }
                        break;

                    default:
                        if (argType == String.class)
                        {
                            args[i] = endpointArg;
                        }
                        break;
                }
            }

            try
            {
                Object res = ed.method.invoke(this, args);

                if (LoggerInstance.isEnabled(Severity.Debug))
                {
                    LoggerInstance.debug("Replying from endpoint '%s':", endpointId);
                    LoggerInstance.debugObnoxious(ObjectMappers.prettyPrintAsJson(res));
                }

                return ObjectMappers.SkipNulls.valueToTree(res);
            }
            catch (InvocationTargetException e)
            {
                Throwable e2 = Exceptions.unwrapException(e);

                LoggerInstance.debug("Error processing endpoint '%s': %s", endpointId, e2);

                if (e2 instanceof DetailedApplicationException)
                {
                    throw e2;
                }

                throw new WebApplicationException(e2.getMessage());
            }
        }

        throw new NotImplementedException(endpointId);
    }

    public boolean canAcceptNewSamples(CookiePrincipal principal,
                                       DeviceElementRecord rec_element,
                                       TimeSeriesPropertyType pt,
                                       BaseObjectModel model)
    {
        return false;
    }

    //--//

    private EndpointDescriptor[] m_endpointDescriptors;

    private EndpointDescriptor[] getEndpointDescriptors()
    {
        if (m_endpointDescriptors == null)
        {
            List<EndpointDescriptor> lst = Lists.newArrayList();

            NextMethod:
            for (Method m : Reflection.collectMethods(getClass())
                                      .values())
            {
                Path anno = m.getAnnotation(Path.class);
                if (anno == null)
                {
                    continue;
                }

                EndpointDescriptor ed = new EndpointDescriptor();
                ed.method = m;
                ed.id     = anno.value();

                Parameter[] parameters = m.getParameters();
                ed.argsMapping = new EndpointArgument[parameters.length];

                NextArg:
                for (int i = 0; i < parameters.length; i++)
                {
                    Parameter parameter = parameters[i];
                    Type      argType   = parameter.getType();

                    if (argType == SessionProvider.class)
                    {
                        ed.argsMapping[i] = EndpointArgument.SessionProvider;
                        continue NextArg;
                    }

                    if (argType == ContainerRequestContext.class)
                    {
                        ed.argsMapping[i] = EndpointArgument.ContainerRequestContext;
                        continue NextArg;
                    }

                    if (argType == CookiePrincipal.class)
                    {
                        ed.argsMapping[i] = EndpointArgument.CookiePrincipal;
                        continue NextArg;
                    }

                    QueryParam annoParam = parameter.getAnnotation(QueryParam.class);
                    if (annoParam != null)
                    {
                        switch (annoParam.value())
                        {
                            case "arg":
                                ed.argsMapping[i] = EndpointArgument.Query;
                                continue NextArg;
                        }
                    }
                    else
                    {
                        ed.argsMapping[i] = argType == InputStream.class ? EndpointArgument.Stream : EndpointArgument.Json;
                        continue NextArg;
                    }

                    continue NextMethod;
                }

                lst.add(ed);
            }

            m_endpointDescriptors = new EndpointDescriptor[lst.size()];
            lst.toArray(m_endpointDescriptors);
        }

        return m_endpointDescriptors;
    }

    //--//

    public static class InstanceState
    {
        public GatewayAssetRecord rec_gateway;
        public boolean            createdGateway;

        public NetworkAssetRecord rec_network;
        public boolean            createdNetwork;
    }

    public final InstanceState resolveInstanceId(SessionHolder sessionHolder,
                                                 String instanceId) throws
                                                                    Exception
    {
        RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);

        InstanceState res = new InstanceState();

        TypedRecordIdentity<GatewayAssetRecord> ri_gateway = GatewayAssetRecord.findByInstanceId(helper_gateway, instanceId);
        if (ri_gateway == null)
        {
            helper_gateway.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

            res.createdGateway = true;
            res.rec_gateway    = new GatewayAssetRecord();
            res.rec_gateway.setInstanceId(instanceId);

            if (shouldNotifyNewGateway(instanceId))
            {
                res.rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.reportAsNew, true);
            }

            res.rec_network = createInstanceId(sessionHolder, instanceId, res.rec_gateway);
            if (res.rec_network != null)
            {
                res.createdNetwork = true;

                afterNetworkCreation(sessionHolder, res.rec_gateway, res.rec_network);
            }
            else
            {
                helper_gateway.persist(res.rec_gateway);
            }
        }
        else
        {
            res.rec_gateway = sessionHolder.fromIdentity(ri_gateway);
            res.rec_network = CollectionUtils.firstElement(res.rec_gateway.getBoundNetworks());
        }

        return res;
    }

    protected abstract boolean shouldNotifyNewGateway(String instanceId);

    protected abstract NetworkAssetRecord createInstanceId(SessionHolder sessionHolder,
                                                           String instanceId,
                                                           GatewayAssetRecord rec_gateway) throws
                                                                                           Exception;

    protected NetworkAssetRecord createInstanceIdImpl(SessionHolder sessionHolder,
                                                      String instanceId,
                                                      GatewayAssetRecord rec_gateway) throws
                                                                                      Exception
    {
        RecordHelper<GatewayAssetRecord> helper_gateway  = sessionHolder.createHelper(GatewayAssetRecord.class);
        RecordHelper<NetworkAssetRecord> helper_network  = sessionHolder.createHelper(NetworkAssetRecord.class);
        RecordHelper<LocationRecord>     helper_location = sessionHolder.createHelper(LocationRecord.class);

        helper_network.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

        helper_location.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

        final String prefix          = "[A-Za-z]+ #";
        int          gatewaySequence = 1;

        gatewaySequence = AbstractApplicationWithDatabase.findUniqueSequence(prefix, gatewaySequence, helper_gateway.listAll(), AssetRecord::getPhysicalName);
        gatewaySequence = AbstractApplicationWithDatabase.findUniqueSequence(prefix, gatewaySequence, helper_network.listAll(), AssetRecord::getPhysicalName);
        gatewaySequence = AbstractApplicationWithDatabase.findUniqueSequence(prefix, gatewaySequence, helper_location.listAll(), LocationRecord::getPhysicalName);

        if (!shouldAutoConfig())
        {
            rec_gateway.setPhysicalName(String.format("Gateway #%05d", gatewaySequence));
            return null;
        }

        LocationRecord rec_location = new LocationRecord();
        rec_location.setPhysicalName(String.format("Truck #%05d", gatewaySequence));
        rec_location.setType(LocationType.TRUCK);
        helper_location.persist(rec_location);

        //--//

        NetworkAssetRecord rec_network = new NetworkAssetRecord();
        rec_network.setPhysicalName(String.format("Network #%05d", gatewaySequence));
        rec_network.setLocation(rec_location);
        rec_network.setSamplingPeriod(1800);
        helper_network.persist(rec_network);

        //--//

        rec_gateway.setPhysicalName(String.format("Gateway #%05d", gatewaySequence));
        rec_gateway.setLocation(rec_location);
        rec_gateway.setWarningThreshold(900);
        rec_gateway.setAlertThreshold(1000);
        helper_gateway.persist(rec_gateway);

        rec_gateway.getBoundNetworks()
                   .add(rec_network);

        return rec_network;
    }

    protected abstract void afterNetworkCreation(SessionHolder sessionHolder,
                                                 GatewayAssetRecord rec_gateway,
                                                 NetworkAssetRecord rec_network) throws
                                                                                 Exception;

    //--//

    protected <T> T retryRecordCreation(SessionProvider sessionProvider,
                                        String context,
                                        Object id,
                                        FunctionWithException<SessionHolder, T> callback)
    {
        int retry = 0;

        while (true)
        {
            try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
            {
                return callback.apply(sessionHolder);
            }
            catch (Throwable t)
            {
                if (++retry >= 10)
                {
                    Throwable t2 = Exceptions.unwrapException(t);

                    DetailedApplicationException t3 = Reflection.as(t2, DetailedApplicationException.class);
                    if (t3 != null)
                    {
                        throw t3;
                    }

                    throw new WebApplicationException(t2.getMessage());
                }

                Executors.safeSleep(5 + Encryption.generateRandomValue32Bit(50));
            }
        }
    }

    //--//

    public static GatewayDiscoveryEntity newDiscoveryEntry(GatewayDiscoveryEntity parent,
                                                           GatewayDiscoveryEntitySelector key,
                                                           Object value) throws
                                                                         JsonProcessingException
    {
        return newDiscoveryEntry(parent, key, ObjectMappers.SkipNulls.writeValueAsString(value));
    }

    public static GatewayDiscoveryEntity newDiscoveryEntry(GatewayDiscoveryEntity parent,
                                                           GatewayDiscoveryEntitySelector key,
                                                           String value)
    {
        GatewayDiscoveryEntity en;

        if (parent == null)
        {
            en = GatewayDiscoveryEntity.create(key, value);
        }
        else
        {
            en = parent.createAsRequest(key, value);
        }

        return en;
    }

    public static void newDiscoverySample(GatewayDiscoveryEntity en_object,
                                          double timestampEpochSeconds,
                                          Object obj) throws
                                                      Exception
    {
        newDiscoverySample(en_object, GatewayDiscoveryEntitySelector.Ipn_ObjectSample, timestampEpochSeconds, obj);
    }

    public static void newDiscoverySample(GatewayDiscoveryEntity en_object,
                                          GatewayDiscoveryEntitySelector selector,
                                          double timestampEpochSeconds,
                                          Object obj) throws
                                                      Exception
    {
        GatewayDiscoveryEntity en_objectSample = newDiscoveryEntry(en_object, selector, null);
        en_objectSample.setTimestampEpoch(timestampEpochSeconds);
        en_objectSample.setContentsAsObject(ObjectMappers.SkipNulls, obj);
    }
}
