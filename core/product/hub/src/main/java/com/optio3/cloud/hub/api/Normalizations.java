/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationMatchHistory;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.logic.normalizations.NormalizationState;
import com.optio3.cloud.hub.model.asset.DeviceElementFilterRequest;
import com.optio3.cloud.hub.model.normalization.ClassificationPointInput;
import com.optio3.cloud.hub.model.normalization.ClassificationPointOutput;
import com.optio3.cloud.hub.model.normalization.ClassificationPointOutputDetails;
import com.optio3.cloud.hub.model.normalization.DeviceElementClassificationOverridesRequest;
import com.optio3.cloud.hub.model.normalization.DeviceElementNormalizationProgress;
import com.optio3.cloud.hub.model.normalization.DeviceElementNormalizationRun;
import com.optio3.cloud.hub.model.normalization.DeviceElementNormalizationSample;
import com.optio3.cloud.hub.model.normalization.DeviceNormalizationExport;
import com.optio3.cloud.hub.model.normalization.Normalization;
import com.optio3.cloud.hub.model.normalization.NormalizationEquipmentLocation;
import com.optio3.cloud.hub.model.normalization.NormalizationEvaluation;
import com.optio3.cloud.hub.model.workflow.WorkflowOverrides;
import com.optio3.cloud.hub.orchestration.tasks.TaskForDeviceElementNormalization;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.logging.Severity;
import com.optio3.metadata.normalization.BACnetImportExportData;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "Normalizations" }) // For Swagger
@Optio3RestEndpoint(name = "Normalizations") // For Optio3 Shell
@Path("/v1/normalizations")
public class Normalizations
{
    @Inject
    private HubApplication m_app;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<NormalizationRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<NormalizationRecord> helper = sessionHolder.createHelper(NormalizationRecord.class);

            return NormalizationRecord.list(helper);
        }
    }

    @POST
    @Path("batch")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<Normalization> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<NormalizationRecord> helper = sessionHolder.createHelper(NormalizationRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, NormalizationRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public NormalizationRules parseImport(RawImport rawImport)
    {
        NormalizationRules rules = rawImport.validate(NormalizationRules.class);
        if (rules != null)
        {
            try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
            {
                rules.cleanUp(sessionHolder);
            }
        }

        return rules;
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Normalization create(Normalization model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            if (model.rules != null)
            {
                model.rules.cleanUp(sessionHolder);
            }

            RecordHelper<NormalizationRecord> helper = sessionHolder.createHelper(NormalizationRecord.class);

            NormalizationRecord rec = NormalizationRecord.newInstance(helper, model.rules, null);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/activate")
    @Produces(MediaType.APPLICATION_JSON)
    public Normalization makeActive(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<NormalizationRecord> helper = sessionHolder.createHelper(NormalizationRecord.class);

            NormalizationRecord rec = helper.get(id);

            rec.makeActive(helper);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public Normalization get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, NormalizationRecord.class, id);
    }

    @GET
    @Path("item/active")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentity<NormalizationRecord> getActive()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<NormalizationRecord> helper = sessionHolder.createHelper(NormalizationRecord.class);
            NormalizationRecord               rec    = NormalizationRecord.findActive(helper);

            if (rec != null)
            {
                return RecordIdentity.newTypedInstance(rec);
            }

            return null;
        }
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator);

            RecordHelper<NormalizationRecord> helper = validation.sessionHolder.createHelper(NormalizationRecord.class);
            NormalizationRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                if (rec.isActive())
                {
                    validation.addFailure("active", "Active version '%d' cannot be deleted", rec.getVersion());
                }

                if (validation.canProceed())
                {
                    helper.delete(rec);
                }
            }

            return validation.getResults();
        }
    }

    @POST
    @Path("normalization/override/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DeviceElementClassificationOverridesRequest> parseOverridesImport(RawImport rawImport)
    {
        try
        {
            if (rawImport.contentsAsJSON != null)
            {
                return ObjectMappers.SkipNulls.readValue(rawImport.contentsAsJSON, new TypeReference<List<DeviceElementClassificationOverridesRequest>>()
                {
                });
            }

            return null;
        }
        catch (Throwable t)
        {
            throw new InvalidArgumentException("Invalid import format");
        }
    }

    @GET
    @Path("export/device/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceNormalizationExport exportNormalization(@PathParam("id") String sysId) throws
                                                                                        Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeviceRecord              rec_target = sessionHolder.getEntity(DeviceRecord.class, sysId);
            DeviceNormalizationExport input      = new DeviceNormalizationExport();

            BACnetDeviceRecord rec_device_bacnet = Reflection.as(rec_target, BACnetDeviceRecord.class);
            if (rec_device_bacnet != null)
            {
                RecordHelper<DeviceElementRecord> helper = sessionHolder.createHelper(DeviceElementRecord.class);

                LocationsEngine          locationsEngine   = sessionHolder.getServiceNonNull(LocationsEngine.class);
                LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(false);

                DeviceElementRecord rec_object_device = rec_device_bacnet.findDeviceObject(helper);
                if (rec_object_device != null)
                {
                    input.deviceData = rec_device_bacnet.extractImportExportData(locationsSnapshot, rec_object_device);
                }

                final DeviceElementFilterRequest filters = DeviceElementFilterRequest.createFilterForParent(rec_target);
                DeviceElementRecord.enumerateNoNesting(helper, filters, (rec_object) ->
                {
                    input.objects.add(rec_device_bacnet.extractImportExportData(locationsSnapshot, rec_object));
                    return StreamHelperNextAction.Continue_Evict;
                });
            }

            return input;
        }
    }

    //--//

    @POST
    @Path("normalization/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClassificationPointOutput testNormalization(DeviceElementNormalizationSample sample) throws
                                                                                                Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            if (!sample.ensureRules(sessionHolder.createHelper(NormalizationRecord.class)))
            {
                return null;
            }
            NormalizationEngine      engine  = new NormalizationEngine(m_sessionProvider, sample.rules, true);
            ClassificationPointInput input   = getInput(sample);
            NormalizationState       stateIn = NormalizationState.fromClassificationInput(input);

            NormalizationState stateOut   = engine.normalizeWithHistory(stateIn, true);
            String             outputName = stateOut.controlPointName;

            if (StringUtils.isNotEmpty(stateIn.controlPointWorkflowOverrideName))
            {
                outputName = stateIn.controlPointWorkflowOverrideName;
                stateOut.history.add(new NormalizationMatchHistory(null, stateOut.controlPointName, outputName));
            }

            ClassificationPointOutput res = input.asResult();
            res.normalizationHistory = stateOut.history;
            res.normalizedName       = outputName;
            res.locations            = CollectionUtils.transformToList(stateOut.locations, NormalizationEquipmentLocation::fromEngineLocation);

            TaskForDeviceElementNormalization.extractEquipmentFromNormalizationOutput(res.equipments,
                                                                                      res.equipmentRelationships,
                                                                                      input.networkSysId,
                                                                                      input.equipmentOverrides,
                                                                                      null,
                                                                                      stateOut,
                                                                                      engine,
                                                                                      new WorkflowOverrides());

            return res;
        }
    }

    @POST
    @Path("normalization/start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String startNormalization(DeviceElementNormalizationRun run,
                                     @QueryParam("dryRun") Boolean dryRun) throws
                                                                           Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
        {
            if (!run.ensureRules(sessionHolder.createHelper(NormalizationRecord.class)))
            {
                return null;
            }

            RecordHelper<DeviceRecord>        helper   = sessionHolder.createHelper(DeviceRecord.class);
            List<RecordLocator<DeviceRecord>> locators = NormalizationRecord.extractDevices(helper, run);

            return TaskForDeviceElementNormalization.scheduleTask(sessionHolder, locators, run.rules, null, BoxingUtils.get(dryRun));
        });

        return loc_task.getIdRaw();
    }

    @GET
    @Path("normalization/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceElementNormalizationProgress checkNormalization(@PathParam("id") String id,
                                                                 @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForDeviceElementNormalization.class);
        }
    }

    //--//

    @GET
    @Path("classification/sample/{sysId}")
    @Produces(MediaType.APPLICATION_JSON)
    public DeviceElementNormalizationSample loadSample(@PathParam("sysId") String sysId) throws
                                                                                         Exception
    {
        try (SessionHolder holder = m_sessionProvider.newReadOnlySession())
        {
            DeviceElementNormalizationSample sample     = new DeviceElementNormalizationSample();
            DeviceElementRecord              rec_object = holder.getEntity(DeviceElementRecord.class, sysId);
            BACnetDeviceRecord               rec_device = rec_object.getParentAssetOrNull(BACnetDeviceRecord.class);

            MetadataMap metadata = rec_object.getMetadata();

            WorkflowOverrides workflowOverrides = WorkflowOverrides.load(holder);

            DeviceElementRecord    rec_device_object = rec_device.findDeviceObject(holder.createHelper(DeviceElementRecord.class));
            BACnetImportExportData device_info       = rec_device.extractImportExportData(null, rec_device_object);
            BACnetImportExportData item              = rec_device.extractImportExportData(null, rec_object);

            sample.details.objectIdentifier           = item.objectId.toJsonValue();
            sample.details.objectName                 = item.deviceName;
            sample.details.objectWorkflowOverrideName = workflowOverrides.pointNames.get(sysId);
            sample.details.objectDescription          = item.deviceDescription;
            sample.details.objectBackupName           = AssetRecord.WellKnownMetadata.nameFromLegacyImport.get(metadata);
            sample.details.objectBackupEquipmentName  = AssetRecord.WellKnownMetadata.equipmentNameFromLegacyImport.get(metadata);
            sample.details.objectUnits                = Objects.toString(item.units);
            sample.details.objectType                 = Objects.toString(item.objectId.object_type);
            sample.details.objectBackupStructure      = item.dashboardStructure;
            sample.details.objectLocation             = item.deviceLocation;

            if (device_info != null)
            {
                sample.details.controllerIdentifier       = device_info.objectId.toJsonValue();
                sample.details.controllerName             = device_info.deviceName;
                sample.details.controllerBackupName       = device_info.dashboardName;
                sample.details.controllerDescription      = device_info.deviceDescription;
                sample.details.controllerLocation         = device_info.deviceLocation;
                sample.details.controllerVendorName       = device_info.deviceVendor;
                sample.details.controllerModelName        = device_info.deviceModel;
                sample.details.controllerTransportAddress = device_info.transport;
            }

            return sample;
        }
    }

    @POST
    @Path("classification/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ClassificationPointOutput testClassification(DeviceElementNormalizationSample sample) throws
                                                                                                 Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            if (!sample.ensureRules(sessionHolder.createHelper(NormalizationRecord.class)))
            {
                return null;
            }

            NormalizationEngine       engine  = new NormalizationEngine(m_sessionProvider, sample.rules, true);
            ClassificationPointInput  input   = getInput(sample);
            ClassificationPointOutput res     = input.asResult();
            NormalizationState        stateIn = NormalizationState.fromClassificationInput(input);

            NormalizationState stateOut = engine.normalizeWithHistory(stateIn, true);

            String outputName = stateIn.controlPointWorkflowOverrideName;
            if (StringUtils.isEmpty(outputName))
            {
                outputName = stateOut.controlPointName;
            }

            res.normalizedName = outputName;
            res.currentResult  = new ClassificationPointOutputDetails();

            String pointClassId = null;

            TaskForDeviceElementNormalization.extractEquipmentFromNormalizationOutput(res.equipments,
                                                                                      res.equipmentRelationships,
                                                                                      input.networkSysId,
                                                                                      input.equipmentOverrides,
                                                                                      null,
                                                                                      stateOut,
                                                                                      engine,
                                                                                      new WorkflowOverrides());

            if (StringUtils.isNotBlank(stateOut.pointClassId) || stateOut.setUnclassified)
            {
                pointClassId = stateOut.setUnclassified ? null : stateOut.pointClassId;

                res.currentResult.id            = pointClassId;
                res.currentResult.positiveScore = stateOut.positiveScore;
                res.currentResult.negativeScore = stateOut.negativeScore;
                res.currentResult.reason        = stateOut.classificationReason;
            }

            res.normalizationTags = TaskForDeviceElementNormalization.getTags(engine, stateOut.tagsSet, stateOut.tags, pointClassId, outputName);

            return res;
        }
    }

    //--//

    @POST
    @Path("normalization/eval")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public NormalizationEvaluation evaluate(DeviceElementNormalizationSample sample,
                                            @QueryParam("maxSteps") Integer maxSteps,
                                            @QueryParam("trace") Boolean trace) throws
                                                                                Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            if (!sample.ensureRules(sessionHolder.createHelper(NormalizationRecord.class)))
            {
                return null;
            }

            maxSteps = BoxingUtils.get(maxSteps, 1000);

            ClassificationPointInput input = getInput(sample);

            NormalizationState stateIn = NormalizationState.fromClassificationInput(input);

            boolean shouldTrace = BoxingUtils.get(trace);

            NormalizationEngine engine = new NormalizationEngine(m_sessionProvider, sample.rules, true);
            engine.traceExecution = shouldTrace;
            engine.maxSteps       = maxSteps;
            engine.logScripts     = true;

            engine.normalizeWithHistory(stateIn, true);

            NormalizationEngineExecutionContext context = engine.getLogic();

            NormalizationEvaluation evaluation = new NormalizationEvaluation();
            evaluation.steps      = context.steps;
            evaluation.logEntries = context.logEntries;

            return evaluation;
        }
    }

    private ClassificationPointInput getInput(DeviceElementNormalizationSample sample)
    {
        ClassificationPointInput input = new ClassificationPointInput();
        input.objectUnits = EngineeringUnits.parse(sample.details.objectUnits);

        BACnetObjectType type = BACnetObjectType.parse(sample.details.objectType);
        if (type == null)
        {
            type = BACnetObjectType.analog_input;
        }
        input.objectType = type;

        input.details.copy(sample.details);

        return input;
    }
}
