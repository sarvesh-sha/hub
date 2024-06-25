/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

public final class PostCommitNotificationState
{
    public PostCommitNotificationStateField[] fields;

    public PostCommitNotificationStateField findField(String name,
                                                      boolean onlyIfDirty)
    {
        for (PostCommitNotificationStateField field : fields)
        {
            if (field.name.equals(name))
            {
                if (onlyIfDirty && !field.dirty)
                {
                    continue;
                }

                return field;
            }
        }

        return null;
    }
}
