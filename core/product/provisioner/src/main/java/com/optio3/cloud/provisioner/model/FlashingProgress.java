/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.provisioner.model;

public class FlashingProgress
{
    public FlashingStatus state;
    public String         failureReason;
    public long           imageSize;
    public long           imageOffset;
    public int            phase;
    public String         phaseName;
}
