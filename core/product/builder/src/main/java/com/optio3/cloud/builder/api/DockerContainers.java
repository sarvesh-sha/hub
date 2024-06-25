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
import com.optio3.cloud.builder.model.worker.DockerContainer;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "DockerContainers" }) // For Swagger
@Optio3RestEndpoint(name = "DockerContainers") // For Optio3 Shell
@Path("/v1/docker-containers")
@Optio3RequestLogLevel(Severity.Debug)
public class DockerContainers
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public TypedRecordIdentityList<DockerContainerRecord> getContainers(@QueryParam("host") String host)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            HostRecord rec_host = host != null ? sessionHolder.getEntityOrNull(HostRecord.class, host) : null;

            return DockerContainerRecord.list(sessionHolder.createHelper(DockerContainerRecord.class), rec_host);
        }
    }

    @GET
    @Path("fetch/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public DockerContainer getContainerByID(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, DockerContainerRecord.class, id);
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<DockerContainer> getContainerBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DockerContainerRecord> helper = sessionHolder.createHelper(DockerContainerRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, DockerContainerRecord.getBatch(helper, ids));
        }
    }
}
