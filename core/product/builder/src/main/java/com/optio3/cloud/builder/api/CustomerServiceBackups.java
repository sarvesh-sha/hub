/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.io.InputStream;
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
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerServiceBackup;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "CustomerServiceBackups" }) // For Swagger
@Optio3RestEndpoint(name = "CustomerServiceBackups") // For Optio3 Shell
@Path("/v1/customer-service-backups")
public class CustomerServiceBackups
{
    @Inject
    private BuilderConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<CustomerServiceBackup> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<CustomerServiceBackupRecord> helper = sessionHolder.createHelper(CustomerServiceBackupRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, CustomerServiceBackupRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public CustomerServiceBackup get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, CustomerServiceBackupRecord.class, id);
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun) throws
                                                                          Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<CustomerServiceBackupRecord> helper = validation.sessionHolder.createHelper(CustomerServiceBackupRecord.class);

            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            CustomerServiceBackupRecord rec = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
        }
    }

    @GET
    @Path("item/{id}/stream/{fileName}")
    @Produces("application/gzip")
    @Optio3RequestLogLevel(Severity.Debug)
    public InputStream stream(@PathParam("id") String id,
                              @PathParam("fileName") String fileName) throws
                                                                      Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            CustomerServiceBackupRecord rec = sessionHolder.getEntity(CustomerServiceBackupRecord.class, id);

            return rec.streamFileFromCloud(m_cfg.credentials);
        }
    }
}
