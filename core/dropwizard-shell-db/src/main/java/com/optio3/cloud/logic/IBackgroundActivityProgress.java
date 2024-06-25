/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.logic;

import java.io.IOException;
import java.io.InputStream;

import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;
import com.optio3.cloud.persistence.SessionHolder;

public interface IBackgroundActivityProgress<R extends BaseBackgroundActivityProgress>
{
    R fetchProgress(SessionHolder sessionHolder,
                    boolean detailed);

    void generateStream() throws
                          IOException;

    InputStream streamContents() throws
                                 IOException;
}
