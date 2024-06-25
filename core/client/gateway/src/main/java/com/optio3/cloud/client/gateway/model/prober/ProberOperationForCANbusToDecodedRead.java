/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;

@JsonTypeName("ProberOperationForCANbusToDecodedRead")
public class ProberOperationForCANbusToDecodedRead extends ProberOperationForCANbus
{
    @JsonTypeName("ProberOperationForCANbusToDecodedReadResults") // No underscore in model name, due to Swagger issues.
    public static class Results extends BaseResults
    {
        public List<ProberObjectCANbus> frames = Lists.newArrayList();
    }

    //--//

    public int samplingSeconds;
}
