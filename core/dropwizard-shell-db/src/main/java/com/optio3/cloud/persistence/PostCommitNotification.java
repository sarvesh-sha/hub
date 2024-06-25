/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.io.Serializable;

@FunctionalInterface
public interface PostCommitNotification
{
    void accept(String databaseId,
                Object entity,
                Serializable id,
                PostCommitNotificationReason action,
                PostCommitNotificationState state) throws
                                                   Exception;
}
