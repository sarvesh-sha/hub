/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.serialization.SerializationTag;

public final class VtClose extends ConfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public List<Unsigned8> list_of_remote_vt_session_identifiers = Lists.newArrayList();
}
