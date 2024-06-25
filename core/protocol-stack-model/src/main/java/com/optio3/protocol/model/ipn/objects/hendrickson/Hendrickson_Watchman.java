/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.hendrickson;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownEquipmentClass;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.can.CanExtendedMessageType;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelBalanceHealth;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelBearingStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelConnectionStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelElectricalStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelLeakStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelPressureStatus;
import com.optio3.protocol.model.ipn.objects.hendrickson.enums.WheelTemperatureStatus;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializablePiece;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.SerializationValueProcessor;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("Ipn:Can:Hendrickson_Watchman")
@CanExtendedMessageType(priority = 6, dataPage = false, pduFormat = 0xFF, destinationAddress = 0xEB, sourceAddress = 0xCE, littleEndian = false)
public class Hendrickson_Watchman extends BaseHendricksonModel
{
    @JsonIgnore
    @SerializationTag(number = 1, width = 4, bitOffset = 0)
    public int wheelPosition;

    @JsonIgnore
    @SerializationTag(number = 1, width = 4, bitOffset = 4)
    public int wheelRow;

    @FieldModelDescription(description = "Wheel temperature", pointClass = WellKnownPointClass.SensorTemperature, units = EngineeringUnits.degrees_celsius)
    @SerializationTag(number = 2, width = 8, preProcessor = DetectTemperature.class, scaling = { @SerializationScaling(scalingFactor = 1.0, postScalingOffset = -52, assumeUnsigned = true) })
    public float wheelTemperature;

    @FieldModelDescription(description = "Wheel temperature status", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Temperature", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 3, width = 3, bitOffset = 0)
    public WheelTemperatureStatus wheelTemperatureStatus;

    @FieldModelDescription(description = "Wheel bearing status", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Bearing", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 3, width = 2, bitOffset = 3)
    public WheelBearingStatus wheelBearingHealth;

    @FieldModelDescription(description = "Wheel balance health", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Balance", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 3, width = 3, bitOffset = 5)
    public WheelBalanceHealth wheelBalanceHealth;

    @FieldModelDescription(description = "Wheel pressure status - Port A", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Pressure,PortA", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 4, width = 3, bitOffset = 0)
    public WheelPressureStatus wheelPressureStatusPortA;

    @FieldModelDescription(description = "Wheel pressure status - Port B", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Pressure,PortB", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 4, width = 3, bitOffset = 3)
    public WheelPressureStatus wheelPressureStatusPortB;

    @FieldModelDescription(description = "Wheel connection status", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Connection", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 4, width = 2, bitOffset = 6)
    public WheelConnectionStatus wheelConnectionStatus;

    @FieldModelDescription(description = "Wheel leak status - Port A", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Leak,PortA", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 5, width = 3, bitOffset = 0)
    public WheelLeakStatus wheelLeakStatusPortA;

    @FieldModelDescription(description = "Wheel leak status - Port B", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Leak,PortB", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 5, width = 3, bitOffset = 3)
    public WheelLeakStatus wheelLeakStatusPortB;

    @FieldModelDescription(description = "Wheel electrical status", pointClass = WellKnownPointClass.SensorStatus, pointTags = "Electrical", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 5, width = 2, bitOffset = 6)
    public WheelElectricalStatus wheelElectricalStatus;

    @FieldModelDescription(description = "Wheel pressure - Port A", pointClass = WellKnownPointClass.SensorPressure, pointTags = "PortA", units = EngineeringUnits.pounds_force_per_square_inch)
    @SerializationTag(number = 6, width = 8, preProcessor = DetectPressure.class, scaling = { @SerializationScaling(scalingFactor = 0.7977076, postScalingOffset = -0.7977076, assumeUnsigned = true) })
    public float wheelPressurePortA;

    @FieldModelDescription(description = "Wheel pressure - Port B", pointClass = WellKnownPointClass.SensorPressure, pointTags = "PortB", units = EngineeringUnits.pounds_force_per_square_inch)
    @SerializationTag(number = 7, width = 8, preProcessor = DetectPressure.class, scaling = { @SerializationScaling(scalingFactor = 0.7977076, postScalingOffset = -0.7977076, assumeUnsigned = true) })
    public float wheelPressurePortB;

    //--//

    public static class DetectTemperature extends SerializationValueProcessor
    {
        @Override
        public Optional<Object> handle(SerializablePiece piece,
                                       Object value)
        {
            double value2 = Reflection.coerceNumber(value, Double.class);

            if (value2 <= 2 || value2 >= 0xF0)
            {
                return Optional.of(Double.NaN);
            }

            return null;
        }
    }

    public static class DetectPressure extends SerializationValueProcessor
    {
        @Override
        public Optional<Object> handle(SerializablePiece piece,
                                       Object value)
        {
            double value2 = Reflection.coerceNumber(value, Double.class);

            if (value2 < 1 || value2 > 0xFD)
            {
                return Optional.of(Double.NaN);
            }

            if (value2 == 0xFD)
            {
                return Optional.of(0);
            }

            return null;
        }
    }

    //--//

    @Override
    protected BaseObjectModel createEmptyCopy()
    {
        Hendrickson_Watchman copy = (Hendrickson_Watchman) super.createEmptyCopy();
        copy.wheelPosition = wheelPosition;
        copy.wheelRow      = wheelRow;
        return copy;
    }

    @Override
    public String extractBaseId()
    {
        return "Hendrickson_Watchman";
    }

    @Override
    public String extractUnitId()
    {
        return String.format("%d#%d", wheelRow, wheelPosition);
    }

    @Override
    public boolean parseId(String id)
    {
        final String baseId = extractBaseId();
        if (StringUtils.startsWith(id, baseId))
        {
            String[] parts = StringUtils.split(id, '/');
            if (parts.length == 2 && StringUtils.equals(baseId, parts[0]))
            {
                try
                {
                    String[] parts2 = StringUtils.split(parts[1], '#');
                    wheelRow      = Integer.parseInt(parts2[0]);
                    wheelPosition = Integer.parseInt(parts2[1]);
                    return true;
                }
                catch (NumberFormatException e)
                {
                    // Not a valid id.
                }
            }
        }

        return false;
    }

    //--//

    @Override
    public boolean shouldIgnoreSample()
    {
        return wheelConnectionStatus == null || wheelConnectionStatus != WheelConnectionStatus.Ok;
    }

    @Override
    public boolean shouldCommitReachabilityChange(boolean isReachable,
                                                  ZonedDateTime lastReachable)
    {
        return false; // Unfortunately, Watchman is not always powered on.
    }

    @Override
    public void fillClassificationDetails(ClassificationDetails detailsForGroup,
                                          ClassificationDetails detailsForParent,
                                          ClassificationDetails detailsForPoint)
    {
        detailsForParent.equipmentClass   = WellKnownEquipmentClass.TireSensor.asWrapped();
        detailsForParent.instanceSelector = String.format("Row %d / Position %d", wheelRow, wheelPosition);
    }
}
