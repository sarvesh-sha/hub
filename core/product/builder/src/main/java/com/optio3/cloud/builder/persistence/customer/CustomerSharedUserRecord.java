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
import javax.persistence.Transient;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerSharedUser;
import com.optio3.cloud.builder.model.identity.UserCreationRequest;
import com.optio3.cloud.persistence.EncryptedPayload;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.serialization.ObjectMappers;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "CUSTOMER_SHARED_USER")
@Optio3TableInfo(externalId = "CustomerSharedUser", model = CustomerSharedUser.class, metamodel = CustomerSharedUserRecord_.class)
public class CustomerSharedUserRecord extends RecordWithCommonFields implements ModelMapperTarget<CustomerSharedUser, CustomerSharedUserRecord_>
{
    /**
     * The context of this record.
     */
    @Optio3ControlNotifications(reason = "Only notify service", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getCustomer")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getCustomer")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "customer", nullable = false, foreignKey = @ForeignKey(name = "CUSTOMER_SHARED_USER__CUSTOMER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private CustomerRecord customer;

    @Column(name = "first_name", nullable = true)
    private String firstName;

    @Column(name = "last_name", nullable = true)
    private String lastName;

    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    @Embedded
    private EncryptedPayload password;

    @Column(name = "roles", nullable = true)
    private String roles;

    @Transient
    private final PersistAsJsonHelper<String, List<String>> m_rolesHelper = new PersistAsJsonHelper<>(() -> roles,
                                                                                                      (val) -> roles = val,
                                                                                                      String.class,
                                                                                                      MetadataField.TypeRef_listOfStrings,
                                                                                                      ObjectMappers.SkipNulls);

    //--//

    public CustomerSharedUserRecord()
    {
    }

    public static CustomerSharedUserRecord newInstance(BuilderConfiguration cfg,
                                                       CustomerRecord rec_cust,
                                                       UserCreationRequest request) throws
                                                                                    Exception
    {
        CustomerSharedUserRecord rec_newUser = new CustomerSharedUserRecord();
        rec_newUser.customer = rec_cust;

        rec_newUser.setFirstName(request.firstName);
        rec_newUser.setLastName(request.lastName);
        rec_newUser.setEmailAddress(request.emailAddress);
        rec_newUser.setPhoneNumber(request.phoneNumber);
        rec_newUser.setPassword(cfg.encrypt(request.password));

        rec_newUser.setRoles(request.roles);

        return rec_newUser;
    }

    //--//

    public CustomerRecord getCustomer()
    {
        return customer;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    public String getLastName()
    {
        return lastName;
    }

    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    public String getEmailAddress()
    {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress)
    {
        this.emailAddress = emailAddress;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public EncryptedPayload getPassword()
    {
        return password;
    }

    public void setPassword(EncryptedPayload value)
    {
        this.password = value;
    }

    public void setRoles(List<String> roles)
    {
        m_rolesHelper.set(roles);
    }

    public List<String> getRoles()
    {
        return m_rolesHelper.get();
    }

    //--//

    public static List<CustomerSharedUserRecord> getBatch(RecordHelper<CustomerSharedUserRecord> helper,
                                                          List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }
}
