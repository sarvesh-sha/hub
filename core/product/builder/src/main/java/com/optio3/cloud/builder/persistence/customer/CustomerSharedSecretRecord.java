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
import com.optio3.cloud.builder.model.customer.CustomerSharedSecret;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "CUSTOMER_SHARED_SECRET")
@Optio3TableInfo(externalId = "CustomerSharedSecret", model = CustomerSharedSecret.class, metamodel = CustomerSharedSecretRecord_.class)
public class CustomerSharedSecretRecord extends RecordWithCommonFields implements ModelMapperTarget<CustomerSharedSecret, CustomerSharedSecretRecord_>
{
    /**
     * The context of this record.
     */
    @Optio3ControlNotifications(reason = "Only notify service", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getCustomer")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getCustomer")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "customer", nullable = false, foreignKey = @ForeignKey(name = "CUSTOMER_SHARED_SECRET__CUSTOMER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private CustomerRecord customer;

    @Column(name = "context", nullable = false)
    private String context;

    @Column(name = "keyId", nullable = false) // key is a reserved keyword in MySQL/MariaDB...
    private String key;

    @Embedded
    private EncryptedPayload value;

    //--//

    public CustomerSharedSecretRecord()
    {
    }

    public static CustomerSharedSecretRecord newInstance(BuilderConfiguration cfg,
                                                         CustomerRecord rec_cust,
                                                         CustomerSharedSecret val) throws
                                                                                   Exception
    {
        CustomerSharedSecretRecord rec_newSecret = new CustomerSharedSecretRecord();
        rec_newSecret.customer = rec_cust;

        rec_newSecret.setContext(val.context);
        rec_newSecret.setKey(val.key);
        rec_newSecret.setValue(cfg.encrypt(val.value));

        return rec_newSecret;
    }

    //--//

    public CustomerRecord getCustomer()
    {
        return customer;
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

    public static List<CustomerSharedSecretRecord> getBatch(RecordHelper<CustomerSharedSecretRecord> helper,
                                                            List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }
}
