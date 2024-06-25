/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.metadata.normalization;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;

@JsonTypeName("BACnetBulkRenamingData")
public final class BACnetBulkRenamingData extends BACnetImportExportData
{
    public BACnetObjectIdentifier objectIdNew;
}
