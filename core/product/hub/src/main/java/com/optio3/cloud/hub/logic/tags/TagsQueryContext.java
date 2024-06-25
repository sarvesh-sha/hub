/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.tags;

import java.util.BitSet;
import java.util.Set;
import java.util.function.Function;

import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

public interface TagsQueryContext
{
    int getNumberOrRecords();

    default BitSet lookupTag(String tag)
    {
        return lookupTag(tag, null);
    }

    BitSet lookupTag(String tag,
                     String value);

    BitSet lookupAsset(String sysId);

    Set<TypedRecordIdentity<? extends AssetRecord>> lookupAssets(BitSet mask);

    TagsStreamNextAction streamAssets(BitSet mask,
                                      Function<TypedRecordIdentity<? extends AssetRecord>, TagsStreamNextAction> callback);
}
