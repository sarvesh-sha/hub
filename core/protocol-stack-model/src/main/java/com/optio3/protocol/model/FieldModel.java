/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import org.apache.commons.lang3.StringUtils;

public class FieldModel
{
    public final String name;
    public final Type   type;

    public final FieldTemporalResolution temporalResolution;
    public final int                     digitsOfPrecision;
    public final boolean                 fixedRate;
    public final boolean                 flushOnChange;
    public final double                  minimumDelta;
    public final int                     debounceSeconds;
    public final boolean                 indexed;
    public final FieldChangeMode         stickyMode;
    public final Class<?>                desiredTypeForSamples;
    public final double                  noValueMarker;

    private final String              m_description;
    private final EngineeringUnits    m_units;
    private final List<String>        m_enumeratedValues;
    private final WellKnownPointClass m_pointClass;
    private final int                 m_pointClassPriority;
    private final List<String>        m_pointTags;

    public FieldModel(String name,
                      Type type,
                      FieldModelDescription anno,
                      List<String> enumeratedValues)
    {
        this.name                  = name;
        this.type                  = type;
        this.temporalResolution    = anno.temporalResolution();
        this.digitsOfPrecision     = anno.digitsOfPrecision();
        this.fixedRate             = anno.fixedRate();
        this.flushOnChange         = anno.flushOnChange();
        this.minimumDelta          = anno.minimumDelta();
        this.debounceSeconds       = anno.debounceSeconds();
        this.indexed               = anno.indexed();
        this.stickyMode            = anno.stickyMode();
        this.desiredTypeForSamples = anno.desiredTypeForSamples();
        this.noValueMarker         = anno.noValueMarker();

        m_description        = anno.description();
        m_units              = anno.units();
        m_enumeratedValues   = Collections.unmodifiableList(enumeratedValues);
        m_pointClass         = anno.pointClass();
        m_pointClassPriority = anno.pointClassPriority();
        m_pointTags          = asCommaSeparated(anno.pointTags());
    }

    public boolean isNumeric(BaseObjectModel obj)
    {
        return getEnumeratedValues(obj).isEmpty() && Reflection.getDescriptor(type) != null;
    }

    public String getDescription(BaseObjectModel obj)
    {
        return obj == null ? m_description : obj.overrideDescription(this, m_description);
    }

    public EngineeringUnits getUnits(BaseObjectModel obj)
    {
        EngineeringUnits units = BoxingUtils.get(m_units, EngineeringUnits.no_units);

        if (obj != null)
        {
            units = obj.overrideUnits(this, units);
            units = BoxingUtils.get(units, EngineeringUnits.no_units);
        }

        return units;
    }

    public List<String> getEnumeratedValues(BaseObjectModel obj)
    {
        return obj == null ? m_enumeratedValues : obj.overrideEnumeratedValues(this, m_enumeratedValues);
    }

    public WellKnownPointClassOrCustom getPointClass(BaseObjectModel obj)
    {
        var pc = m_pointClass.asWrapped();

        if (obj != null)
        {
            pc = obj.overridePointClass(this, pc);
        }

        return pc;
    }

    public int getPointClassPriority(BaseObjectModel obj)
    {
        return obj == null ? m_pointClassPriority : obj.overridePointClassPriority(this, m_pointClassPriority);
    }

    public List<String> getPointTags(BaseObjectModel obj)
    {
        return obj == null ? m_pointTags : obj.overridePointTags(this, m_pointTags);
    }

    private static List<String> asCommaSeparated(String pointTags)
    {
        if (StringUtils.isBlank(pointTags))
        {
            return Collections.emptyList();
        }

        String[] parts = StringUtils.split(pointTags, ',');
        return Collections.unmodifiableList(Lists.newArrayList(parts));
    }
}
