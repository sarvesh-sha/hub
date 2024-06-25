/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digineous.model;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.protocol.model.WellKnownEquipmentClassOrCustom;

@Optio3IncludeInApiDefinitions
public class DigineousMachineLibrary
{
    @Optio3AutoTrim()
    public String id;

    @Optio3AutoTrim()
    public String name;

    //--//

    public WellKnownEquipmentClassOrCustom equipmentClass;

    public final List<String> deviceTemplates = Lists.newArrayList();
}
