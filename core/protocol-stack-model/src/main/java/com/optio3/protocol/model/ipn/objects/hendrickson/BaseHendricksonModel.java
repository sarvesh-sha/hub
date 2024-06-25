/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.hendrickson;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.can.CanObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = Hendrickson_Watchman.class) })
public abstract class BaseHendricksonModel extends CanObjectModel
{
}
