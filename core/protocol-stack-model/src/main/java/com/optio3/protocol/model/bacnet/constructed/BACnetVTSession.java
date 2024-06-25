/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetVTSession extends Sequence
{
    @SerializationTag(number = 0)
    public Unsigned8 local_vt_session_id;

    @SerializationTag(number = 1)
    public Unsigned8 remote_vt_session_id;

    @SerializationTag(number = 2)
    public BACnetAddress remote_vt_address;
}
