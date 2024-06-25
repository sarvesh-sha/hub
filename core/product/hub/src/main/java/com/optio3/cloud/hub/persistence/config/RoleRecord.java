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
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.identity.Role;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "ROLE")
@Optio3TableInfo(externalId = "Role", model = Role.class, metamodel = RoleRecord_.class)
public class RoleRecord extends RecordWithCommonFields implements ModelMapperTarget<Role, RoleRecord_>
{
    @NaturalId
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "add_allowed", nullable = false)
    private boolean addAllowed;

    @Column(name = "remove_allowed", nullable = false)
    private boolean removeAllowed;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private Set<UserRecord> members = Sets.newHashSet();

    public RoleRecord()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public boolean isAddAllowed()
    {
        return addAllowed;
    }

    public void setAddAllowed(boolean addAllowed)
    {
        this.addAllowed = addAllowed;
    }

    public boolean isRemoveAllowed()
    {
        return removeAllowed;
    }

    public void setRemoveAllowed(boolean removeAllowed)
    {
        this.removeAllowed = removeAllowed;
    }

    public Set<UserRecord> getMembers()
    {
        return members;
    }

    //--//

    public boolean isRole(String role)
    {
        return StringUtils.equals(name, role);
    }

    //--//

    public static RoleRecord getRole(List<RoleRecord> roles,
                                     String role)
    {
        for (RoleRecord rec_role : roles)
        {
            if (rec_role.getName()
                        .equals(role))
            {
                return rec_role;
            }
        }

        return null;
    }

    //--//

    public static List<RoleRecord> getAll(RecordHelper<RoleRecord> helper)
    {
        return QueryHelperWithCommonFields.filter(helper, null);
    }

    public static TypedRecordIdentityList<RoleRecord> list(RecordHelper<RoleRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, RoleRecord_.name, true);
        });
    }

    public static List<RoleRecord> getBatch(RecordHelper<RoleRecord> helper,
                                            List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static List<RoleRecord> getBatch(RecordHelper<RoleRecord> helper,
                                            TypedRecordIdentityList<RoleRecord> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static RoleRecord findByName(SessionHolder sessionHolder,
                                        String name)
    {
        return sessionHolder.byNaturalId(RoleRecord.class)
                            .using(RoleRecord_.name.getName(), name)
                            .load();
    }
}
