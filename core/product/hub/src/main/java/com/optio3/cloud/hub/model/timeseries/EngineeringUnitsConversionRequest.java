/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;

public class EngineeringUnitsConversionRequest
{
    public EngineeringUnitsFactors convertFrom;
    public EngineeringUnitsFactors convertTo;
    public double                  value;

    //--//

    public EngineeringUnitsConversionResponse convert()
    {
        EngineeringUnitsConversionResponse response = new EngineeringUnitsConversionResponse();
        response.value = EngineeringUnits.convert(value, convertFrom, convertTo);
        if (convertFrom == null || convertTo == null)
        {
            response.units = convertFrom;
        }
        else
        {
            response.units = convertTo;
        }

        return response;
    }
}
