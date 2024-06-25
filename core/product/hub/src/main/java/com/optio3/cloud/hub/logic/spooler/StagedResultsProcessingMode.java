/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.spooler;

public enum StagedResultsProcessingMode
{
    // @formatter:off
    OnlyMarkedObjects(true , false, false),
    OnlyObjects      (false, true , false),
    OnlySamples      (false, false, true ),
    Everything       (false, true , true );
    // @formatter:on

    private final boolean m_onlyMarkedObjects;
    private final boolean m_includeObjects;
    private final boolean m_includeSamples;

    StagedResultsProcessingMode(boolean onlyMarkedObjects,
                                boolean includeObjects,
                                boolean includeSamples)
    {
        m_onlyMarkedObjects = onlyMarkedObjects;
        m_includeObjects    = includeObjects;
        m_includeSamples    = includeSamples;
    }

    public boolean shouldProcessOnlyMarkedObjects()
    {
        return m_onlyMarkedObjects;
    }

    public boolean shouldProcessObjects()
    {
        return m_includeObjects;
    }

    public boolean shouldProcessSamples()
    {
        return m_includeSamples;
    }
}
