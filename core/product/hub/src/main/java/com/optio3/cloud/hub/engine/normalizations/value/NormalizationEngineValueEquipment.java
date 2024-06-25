/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.value;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.util.CollectionUtils;

@JsonTypeName("NormalizationEngineValueEquipment")
public class NormalizationEngineValueEquipment extends EngineValue
{
    public String  name;
    public String  equipmentClassId;
    public String  equipmentClassHint;
    public boolean setUnclassified;

    public List<NormalizationEngineValueLocation> locations;

    public List<NormalizationEngineValueEquipment> childEquipment;

    public static NormalizationEngineValueEquipment create(String name,
                                                           String equipmentClassId,
                                                           String hint)
    {
        NormalizationEngineValueEquipment res = new NormalizationEngineValueEquipment();
        res.name               = name;
        res.equipmentClassId   = equipmentClassId;
        res.equipmentClassHint = hint;
        return res;
    }

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return name;
    }

    public void addChild(NormalizationEngineValueEquipment child)
    {
        if (childEquipment == null)
        {
            childEquipment = Lists.newArrayList();
        }

        childEquipment.add(child);
    }

    public NormalizationEngineValueEquipment copy()
    {
        NormalizationEngineValueEquipment copy = create(name, equipmentClassId, equipmentClassHint);
        copy.setUnclassified = setUnclassified;
        return copy;
    }

    @JsonIgnore
    public NormalizationEngineValueEquipment getFirstLeaf()
    {
        NormalizationEngineValueEquipment leaf = this;
        NormalizationEngineValueEquipment nextLeaf;

        while ((nextLeaf = CollectionUtils.firstElement(leaf.childEquipment)) != null)
        {
            leaf = nextLeaf;
        }

        return leaf;
    }
}
