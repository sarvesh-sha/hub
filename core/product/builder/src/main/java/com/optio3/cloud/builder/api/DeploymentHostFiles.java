/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.annotation.security.RolesAllowed;
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
import com.optio3.cloud.builder.model.deployment.DeploymentHostFile;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFileContents;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostFileRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "DeploymentHostFiles" }) // For Swagger
@Optio3RestEndpoint(name = "DeploymentHostFiles") // For Optio3 Shell
@Path("/v1/deployment-host-files")
public class DeploymentHostFiles
{
    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<DeploymentHostFileRecord> getAll(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostRecord rec = sessionHolder.getEntity(DeploymentHostRecord.class, id);

            return TypedRecordIdentityList.toList(rec.getFiles());
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DeploymentHostFile> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DeploymentHostFileRecord> helper = sessionHolder.createHelper(DeploymentHostFileRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, QueryHelperWithCommonFields.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create/{hostId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DeploymentHostFile create(@PathParam("hostId") String hostId,
                                     DeploymentHostFile model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, hostId);

            for (DeploymentHostFileRecord file : rec_host.getFiles())
            {
                if (StringUtils.equals(file.getPath(), model.path))
                {
                    return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, file);
                }
            }

            DeploymentHostFileRecord rec_file = DeploymentHostFileRecord.newInstance(rec_host, model.path, sessionHolder.fromIdentityOrNull(model.task));
            sessionHolder.persistEntity(rec_file);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_file);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentHostFile get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, DeploymentHostFileRecord.class, id);
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<DeploymentHostFileRecord> helper = validation.sessionHolder.createHelper(DeploymentHostFileRecord.class);

            DeploymentHostFileRecord rec = helper.getOrNull(id);
            if (rec != null)
            {
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
    @Path("item/{id}/contents-set")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentHostFile setContents(@PathParam("id") String id,
                                          DeploymentHostFileContents input) throws
                                                                            IOException
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostFileRecord rec = sessionHolder.getEntity(DeploymentHostFileRecord.class, id);

            rec.setUploadedOn(null);
            rec.setDownloadedOn(null);
            rec.deleteContents(sessionHolder);

            sessionHolder.commit();

            try (OutputStream stream = DeploymentHostFileRecord.writeAsStream(m_sessionProvider, sessionHolder.createLocator(rec)))
            {
                if (input.text != null)
                {
                    stream.write(input.text.getBytes());
                }
                else
                {
                    stream.write(input.binary);
                }
            }

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/contents-get-as-text")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentHostFileContents getAsText(@PathParam("id") String id) throws
                                                                            IOException
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostFileRecord rec = sessionHolder.getEntity(DeploymentHostFileRecord.class, id);

            DeploymentHostFileContents res = new DeploymentHostFileContents();

            res.text = new String(IOUtils.toByteArray(rec.readAsStream(m_sessionProvider)));
            return res;
        }
    }

    @GET
    @Path("item/{id}/contents-get-as-binary")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentHostFileContents getAsBinary(@PathParam("id") String id) throws
                                                                              IOException
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostFileRecord rec = sessionHolder.getEntity(DeploymentHostFileRecord.class, id);

            DeploymentHostFileContents res = new DeploymentHostFileContents();

            res.binary = IOUtils.toByteArray(rec.readAsStream(m_sessionProvider));
            return res;
        }
    }

    //--//

    @POST
    @Path("item/{id}/stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentHostFile setStream(@PathParam("id") String id,
                                        InputStream stream) throws
                                                            IOException
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostFileRecord rec = sessionHolder.getEntity(DeploymentHostFileRecord.class, id);

            rec.setUploadedOn(null);
            rec.setDownloadedOn(null);
            rec.deleteContents(sessionHolder);

            sessionHolder.commit();

            try (OutputStream streamOut = DeploymentHostFileRecord.writeAsStream(m_sessionProvider, sessionHolder.createLocator(rec)))
            {
                IOUtils.copyLarge(stream, streamOut);
            }

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/stream/{fileName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Optio3RequestLogLevel(Severity.Debug)
    public Object getStream(@PathParam("id") String id,
                            @PathParam("fileName") String fileName)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentHostFileRecord rec = sessionHolder.getEntity(DeploymentHostFileRecord.class, id);

            return rec.readAsStream(m_sessionProvider);
        }
    }

    //--//

    @GET
    @Path("item/{id}/start-download")
    @Produces(MediaType.APPLICATION_JSON)
    public DeploymentHostFile startDownload(@PathParam("id") String id) throws
                                                                        Exception
    {
        return startTransfer(id, false);
    }

    @GET
    @Path("item/{id}/start-upload")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Administrator })
    public DeploymentHostFile startUpload(@PathParam("id") String id) throws
                                                                      Exception
    {
        return startTransfer(id, true);
    }

    private DeploymentHostFile startTransfer(String id,
                                             boolean upload) throws
                                                             Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeploymentHostFileRecord rec = sessionHolder.getEntityOrNull(DeploymentHostFileRecord.class, id);
            if (rec == null)
            {
                return null;
            }

            rec.start(sessionHolder, upload);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }
}
