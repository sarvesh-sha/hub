/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.builder.model.customer.Customer;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularChargePerHost;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCharges;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularChargesSummary;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord_;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
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
import com.optio3.util.BoxingUtils;
import io.swagger.annotations.Api;
import org.apache.commons.lang3.StringUtils;

@Api(tags = { "Customers" }) // For Swagger
@Optio3RestEndpoint(name = "Customers") // For Optio3 Shell
@Path("/v1/customers")
public class Customers
{
    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @GET
    @Path("all")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<CustomerRecord> getAll()
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<CustomerRecord> helper = sessionHolder.createHelper(CustomerRecord.class);

            return CustomerRecord.list(helper);
        }
    }

    //--//

    @GET
    @Path("charges")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentCellularChargesSummary getAllCharges(@QueryParam("maxTopHosts") Integer maxTopHosts)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentGlobalDescriptor globalDescriptor = fetchDeploymentGlobalDescriptor(sessionHolder);

            Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map = Maps.newHashMap();

            for (DeploymentHost host : globalDescriptor.hosts.values())
            {
                DeploymentCellularCharges res = DeploymentHostRecord.WellKnownMetadata.cellularCharges.get(host.decodeMetadata());
                if (res != null)
                {
                    map.put(RecordIdentity.newTypedInstance(DeploymentHostRecord.class, host.sysId), res);
                }
            }

            DeploymentCellularChargesSummary res = new DeploymentCellularChargesSummary();
            res.compute(map, BoxingUtils.get(maxTopHosts, 100));

            return res;
        }
    }

    @GET
    @Path("charges-report/{fileName}")
    @Produces("application/csv")
    @Optio3RequestLogLevel(Severity.Debug)
    public String getAllChargesReport(@PathParam("fileName") String fileName) throws
                                                                              IOException
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentGlobalDescriptor globalDescriptor = fetchDeploymentGlobalDescriptor(sessionHolder);

            Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map = Maps.newHashMap();

            for (DeploymentHost host : globalDescriptor.hosts.values())
            {
                DeploymentCellularCharges res = DeploymentHostRecord.WellKnownMetadata.cellularCharges.get(host.decodeMetadata());
                if (res != null)
                {
                    map.put(RecordIdentity.newTypedInstance(DeploymentHostRecord.class, host.sysId), res);
                }
            }

            return DeploymentCellularChargePerHost.report(sessionHolder, map);
        }
    }

    //--//

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<Customer> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<CustomerRecord> helper = sessionHolder.createHelper(CustomerRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, CustomerRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(WellKnownRoleIds.Administrator)
    public Customer create(Customer model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<CustomerRecord> helper = sessionHolder.createHelper(CustomerRecord.class);

            CustomerRecord rec_customer = new CustomerRecord();

            if (StringUtils.isBlank(model.name))
            {
                throw new InvalidArgumentException("Customer name required");
            }

            long count = QueryHelperWithCommonFields.count(helper, (qh) ->
            {
                qh.addWhereClauseWithEqual(qh.root, CustomerRecord_.name, model.name);
            });

            if (count != 0)
            {
                throw new InvalidArgumentException("Similar customer already exists");
            }

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_customer);

            String cloudId = rec_customer.getCloudId();
            if (CustomerRecord.findByCloudId(helper, cloudId) != null)
            {
                throw new InvalidArgumentException("Similar customer already exists");
            }

            helper.persist(rec_customer);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_customer);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public Customer get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, CustomerRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    Customer model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<CustomerRecord> helper = validation.sessionHolder.createHelper(CustomerRecord.class);
            CustomerRecord               rec    = helper.get(id);

            if (!StringUtils.equals(rec.getName(), model.name))
            {
                for (CustomerServiceRecord rec_svc : rec.getServices())
                {
                    if (rec_svc.hasBackups())
                    {
                        validation.addFailure("name", "Can't change the name of a customer with backups");
                    }
                }

                long count = QueryHelperWithCommonFields.count(helper, (qh) ->
                {
                    qh.addWhereClauseWithEqual(qh.root, CustomerRecord_.name, model.name);
                });

                if (count != 0)
                {
                    validation.addFailure("name", "Customer named '%s' already exists!", model.name);
                }
            }

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec);
            }

            return validation.getResults();
        }
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
            validation.checkRole(m_principalAccessor, WellKnownRoleIds.Administrator);

            RecordHelper<CustomerRecord> helper = validation.sessionHolder.createHelper(CustomerRecord.class);
            CustomerRecord               rec    = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
        }
    }

    //--//

    @GET
    @Path("item/{id}/charges")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DeploymentCellularChargesSummary getCharges(@PathParam("id") String id,
                                                       @QueryParam("maxTopHosts") Integer maxTopHosts)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentGlobalDescriptor globalDescriptor = fetchDeploymentGlobalDescriptor(sessionHolder);

            CustomerRecord rec = sessionHolder.getEntity(CustomerRecord.class, id);

            Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map = Maps.newHashMap();
            rec.collectCharges(globalDescriptor, map);

            DeploymentCellularChargesSummary res = new DeploymentCellularChargesSummary();
            res.compute(map, BoxingUtils.get(maxTopHosts, 100));

            return res;
        }
    }

    @GET
    @Path("item/{id}/charges-report/{fileName}")
    @Produces("application/csv")
    @Optio3RequestLogLevel(Severity.Debug)
    public String getChargesReport(@PathParam("id") String id,
                                   @PathParam("fileName") String fileName) throws
                                                                           IOException
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            DeploymentGlobalDescriptor globalDescriptor = fetchDeploymentGlobalDescriptor(sessionHolder);

            CustomerRecord rec = sessionHolder.getEntity(CustomerRecord.class, id);

            Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map = Maps.newHashMap();
            rec.collectCharges(globalDescriptor, map);

            return DeploymentCellularChargePerHost.report(sessionHolder, map);
        }
    }

    //--//

    private DeploymentGlobalDescriptor fetchDeploymentGlobalDescriptor(SessionHolder sessionHolder)
    {
        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadDeployments = true;
        settings.loadServices    = true;
        return DeploymentGlobalDescriptor.get(sessionHolder, settings);
    }
}
