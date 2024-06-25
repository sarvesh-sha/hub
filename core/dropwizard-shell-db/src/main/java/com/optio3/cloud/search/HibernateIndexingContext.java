/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.search;

import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.collection.Memoizer;

public abstract class HibernateIndexingContext
{
    public abstract void initialize(AbstractApplicationWithDatabase<?> app,
                                    String databaseId,
                                    Memoizer memoizer);
}
