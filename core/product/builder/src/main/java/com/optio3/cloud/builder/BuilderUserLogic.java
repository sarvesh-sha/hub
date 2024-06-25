/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.optio3.cloud.authentication.IdentityAdapter;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.builder.model.identity.User;
import com.optio3.cloud.builder.persistence.config.RoleRecord;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.HashedPassword;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import org.apache.commons.lang3.StringUtils;

public abstract class BuilderUserLogic extends IdentityAdapter<UserRecord, RoleRecord>
{
    private final BuilderApplication m_app;

    protected BuilderUserLogic(BuilderApplication app)
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
    public boolean changePassword(SessionHolder sessionHolder,
                                  String principal,
                                  String newPassword)
    {
        if (newPassword == null)
        {
            throw new IllegalArgumentException("Invalid password");
        }

        UserRecord rec_user = findUserInner(sessionHolder, principal, null, false);
        if (rec_user == null)
        {
            return false;
        }

        return changeUserPassword(sessionHolder, rec_user, newPassword);
    }

    //--//

    @Override
    public List<UserRecord> listUsers(SessionHolder sessionHolder) throws
                                                                   Exception
    {
        RecordHelper<UserRecord> helper = sessionHolder.createHelper(UserRecord.class);
        return QueryHelperWithCommonFields.filter(helper, null);
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
        RecordHelper<RoleRecord> helper = sessionHolder.createHelper(RoleRecord.class);
        return QueryHelperWithCommonFields.filter(helper, null);
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

    protected abstract UserRecord ensureUserRecord(SessionHolder holder,
                                                   String principal);

    protected abstract boolean verifyUserPassword(SessionHolder holder,
                                                  UserRecord user,
                                                  String password);

    protected abstract boolean changeUserPassword(SessionHolder holder,
                                                  UserRecord user,
                                                  String password);

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

        UserRecord rec;

        if (sysId == null)
        {
            rec = ensureUserRecord(holder, principal);
        }
        else
        {
            rec = holder.getEntityOrNull(UserRecord.class, sysId);
        }

        if (rec != null)
        {
            if (checkPassword)
            {
                if (StringUtils.isBlank(password))
                {
                    return null;
                }

                Boolean hit = authenticateWithCachedPassword(sysId, password);
                if (hit != null)
                {
                    if (!hit)
                    {
                        return null;
                    }
                }
                else
                {
                    if (!verifyUserPassword(holder, rec, password))
                    {
                        HashedPassword pwd = rec.getPassword();
                        if (pwd == null || !pwd.authenticate(password))
                        {
                            return null;
                        }
                    }

                    cacheHashOfValidPassword(sysId, password);
                }
            }

            rec.eagerInit();
        }

        return rec;
    }
}
