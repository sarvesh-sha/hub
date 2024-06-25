/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.provisioner.model;

public enum FlashingStatus
{
    NoBoard,
    DownloadingFirmware,
    AlreadyFlashing,
    Flashing,
    Done,
    Failed,
}
