/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

public enum StreamHelperNextAction
{
    Continue,
    Continue_Evict,
    Continue_Flush_Evict,
    Continue_Flush_Evict_Commit,
    Stop,
    Stop_Evict,
}
