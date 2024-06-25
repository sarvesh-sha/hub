/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digineous.model;

import java.util.List;

import com.optio3.cloud.annotation.Optio3AutoTrim;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.WellKnownPointClassOrCustom;
import com.optio3.protocol.model.ipn.objects.digineous.BaseDigineousModel;
import com.optio3.protocol.model.ipn.objects.digineous.Digineous_AnalogSignal;
import com.optio3.protocol.model.ipn.objects.digineous.Digineous_StatusSignal;

public class DigineousPointLibrary
{
    @Optio3AutoTrim()
    public String identifier;

    @Optio3AutoTrim()
    public String description;

    public WellKnownPointClassOrCustom pointClass;
    public List<String>                tags;
    public EngineeringUnits            units;
    public float                       lowInputRange   = -1000.0f;
    public float                       highInputRange  = +1000.0f;
    public float                       lowOutputRange  = -1000.0f;
    public float                       highOutputRange = +1000.0f;
    public boolean                     enabled;

    public Digineous_AnalogSignal buildAnalog(float val)
    {
        Digineous_AnalogSignal obj = new Digineous_AnalogSignal();

        float slope = (highOutputRange - lowOutputRange) / (highInputRange - lowInputRange);

        obj.value = (val - lowInputRange) * slope + lowOutputRange;

        return obj;
    }

    public BaseDigineousModel buildDigital(boolean val)
    {
        Digineous_StatusSignal obj = new Digineous_StatusSignal();
        obj.active = val;
        return obj;
    }
}
