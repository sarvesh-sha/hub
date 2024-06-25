/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.config;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.builder.BuilderUserLogic;
import com.optio3.cloud.builder.model.identity.User;
import com.optio3.cloud.builder.model.identity.UserCreationRequest;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.HashedPassword;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.search.Optio3QueryAnalyzerOverride;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "USER")
@Indexed
@Analyzer(definition = "prefix")
@Optio3QueryAnalyzerOverride("prefix_query")
@Optio3TableInfo(externalId = "User", model = User.class, metamodel = UserRecord_.class)
public class UserRecord extends RecordWithCommonFields implements ModelMapperTarget<User, UserRecord_>
{
    @Field
    @Column(name = "first_name", nullable = true)
    private String firstName;

    @Field
    @Column(name = "last_name", nullable = true)
    private String lastName;

    @Field
    @Analyzer(definition = "prefix_email")
    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    @Embedded
    private HashedPassword password;

    @Optio3ControlNotifications(reason = "Notify user of changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getRoles")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "USER_ROLE", joinColumns = @JoinColumn(name = "UserRecord_sys_id"), inverseJoinColumns = @JoinColumn(name = "roles_sys_id"))
    private List<RoleRecord> roles = Lists.newArrayList();

    @Column(name = "from_ldap", nullable = false)
    private boolean fromLdap;

    @Column(name = "identity_version", nullable = false)
    private int identityVersion;

    @Column(name = "reset_token")
    private String resetToken;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<UserPreferenceRecord> preferences;

    @Transient
    private UserPreferenceRecord.Tree parsedPreferences;

    //--//

    public UserRecord()
    {
    }

    public UserRecord(String sysId)
    {
        setSysId(sysId);
        fromLdap = true;
    }

    public void eagerInit()
    {
        for (RoleRecord rec_role : getRoles())
        {
            // Just to load all the lazy-initialized values.
        }
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

    public HashedPassword getPassword()
    {
        return password;
    }

    public void setPassword(SessionHolder sessionHolder,
                            BuilderUserLogic userLogic,
                            String newPassword)
    {
        if (newPassword == null)
        {
            throw new IllegalArgumentException("Invalid password");
        }

        if (isFromLdap())
        {
            userLogic.changePassword(sessionHolder, getEmailAddress(), newPassword);
        }
        else
        {
            this.password = new HashedPassword(newPassword);
        }

        userLogic.invalidateCaches();
        this.identityVersion++;
    }

    public List<RoleRecord> getRoles()
    {
        return roles;
    }

    public boolean isFromLdap()
    {
        return fromLdap;
    }

    public int getIdentityVersion()
    {
        return identityVersion;
    }

    public String getResetToken()
    {
        return resetToken;
    }

    public void setResetToken(String resetToken)
    {
        this.resetToken = resetToken;
    }

    //--//

    public static TypedRecordIdentity<UserRecord> findByEmailAddress(RecordHelper<UserRecord> helper,
                                                                     String emailAddress) throws
                                                                                          NoResultException
    {
        return QueryHelperWithCommonFields.single(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, UserRecord_.emailAddress, emailAddress);
        });
    }

    //--//

    public static UserRecord createPlaceholder(WellKnownRole role)
    {
        RoleRecord rec_role = new RoleRecord();
        rec_role.setSysId(role.getId());
        rec_role.setName(role.getId());

        UserRecord rec_user = new UserRecord(role.getId());
        rec_user.roles.add(rec_role);

        return rec_user;
    }

    public static UserRecord createNewUser(SessionHolder sessionHolder,
                                           BuilderUserLogic userLogic,
                                           UserCreationRequest request,
                                           RecordHelper<RoleRecord> helper,
                                           UserRecord rec_caller,
                                           boolean isAdmin)
    {
        String userIdentifier = userLogic.createUser(sessionHolder, request.emailAddress, request.firstName, request.lastName, request.password);
        if (userIdentifier == null)
        {
            return null;
        }

        UserRecord rec_newUser = new UserRecord(userIdentifier);
        rec_newUser.setFirstName(request.firstName);
        rec_newUser.setLastName(request.lastName);
        rec_newUser.setEmailAddress(request.emailAddress);
        rec_newUser.setPhoneNumber(request.phoneNumber);

        try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, null, false))
        {
            rec_newUser.updateRoles(validation, userLogic, helper, request.roles, rec_caller, isAdmin);
        }

        return rec_newUser;
    }

    public void updateRoles(ValidationResultsHolder validation,
                            BuilderUserLogic userLogic,
                            RecordHelper<RoleRecord> helper,
                            List<String> roleNames,
                            UserRecord rec_caller,
                            boolean isAdmin)
    {
        Set<RoleRecord> roleIds = Sets.newHashSet();

        for (RoleRecord rec_role : helper.listAll())
        {
            if (roleNames.contains(rec_role.getName()))
            {
                roleIds.add(rec_role);
            }
        }

        updateRoles(validation, userLogic, helper, TypedRecordIdentityList.toList(roleIds), rec_caller, isAdmin);
    }

    public void updateRoles(ValidationResultsHolder validation,
                            BuilderUserLogic userLogic,
                            RecordHelper<RoleRecord> helper,
                            TypedRecordIdentityList<RoleRecord> roleIds,
                            UserRecord rec_caller,
                            boolean isAdmin)
    {
        if (isAdmin)
        {
            List<RoleRecord> desiredRoles  = RoleRecord.getBatch(helper, roleIds);
            List<RoleRecord> existingRoles = getRoles();

            existingRoles.removeIf((role) ->
                                   {
                                       if (desiredRoles.contains(role))
                                       {
                                           return false;
                                       }

                                       if (role.isRole(WellKnownRoleIds.Administrator) && rec_caller == this)
                                       {
                                           validation.addFailure("role", "Can't remove ADMIN role from yourself");
                                       }

                                       if (validation.canProceed())
                                       {
                                           userLogic.removeUserFromGroup(validation.sessionHolder, getEmailAddress(), role);
                                       }

                                       return validation.canProceed();
                                   });

            for (RoleRecord desiredRole : desiredRoles)
            {
                if (!existingRoles.contains(desiredRole))
                {
                    if (validation.canProceed())
                    {
                        existingRoles.add(desiredRole);

                        userLogic.addUserToGroup(validation.sessionHolder, getEmailAddress(), desiredRole);
                    }
                }
            }
        }
    }

    //--//

    public RoleRecord getRole(String role)
    {
        return RoleRecord.getRole(getRoles(), role);
    }

    public boolean hasRole(String role)
    {
        return getRole(role) != null;
    }

    public boolean hasAnyRoles(String... roles)
    {
        for (String role : roles)
        {
            if (hasRole(role))
            {
                return true;
            }
        }

        return false;
    }

    public boolean hasAllRoles(String... roles)
    {
        for (String role : roles)
        {
            if (!hasRole(role))
            {
                return false;
            }
        }

        return true;
    }

    //--//

    public List<UserPreferenceRecord> getPreferences()
    {
        return CollectionUtils.asEmptyCollectionIfNull(preferences);
    }

    public UserPreferenceRecord.Tree getPreferencesTree(RecordHelper<UserPreferenceRecord> helper)
    {
        if (parsedPreferences == null)
        {
            parsedPreferences = UserPreferenceRecord.Tree.build(helper, this, getPreferences());
        }

        return parsedPreferences;
    }

    void flushPreferences()
    {
        parsedPreferences = null;
    }
}
