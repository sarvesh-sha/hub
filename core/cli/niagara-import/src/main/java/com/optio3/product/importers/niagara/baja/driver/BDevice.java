/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.driver;

import java.util.Collections;

import com.optio3.product.importers.niagara.baja.control.BControlPoint;
import com.optio3.product.importers.niagara.baja.sys.BComponent;

public abstract class BDevice extends BComponent
{
    public <TP extends BControlPoint, TE extends BAbstractProxyExt, TF extends BPointFolder> void enumeratePoints(Class<TP> pointClz,
                                                                                                                  Class<TE> pointExtClz,
                                                                                                                  Class<TF> folderClz,
                                                                                                                  PointEnumeration<TP, TE, TF> callback) throws
                                                                                                                                                         Exception
    {
        BPointDeviceExt points = getPoints();
        if (points != null)
        {
            points.enumerateWithFolders(pointClz, folderClz, Collections.emptyList(), (point, path) ->
            {
                if (pointExtClz.isInstance(point.proxyExt))
                {
                    callback.apply(point, pointExtClz.cast(point.proxyExt), path);
                }
            });
        }
    }

    protected abstract BPointDeviceExt getPoints();
}

