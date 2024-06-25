/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.model.IEnumDescription;
import org.apache.commons.lang3.StringUtils;

public enum WellKnownEquipmentClass implements IEnumDescription
{
    // @formatter:off
    None              (0     , null                  ),

    Deployment        (0x1000, "Deployment"          ),
    Tractor           (0x1001, "Tractor"             ),
    Trailer           (0x1002, "Trailer"             ),
    Microgrid         (0x1003, "Microgrid"           ),
    Substation        (0x1004, "Substation"          ),
    Machine           (0x1005, "Machine"             ),
    RTU               (0x1006, "RTU"                 ),

    GPS               (0x2001, "GPS unit"            ),
    ChargeController  (0x2002, "Charge Controller"   ),
    Liftgate          (0x2003, "Liftgate"            ),
    HVAC              (0x2004, "HVAC unit"           ),
    OnBoardDiagnostics(0x2005, "On-board Diagnostics"),
    NoIdle            (0x2006, "NoIdle unit"         ),
    Sensor            (0x2007, "Sensor"              ),
    SensorDiagnostics (0x2008, "Sensor Diagnostics"  ),
    SensorAlerts      (0x2009, "Sensor Alerts"       ),
    SensorEvents      (0x2010, "Sensor Events"       ),
    LevelSensor       (0x2011, "Level Sensor"        ),
    Accelerometer     (0x2012, "Accelerometer"       ),
    Vibration         (0x2013, "Vibration"           ),
    SensorSecondary   (0x2014, "Secondary Sensor"    ),
    SensorTertiary    (0x2015, "Tertiary Sensor"     ),
    TireSensor        (0x2016, "Tire Sensor"         ),
    TemperatureSensor (0x2017, "Temperature Sensor"  ),
    HumiditySensor    (0x2018, "Humidity Sensor"     ),
    VoltageSensor     (0x2019, "Voltage Sensor"      ),
    CurrentSensor     (0x201A, "Current Sensor"      ),
    SensorCluster     (0x201B, "Sensor Cluster"      ),
    SmartLock         (0x201C, "Smart Lock"          ),
    AssetTag          (0x201D, "Asset Tag"           );
    // @formatter:on

    private final int                             m_id;
    private final String                          m_description;
    private final WellKnownEquipmentClassOrCustom m_wrapped;

    WellKnownEquipmentClass(int id,
                            String description)
    {
        m_id          = id;
        m_description = description;
        m_wrapped     = new WellKnownEquipmentClassOrCustom(this, 0);
    }

    public static WellKnownEquipmentClass parse(String name)
    {
        for (WellKnownEquipmentClass t : values())
        {
            if (StringUtils.equals(t.name(), name))
            {
                return t;
            }

            if (StringUtils.equals(Integer.toString(t.m_id), name))
            {
                return t;
            }
        }

        return null;
    }

    public static boolean isValid(WellKnownEquipmentClass clz)
    {
        return clz != null && clz != None;
    }

    @JsonIgnore
    public WellKnownEquipmentClassOrCustom asWrapped()
    {
        return m_wrapped;
    }

    public int getId()
    {
        return m_id;
    }

    @Override
    public String getDisplayName()
    {
        return name();
    }

    @Override
    public String getDescription()
    {
        return m_description;
    }
}
