/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.logic;

import com.optio3.cloud.model.IEnumDescription;

public enum BackgroundActivityStatus implements IEnumDescription
{
    // @formatter:off
    ACTIVE                  ("Active"                , null, false, null     ),
    ACTIVE_BUT_CANCELLING   ("Active (cancelling)"   , null, false, ACTIVE   ),
    PAUSED                  ("Paused"                , null, false, null     ),
    PAUSED_BUT_CANCELLING   ("Paused (cancelling)"   , null, false, PAUSED   ),
    WAITING                 ("Waiting"               , null, false, null     ),
    WAITING_BUT_CANCELLING  ("Waiting (cancelling)"  , null, false, WAITING  ),
    SLEEPING                ("Sleeping"              , null, false, null     ),
    SLEEPING_BUT_CANCELLIN  ("Sleeping (cancelling)" , null, false, SLEEPING ),
    EXECUTING               ("Executing"             , null, false, null     ),
    EXECUTING_BUT_CANCELLING("Executing (cancelling)", null, false, EXECUTING),
    CANCELLED               ("Cancelled"             , null, true , null     ),
    COMPLETED               ("Completed"             , null, true , null     ),
    FAILED                  ("Failed"                , null, true , null     );
    // @formatter:on

    private final String                   m_displayName;
    private final String                   m_description;
    private final boolean                  m_done;
    private final BackgroundActivityStatus m_normalState;
    private       BackgroundActivityStatus m_cancellingState;

    BackgroundActivityStatus(String displayName,
                             String description,
                             boolean done,
                             BackgroundActivityStatus normalState)
    {
        m_displayName = displayName;
        m_description = description;

        m_done = done;
        m_normalState = normalState;

        if (normalState != null)
        {
            normalState.m_cancellingState = this;
        }
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

    //--//

    public boolean isDone()
    {
        return m_done;
    }

    public boolean isCancelling()
    {
        return m_normalState != null;
    }

    public BackgroundActivityStatus getNormalStatus()
    {
        return m_normalState != null ? m_normalState : this;
    }

    public BackgroundActivityStatus getCancellingStatus()
    {
        return m_cancellingState;
    }
}
