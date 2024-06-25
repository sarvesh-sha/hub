/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs.output;

import com.optio3.cloud.model.scheduler.BaseBackgroundActivityProgress;

public class RegistryRefresh extends BaseBackgroundActivityProgress
{
    public int images;
    public int imagesAdded;
    public int imagesRemoved;

    public int tagsAdded;
    public int tagsRemoved;
}
