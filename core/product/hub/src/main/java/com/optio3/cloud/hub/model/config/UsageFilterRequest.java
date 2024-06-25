/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.config;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import org.apache.commons.lang3.StringUtils;

public class UsageFilterRequest
{
    public static class RawContents
    {
        public String sysId;
        public String value1;
        public String value2;
        public int    count;
    }

    //--//

    public final List<String> items = Lists.newArrayList();

    public boolean caseInsensitive;
    public int     maxResults;

    public boolean isIncluded(String payload)
    {
        if (caseInsensitive)
        {
            for (String item : items)
            {
                if (StringUtils.containsIgnoreCase(payload, item))
                {
                    return true;
                }
            }
        }
        else
        {
            for (String item : items)
            {
                if (StringUtils.contains(payload, item))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public <T extends RecordWithCommonFields> int analyzeRecords(SessionProvider sessionProvider,
                                                                 TypedRecordIdentityList<T> lst,
                                                                 Class<T> entityClass,
                                                                 Consumer<RawQueryHelper<T, RawContents>> addFields)
    {

        // Reuse the same instance, since we don't store the individual models.
        final var singletonModel = new RawContents();

        int limit = maxResults > 0 ? maxResults : 10;

        try (SessionHolder sessionHolder = sessionProvider.newReadOnlySession())
        {
            RawQueryHelper<T, RawContents> qh = new RawQueryHelper<>(sessionHolder, entityClass);

            qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);

            addFields.accept(qh);

            qh.stream(() -> singletonModel, (model) ->
            {
                boolean match = false;

                match |= (model.value1 != null && isIncluded(model.value1));
                match |= (model.value2 != null && isIncluded(model.value2));

                if (match)
                {
                    int total = model.count++;
                    if (total <= limit)
                    {
                        lst.add(TypedRecordIdentity.newTypedInstance(entityClass, model.sysId));
                    }
                }
            });
        }

        return singletonModel.count;
    }
}
