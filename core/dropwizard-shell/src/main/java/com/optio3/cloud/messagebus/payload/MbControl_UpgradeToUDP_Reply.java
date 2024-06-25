/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.util.MonotonousTime;

@JsonTypeName("MbControlUpgradeToUDPReply") // No underscore in model name, due to Swagger issues.
public class MbControl_UpgradeToUDP_Reply extends MbControl_Reply
{
    public int port;

    //
    // Used in the unencrypted portion of the header, to detect stale sessions.
    //
    public short headerId;

    //
    // AES128 key for the shared portion of the frame.
    //
    public byte[] headerKey;

    //--//

    //
    // Identity used in the shared encryption part of the frame.
    //
    public long sessionId;

    //
    // AES128 key for the private portion of the frame.
    //
    public byte[] sessionKey;

    //
    // How many seconds the session key will be valid.
    //
    public int sessionValidity;

    //--//

    //
    // Identity used in the MessageBus transport
    //
    public String endpointId;
}
