/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.Sets;
import com.optio3.cloud.client.SwaggerTypeReplacement;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import org.apache.commons.lang3.StringUtils;

@SwaggerTypeReplacement(targetElement = RecordIdentity.class, targetCollection = List.class)
public class TypedRecordIdentityList<T extends RecordWithCommonFields> extends ArrayList<TypedRecordIdentity<T>>
{
    public static <T extends RecordWithCommonFields> TypedRecordIdentityList<T> toList(Collection<T> records)
    {
        TypedRecordIdentityList<T> res = new TypedRecordIdentityList<>();
        for (T rec : records)
        {
            res.add(RecordIdentity.newTypedInstance(rec));
        }

        return res;
    }

    public static <T extends RecordWithCommonFields> boolean sameRecords(TypedRecordIdentityList<T> a,
                                                                         TypedRecordIdentityList<T> b)
    {
        if (a == null || b == null)
        {
            return a == b;
        }

        Set<String> setA = a.collectSysIds(Sets.newHashSet());
        Set<String> setB = b.collectSysIds(Sets.newHashSet());

        return setA.equals(setB);
    }

    public Set<String> collectSysIds(Set<String> ids)
    {
        for (RecordIdentity ri : this)
        {
            ids.add(ri.sysId);
        }

        return ids;
    }

    //--//

    public boolean refersTo(T rec)
    {
        return refersTo((ri) -> StringUtils.equals(ri.sysId, rec.getSysId()));
    }

    public boolean refersTo(Predicate<RecordIdentity> checker)
    {
        for (RecordIdentity ri : this)
        {
            if (checker.test(ri))
            {
                return true;
            }
        }

        return false;
    }
}
