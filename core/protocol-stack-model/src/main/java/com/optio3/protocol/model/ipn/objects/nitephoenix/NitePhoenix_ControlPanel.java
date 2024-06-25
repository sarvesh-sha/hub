/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.nitephoenix;

import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.can.CanExtendedMessageType;
import com.optio3.protocol.model.ipn.IpnObjectsState;
import com.optio3.protocol.model.ipn.objects.nitephoenix.enums.OperatingMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;
import com.optio3.util.BoxingUtils;

@JsonTypeName("Ipn:Can:NitePhoenix_ControlPanel")
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0x3A, sourceAddress = 0x3A, littleEndian = true)
public class NitePhoenix_ControlPanel extends BaseNitePhoenixModel
{
    private static final Supplier<Map<Integer, Float>> c_temperatureSettingLookup = Suppliers.memoize(() ->
                                                                                                      {
                                                                                                          Map<Integer, Float> map = Maps.newHashMap();

                                                                                                          map.put(0, 15.5f);
                                                                                                          map.put(1, 16.5f);
                                                                                                          map.put(2, 17.5f);
                                                                                                          map.put(3, 18.5f);
                                                                                                          map.put(4, 19.5f);
                                                                                                          map.put(5, 20f);
                                                                                                          map.put(6, 20.5f);
                                                                                                          map.put(7, 21f);
                                                                                                          map.put(8, 21.5f);
                                                                                                          map.put(9, 22f);
                                                                                                          map.put(10, 22.5f);
                                                                                                          map.put(11, 23f);
                                                                                                          map.put(12, 23.5f);
                                                                                                          map.put(13, 24f);
                                                                                                          map.put(14, 24.5f);
                                                                                                          map.put(15, 25f);
                                                                                                          map.put(16, 25.5f);
                                                                                                          map.put(17, 26.5f);
                                                                                                          map.put(18, 27.5f);
                                                                                                          map.put(19, 28.5f);
                                                                                                          map.put(20, 29.5f);

                                                                                                          return map;
                                                                                                      });

    @JsonIgnore
    @SerializationTag(number = 0, width = 8)
    public int reserved1;

    @FieldModelDescription(description = "Operating Mode", units = EngineeringUnits.enumerated, debounceSeconds = 15)
    @SerializationTag(number = 1, width = 2)
    public OperatingMode operatingMode;

    @JsonIgnore
    @SerializationTag(number = 2, width = 8)
    public int reserved3;

    @JsonIgnore
    @SerializationTag(number = 3, width = 8)
    public int reserved4;

    @JsonIgnore
    @SerializationTag(number = 4, width = 8)
    public int temperatureSettingRaw;

    @FieldModelDescription(description = "Temperature Setting", pointClass = WellKnownPointClass.HvacSetTemperature, units = EngineeringUnits.degrees_celsius)
    public float temperatureSetting;

    @FieldModelDescription(description = "Requested Fan Speed", units = EngineeringUnits.ticks, debounceSeconds = 15)
    @SerializationTag(number = 5, width = 8)
    public int requestedFanSpeed;

    @FieldModelDescription(description = "Sleeper temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15)
    @SerializationTag(number = 6, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.11, postScalingOffset = -28.92, assumeUnsigned = true) })
    public float sleeperTemperature;

    //--//

    @Override
    public String extractBaseId()
    {
        return "NitePhoenix_ControlPanel";
    }

    @Override
    public void postProcess(IpnObjectsState state,
                            BaseNitePhoenixModel previousValue)
    {
        super.postProcess(state, previousValue);

        Float val = c_temperatureSettingLookup.get()
                                              .get(temperatureSettingRaw);

        temperatureSetting = BoxingUtils.get(val, Float.NaN);
    }
}
