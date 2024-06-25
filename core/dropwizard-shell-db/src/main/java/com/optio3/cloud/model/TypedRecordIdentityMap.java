/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.util.HashMap;

import com.optio3.cloud.persistence.RecordWithCommonFields;

public class TypedRecordIdentityMap<K, T extends RecordWithCommonFields> extends HashMap<K, RecordIdentity>
{
}
