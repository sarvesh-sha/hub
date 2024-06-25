/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.protocol.model.obdii.pgn.sys.BaseSysPgnObjectModel;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializablePiece;
import com.optio3.serialization.SerializationValueProcessor;

@JsonSubTypes({ @JsonSubTypes.Type(value = AdaptiveCruiseControl1.class),
                @JsonSubTypes.Type(value = Aftertreatment1AirControl1.class),
                @JsonSubTypes.Type(value = Aftertreatment1DieselExhaustFluidInformation1.class),
                @JsonSubTypes.Type(value = Aftertreatment1DieselExhaustFluidSupplyInformation.class),
                @JsonSubTypes.Type(value = Aftertreatment1DieselExhaustFluidTank1Information1.class),
                @JsonSubTypes.Type(value = Aftertreatment1DieselOxidationCatalyst1.class),
                @JsonSubTypes.Type(value = Aftertreatment1FuelControl1.class),
                @JsonSubTypes.Type(value = Aftertreatment1FuelControl2.class),
                @JsonSubTypes.Type(value = Aftertreatment1GasParameters.class),
                @JsonSubTypes.Type(value = Aftertreatment1IntakeGas1.class),
                @JsonSubTypes.Type(value = Aftertreatment1IntakeGas2.class),
                @JsonSubTypes.Type(value = Aftertreatment1IntermediateGas.class),
                @JsonSubTypes.Type(value = Aftertreatment1OutletGas1.class),
                @JsonSubTypes.Type(value = Aftertreatment1OutletGas2.class),
                @JsonSubTypes.Type(value = Aftertreatment1ScrDosingSystemInformation1.class),
                @JsonSubTypes.Type(value = Aftertreatment1ScrDosingSystemInformation2.class),
                @JsonSubTypes.Type(value = Aftertreatment1ScrExhaustGasTemperature1.class),
                @JsonSubTypes.Type(value = Aftertreatment1ScrExhaustGasTemperature2.class),
                @JsonSubTypes.Type(value = Aftertreatment1Service1.class),
                @JsonSubTypes.Type(value = Aftertreatment2IntakeGas.class),
                @JsonSubTypes.Type(value = Aftertreatment2IntermediateGas.class),
                @JsonSubTypes.Type(value = Aftertreatment2OutletGas.class),
                @JsonSubTypes.Type(value = AirSupplyPressure.class),
                @JsonSubTypes.Type(value = AmbientConditions1.class),
                @JsonSubTypes.Type(value = AmbientConditions2.class),
                @JsonSubTypes.Type(value = AntiTheftRequest.class),
                @JsonSubTypes.Type(value = AuxiliaryAnalogInformation.class),
                @JsonSubTypes.Type(value = Brakes1.class),
                @JsonSubTypes.Type(value = CabIlluminationMessage.class),
                @JsonSubTypes.Type(value = CabMessage1.class),
                @JsonSubTypes.Type(value = CabMessage2.class),
                @JsonSubTypes.Type(value = ColdStartAids.class),
                @JsonSubTypes.Type(value = CombinationVehicleWeight.class),
                @JsonSubTypes.Type(value = CruiseControlVehicleSpeed1.class),
                @JsonSubTypes.Type(value = CruiseControlVehicleSpeed2.class),
                @JsonSubTypes.Type(value = CruiseControlVehicleSpeed3.class),
                @JsonSubTypes.Type(value = CruiseControlVehicleSpeed4.class),
                @JsonSubTypes.Type(value = CruiseControlVehicleSpeedSetup.class),
                @JsonSubTypes.Type(value = DashDisplay1.class),
                @JsonSubTypes.Type(value = DieselParticulateFilterControl1.class),
                @JsonSubTypes.Type(value = DirectLampControlData1.class),
                @JsonSubTypes.Type(value = DirectLampControlData2.class),
                @JsonSubTypes.Type(value = ElectronicBrakeController1.class),
                @JsonSubTypes.Type(value = ElectronicBrakeController5.class),
                @JsonSubTypes.Type(value = ElectronicEngineController1.class),
                @JsonSubTypes.Type(value = ElectronicEngineController2.class),
                @JsonSubTypes.Type(value = ElectronicEngineController3.class),
                @JsonSubTypes.Type(value = ElectronicEngineController6.class),
                @JsonSubTypes.Type(value = ElectronicEngineController7.class),
                @JsonSubTypes.Type(value = ElectronicEngineController9.class),
                @JsonSubTypes.Type(value = ElectronicRetarderController1.class),
                @JsonSubTypes.Type(value = ElectronicRetarderController2.class),
                @JsonSubTypes.Type(value = ElectronicTransmissionController1.class),
                @JsonSubTypes.Type(value = ElectronicTransmissionController2.class),
                @JsonSubTypes.Type(value = ElectronicTransmissionController5.class),
                @JsonSubTypes.Type(value = ElectronicTransmissionController7.class),
                @JsonSubTypes.Type(value = ElectronicTransmissionController8.class),
                @JsonSubTypes.Type(value = EngineInformation1.class),
                @JsonSubTypes.Type(value = EngineInformation2.class),
                @JsonSubTypes.Type(value = EngineFluidLevel_Pressure1.class),
                @JsonSubTypes.Type(value = EngineFluidLevel_Pressure2.class),
                @JsonSubTypes.Type(value = EngineFluidLevel_Pressure3.class),
                @JsonSubTypes.Type(value = EngineFluidLevel_Pressure4.class),
                @JsonSubTypes.Type(value = EngineFuelInformation1.class),
                @JsonSubTypes.Type(value = EngineGasFlowRate.class),
                @JsonSubTypes.Type(value = EngineHours.class),
                @JsonSubTypes.Type(value = EngineOperatingInformation.class),
                @JsonSubTypes.Type(value = EngineStartControl.class),
                @JsonSubTypes.Type(value = EngineStateRequests.class),
                @JsonSubTypes.Type(value = EngineTemperature1.class),
                @JsonSubTypes.Type(value = EngineTemperature2.class),
                @JsonSubTypes.Type(value = EngineTemperature3.class),
                @JsonSubTypes.Type(value = EngineTemperature4.class),
                @JsonSubTypes.Type(value = EngineThrottle.class),
                @JsonSubTypes.Type(value = FanDrive1.class),
                @JsonSubTypes.Type(value = FuelConsumptionLiquid1.class),
                @JsonSubTypes.Type(value = FuelConsumptionLiquidHighResolution.class),
                @JsonSubTypes.Type(value = FuelEconomy1.class),
                @JsonSubTypes.Type(value = FuelEconomy2.class),
                @JsonSubTypes.Type(value = FuelInformation1.class),
                @JsonSubTypes.Type(value = HighResolutionVehicleDistance.class),
                @JsonSubTypes.Type(value = HighResolutionWheelSpeed.class),
                @JsonSubTypes.Type(value = IdleOperation.class),
                @JsonSubTypes.Type(value = IntakeExhaustConditions1.class),
                @JsonSubTypes.Type(value = IntakeExhaustConditions2.class),
                @JsonSubTypes.Type(value = LightingCommand.class),
                @JsonSubTypes.Type(value = OperatorIndicators.class),
                @JsonSubTypes.Type(value = OperatorsExternalLightControlsMessage.class),
                @JsonSubTypes.Type(value = OperatorWiperAndWasherControlsMessage.class),
                @JsonSubTypes.Type(value = PowerTakeoffDriveEngagement.class),
                @JsonSubTypes.Type(value = PowerTakeoffInformation.class),
                @JsonSubTypes.Type(value = Reset.class),
                @JsonSubTypes.Type(value = SensorElectricalPower1.class),
                @JsonSubTypes.Type(value = Shutdown.class),
                @JsonSubTypes.Type(value = TimeDate.class),
                @JsonSubTypes.Type(value = TransmissionConfiguration2.class),
                @JsonSubTypes.Type(value = TransmissionControl1.class),
                @JsonSubTypes.Type(value = TransmissionFluids1.class),
                @JsonSubTypes.Type(value = TransmissionFluids2.class),
                @JsonSubTypes.Type(value = Turbocharger.class),
                @JsonSubTypes.Type(value = TurbochargerInformation2.class),
                @JsonSubTypes.Type(value = TurbochargerInformation3.class),
                @JsonSubTypes.Type(value = TurbochargerInformation4.class),
                @JsonSubTypes.Type(value = TurbochargerInformation5.class),
                @JsonSubTypes.Type(value = TurbochargerInformation6.class),
                @JsonSubTypes.Type(value = TurbochargerWastegate.class),
                @JsonSubTypes.Type(value = VehicleDistance.class),
                @JsonSubTypes.Type(value = VehicleDynamicStabilityControl1.class),
                @JsonSubTypes.Type(value = VehicleDynamicStabilityControl2.class),
                @JsonSubTypes.Type(value = VehicleElectricalPower1.class),
                @JsonSubTypes.Type(value = VehicleHours.class),
                @JsonSubTypes.Type(value = VehiclePosition1.class),
                @JsonSubTypes.Type(value = VehicleSpeedLimiter.class),
                @JsonSubTypes.Type(value = WheelSpeedInformation.class),
                @JsonSubTypes.Type(value = BaseSysPgnObjectModel.class) })
public abstract class BasePgnObjectModel extends ObdiiObjectModel
{
    public boolean shouldIncludeObject()
    {
        PgnMessageType anno = this.getClass()
                                  .getAnnotation(PgnMessageType.class);
        if (anno != null)
        {
            if (anno.ignoreWhenReceived())
            {
                return false;
            }
        }

        return true;
    }

    public int extractPgn()
    {
        PgnMessageType anno = this.getClass()
                                  .getAnnotation(PgnMessageType.class);
        return anno != null ? anno.pgn() : null;
    }

    public boolean shouldIncludeProperty(String prop)
    {
        return getField(prop) != null;
    }

    //--//

    public static class DetectMissingUnsigned8 extends SerializationValueProcessor
    {
        @Override
        public Optional<Object> handle(SerializablePiece piece,
                                       Object value)
        {
            if ((long) value == 0xFF)
            {
                return Optional.empty();
            }

            return null;
        }
    }

    //--//

    public static class DetectNAN extends SerializationValueProcessor
    {
        @Override
        public Optional<Object> handle(SerializablePiece piece,
                                       Object value)
        {
            double value2 = Reflection.coerceNumber(value, Double.class);

            if (value2 <= piece.preProcessorLowerRange)
            {
                return Optional.of(Double.NaN);
            }

            if (value2 >= piece.preProcessorUpperRange)
            {
                return Optional.of(Double.NaN);
            }

            return null;
        }
    }

    //--//

    public static class DetectMissing extends SerializationValueProcessor
    {
        @Override
        public Optional<Object> handle(SerializablePiece piece,
                                       Object value)
        {
            double value2 = Reflection.coerceNumber(value, Double.class);

            if (value2 <= piece.preProcessorLowerRange)
            {
                return Optional.empty();
            }

            if (value2 >= piece.preProcessorUpperRange)
            {
                return Optional.empty();
            }

            return null;
        }
    }

    //--//

    public static class DetectOutOfRange extends SerializationValueProcessor
    {
        @Override
        public Optional<Object> handle(SerializablePiece piece,
                                       Object value)
        {
            double value2 = Reflection.coerceNumber(value, Double.class);

            if (value2 <= piece.preProcessorLowerRange)
            {
                return computeMarkerValue(piece);
            }

            if (value2 >= piece.preProcessorUpperRange)
            {
                return computeMarkerValue(piece);
            }

            return null;
        }

        private static Optional<Object> computeMarkerValue(SerializablePiece piece)
        {
            long value = -100;

            if (piece.scaling != null)
            {
                value += (long) Math.floor(piece.scaling.postScalingOffset());
            }

            return Optional.of(value);
        }
    }
}
