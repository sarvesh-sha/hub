/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;

@JsonSubTypes({ @JsonSubTypes.Type(value = CalculatedEngineLoad.class),
                @JsonSubTypes.Type(value = DistanceTraveledWithMalfunction.class),
                @JsonSubTypes.Type(value = DtcStatus.class),
                @JsonSubTypes.Type(value = EngineCoolantTemperature.class),
                @JsonSubTypes.Type(value = EngineHours.class),
                @JsonSubTypes.Type(value = EngineOilTemperature.class),
                @JsonSubTypes.Type(value = EngineRPM.class),
                @JsonSubTypes.Type(value = FuelPressure.class),
                @JsonSubTypes.Type(value = FuelRailGaugePressure.class),
                @JsonSubTypes.Type(value = FuelSystemStatus.class),
                @JsonSubTypes.Type(value = IntakeAirTemperature.class),
                @JsonSubTypes.Type(value = IntakeManifoldAbsolutePressure.class),
                @JsonSubTypes.Type(value = MassAirFlowRate.class),
                @JsonSubTypes.Type(value = Odometer.class),
                @JsonSubTypes.Type(value = RunTimeSinceEngineStart.class),
                @JsonSubTypes.Type(value = SupportedPIDs.class),
                @JsonSubTypes.Type(value = ThrottlePosition.class),
                @JsonSubTypes.Type(value = TimeRunWithMalfunction.class),
                @JsonSubTypes.Type(value = TimingAdvance.class),
                @JsonSubTypes.Type(value = VehicleSpeed.class),
                @JsonSubTypes.Type(value = VIN.class) })
public abstract class BaseIso15765ObjectModel extends ObdiiObjectModel
{
}
