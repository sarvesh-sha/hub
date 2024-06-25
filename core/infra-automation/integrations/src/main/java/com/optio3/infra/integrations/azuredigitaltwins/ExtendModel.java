/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azuredigitaltwins;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ExtendModel extends BaseModelWithId
{
    @JsonIgnore
    public InterfaceModel target;
}

