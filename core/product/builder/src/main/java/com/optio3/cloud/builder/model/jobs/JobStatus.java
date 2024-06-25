/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.jobs;

public enum JobStatus
{
    CREATED,
    INITIALIZED,
    EXECUTING,
    UNKNOWNTOKEN,
    COMPLETED,
    FAILED,
    CANCELLING,
    CANCELLED,
    TIMEOUT
}
