/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.output;

import com.optio3.cloud.model.IEnumDescription;

public enum RegistryImageReleaseStatus implements IEnumDescription
{
    None("Regular Build", null),
    ReleaseCandidate("Release Candidate", null),
    Release("Release", null),
    LastGoodKnown("Last Good Known", null);

    private final String m_displayName;
    private final String m_description;

    RegistryImageReleaseStatus(String displayName,
                               String description)
    {
        m_displayName = displayName;
        m_description = description;
    }

    @Override
    public String getDisplayName()
    {
        return m_displayName;
    }

    @Override
    public String getDescription()
    {
        return m_description;
    }
}
