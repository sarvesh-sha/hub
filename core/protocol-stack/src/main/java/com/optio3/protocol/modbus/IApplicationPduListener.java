/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus;

import com.optio3.protocol.modbus.pdu.ApplicationPDU;

public interface IApplicationPduListener
{
    void processResponse(int deviceIdentifier,
                         ApplicationPDU.Response res);
}
