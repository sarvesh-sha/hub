/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;

import javax.annotation.security.RolesAllowed;
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

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.dataImports.DataImportProgress;
import com.optio3.cloud.hub.model.dataImports.DataImportRun;
import com.optio3.cloud.hub.model.normalization.ImportedMetadata;
import com.optio3.cloud.hub.orchestration.tasks.TaskForDataImport;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.normalization.ImportedMetadataRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.metadata.normalization.ImportExportData;
import com.optio3.util.BoxingUtils;
import io.swagger.annotations.Api;

@Api(tags = { "DataImports" }) // For Swagger
@Optio3RestEndpoint(name = "DataImports") // For Optio3 Shell
@Path("/v1/data-imports")
public class DataImports
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
    public TypedRecordIdentityList<ImportedMetadataRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<ImportedMetadataRecord> helper = sessionHolder.createHelper(ImportedMetadataRecord.class);

            return ImportedMetadataRecord.list(helper);
        }
    }

    @POST
    @Path("batch")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<ImportedMetadata> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<ImportedMetadataRecord> helper = sessionHolder.createHelper(ImportedMetadataRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, ImportedMetadataRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ImportedMetadata parseImport(RawImport rawImport)
    {
        if (rawImport.contentsAsJSON != null)
        {
            ImportedMetadata res = new ImportedMetadata();
            res.metadata = rawImport.validate(ImportExportData.c_typeRef);
            return res;
        }

        return null;
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public ImportedMetadata create(ImportedMetadata model) throws
                                                           Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            ImportedMetadataRecord rec = ImportedMetadataRecord.newInstance(sessionHolder.createHelper(ImportedMetadataRecord.class), model.metadata, null);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/activate")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public ImportedMetadata makeActive(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<ImportedMetadataRecord> helper = sessionHolder.createHelper(ImportedMetadataRecord.class);
            ImportedMetadataRecord               rec    = helper.get(id);

            rec.makeActive(helper);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public ImportedMetadata get(@PathParam("id") String id,
                                @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            ImportedMetadataRecord rec = sessionHolder.getEntity(ImportedMetadataRecord.class, id);

            ImportedMetadata res = ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
            if (res != null && BoxingUtils.get(detailed))
            {
                res.metadata = rec.getMetadata();
            }
            return res;
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

            RecordHelper<ImportedMetadataRecord> helper = validation.sessionHolder.createHelper(ImportedMetadataRecord.class);
            ImportedMetadataRecord               rec    = helper.getOrNull(id);
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

    //--//

    @POST
    @Path("start")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ WellKnownRoleIds.Maintenance, WellKnownRoleIds.Administrator })
    public String startImport(DataImportRun run) throws
                                                 Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, (sessionHolder) ->
        {
            ImportedMetadataRecord rec = sessionHolder.getEntityOrNull(ImportedMetadataRecord.class, run.dataImportsId);
            if (rec == null)
            {
                return null;
            }

            List<RecordLocator<DeviceRecord>> locators = ImportedMetadataRecord.extractDevices(sessionHolder.createHelper(DeviceRecord.class), run);

            return TaskForDataImport.scheduleTask(sessionHolder, rec, locators);
        });

        return loc_task.getIdRaw();
    }

    @GET
    @Path("check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DataImportProgress checkImport(@PathParam("id") String id,
                                          @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForDataImport.class);
        }
    }
}
