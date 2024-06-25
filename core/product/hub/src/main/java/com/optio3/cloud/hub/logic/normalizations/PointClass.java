/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.Resources;
import org.apache.commons.lang3.StringUtils;

public class PointClass
{
    public boolean             disabled;
    public boolean             ignorePointIfMatched;
    public int                 id;
    public String              pointClassName;
    public String              pointClassDescription;
    public PointClassType      type;
    public int                 unitId;
    public String              unitDescription;
    public EngineeringUnits    unit;
    public int                 kindId;
    public String              kindDescription;
    public String              hints;
    public String              aliasPointClassId;
    public String              azureDigitalTwin;
    public WellKnownPointClass wellKnown;
    public List<String>        tags;

    private String m_idAsString;

    @JsonIgnore
    public String idAsString()
    {
        if (m_idAsString == null)
        {
            m_idAsString = Integer.toString(id);
        }

        return m_idAsString;
    }

    //--//

    public static int compare(PointClass a,
                              PointClass b,
                              boolean ascending)
    {
        int diff = 0;

        if (a != null && b != null)
        {
            diff = StringUtils.compareIgnoreCase(a.pointClassName, b.pointClassName);
        }
        else if (a != null)
        {
            diff = -1;
        }
        else if (b != null)
        {
            diff = 1;
        }

        return ascending ? diff : -diff;
    }

    public static List<PointClass> load() throws
                                          Exception
    {
        try (InputStream stream = Resources.openResourceAsStream(EquipmentClass.class, "normalization/McKinstryPointClass.json"))
        {
            return ObjectMappers.SkipNullsCaseInsensitive.readValue(stream, new TypeReference<List<PointClass>>()
            {
            });
        }
    }

    public static void assignUnits(List<PointClass> pointClasses,
                                   UnitsClassifier unitsClassifier) throws
                                                                    Exception
    {
        for (PointClass pt : pointClasses)
        {
            if (pt.unitDescription != null)
            {
                NormalizationScore.Context<EngineeringUnits> topScore = unitsClassifier.scoreTop(pt.unitDescription);
                if (topScore != null)
                {
                    pt.unit = topScore.context;
                }
            }
        }
    }

    //--//

    public static Function<PointClass, Boolean> filterBasedOnObjectType(BACnetObjectType type)
    {
        return (pc) ->
        {
            // If we don't know type, we cannot classify
            if (type == null)
            {
                return false;
            }

            switch (type)
            {
                case analog_input:
                    switch (pc.type)
                    {
                        case Value:
                        case Analytic:
                            return true;
                    }
                    break;

                case analog_output:
                case analog_value:
                    switch (pc.type)
                    {
                        case Setpoint:
                        case Value:
                        case Command:
                        case Analytic:
                            return true;
                    }
                    break;

                case binary_input:
                case multi_state_input:
                    switch (pc.type)
                    {
                        case Status:
                        case Analytic:
                            return true;
                    }
                    break;

                case binary_output:
                case binary_value:
                case multi_state_output:
                case multi_state_value:
                    switch (pc.type)
                    {
                        case Status:
                        case Command:
                        case Analytic:
                            return true;
                    }
                    break;
            }

            return false;
        };
    }

    public static BiConsumer<PointClass, NormalizationScore> boostScoreOnUnitsMatch(EngineeringUnits units)
    {
        return (pc, score) ->
        {
            if (pc.unit != null && units != null && units != EngineeringUnits.no_units)
            {
                if (pc.unit == units || pc.unit.isEquivalent(units))
                {
                    score.positiveScore += 4.0;
                }
                else
                {
                    score.negativeScore -= 8.0;
                }
            }
        };
    }
}
