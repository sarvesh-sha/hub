/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.config;

import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.identity.UserGroup;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "USER_GROUP")
@Optio3TableInfo(externalId = "UserGroup", model = UserGroup.class, metamodel = UserGroupRecord_.class, metadata = UserGroupRecord.WellKnownMetadata.class)
public class UserGroupRecord extends RecordWithMetadata implements ModelMapperTarget<UserGroup, UserGroupRecord_>
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<Boolean> reachabilityGroup   = new MetadataField<>("reachabilityGroup", Boolean.class); // True if SYS.NOTIFICATION.UNREACHABLE has been created
        public static final MetadataField<Boolean> responsivenessGroup = new MetadataField<>("responsivenessGroup", Boolean.class); // True if SYS.NOTIFICATION.UNRESPONSIVE has been created
    }

    @NaturalId
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Optio3ControlNotifications(reason = "Report changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getRoles")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "USER_GROUP_ROLE", joinColumns = @JoinColumn(name = "UserGroupRecord_sys_id"), inverseJoinColumns = @JoinColumn(name = "roles_sys_id"))
    private List<RoleRecord> roles = Lists.newArrayList();

    @Optio3ControlNotifications(reason = "Report changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getSubGroups")
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "USER_GROUP_USER_GROUP", joinColumns = @JoinColumn(name = "UserGroupRecord_sys_id"), inverseJoinColumns = @JoinColumn(name = "subGroups_sys_id"))
    private List<UserGroupRecord> subGroups = Lists.newArrayList();

    @ManyToMany(mappedBy = "groups", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<UserRecord> members = Sets.newHashSet();

    //--//

    public UserGroupRecord()
    {
    }

    public void eagerInit()
    {
        eagerInit(Sets.newHashSet());
    }

    private void eagerInit(Set<UserGroupRecord> visited)
    {
        if (visited.add(this))
        {
            for (RoleRecord rec_role : getRoles())
            {
                // Just to load all the lazy-initialized values.
            }

            for (UserGroupRecord rec_userGroup : getSubGroups())
            {
                rec_userGroup.eagerInit(visited);
            }
        }
    }

    //--//

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<RoleRecord> getRoles()
    {
        return roles;
    }

    public List<UserGroupRecord> getSubGroups()
    {
        return subGroups;
    }

    public Set<UserGroupRecord> getGroupsClosure()
    {
        final Set<UserGroupRecord> set = Sets.newHashSet();

        computeGroupsClosure(set);

        return set;
    }

    private void computeGroupsClosure(Set<UserGroupRecord> visited)
    {
        if (visited.add(this))
        {
            for (UserGroupRecord subGroup : this.getSubGroups())
            {
                subGroup.computeGroupsClosure(visited);
            }
        }
    }

    public Set<UserRecord> getMembers()
    {
        return members;
    }

    //--//

    public boolean hasRole(String role)
    {
        return getRole(role) != null;
    }

    public RoleRecord getRole(String role)
    {
        for (UserGroupRecord rec_group : getGroupsClosure())
        {
            RoleRecord rec_role = RoleRecord.getRole(rec_group.getRoles(), role);
            if (rec_role != null)
            {
                return rec_role;
            }
        }

        return null;
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

    public static List<UserGroupRecord> getAll(RecordHelper<UserGroupRecord> helper)
    {
        return QueryHelperWithCommonFields.filter(helper, null);
    }

    public static TypedRecordIdentityList<UserGroupRecord> list(RecordHelper<UserGroupRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, UserGroupRecord_.name, true);
        });
    }

    public static List<UserGroupRecord> getBatch(RecordHelper<UserGroupRecord> helper,
                                                 List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static List<UserGroupRecord> getBatch(RecordHelper<UserGroupRecord> helper,
                                                 TypedRecordIdentityList<UserGroupRecord> ids)
    {
        List<UserGroupRecord> res = Lists.newArrayList();

        for (RecordIdentity ri : ids)
        {
            UserGroupRecord userGroup = helper.getOrNull(ri.sysId);
            if (userGroup == null)
            {
                throw Exceptions.newIllegalArgumentException("Invalid user group: %s", ri.sysId);
            }

            res.add(userGroup);
        }

        return res;
    }

    public static UserGroupRecord findByName(SessionHolder sessionHolder,
                                             String name)
    {
        return sessionHolder.byNaturalId(UserGroupRecord.class)
                            .using(UserGroupRecord_.name.getName(), name)
                            .load();
    }

    public static UserGroupRecord findByMetadata(SessionHolder sessionHolder,
                                                 MetadataField<Boolean> key)
    {
        return CollectionUtils.findFirst(getAll(sessionHolder.createHelper(UserGroupRecord.class)), (UserGroupRecord rec_group) -> rec_group.getMetadata(key));
    }
}
