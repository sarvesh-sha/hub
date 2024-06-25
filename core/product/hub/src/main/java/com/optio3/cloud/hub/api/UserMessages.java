/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;

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
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.model.message.UserMessage;
import com.optio3.cloud.hub.model.message.UserMessageFilterRequest;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageRecord;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "UserMessages" }) // For Swagger
@Optio3RestEndpoint(name = "UserMessages") // For Optio3 Shell
@Path("/v1/user-messages")
public class UserMessages
{
    @Inject
    private HubConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<RecordIdentity> getFiltered(UserMessageFilterRequest filters) throws
                                                                              Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            UserRecord rec_user = getCurrentUser(sessionHolder);

            return UserMessageRecord.filter(sessionHolder.createHelper(UserMessageRecord.class), rec_user, filters);
        }
    }

    @POST
    @Path("count")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public long getFilteredCount(UserMessageFilterRequest filters) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            UserRecord rec_user = getCurrentUser(sessionHolder);

            return UserMessageRecord.count(sessionHolder.createHelper(UserMessageRecord.class), rec_user, filters);
        }
    }

    @POST
    @Path("batch")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<UserMessage> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<UserMessageRecord> helper = sessionHolder.createHelper(UserMessageRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, UserMessageRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{msgId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public UserMessage get(@PathParam("msgId") String msgId)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, UserMessageRecord.class, msgId);
    }

    @DELETE
    @Path("item/{msgId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("msgId") String msgId,
                                    @QueryParam("dryRun") Boolean dryRun)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<UserMessageRecord> helper  = validation.sessionHolder.createHelper(UserMessageRecord.class);
            UserMessageRecord               rec_msg = helper.getOrNull(msgId);
            if (rec_msg != null)
            {
                if (validation.canProceed())
                {
                    helper.delete(rec_msg);
                }
            }

            return validation.getResults();
        }
    }

    @POST
    @Path("item/{msgId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("msgId") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    UserMessage model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            UserMessageRecord rec = validation.sessionHolder.getEntity(UserMessageRecord.class, id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec);
            }

            return validation.getResults();
        }
    }

    //--//

    private UserRecord getCurrentUser(SessionHolder sessionHolder) throws
                                                                   Exception
    {
        return m_cfg.userLogic.getUserWithAuthentication(sessionHolder, null, m_principalAccessor, null);
    }
}
