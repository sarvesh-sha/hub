/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.acl;

import java.time.ZonedDateTime;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Tuple;
import javax.persistence.criteria.Root;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.AccessControlList;
import com.optio3.cloud.hub.model.AccessControlListPolicy;
import com.optio3.cloud.persistence.AbstractSelectHelper;
import com.optio3.cloud.persistence.InterceptorState;
import com.optio3.cloud.persistence.Optio3Lifecycle;
import com.optio3.cloud.persistence.PersistAsJsonHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Encryption;
import com.optio3.util.TimeUtils;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "ACL", indexes = { @Index(columnList = "policy_hash") })
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "Acl", model = AccessControlList.class, metamodel = AccessControlListRecord_.class)
public class AccessControlListRecord implements Optio3Lifecycle
{
    private static final Reflection.FieldAccessor c_accessor_createdOn = new Reflection.FieldAccessor(AccessControlListRecord.class, "createdOn");
    private static final Reflection.FieldAccessor c_accessor_updatedOn = new Reflection.FieldAccessor(AccessControlListRecord.class, "updatedOn");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sys_seq", updatable = false, nullable = false)
    private int sysSeq;

    @Column(name = "sys_created_on", nullable = false)
    private ZonedDateTime createdOn;

    @Column(name = "sys_updated_on", nullable = false)
    private ZonedDateTime updatedOn;

    @Transient
    private boolean skipUpdatedOn;

    //--//

    @Lob
    @Column(name = "policy", nullable = false)
    private byte[] policy;

    @Column(name = "policy_hash", nullable = false, length = 20, columnDefinition = "BINARY(20)")
    private byte[] policyHash;

    @Transient
    private final PersistAsJsonHelper<byte[], AccessControlListPolicy> m_policyParser = new PersistAsJsonHelper<>(() -> policy, (val) ->
    {
        policy     = val;
        policyHash = Encryption.computeSha1(val);
    }, byte[].class, AccessControlListPolicy.class, ObjectMappers.SkipNulls);

    //--//

    @Override
    public void onSave(InterceptorState interceptorState)
    {
        Object oldCreatedOn = interceptorState.getValue(RecordWithCommonFields_.createdOn);
        if (oldCreatedOn == null)
        {
            ZonedDateTime now = TimeUtils.now();
            interceptorState.setValue(RecordWithCommonFields_.createdOn, now);
            interceptorState.setValue(RecordWithCommonFields_.updatedOn, now);

            //
            // Update the fields as well.
            // We need to use reflection, or Hibernate will intercept the field write and mark the entity as dirty.
            //
            c_accessor_createdOn.set(this, now);
            c_accessor_updatedOn.set(this, now);
        }
    }

    @Override
    public void onLoad(InterceptorState interceptorState)
    {
    }

    @Override
    public void onFlushDirty(InterceptorState interceptorState)
    {
        if (!skipUpdatedOn && !interceptorState.hasChanged(RecordWithCommonFields_.updatedOn))
        {
            ZonedDateTime now = TimeUtils.now();
            interceptorState.setValue(RecordWithCommonFields_.updatedOn, now);

            //
            // Update the field as well.
            // We need to use reflection, or Hibernate will intercept the field write and mark the entity as dirty.
            //
            c_accessor_updatedOn.set(this, now);
        }

        skipUpdatedOn = false;
    }

    @Override
    public void onPreDelete(SessionHolder sessionHolder)
    {
    }

    @Override
    public void onDelete(InterceptorState interceptorState)
    {
    }

    @Override
    public void onEviction()
    {
    }

    //--//

    public int getSysSeq()
    {
        return sysSeq;
    }

    public ZonedDateTime getCreatedOn()
    {
        return createdOn;
    }

    public void setCreatedOn(ZonedDateTime value)
    {
        createdOn = value;
    }

    public ZonedDateTime getUpdatedOn()
    {
        return updatedOn;
    }

    public void setUpdatedOn(ZonedDateTime value)
    {
        updatedOn = value;
    }

    public void dontRefreshUpdatedOn()
    {
        skipUpdatedOn = true;
    }

    //--//

    public byte[] getPolicyHash()
    {
        return policyHash;
    }

    public AccessControlListPolicy getPolicy()
    {
        return m_policyParser.get();
    }

    public boolean setPolicy(AccessControlListPolicy policy)
    {
        return m_policyParser.set(policy);
    }

    //--//

    static class AclJoinHelper<T, C extends AccessControlListRecord> extends AbstractSelectHelper<T, C>
    {
        public final Root<C> root;

        public AclJoinHelper(RecordHelper<C> helper,
                             Class<T> clz)
        {
            super(helper, clz);

            root = cq.from(helper.getEntityClass());
        }
    }

    public static Integer ensurePolicy(SessionHolder sessionHolder,
                                       AccessControlListPolicy policy)
    {
        if (policy == null)
        {
            return null;
        }

        AccessControlListRecord rec_new = new AccessControlListRecord();
        rec_new.setPolicy(policy);

        RecordHelper<AccessControlListRecord> helper = sessionHolder.createHelper(AccessControlListRecord.class);

        AclJoinHelper<Tuple, AccessControlListRecord> jh = new AclJoinHelper<>(helper, Tuple.class);

        jh.cq.multiselect(jh.root.get(AccessControlListRecord_.sysSeq), jh.root.get(AccessControlListRecord_.policy));

        jh.addWhereClauseWithEqual(jh.root, AccessControlListRecord_.policyHash, rec_new.policyHash);

        for (Tuple t : jh.list(1))
        {
            final byte[] policyRaw = (byte[]) t.get(1);

            if (Arrays.equals(policyRaw, rec_new.policy))
            {
                return (Integer) t.get(0);
            }
        }

        sessionHolder.persistEntity(rec_new);

        return rec_new.getSysSeq();
    }

    public static AccessControlListPolicy fetchPolicy(SessionHolder sessionHolder,
                                                      Integer seq)
    {
        if (seq == null)
        {
            return null;
        }

        AccessControlListRecord rec = sessionHolder.getEntityOrNull(AccessControlListRecord.class, seq);
        return rec != null ? rec.getPolicy() : null;
    }
}
