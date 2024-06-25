/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.hub.model.asset.LogicalAsset;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "ASSET_LOGICAL")
@DynamicUpdate // Due to HHH-11506
@Indexed()
@Optio3TableInfo(externalId = "LogicalAsset", model = LogicalAsset.class, metamodel = LogicalAssetRecord_.class)
public class LogicalAssetRecord extends AssetRecord
{
    @Override
    public void assetPostCreate(SessionHolder sessionHolder)
    {
        // Nothing to do.
    }

    @Override
    protected void assetPostUpdateInner(SessionHolder sessionHolder)
    {
        // Nothing to do.
    }

    @Override
    protected boolean canRemoveChildren()
    {
        // TODO: do we store other things under a LogicalGroup record? For now, okay to delete recursively.
        return true;
    }
}
