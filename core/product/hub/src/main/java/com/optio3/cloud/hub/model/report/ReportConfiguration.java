/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.asset.graph.SharedAssetGraph;

public class ReportConfiguration
{
    public final List<SharedAssetGraph> sharedGraphs = Lists.newArrayList();

    public com.optio3.client.reporter.model.PaperFormat pdfFormat;

    public boolean landscape;

    public ReportLayoutBase container;

    public void setGraphs(Map<String, SharedAssetGraph> graphs)
    {
        if (graphs != null)
        {
            sharedGraphs.addAll(graphs.values());
        }
    }
}
