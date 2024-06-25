/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.epower;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.config.I2CSensor_SHT3x;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;

@JsonTypeName("InstanceConfigurationForEPower_Amazon")
public class InstanceConfigurationForEPower_Amazon extends InstanceConfigurationForEPower
{
    @Override
    public boolean fixupAutoConfig(ProtocolConfigForIpn cfg)
    {
        boolean modified = false;

        if (cfg.i2cSensors.isEmpty())
        {
            var cab = new I2CSensor_SHT3x();
            cab.bus              = 0;
            cab.samplingPeriod   = 10;
            cab.averagingSamples = 10;
            cab.equipmentClass   = WellKnownEquipmentClass.SensorCluster.asWrapped();
            cab.instanceSelector = "Cab";
            cfg.i2cSensors.add(cab);

            var frontCargo = new I2CSensor_SHT3x();
            frontCargo.bus              = 1;
            frontCargo.samplingPeriod   = 10;
            frontCargo.averagingSamples = 10;
            frontCargo.equipmentClass   = WellKnownEquipmentClass.SensorCluster.asWrapped();
            frontCargo.instanceSelector = "Front Cargo";
            cfg.i2cSensors.add(frontCargo);

            var backCargo = new I2CSensor_SHT3x();
            backCargo.bus              = 2;
            backCargo.samplingPeriod   = 10;
            backCargo.averagingSamples = 10;
            backCargo.equipmentClass   = WellKnownEquipmentClass.SensorCluster.asWrapped();
            backCargo.instanceSelector = "Back Cargo";
            cfg.i2cSensors.add(backCargo);

            var ambient = new I2CSensor_SHT3x();
            ambient.bus              = 3;
            ambient.samplingPeriod   = 10;
            ambient.averagingSamples = 10;
            ambient.equipmentClass   = WellKnownEquipmentClass.SensorCluster.asWrapped();
            ambient.instanceSelector = "Ambient";
            cfg.i2cSensors.add(ambient);

            modified = true;
        }

        return modified;
    }
}
