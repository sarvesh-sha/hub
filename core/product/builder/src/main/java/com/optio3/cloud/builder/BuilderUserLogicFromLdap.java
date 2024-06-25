/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import com.google.common.collect.Maps;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.builder.persistence.config.RoleRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.LdapHelper;
import com.optio3.infra.directory.UserInfo;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class BuilderUserLogicFromLdap extends BuilderUserLogic
{
    public static final String LDAP_SUFFIX = "dc=optio3,dc=com";

    public static final String GROUP_DEV     = "cn=dev,ou=groups," + LDAP_SUFFIX;
    public static final String GROUP_INFRA   = "cn=infra,ou=groups," + LDAP_SUFFIX;
    public static final String GROUP_MACHINE = "cn=machine,ou=groups," + LDAP_SUFFIX;
    public static final String GROUP_BUILD   = "cn=build,ou=admin,ou=groups," + LDAP_SUFFIX;

    //--//

    private final UserInfo       m_ldapRoot;
    private       MonotonousTime m_refreshUsers;

    BuilderUserLogicFromLdap(BuilderApplication app,
                             UserInfo ldapRoot)
    {
        super(app);

        m_ldapRoot = ldapRoot;

        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(app, null, Optio3DbRateLimiter.System))
        {
            ensureRole(holder, GROUP_BUILD, WellKnownRole.Administrator);
            ensureRole(holder, GROUP_INFRA, WellKnownRole.Infrastructure);
            ensureRole(holder, GROUP_MACHINE, WellKnownRole.Machine);
            ensureRole(holder, GROUP_DEV, WellKnownRole.User);

            holder.commit();
        }
    }

    private void ensureRole(SessionHolder holder,
                            String sysId,
                            WellKnownRole role)
    {
        RecordHelper<RoleRecord> helper = holder.createHelper(RoleRecord.class);

        if (helper.getOrNull(sysId) == null)
        {
            RoleRecord rec = new RoleRecord();
            rec.setSysId(sysId);
            rec.setName(role.getId());
            rec.setDisplayName(role.getDisplayName());

            helper.persist(rec);
        }
    }

    //--//

    @Override
    public List<UserRecord> listUsers(SessionHolder holder) throws
                                                            Exception
    {
        if (TimeUtils.isTimeoutExpired(m_refreshUsers))
        {
            holder.beginTransaction();

            //
            // Assume all users are stale.
            //
            Map<String, UserRecord> staleLdapUsers = Maps.newHashMap();
            for (UserRecord rec_user : super.listUsers(holder))
            {
                if (rec_user.isFromLdap())
                {
                    staleLdapUsers.put(rec_user.getEmailAddress(), rec_user);
                }
            }

            try (LdapHelper helper = new LdapHelper(m_ldapRoot))
            {
                NamingEnumeration<SearchResult> results = helper.searchSubtree(LDAP_SUFFIX, LdapHelper.OBJECT_CLASS + "=inetOrgPerson");
                while (results.hasMore())
                {
                    SearchResult result = results.next();

                    UserRecord rec_user = fromLdapToUser(holder, helper, result);

                    //
                    // User exists, remove from stale set.
                    //
                    staleLdapUsers.remove(rec_user.getEmailAddress());
                }
            }

            //
            // Delete stale users.
            //
            if (!staleLdapUsers.isEmpty())
            {
                RecordHelper<UserRecord> helper = holder.createHelper(UserRecord.class);
                for (UserRecord rec_user : staleLdapUsers.values())
                {
                    helper.delete(rec_user);
                }
            }

            holder.commit();

            m_refreshUsers = TimeUtils.computeTimeoutExpiration(1, TimeUnit.HOURS);
        }

        return super.listUsers(holder);
    }

    @Override
    protected UserRecord ensureUserRecord(SessionHolder holder,
                                          String principal)
    {
        try (LdapHelper helper = new LdapHelper(m_ldapRoot))
        {
            SearchResult result = helper.findByEmailAddress(LDAP_SUFFIX, principal);
            if (result == null)
            {
                return null;
            }

            holder.beginTransaction();

            UserRecord rec = fromLdapToUser(holder, helper, result);

            holder.commit();

            return rec;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    protected boolean verifyUserPassword(SessionHolder holder,
                                         UserRecord user,
                                         String password)
    {
        UserInfo ui = new UserInfo();
        ui.site = m_ldapRoot.site;
        ui.ldapDn = user.getSysId();
        ui.password = password;

        //
        // Try login to LDAP.
        //
        try (LdapHelper helper = new LdapHelper(ui))
        {
            // If we get here, the password is okay.
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    protected boolean changeUserPassword(SessionHolder holder,
                                         UserRecord user,
                                         String password)
    {
        try (LdapHelper helper = new LdapHelper(m_ldapRoot))
        {
            SearchResult result = helper.findByEmailAddress(LDAP_SUFFIX, user.getEmailAddress());
            if (result == null)
            {
                return false;
            }

            String name = result.getNameInNamespace();

            String hashedPassword = LdapHelper.hashPassword(password);
            helper.changeUserPassword(name, hashedPassword);
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    @Override
    public String createUser(SessionHolder sessionHolder,
                             String principal,
                             String firstName,
                             String lastName,
                             String password)
    {
        try (LdapHelper helper = new LdapHelper(m_ldapRoot))
        {
            UserId id = decodeEmail(principal, false);

            try
            {
                helper.getAttributes(id.dn);
                // No exception => user already exists!
                return null;
            }
            catch (NameNotFoundException ex)
            {
            }

            String hashedPassword = LdapHelper.hashPassword(password);
            helper.createUser(LdapHelper.LDAP_SUFFIX_USERS, id.uid, firstName, lastName, principal, hashedPassword);
            return id.dn;
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    @Override
    public boolean deleteUser(SessionHolder sessionHolder,
                              UserRecord user)
    {
        try (LdapHelper helper = new LdapHelper(m_ldapRoot))
        {
            UserId id = decodeEmail(user.getEmailAddress(), false);

            helper.deleteUser(id.dn);
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    @Override
    public boolean addUserToGroup(SessionHolder sessionHolder,
                                  String emailAddress,
                                  RoleRecord role)
    {
        try (LdapHelper helper = new LdapHelper(m_ldapRoot))
        {
            UserId id = decodeEmail(emailAddress, false);

            helper.addMemberToGroup(role.getSysId(), id.dn);
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    @Override
    public boolean removeUserFromGroup(SessionHolder sessionHolder,
                                       String emailAddress,
                                       RoleRecord role)
    {
        try (LdapHelper helper = new LdapHelper(m_ldapRoot))
        {
            UserId id = decodeEmail(emailAddress, false);

            helper.removeMemberFromGroup(role.getSysId(), id.dn);
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    //--//

    private UserRecord fromLdapToUser(SessionHolder holder,
                                      LdapHelper helper,
                                      SearchResult result) throws
                                                           NamingException
    {
        Attributes attrs = result.getAttributes();
        String     email = helper.getValue(attrs, LdapHelper.MAIL);

        RecordHelper<UserRecord> helper_user = holder.createHelper(UserRecord.class);

        TypedRecordIdentity<UserRecord> ri       = UserRecord.findByEmailAddress(helper_user, email);
        UserRecord                      rec_user = holder.fromIdentityOrNull(ri);
        if (rec_user == null)
        {
            final String fullName = result.getNameInNamespace();

            rec_user = new UserRecord(fullName);
            rec_user.setFirstName(helper.getValue(attrs, LdapHelper.GN, LdapHelper.GIVENNAME));
            rec_user.setLastName(helper.getValue(attrs, LdapHelper.SN));
            rec_user.setEmailAddress(email);

            final List<RoleRecord> roles = rec_user.getRoles();

            for (RoleRecord rec_role : RoleRecord.getAll(holder.createHelper(RoleRecord.class)))
            {
                Set<String> members = helper.getMembersOfGroup(rec_role.getSysId());
                if (members.contains(fullName))
                {
                    roles.add(rec_role);
                }
            }

            helper_user.persist(rec_user);
        }

        return rec_user;
    }

    static class UserId
    {
        boolean isAutomation;
        String  uid;
        String  dn;
    }

    private static UserId decodeEmail(String emailAddress,
                                      boolean isAutomation)
    {
        UserId res = new UserId();
        res.isAutomation = isAutomation;
        res.uid = emailAddress.split("@")[0];
        res.dn = LdapHelper.getUidInNamespace(isAutomation ? LdapHelper.LDAP_SUFFIX_AUTOMATION : LdapHelper.LDAP_SUFFIX_USERS, res.uid);
        return res;
    }
}
