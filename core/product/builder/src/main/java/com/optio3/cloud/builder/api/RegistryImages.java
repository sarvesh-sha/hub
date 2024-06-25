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
import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryRefresh;
import com.optio3.cloud.builder.orchestration.tasks.bookkeeping.TaskForRegistryRefresh;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordForBackgroundActivity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import io.swagger.annotations.Api;

@Api(tags = { "RegistryImages" }) // For Swagger
@Optio3RestEndpoint(name = "RegistryImages") // For Optio3 Shell
@Path("/v1/registry-images")
@Optio3RequestLogLevel(Severity.Debug)
public class RegistryImages
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    public TypedRecordIdentityList<RegistryImageRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RegistryImageRecord> helper = sessionHolder.createHelper(RegistryImageRecord.class);

            return RegistryImageRecord.list(helper);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<RegistryImage> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RegistryImageRecord> helper = sessionHolder.createHelper(RegistryImageRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, RegistryImageRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public RegistryImage get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, RegistryImageRecord.class, id);
    }

    //--//

    @GET
    @Path("refresh/start")
    @Produces(MediaType.TEXT_PLAIN)
    public String startRefresh() throws
                                 Exception
    {
        RecordLocator<BackgroundActivityRecord> loc_task = BackgroundActivityRecord.wrapTask(m_sessionProvider, TaskForRegistryRefresh::scheduleTask);

        return loc_task.getIdRaw();
    }

    @GET
    @Path("refresh/check/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public RegistryRefresh checkRefresh(@PathParam("id") String id,
                                        @QueryParam("detailed") Boolean detailed)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<BackgroundActivityRecord> helper = sessionHolder.createHelper(BackgroundActivityRecord.class);

            return RecordForBackgroundActivity.getProgress(helper, id, BoxingUtils.get(detailed), TaskForRegistryRefresh.class);
        }
    }

    //--//

    @GET
    @Path("find/{imageSha}")
    @Produces(MediaType.APPLICATION_JSON)
    public RegistryImage findBySha(@PathParam("imageSha") String imageSha)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<RegistryImageRecord> helper = sessionHolder.createHelper(RegistryImageRecord.class);
            RegistryImageRecord               rec    = RegistryImageRecord.findBySha(helper, imageSha);

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }
}
