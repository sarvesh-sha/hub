/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.time.ZonedDateTime;

import javax.persistence.Tuple;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public class RecordIdentity
{
    private Class<?> m_entityClass;

    @JsonIgnore
    public Class<?> getEntityClass()
    {
        return m_entityClass;
    }

    @JsonIgnore
    public void setEntityClass(Class<?> entityClass)
    {
        if (entityClass != null)
        {
            // In case the class is an Hibernate proxy, we have to look for the first real entity class.
            for (Class<?> clz = entityClass; ; clz = clz.getSuperclass())
            {
                if (clz == null)
                {
                    throw Exceptions.newRuntimeException("Missing @Optio3TableInfo on type '%s'", entityClass);
                }

                Optio3TableInfo anno = clz.getAnnotation(Optio3TableInfo.class);
                if (anno != null)
                {
                    entityClass = clz;
                    break;
                }
            }
        }

        m_entityClass = entityClass;
    }

    public String getTable()
    {
        if (m_entityClass == null)
        {
            return null;
        }

        Optio3TableInfo anno = m_entityClass.getAnnotation(Optio3TableInfo.class);
        if (anno == null)
        {
            throw Exceptions.newRuntimeException("Missing @Optio3TableInfo on type '%s'", m_entityClass);
        }

        return anno.externalId();
    }

    public void setTable(String table)
    {
        m_entityClass = RecordHelper.resolveEntityClass(table);
    }

    public String sysId;

    public ZonedDateTime lastUpdate;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setScore(Float val)
    {
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        return sameRecord(this, Reflection.as(o, RecordIdentity.class));
    }

    @Override
    public int hashCode()
    {
        return sysId != null ? sysId.hashCode() : 0;
    }

    //--//

    public static boolean isValid(RecordIdentity ri)
    {
        return ri != null && ri.sysId != null;
    }

    public static boolean sameRecord(RecordIdentity a,
                                     RecordIdentity b)
    {
        if (a == null || b == null)
        {
            return a == b;
        }

        if (!StringUtils.equals(a.sysId, b.sysId))
        {
            return false;
        }

        Class<?> aClz = a.m_entityClass;
        Class<?> bClz = b.m_entityClass;

        if (aClz == null || bClz == null)
        {
            // If the entity class is missing, assume a match.
            return true;
        }

        return aClz == bClz || Reflection.isSubclassOf(aClz, bClz) || Reflection.isSubclassOf(bClz, aClz);
    }

    //--//

    public static <T extends RecordWithCommonFields> TypedRecordIdentity<T> newInstance(RecordHelper<T> helper,
                                                                                        Tuple t,
                                                                                        int indexSysId,
                                                                                        int indexLastUpdate)
    {
        final String sysId = (String) t.get(indexSysId);
        if (sysId == null)
        {
            return null;
        }

        TypedRecordIdentity<T> ri = TypedRecordIdentity.newTypedInstance(helper.getEntityClass(), sysId);
        ri.lastUpdate = (ZonedDateTime) t.get(indexLastUpdate);
        return ri;
    }

    public static RecordIdentity newInstance(Class<?> clz,
                                             String sysId)
    {
        if (sysId == null)
        {
            return null;
        }

        RecordIdentity ri = new RecordIdentity();
        ri.m_entityClass = clz;
        ri.sysId         = sysId;
        return ri;
    }

    public static <T extends RecordWithCommonFields> TypedRecordIdentity<T> newTypedInstance(RecordHelper<T> helper,
                                                                                             String sysId)
    {
        return newTypedInstance(helper.getEntityClass(), sysId);
    }

    public static <T extends RecordWithCommonFields> TypedRecordIdentity<T> newTypedInstance(Class<T> clz,
                                                                                             String sysId)
    {
        if (sysId == null)
        {
            return null;
        }

        TypedRecordIdentity<T> ri = new TypedRecordIdentity<>();
        ri.setEntityClass(clz);
        ri.sysId = sysId;
        return ri;
    }

    public static <T extends RecordWithCommonFields> TypedRecordIdentity<T> newTypedInstance(RecordHelper<T> helper,
                                                                                             T rec)
    {
        return rec != null ? newTypedInstance(helper, rec.getSysId()) : null;
    }

    public static <T extends RecordWithCommonFields> TypedRecordIdentity<T> newTypedInstance(T rec)
    {
        if (rec == null)
        {
            return null;
        }

        @SuppressWarnings("unchecked") Class<T> clz = (Class<T>) rec.getClass();

        TypedRecordIdentity<T> ri = TypedRecordIdentity.newTypedInstance(clz, rec.getSysId());
        ri.lastUpdate = RecordWithCommonFields.getUpdatedOnSafe(rec, true); // If it's a proxy, don't set lastUpdate, it would cause the record to be fetched!
        return ri;
    }

    public static <T extends RecordWithCommonFields> TypedRecordIdentity<T> newTypedInstance(RecordLocator<T> rec)
    {
        if (rec == null)
        {
            return null;
        }

        TypedRecordIdentity<T> ri = TypedRecordIdentity.newTypedInstance(rec.getEntityClass(), rec.getIdRaw());
        ri.lastUpdate = rec.getLastUpdate();
        return ri;
    }
}
