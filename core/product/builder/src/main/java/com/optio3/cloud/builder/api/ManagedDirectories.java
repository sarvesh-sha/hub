/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.builder.model.worker.ManagedDirectory;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "ManagedDirectories" }) // For Swagger
@Optio3RestEndpoint(name = "ManagedDirectories") // For Optio3 Shell
@Path("/v1/managed-directories")
@Optio3RequestLogLevel(Severity.Debug)
public class ManagedDirectories
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public TypedRecordIdentityList<ManagedDirectoryRecord> getContainers(@QueryParam("host") String host)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            HostRecord rec_host = host != null ? sessionHolder.getEntity(HostRecord.class, host) : null;

            return ManagedDirectoryRecord.list(sessionHolder.createHelper(ManagedDirectoryRecord.class), rec_host);
        }
    }

    @GET
    @Path("fetch/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ManagedDirectory getContainerByID(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, ManagedDirectoryRecord.class, id);
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<ManagedDirectory> getContainerBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<ManagedDirectoryRecord> helper = sessionHolder.createHelper(ManagedDirectoryRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, ManagedDirectoryRecord.getBatch(helper, ids));
        }
    }
}
