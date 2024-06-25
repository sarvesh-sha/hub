/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

import org.hibernate.event.spi.EventSource;

public interface IRemoveNotification
{
    void onRemove(EventSource eventSource);
}
