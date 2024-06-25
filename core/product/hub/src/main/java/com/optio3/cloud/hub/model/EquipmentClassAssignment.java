/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.List;

import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;

public class EquipmentClassAssignment
{
    public String regex;

    public String equipmentClassId;

    public boolean caseSensitive;

    public static EquipmentClassAssignment matchAssignments(EngineExecutionContext<?, ?> ctx,
                                                            List<EquipmentClassAssignment> assignments,
                                                            NormalizationEngineValueEquipment equipment)
    {
        if (equipment != null && assignments != null)
        {
            for (EquipmentClassAssignment assignment : assignments)
            {
                String regex = assignment.regex != null ? assignment.regex : "";
                boolean matches = ctx.compileRegex(regex, assignment.caseSensitive)
                                     .matcher(equipment.name)
                                     .find();

                if (matches)
                {
                    if (assignment.equipmentClassId != null)
                    {
                        equipment.equipmentClassId = assignment.equipmentClassId;
                    }
                    else
                    {
                        equipment.equipmentClassId = null;
                        equipment.setUnclassified  = true;
                    }

                    return assignment;
                }
            }
        }

        return null;
    }
}
