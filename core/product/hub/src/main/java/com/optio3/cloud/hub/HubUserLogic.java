/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import javax.validation.constraints.NotNull;

import com.optio3.cloud.authentication.IdentityAdapter;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.exception.NotAuthorizedException;
import com.optio3.cloud.hub.model.identity.User;
import com.optio3.cloud.hub.persistence.config.RoleRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.LdapHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.UserInfo;
import com.optio3.util.function.BiFunctionWithException;
import org.apache.commons.lang3.StringUtils;

public class HubUserLogic extends IdentityAdapter<UserRecord, RoleRecord>
{
    private final HubApplication m_app;

    HubUserLogic(HubApplication app)
    {
        m_app = app;
    }

    @Override
    public void initialize()
    {
        DatabaseActivity.LocalSubscriber reg = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));
        reg.subscribeToTable(UserRecord.class, (dbEvent) ->
        {
            invalidateCaches();
        });
    }

    @Override
    public boolean authenticate(SessionHolder sessionHolder,
                                String principal,
                                String password,
                                boolean useCachedInfo)
    {
        UserRecord rec_user = findUserInner(sessionHolder, principal, password, true);
        return (rec_user != null);
    }

    @Override
    public String createUser(SessionHolder sessionHolder,
                             String principal,
                             String firstName,
                             String lastName,
                             String password)
    {
        return principal;
    }

    @Override
    public boolean changePassword(SessionHolder sessionHolder,
                                  String principal,
                                  String newPassword)
    {
        // The Hub doesn't have permissions to change a pasword in LDAP.
        throw new NotAuthorizedException("Can't change password of Maint users");
    }

    @Override
    public boolean deleteUser(SessionHolder sessionHolder,
                              UserRecord rec_user)
    {
        // The Hub doesn't have permissions to delete users in LDAP.
        // Just remove the local user.
        return true;
    }

    @Override
    public boolean addUserToGroup(SessionHolder sessionHolder,
                                  String emailAddress,
                                  RoleRecord role)
    {
        // The Hub doesn't have permissions to change user's roles in LDAP.
        return true;
    }

    @Override
    public boolean removeUserFromGroup(SessionHolder sessionHolder,
                                       String emailAddress,
                                       RoleRecord role)
    {
        // The Hub doesn't have permissions to change user's roles in LDAP.
        return true;
    }

    //--//

    @Override
    public List<UserRecord> listUsers(SessionHolder sessionHolder)
    {
        return UserRecord.getAll(sessionHolder.createHelper(UserRecord.class));
    }

    @Override
    public UserRecord findUser(SessionHolder sessionHolder,
                               String principal,
                               boolean useCachedInfo)
    {
        return findUserInner(sessionHolder, principal, null, false);
    }

    @Override
    public String getUserId(SessionHolder sessionHolder,
                            UserRecord user)
    {
        return user.getSysId();
    }

    @Override
    public UserRecord getUser(SessionHolder sessionHolder,
                              String sysId)
    {
        return sessionHolder.getEntityOrNull(UserRecord.class, sysId);
    }

    @Override
    public List<RoleRecord> getRoles(SessionHolder sessionHolder,
                                     UserRecord user)
    {
        return user.getRoles();
    }

    @Override
    public List<RoleRecord> listRoles(SessionHolder sessionHolder)
    {
        return RoleRecord.getAll(sessionHolder.createHelper(RoleRecord.class));
    }

    @Override
    public String getRoleId(SessionHolder sessionHolder,
                            RoleRecord role)
    {
        return role.getSysId();
    }

    @Override
    public RoleRecord getRole(SessionHolder sessionHolder,
                              String id)
    {
        return sessionHolder.getEntityOrNull(RoleRecord.class, id);
    }

    @Override
    public boolean hasRoles(UserRecord user,
                            RoleRecord... roles)
    {
        String[] names = new String[roles.length];
        for (int i = 0; i < roles.length; i++)
            names[i] = roles[i].getName();

        return user.hasAnyRoles(names);
    }

    //--//

    public User getSyntheticUserFromPrincipal(SessionHolder sessionHolder,
                                              @NotNull CookiePrincipal principal)
    {
        // If we got here, it could be a synthetic token. Generate a synthetic model.
        User user = new User();
        user.emailAddress = principal.getName();

        for (String role : principal.getRoles())
        {
            RoleRecord rec_role = RoleRecord.findByName(sessionHolder, role);
            if (rec_role != null)
            {
                user.roles.add(RecordIdentity.newTypedInstance(rec_role));
            }
        }

        return user;
    }

    //--//

    private UserRecord findUserInner(SessionHolder holder,
                                     String principal,
                                     String password,
                                     boolean checkPassword)
    {
        if (WellKnownRole.Machine.validateAuthCode(principal, password, checkPassword))
        {
            return UserRecord.createPlaceholder(WellKnownRole.Machine);
        }

        String sysId = lookupCachedEmail(principal);
        if (sysId == null)
        {
            RecordHelper<UserRecord> helper = holder.createHelper(UserRecord.class);

            TypedRecordIdentity<UserRecord> ri = UserRecord.findByEmailAddress(helper, principal);
            if (ri != null)
            {
                sysId = ri.sysId;
                trackEmailToSysId(principal, sysId);
            }
        }

        UserRecord rec = holder.getEntityOrNull(UserRecord.class, sysId);
        if (checkPassword)
        {
            rec = checkPassword(rec, holder, principal, password);
        }

        if (rec != null)
        {
            // Just to load all the lazy-initialized values.
            rec.eagerInit();
        }

        return rec;
    }

    private UserRecord checkPassword(UserRecord rec,
                                     SessionHolder holder,
                                     String principal,
                                     String password)
    {
        if (StringUtils.isBlank(password))
        {
            return null;
        }

        while (true)
        {
            if (rec == null)
            {
                rec = tryLdap(principal, password, false, (ldapHelper, attrs) -> createRecordFromLdap(holder, ldapHelper, principal, attrs, false));
                if (rec != null)
                {
                    break;
                }

                rec = tryLdap(principal, password, true, (ldapHelper, attrs) -> createRecordFromLdap(holder, ldapHelper, principal, attrs, true));
                if (rec != null)
                {
                    break;
                }

                return null;
            }

            Boolean hit = authenticateWithCachedPassword(rec.getSysId(), password);
            if (hit != null)
            {
                if (!hit)
                {
                    return null;
                }

                // Exit directly, to avoid refreshing the cache with the same password.
                return rec;
            }

            if (rec.isFromLdap())
            {
                if (tryLdap(principal, password, false, (ldapHelper, attrs) -> principal) != null)
                {
                    break;
                }

                if (tryLdap(principal, password, true, (ldapHelper, attrs) -> principal) != null)
                {
                    break;
                }
            }
            else
            {
                if (rec.getPassword()
                       .authenticate(password))
                {
                    break;
                }
            }

            return null;
        }

        cacheHashOfValidPassword(rec.getSysId(), password);
        return rec;
    }

    private <T> T tryLdap(String principal,
                          String password,
                          boolean automation,
                          BiFunctionWithException<LdapHelper, Attributes, T> callback)
    {
        try
        {
            String uid = LdapHelper.tryInferringDnfromEmail(principal, LdapHelper.OPTIO3_DOMAIN, automation);
            if (uid == null)
            {
                return null;
            }

            UserInfo ui = new UserInfo();
            ui.site = WellKnownSites.ldapServer();
            ui.ldapDn = uid;
            ui.password = password;

            //
            // Try login to LDAP.
            //
            try (LdapHelper helper = new LdapHelper(ui))
            {
                SearchResult result = helper.findByEmailAddress(LdapHelper.LDAP_SUFFIX, principal);
                if (result != null)
                {
                    return callback.apply(helper, result.getAttributes());
                }
            }
        }
        catch (Throwable e)
        {
            // Ignore all failures.
        }

        return null;
    }

    private UserRecord createRecordFromLdap(SessionHolder holder,
                                            LdapHelper ldapHelper,
                                            String principal,
                                            Attributes attrs,
                                            boolean automation) throws
                                                                NamingException
    {
        holder.beginTransaction();

        RecordHelper<UserRecord> helperUser = holder.createHelper(UserRecord.class);
        UserRecord               rec        = new UserRecord(principal);

        rec.setFirstName(ldapHelper.getValue(attrs, LdapHelper.GN, LdapHelper.GIVENNAME));
        rec.setLastName(ldapHelper.getValue(attrs, LdapHelper.SN));
        rec.setEmailAddress(ldapHelper.getValue(attrs, LdapHelper.MAIL));

        helperUser.persist(rec);

        addRoleIfPresent(holder, rec, WellKnownRoleIds.Maintenance);
        addRoleIfPresent(holder, rec, WellKnownRoleIds.Administrator);

        holder.commit();

        return rec;
    }

    private void addRoleIfPresent(SessionHolder holder,
                                  UserRecord rec,
                                  String name)
    {
        RoleRecord role = RoleRecord.findByName(holder, name);
        if (role != null)
        {
            rec.getRoles()
               .add(role);
        }
    }
}
