/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.persistence.config.RoleRecord;
import com.optio3.cloud.hub.persistence.config.UserGroupRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;

public class DeliveryOptions
{
    public static class Resolver
    {
        private final Map<String, Set<String>> m_lookupRoles  = Maps.newHashMap();
        private final Map<String, Set<String>> m_lookupGroups = Maps.newHashMap();
        private final SessionProvider          m_sessionProvider;

        //--//

        public Resolver(SessionProvider sessionProvider)
        {
            m_sessionProvider = sessionProvider;
        }

        public Set<String> resolve(DeliveryOptions d)
        {
            Set<String> resolvedUsers = Sets.newHashSet();

            resolveUsers(d.users, resolvedUsers);
            resolveGroups(d.groups, resolvedUsers);
            resolveRoles(d.roles, resolvedUsers);

            return resolvedUsers;
        }

        private void resolveUsers(TypedRecordIdentityList<UserRecord> users,
                                  Set<String> resolvedUsers)
        {
            if (users != null)
            {
                for (TypedRecordIdentity<UserRecord> user : users)
                {
                    if (user != null && user.sysId != null)
                    {
                        resolvedUsers.add(user.sysId);
                    }
                }
            }
        }

        private void resolveRoles(TypedRecordIdentityList<RoleRecord> roles,
                                  Set<String> resolvedUsers)
        {
            TypedRecordIdentityList<RoleRecord> missed = new TypedRecordIdentityList<>();

            if (roles != null)
            {
                for (TypedRecordIdentity<RoleRecord> role : roles)
                {
                    if (role != null && role.sysId != null)
                    {
                        Set<String> usersInRole = m_lookupRoles.get(role.sysId);
                        if (usersInRole == null)
                        {
                            missed.add(role);
                        }
                        else
                        {
                            resolvedUsers.addAll(usersInRole);
                        }
                    }
                }
            }

            if (!missed.isEmpty())
            {
                try (SessionHolder subHolder = m_sessionProvider.newReadOnlySession())
                {
                    RecordHelper<RoleRecord> helper = subHolder.createHelper(RoleRecord.class);

                    for (TypedRecordIdentity<RoleRecord> role : missed)
                    {
                        Set<String> usersInRole = Sets.newHashSet();

                        RoleRecord rec_role = TypedRecordIdentity.getOrNull(helper, role);
                        if (rec_role != null)
                        {
                            for (UserRecord rec_user : rec_role.getMembers())
                            {
                                usersInRole.add(rec_user.getSysId());
                            }
                        }

                        m_lookupRoles.put(role.sysId, usersInRole);

                        resolvedUsers.addAll(usersInRole);
                    }
                }
            }
        }

        private void resolveGroups(TypedRecordIdentityList<UserGroupRecord> groups,
                                   Set<String> resolvedUsers)
        {
            TypedRecordIdentityList<UserGroupRecord> missed = new TypedRecordIdentityList<>();

            if (groups != null)
            {
                for (TypedRecordIdentity<UserGroupRecord> group : groups)
                {
                    if (group != null && group.sysId != null)
                    {
                        Set<String> usersInGroup = m_lookupGroups.get(group.sysId);
                        if (usersInGroup == null)
                        {
                            missed.add(group);
                        }
                        else
                        {
                            resolvedUsers.addAll(usersInGroup);
                        }
                    }
                }
            }

            if (!missed.isEmpty())
            {
                try (SessionHolder subHolder = m_sessionProvider.newReadOnlySession())
                {
                    RecordHelper<UserGroupRecord> helper = subHolder.createHelper(UserGroupRecord.class);

                    for (TypedRecordIdentity<UserGroupRecord> group : missed)
                    {
                        Set<String> usersInGroup = Sets.newHashSet();

                        UserGroupRecord rec_group = TypedRecordIdentity.getOrNull(helper, group);
                        if (rec_group != null)
                        {
                            for (UserGroupRecord rec_groupClosure : rec_group.getGroupsClosure())
                            {
                                for (UserRecord rec_user : rec_groupClosure.getMembers())
                                {
                                    usersInGroup.add(rec_user.getSysId());
                                }
                            }
                        }

                        m_lookupGroups.put(group.sysId, usersInGroup);

                        resolvedUsers.addAll(usersInGroup);
                    }
                }
            }
        }

        public List<UserRecord> collectUsers(SessionHolder sessionHolder,
                                             Set<String> resolvedUsers)
        {
            List<UserRecord> allUsers = Lists.newArrayList();

            if (resolvedUsers != null)
            {
                for (String resolvedUser : resolvedUsers)
                {
                    UserRecord rec_user = sessionHolder.getEntityOrNull(UserRecord.class, resolvedUser);
                    if (rec_user != null)
                    {
                        allUsers.add(rec_user);
                    }
                }
            }

            return allUsers;
        }
    }

    //--//

    public final TypedRecordIdentityList<UserRecord> users = new TypedRecordIdentityList<>();

    public final TypedRecordIdentityList<UserGroupRecord> groups = new TypedRecordIdentityList<>();

    public final TypedRecordIdentityList<RoleRecord> roles = new TypedRecordIdentityList<>();
}
