/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.product.importers.niagara.baja.driver;

import java.util.List;

import com.optio3.product.importers.niagara.baja.control.BControlPoint;

@FunctionalInterface
public interface PointEnumeration<TP extends BControlPoint, TE extends BAbstractProxyExt, TF extends BPointFolder>
{
    void apply(TP point,
               TE proxy,
               List<TF> path);
}
