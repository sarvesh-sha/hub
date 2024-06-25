/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.acl;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.optio3.cloud.hub.model.AccessControlListPolicy;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;

@MappedSuperclass
public abstract class RecordWithAccessControlList extends RecordWithCommonFields
{
    //
    // Unfortunately, we can't use a reference field, because this is a mapped superclass, Hibernate would create the foreign key constraint only on the first table it encounters...
    //
    @Column(name = "sys_acl", nullable = true)
    private Integer sysAcl;

    @Column(name = "sys_acl_effective", nullable = true)
    private Integer sysAclEffective;

    //--//

    public AccessControlListPolicy getAccessControlList(SessionHolder sessionHolder)
    {
        return AccessControlListRecord.fetchPolicy(sessionHolder, sysAcl);
    }

    public AccessControlListPolicy getEffectiveAccessControlList(SessionHolder sessionHolder)
    {
        return AccessControlListRecord.fetchPolicy(sessionHolder, sysAclEffective);
    }

    public void setAccessControlList(SessionHolder sessionHolder,
                                     AccessControlListPolicy policy)
    {
        Integer acl = AccessControlListRecord.ensurePolicy(sessionHolder, policy);

        if (!Objects.equals(sysAcl, acl))
        {
            AccessControlListPolicy policyEffective = computeEffectiveAccessControlList(sessionHolder, policy);
            Integer                 aclEffective    = AccessControlListRecord.ensurePolicy(sessionHolder, policyEffective);

            if (!Objects.equals(sysAclEffective, aclEffective))
            {
                sysAclEffective = aclEffective;
            }

            sysAcl = acl;
        }
    }

    protected abstract AccessControlListPolicy computeEffectiveAccessControlList(SessionHolder sessionHolder,
                                                                                 AccessControlListPolicy policy);

    @Override
    public void onPreDelete(SessionHolder sessionHolder)
    {
        super.onPreDelete(sessionHolder);

        // TODO: mark ACL records as touched.
    }
}
