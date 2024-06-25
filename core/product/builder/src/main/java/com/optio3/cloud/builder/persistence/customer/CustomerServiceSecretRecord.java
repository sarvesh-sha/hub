/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.customer;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerServiceSecret;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "CUSTOMER_SERVICE_SECRET")
@Optio3TableInfo(externalId = "CustomerServiceSecret", model = CustomerServiceSecret.class, metamodel = CustomerServiceSecretRecord_.class)
public class CustomerServiceSecretRecord extends RecordWithCommonFields implements ModelMapperTarget<CustomerServiceSecret, CustomerServiceSecretRecord_>
{
    /**
     * The context of this record.
     */
    @Optio3ControlNotifications(reason = "Only notify service", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getService")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getService")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "service", nullable = false, foreignKey = @ForeignKey(name = "CUSTOMER_SERVICE_SECRET__CUSTOMER_SERVICE__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private CustomerServiceRecord service;

    @Column(name = "context", nullable = false)
    private String context;

    @Column(name = "keyId", nullable = false) // key is a reserved keyword in MySQL/MariaDB...
    private String key;

    @Embedded
    private EncryptedPayload value;

    //--//

    public CustomerServiceSecretRecord()
    {
    }

    public static CustomerServiceSecretRecord newInstance(BuilderConfiguration cfg,
                                                          CustomerServiceRecord rec_svc,
                                                          CustomerServiceSecret val) throws
                                                                                     Exception
    {
        CustomerServiceSecretRecord rec_newSecret = new CustomerServiceSecretRecord();
        rec_newSecret.service = rec_svc;

        rec_newSecret.setContext(val.context);
        rec_newSecret.setKey(val.key);
        rec_newSecret.setValue(cfg.encrypt(val.value));

        return rec_newSecret;
    }

    //--//

    public CustomerServiceRecord getService()
    {
        return service;
    }

    public String getContext()
    {
        return context;
    }

    public void setContext(String context)
    {
        this.context = context;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public EncryptedPayload getValue()
    {
        return value;
    }

    public void setValue(EncryptedPayload value)
    {
        this.value = value;
    }

    //--//

    public static List<CustomerServiceSecretRecord> getBatch(RecordHelper<CustomerServiceSecretRecord> helper,
                                                             List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }
}
