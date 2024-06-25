/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.state;

import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler.GenericValue;
import com.optio3.cloud.persistence.RecordLocator;

public class DockerImageState extends GenericValue
{
    public RecordLocator<RepositoryCheckoutRecord> rec;

    public String imageTemporaryTag;
}
