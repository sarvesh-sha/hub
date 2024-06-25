/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

interface ISamplingConfig<TKey, TValue>
{
    TKey getKey();

    default TValue asValue()
    {
        @SuppressWarnings("unchecked") TValue res = (TValue) this;
        return res;
    }
}
