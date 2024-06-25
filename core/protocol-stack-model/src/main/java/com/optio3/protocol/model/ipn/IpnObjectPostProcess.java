/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn;

public interface IpnObjectPostProcess<T extends IpnObjectModel>
{
    void postProcess(IpnObjectsState state,
                     T previousValue);
}
