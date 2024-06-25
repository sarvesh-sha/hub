/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.optio3.serialization.Reflection;
import com.optio3.util.IdGenerator;
import com.optio3.util.TimeUtils;
import org.hibernate.HibernateException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * A helper class to add well-known fields to any Hibernate entity.
 * <p>
 * Entities extending this class can override the methods from the {@link Optio3Lifecycle} interface to adjust the state of the record before it gets passed to the database.
 * <p>
 * <b>IMPORTANT:</b> always call the superclass implementation when overriding the common methods.
 */
@MappedSuperclass
public abstract class RecordWithCommonFields implements Optio3Lifecycle
{
    private static final String                   c_CREATED_ON         = "createdOn";
    private static final String                   c_UPDATED_ON         = "updatedOn";
    private static final Reflection.FieldAccessor c_accessor_createdOn = new Reflection.FieldAccessor(RecordWithCommonFields.class, c_CREATED_ON);
    private static final Reflection.FieldAccessor c_accessor_updatedOn = new Reflection.FieldAccessor(RecordWithCommonFields.class, c_UPDATED_ON);

    //
    // Unfortunately, Hibernate checks for a valid ID before firing the PRE_INSERT event.
    // Because of that, we have to use a custom value generator to set the sys_id.
    //
    public static class IdGen implements IdentifierGenerator
    {
        @Override
        public Serializable generate(SharedSessionContractImplementor session,
                                     Object object) throws
                                                    HibernateException
        {
            RecordWithCommonFields rec = (RecordWithCommonFields) object;

            String id = rec.sysId;
            if (id == null)
            {
                if (rec.m_selectedSysId != null)
                {
                    id                  = rec.m_selectedSysId;
                    rec.m_selectedSysId = null;
                }
                else
                {
                    id = IdGenerator.newGuid();
                }
            }

            return id;
        }
    }

    @Id
    @GeneratedValue(generator = "optio3_id")
    @GenericGenerator(name = "optio3_id", strategy = "com.optio3.cloud.persistence.RecordWithCommonFields$IdGen")
    @Column(name = "sys_id", nullable = false)
    private String sysId;

    //
    // If you set the sysId directly, Hibernate thinks it's an update, not an insert.
    // So we store the id in this transient field and then when the Value Generator gets invoked,
    // it will return the desired value, instead of generating a new one.
    //
    @Transient
    private String m_selectedSysId;

    @Column(name = "sys_created_on", nullable = false)
    private ZonedDateTime createdOn;

    @Column(name = "sys_updated_on", nullable = false)
    private ZonedDateTime updatedOn;

    @Transient
    private boolean skipUpdatedOn;

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

    protected <T extends RecordWithCommonFields> boolean addToCollection(Set<T> set,
                                                                         T rec)
    {
        boolean added = set.add(rec);
        if (added)
        {
            ZonedDateTime now = TimeUtils.now();

            rec.setUpdatedOn(now);
            setUpdatedOn(now);
        }

        return added;
    }

    protected <T extends RecordWithCommonFields> boolean removeFromCollection(Set<T> set,
                                                                              T rec)
    {
        boolean removed = set.remove(rec);
        if (removed)
        {
            ZonedDateTime now = TimeUtils.now();

            rec.setUpdatedOn(now);
            setUpdatedOn(now);
        }

        return removed;
    }

    //--//

    public String getSysId()
    {
        return sysId;
    }

    public void setSysId(String value)
    {
        if (sysId != null)
        {
            if (sysId.equals(value))
            {
                return;
            }

            throw new HibernateException("Can't change record sys_id after it has been inserted in the database");
        }

        m_selectedSysId = value;
    }

    protected void forceSetSysId(String value)
    {
        if (sysId != null)
        {
            if (sysId.equals(value))
            {
                return;
            }

            throw new HibernateException("Can't change record sys_id after it has been inserted in the database");
        }

        m_selectedSysId = value;
        sysId           = value;
    }

    public static String getSysIdSafe(RecordWithCommonFields obj)
    {
        // If it's a proxy, using "getSysId()" would not trigger a DB access.
        return obj != null ? obj.getSysId() : null;
    }

    public boolean sameSysId(RecordWithCommonFields other)
    {
        if (other == null)
        {
            return false;
        }

        String thisId  = getSysId();
        String otherId = other.getSysId();

        if (thisId == null)
        {
            return false;
        }

        if (otherId == null)
        {
            return false;
        }

        return otherId.equals(thisId);
    }

    public boolean sameRecord(RecordLocator<?> loc)
    {
        if (loc == null)
        {
            return false;
        }

        if (loc.getEntityClass() != getClass())
        {
            return false;
        }

        return getSysId().equals(loc.getId());
    }

    //--//

    public static ZonedDateTime getCreatedOnSafe(RecordWithCommonFields obj,
                                                 boolean dontUnwrapProxy)
    {
        if (obj == null)
        {
            return null;
        }

        if (dontUnwrapProxy && !SessionHolder.isPropertyInitialized(obj, c_CREATED_ON))
        {
            return null;
        }

        return obj.getCreatedOn();
    }

    public ZonedDateTime getCreatedOn()
    {
        return createdOn;
    }

    public void setCreatedOn(ZonedDateTime value)
    {
        createdOn = value;
    }

    //--//

    public static ZonedDateTime getUpdatedOnSafe(RecordWithCommonFields obj,
                                                 boolean dontUnwrapProxy)
    {
        if (obj == null)
        {
            return null;
        }

        if (dontUnwrapProxy && !SessionHolder.isPropertyInitialized(obj, c_UPDATED_ON))
        {
            return null;
        }

        return obj.getUpdatedOn();
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
}
