/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import com.optio3.cloud.hub.model.normalization.NormalizationEquipment;

public class VAVController extends DeviceGenerator
{
    private boolean m_hasMultipleEquipment;

    public VAVController(String name,
                         String namePrefix,
                         int networkNumber,
                         int instanceNumber,
                         int numVavs)
    {
        super(networkNumber, instanceNumber, name, "HVAC Controller VLC-444", "Alerton", 18);

        m_hasMultipleEquipment = numVavs > 1;
        if (m_hasMultipleEquipment)
        {
            registerGenerator(null, this);
        }

        for (int i = 0; i < numVavs; i++)
        {
            String                 equipmentName = namePrefix + (instanceNumber + i);
            NormalizationEquipment equipment     = new NormalizationEquipment();
            equipment.name             = equipmentName;
            equipment.equipmentClassId = "21";

            if (!m_hasMultipleEquipment)
            {
                registerGenerator(equipment, this);
            }

            registerGenerator(equipment, new BinaryStatus(getPointName(equipmentName, "Discharge Fan Status"), i)).configureSkipSamples(.01, 10);

            registerGenerator(equipment, new SpaceTemperatureSensor(getPointName(equipmentName, "Space Temperature"), 6 * i, 67.0, 70, 65)).configureSkipSamples(.01, 10)
                                                                                                                                           .configureFaults(.1, 10);

            registerGenerator(equipment, new DischargeAirTemperatureSensor(getPointName(equipmentName, "Discharge Air Temperature"), 6 * i + 1, 60.0, 88.0, 95.0)).configureSkipSamples(.01, 10);

            registerGenerator(equipment, new TemperatureSetpoint(getPointName(equipmentName, "Heat Setpoint"), 6 * i + 2, 65.0, 70.0));
            registerGenerator(equipment, new CO2Sensor(getPointName(equipmentName, "Zone CO2"), 6 * i + 3, 400, 800));
            registerGenerator(equipment, new HeatingCoolingSignal(getPointName(equipmentName, "Cooling Signal"), 6 * i + 4, 5, 5));
            registerGenerator(equipment, new HeatingCoolingSignal(getPointName(equipmentName, "Heating Signal"), 6 * i + 5, 20, 60));
        }
    }

    private String getPointName(String equipmentName,
                                String pointName)
    {
        if (!m_hasMultipleEquipment)
        {
            return pointName;
        }

        return String.format("%s - %s", equipmentName, pointName);
    }
}
