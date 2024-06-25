/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubUserLogic;
import com.optio3.cloud.hub.model.identity.User;
import com.optio3.cloud.hub.model.identity.UserCreationRequest;
import com.optio3.cloud.hub.persistence.message.UserMessageRecord;
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
import com.optio3.concurrency.Executors;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.IdGenerator;
import org.apache.commons.lang3.StringUtils;
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
    enum ConfigVariable implements IConfigVariable
    {
        FirstName("FIRST_NAME"),
        LastName("LAST_NAME"),
        SiteUrl("SITE_URL"),
        Token("TOKEN");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator              = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_resetPassword_email = s_configValidator.newTemplate(UserRecord.class, "emails/passwords/reset_token.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_resetPassword_text  = s_configValidator.newTemplate(UserRecord.class, "texts/passwords/reset_token.txt", "${", "}");

    //--//

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

    @Optio3ControlNotifications(reason = "Notify user of changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getGroups")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "USER_USER_GROUP", joinColumns = @JoinColumn(name = "UserRecord_sys_id"), inverseJoinColumns = @JoinColumn(name = "groups_sys_id"))
    private List<UserGroupRecord> groups = Lists.newArrayList();

    @Column(name = "from_ldap", nullable = false)
    private boolean fromLdap;

    @Column(name = "identity_version", nullable = false)
    private int identityVersion;

    @Column(name = "reset_token")
    private String resetToken;

    //--//

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<UserMessageRecord> messages;

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

        for (UserGroupRecord rec_userGroup : getGroups())
        {
            rec_userGroup.eagerInit();
        }
    }

    //--//

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
                            HubUserLogic userLogic,
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

        if (userLogic != null)
        {
            userLogic.invalidateCaches();
        }

        this.identityVersion++;
    }

    public List<RoleRecord> getRoles()
    {
        return roles;
    }

    public List<UserGroupRecord> getGroups()
    {
        return groups;
    }

    public boolean isFromLdap()
    {
        return fromLdap;
    }

    public int getIdentityVersion()
    {
        return identityVersion;
    }

    public List<UserMessageRecord> getMessages()
    {
        return CollectionUtils.asEmptyCollectionIfNull(messages);
    }

    public String getOrigin()
    {
        if (isFromLdap())
        {
            return "ldap.dev.optio3.io";
        }

        return null;
    }

    //--//

    public static List<UserRecord> getAll(RecordHelper<UserRecord> helper)
    {
        return QueryHelperWithCommonFields.filter(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, UserRecord_.firstName, true);
            jh.addOrderBy(jh.root, UserRecord_.lastName, true);
        });
    }

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
                                           HubUserLogic userLogic,
                                           UserCreationRequest request,
                                           UserRecord rec_caller,
                                           boolean isMaint,
                                           boolean isPrivileged)
    {
        RecordHelper<UserRecord> helper = sessionHolder.createHelper(UserRecord.class);
        helper.lockTableUntilEndOfTransaction(10, TimeUnit.SECONDS);

        UserRecord rec_oldUser = userLogic.findUser(sessionHolder, request.emailAddress, false);
        if (rec_oldUser != null)
        {
            throw new InvalidArgumentException("User already exists with same email address");
        }

        UserRecord rec_newUser = new UserRecord();
        rec_newUser.setFirstName(request.firstName);
        rec_newUser.setLastName(request.lastName);
        rec_newUser.setEmailAddress(request.emailAddress);
        rec_newUser.setPhoneNumber(request.phoneNumber);

        rec_newUser.setPassword(sessionHolder, userLogic, request.password);

        try (ValidationResultsHolder validation = new ValidationResultsHolder(sessionHolder, null, false))
        {
            rec_newUser.updateRoles(validation, sessionHolder.createHelper(RoleRecord.class), request.roles, rec_caller, isMaint, isPrivileged);
        }

        return rec_newUser;
    }

    public void updateRoles(ValidationResultsHolder validation,
                            RecordHelper<RoleRecord> helper,
                            TypedRecordIdentityList<RoleRecord> roleIds,
                            UserRecord rec_caller,
                            boolean isMaint,
                            boolean isPrivileged)
    {
        if (isPrivileged)
        {
            List<RoleRecord> desiredRoles  = RoleRecord.getBatch(helper, roleIds);
            List<RoleRecord> existingRoles = getRoles();

            existingRoles.removeIf((role) ->
                                   {
                                       if (desiredRoles.contains(role))
                                       {
                                           return false;
                                       }

                                       if (!role.isRemoveAllowed())
                                       {
                                           validation.addFailure("role", "Can't remove %s role", role.getDisplayName());
                                       }

                                       if (role.isRole(WellKnownRoleIds.Administrator) && rec_caller == this)
                                       {
                                           validation.addFailure("role", "Can't remove ADMIN role from yourself");
                                       }

                                       return validation.canProceed();
                                   });

            for (RoleRecord desiredRole : desiredRoles)
            {
                if (!existingRoles.contains(desiredRole))
                {
                    if (!desiredRole.isAddAllowed() && !isMaint)
                    {
                        validation.addFailure("role", "Can't assign %s role", desiredRole.getDisplayName());
                    }

                    if (validation.canProceed())
                    {
                        existingRoles.add(desiredRole);
                    }
                }
            }
        }
    }

    public void updateGroups(ValidationResultsHolder validation,
                             RecordHelper<UserGroupRecord> helper,
                             TypedRecordIdentityList<UserGroupRecord> userGroupIds,
                             UserRecord rec_caller,
                             boolean isMaint,
                             boolean isPrivileged)
    {
        if (isPrivileged)
        {
            List<UserGroupRecord> desiredUserGroups  = UserGroupRecord.getBatch(helper, userGroupIds);
            List<UserGroupRecord> existingUserGroups = getGroups();

            existingUserGroups.removeIf((userGroup) ->
                                        {
                                            if (desiredUserGroups.contains(userGroup))
                                            {
                                                return false;
                                            }

                                            return validation.canProceed();
                                        });

            for (UserGroupRecord desiredUserGroup : desiredUserGroups)
            {
                if (!existingUserGroups.contains(desiredUserGroup))
                {
                    for (RoleRecord role : desiredUserGroup.getRoles())
                    {
                        if (!role.isAddAllowed() && !isMaint)
                        {
                            validation.addFailure("group", "Can't assign sub group %s role", desiredUserGroup.getName());
                        }
                    }

                    if (validation.canProceed())
                    {
                        existingUserGroups.add(desiredUserGroup);
                    }
                }
            }
        }
    }

    public void startPasswordReset(HubApplication app)
    {
        resetToken = IdGenerator.newGuid();

        Executors.getDefaultThreadPool()
                 .submit(() ->
                         {
                             try (SessionHolder sessionHolder = SessionHolder.createWithNewSessionWithTransaction(app, null, Optio3DbRateLimiter.Normal))
                             {
                                 HubConfiguration cfg = app.getServiceNonNull(HubConfiguration.class);

                                 app.sendEmailNotification(sessionHolder, false, getEmailAddress(), "Password Reset", false, prepareParameters(cfg, s_template_resetPassword_email));

                                 if (StringUtils.isNotBlank(getPhoneNumber()))
                                 {
                                     app.sendTextNotification(sessionHolder, false, null, getPhoneNumber(), prepareParameters(cfg, s_template_resetPassword_text));
                                 }

                                 sessionHolder.commit();
                             }
                         });
    }

    public void completePasswordReset(SessionHolder sessionHolder,
                                      HubUserLogic userLogic,
                                      String token,
                                      String newPassword)
    {
        if (resetToken == null || !StringUtils.equals(resetToken, token))
        {
            throw new InvalidStateException("Incorrect token");
        }

        setPassword(sessionHolder, userLogic, newPassword);

        this.resetToken = null;
    }

    private ConfigVariables<ConfigVariable> prepareParameters(HubConfiguration cfg,
                                                              ConfigVariables.Template<ConfigVariable> template)
    {
        ConfigVariables<ConfigVariable> parameters = template.allocate();
        parameters.setValue(ConfigVariable.FirstName, getFirstName());
        parameters.setValue(ConfigVariable.LastName, getLastName());
        parameters.setValue(ConfigVariable.SiteUrl, cfg.cloudConnectionUrl);
        parameters.setValue(ConfigVariable.Token, resetToken);
        return parameters;
    }

    //--//

    public RoleRecord getRole(String role)
    {
        RoleRecord rec = RoleRecord.getRole(getRoles(), role);
        if (rec != null)
        {
            return rec;
        }

        for (UserGroupRecord group : getGroups())
        {
            rec = group.getRole(role);
            if (rec != null)
            {
                return rec;
            }
        }

        return null;
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
