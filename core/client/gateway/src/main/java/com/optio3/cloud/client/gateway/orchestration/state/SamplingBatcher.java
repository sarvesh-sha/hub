/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

@FunctionalInterface
public interface SamplingBatcher<TDevice, TObject, TProperty>
{
    void addSample(TDevice deviceId,
                   TObject objectId,
                   TProperty propId);
}
