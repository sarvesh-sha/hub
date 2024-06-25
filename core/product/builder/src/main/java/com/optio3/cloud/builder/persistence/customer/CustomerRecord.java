/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.customer;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.customer.Customer;
import com.optio3.cloud.builder.model.deployment.DeploymentCellularCharges;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.search.Optio3QueryAnalyzerOverride;
import com.optio3.infra.AwsHelper;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NaturalId;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "CUSTOMER")
@Indexed
@Analyzer(definition = "fuzzy")
@Optio3QueryAnalyzerOverride("fuzzy_query")
@Optio3TableInfo(externalId = "Customer", model = Customer.class, metamodel = CustomerRecord_.class)
public class CustomerRecord extends RecordWithCommonFields implements ModelMapperTarget<Customer, CustomerRecord_>
{
    @NaturalId
    @Column(name = "cloud_id", nullable = false)
    private String cloudId;

    @Field
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * List of all the various services belonging to this customer.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("name")
    private List<CustomerServiceRecord> services;

    /**
     * List of all the various shared users belonging to this customer.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("email_address")
    private List<CustomerSharedUserRecord> sharedUsers;

    /**
     * List of all the various secrets belonging to this customer.
     */
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("context")
    private List<CustomerSharedSecretRecord> sharedSecrets;

    //--//

    public CustomerRecord()
    {
    }

    //--//

    public String getCloudId()
    {
        return cloudId;
    }

    public void setCloudId(String cloudId)
    {
        this.cloudId = cloudId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;

        if (cloudId == null)
        {
            cloudId = AwsHelper.sanitizeId("Customer__" + name);
        }
    }

    public List<CustomerServiceRecord> getServices()
    {
        return CollectionUtils.asEmptyCollectionIfNull(services);
    }

    public List<CustomerSharedUserRecord> getSharedUsers()
    {
        return CollectionUtils.asEmptyCollectionIfNull(sharedUsers);
    }

    public List<CustomerSharedSecretRecord> getSharedSecrets()
    {
        return CollectionUtils.asEmptyCollectionIfNull(sharedSecrets);
    }

    //--//

    public void collectCharges(DeploymentGlobalDescriptor globalDescriptor,
                               Map<TypedRecordIdentity<DeploymentHostRecord>, DeploymentCellularCharges> map)
    {
        for (CustomerServiceRecord rec_svc : getServices())
        {
            rec_svc.collectCharges(globalDescriptor, map);
        }
    }

    //--//

    public static CustomerRecord findByName(RecordHelper<CustomerRecord> helper,
                                            String name)
    {
        return QueryHelperWithCommonFields.getFirstMatch(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, CustomerRecord_.name, name);
        });
    }

    public static void streamAllRaw(SessionHolder sessionHolder,
                                    Consumer<RawQueryHelper<CustomerRecord, Customer>> applyFilters,
                                    Consumer<Customer> callback)
    {
        RawQueryHelper<CustomerRecord, Customer> qh = new RawQueryHelper<>(sessionHolder, CustomerRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addDate(RecordWithCommonFields_.updatedOn, (obj, val) -> obj.updatedOn = val);

        qh.addString(CustomerRecord_.cloudId, (obj, val) -> obj.cloudId = val);
        qh.addString(CustomerRecord_.name, (obj, val) -> obj.name = val);

        if (applyFilters != null)
        {
            applyFilters.accept(qh);
        }

        qh.stream(Customer::new, callback);
    }

    public static TypedRecordIdentityList<CustomerRecord> list(RecordHelper<CustomerRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, CustomerRecord_.name, true);
        });
    }

    public static List<CustomerRecord> getBatch(RecordHelper<CustomerRecord> helper,
                                                List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static CustomerRecord findByCloudId(RecordHelper<CustomerRecord> helper,
                                               String cloudId)
    {
        return helper.byNaturalId()
                     .using(CustomerRecord_.cloudId.getName(), cloudId)
                     .load();
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        for (CustomerServiceRecord rec_svc : getServices())
        {
            rec_svc.checkRemoveConditions(validation);
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<CustomerRecord> helper) throws
                                                            Exception
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            RecordHelper<CustomerServiceRecord> svcHelper = helper.wrapFor(CustomerServiceRecord.class);
            for (CustomerServiceRecord rec_svc : Lists.newArrayList(getServices()))
            {
                rec_svc.remove(validation, svcHelper);
            }

            helper.delete(this);
        }
    }
}
