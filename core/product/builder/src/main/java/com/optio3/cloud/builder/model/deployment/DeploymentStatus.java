/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import com.optio3.cloud.model.IEnumDescription;

public enum DeploymentStatus implements IEnumDescription
{
    Initialized("Initialized", null),

    LoadingImage("Loading Image", null),
    LoadedImage("Loaded Image", null),

    Booting("Booting", null),
    Booted("Booted", null),
    BootFailed("BootFailed", null),

    Ready("Ready", null),
    Stopped("Stopped", null),

    Terminating("Terminating", null),
    Terminated("Terminated", null),

    Cancelling("Cancelling", null),
    Cancelled("Cancelled", null);

    private final String m_displayName;
    private final String m_description;

    DeploymentStatus(String displayName,
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
