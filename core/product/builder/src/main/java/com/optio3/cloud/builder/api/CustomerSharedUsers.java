/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

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
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerSharedUser;
import com.optio3.cloud.builder.model.identity.UserCreationRequest;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerSharedUserRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "CustomerSharedUsers" }) // For Swagger
@Optio3RestEndpoint(name = "CustomerSharedUsers") // For Optio3 Shell
@Path("/v1/customer-shared-users")
public class CustomerSharedUsers
{
    @Inject
    private BuilderConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @GET
    @Path("all/{customerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<CustomerSharedUserRecord> getAll(@PathParam("customerId") String customerId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            CustomerRecord rec_customer = sessionHolder.getEntity(CustomerRecord.class, customerId);

            return TypedRecordIdentityList.toList(rec_customer.getSharedUsers());
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<CustomerSharedUser> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<CustomerSharedUserRecord> helper = sessionHolder.createHelper(CustomerSharedUserRecord.class);

            ModelMapperPolicy policy = selectPolicy(sessionHolder);

            return ModelMapper.toModels(sessionHolder, policy, CustomerSharedUserRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create/{customerId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public CustomerSharedUser create(@PathParam("customerId") String customerId,
                                     UserCreationRequest model) throws
                                                                Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            CustomerRecord rec_customer = sessionHolder.getEntity(CustomerRecord.class, customerId);

            CustomerSharedUserRecord rec_user = CustomerSharedUserRecord.newInstance(m_cfg, rec_customer, model);
            sessionHolder.persistEntity(rec_user);

            sessionHolder.commit();

            ModelMapperPolicy policy = selectPolicy(sessionHolder);
            return ModelMapper.toModel(sessionHolder, policy, rec_user);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public CustomerSharedUser get(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            CustomerSharedUserRecord rec = sessionHolder.getEntity(CustomerSharedUserRecord.class, id);

            ModelMapperPolicy policy = selectPolicy(sessionHolder);
            return ModelMapper.toModel(sessionHolder, policy, rec);
        }
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    CustomerSharedUser model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            CustomerSharedUserRecord rec = validation.sessionHolder.getEntity(CustomerSharedUserRecord.class, id);

            ModelMapperPolicy policy = selectPolicy(validation.sessionHolder);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, policy, model, rec);
            }

            return validation.getResults();
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
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<CustomerSharedUserRecord> helper = validation.sessionHolder.createHelper(CustomerSharedUserRecord.class);
            CustomerSharedUserRecord               rec    = helper.getOrNull(id);
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

    private ModelMapperPolicy selectPolicy(SessionHolder sessionHolder)
    {
        return m_cfg.getPolicyWithDecryptionForAdministratorUser(sessionHolder, m_principalAccessor);
    }
}
