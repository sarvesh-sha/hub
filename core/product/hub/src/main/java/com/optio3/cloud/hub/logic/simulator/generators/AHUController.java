/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import com.optio3.cloud.hub.model.normalization.NormalizationEquipment;

public class AHUController extends DeviceGenerator
{
    public AHUController(String name,
                         int networkNumber,
                         int instanceId)
    {
        super(networkNumber, instanceId, name, "HVAC Controller VLC-444", "Alerton", 18);
        NormalizationEquipment equip = new NormalizationEquipment();
        equip.name             = name;
        equip.equipmentClassId = "1";
        registerGenerator(equip, this);
        registerGenerator(equip, new DischargeAirTemperatureSensor("Discharge Air Temperature", 1, 60.0, 88.0, 95.0)).configureSkipSamples(.01, 10);
        registerGenerator(equip, new SpaceTemperatureSensor("Outside Air Temperature", 2, 40, 55, 50));
        registerGenerator(equip, new CO2Sensor("Outside Air CO2 Level", 3, 200, 400));
        registerGenerator(equip, new TemperatureSetpoint("Occupied Heating Setpoint", 4, 65.0, 65));
        registerGenerator(equip, new TemperatureSetpoint("Occupied Cooling Setpoint", 5, 75.0, 75.0));
        registerGenerator(equip, new TemperatureSetpoint("Unoccupied Heating Setpoint", 6, 60.0, 60.0));
        registerGenerator(equip, new TemperatureSetpoint("Unoccupied Cooling Setpoint", 7, 80.0, 80.0));
        registerGenerator(equip, new HeatingCoolingSignal("Cooling Signal from VAVs", 8, 20, 20));
        registerGenerator(equip, new HeatingCoolingSignal("Heating Signal from VAVs", 9, 40, 80));
        registerGenerator(equip, new BinaryStatus("Economizer Status", 10));
    }
}
