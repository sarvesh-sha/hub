# coding: utf-8

"""
Copyright (C) 2017-2018, Optio3, Inc. All Rights Reserved.

Proprietary & Confidential Information.

Optio3 Hub APIs
APIs and Definitions for the Optio3 Hub product.

OpenAPI spec version: 1.0.0


NOTE: This class is auto generated by the swagger code generator program.
https://github.com/swagger-api/swagger-codegen.git
Do not edit the class manually.
"""


from pprint import pformat
from six import iteritems
import re

class WellKnownPointClassOrCustom(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, known=None, custom=None):
        """
        WellKnownPointClassOrCustom - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'known': 'str',
            'custom': 'int'
        }

        self.attribute_map = {
            'known': 'known',
            'custom': 'custom'
        }

        self._known = known
        self._custom = custom


    @property
    def known(self):
        """
        Gets the known of this WellKnownPointClassOrCustom.

        :return: The known of this WellKnownPointClassOrCustom.
        :rtype: str
        """
        return self._known

    @known.setter
    def known(self, known):
        """
        Sets the known of this WellKnownPointClassOrCustom.

        :param known: The known of this WellKnownPointClassOrCustom.
        :type: str
        """
        allowed_values = ["None", "Log", "Ignored", "LocationLongitude", "LocationLatitude", "LocationSpeed", "LocationAltitude", "LocationHeading", "AccelerationX", "AccelerationY", "AccelerationZ", "Acceleration", "VelocityX", "VelocityY", "VelocityZ", "Velocity", "ArrayVoltage", "ArrayCurrent", "ArrayPower", "BatteryVoltage", "BatteryCurrent", "BatteryPower", "BatteryStateOfCharge", "BatteryTemperature", "ExternalVoltage1", "ExternalVoltage2", "LoadVoltage", "LoadCurrent", "LoadPower", "LoadVoltAmpere", "LoadPowerReactive", "LoadPowerFactor", "LoadEnergy", "LoadEnergyReactive", "ChargingStatus", "TotalCharge", "TotalDischarge", "HeatsinkTemperature", "FaultCode", "FaultCodeCharging", "FaultCodeDischarging", "CounterResettable", "CounterNonResettable", "CommandOpen", "CommandClose", "CommandLift", "CommandLower", "CommandTiltUp", "CommandTiltDown", "CommandSlideIn", "CommandSlideOut", "DigitalOutput", "DigitalInput", "MotorSolenoid", "HvacTemperature", "HvacSetTemperature", "HvacCompressorSpeed", "HvacOperatingMode", "HvacStateOfCharge", "HvacStateOfHealth", "NoIdleState", "NoIdleSupplyVoltage", "NoIdleOemVoltage", "NoIdleParkNeutralVoltage", "NoIdleParkingBrakeVoltage", "NoIdleShorelineDetectionVoltage", "NoIdleEmergencyLightsVoltage", "NoIdleDischargeCurrent", "NoIdleAlternatorCurrent", "NoIdleRelays", "NoIdleIgnitionSignal", "NoIdleParkSignal", "NoIdleParkingBrakeSignal", "NoIdleHoodClosedSignal", "NoIdleEmergencyLightsSignal", "NoIdleTemperature", "NoIdleMinTemperature", "NoIdleMaxTemperature", "NoIdleKeyInserted", "NoIdleEngineRunning", "NoIdleMaxDischargeTime", "NoIdleCutoffVoltage", "NoIdleEngineStartCounter", "NoIdleEngineStopCounter", "NoIdleEmergencyLight", "NoIdleChargeEnable", "NoIdleDischargeEnable", "NoIdleRampDoorOpen", "NoIdleACRequest", "ObdiiFaultCodes", "ObdiiTimeRunWithMalfunction", "ObdiiDistanceTraveledWithMalfunction", "ObdiiEngineRPM", "ObdiiCalculatedEngineLoad", "ObdiiEngineCoolantTemperature", "ObdiiEngineOilTemperature", "ObdiiVehicleSpeed", "ObdiiVin", "ObdiiSupportedPIDs", "ObdiiOdometer", "ObdiiEngineRuntime", "ObdiiEngineRuntimeTotal", "SensorTemperature", "SensorPressure", "SensorRSSI", "SensorSignalQuality", "SensorBitErrorRate", "SensorEvent", "SensorExtraTemperature1", "SensorExtraTemperature2", "SensorFlood", "SensorAxisX", "SensorAxisY", "SensorAxisZ", "SensorAxisPitch", "SensorAxisYaw", "SensorAxisRoll", "SensorLevel", "SensorNoise", "SensorAcidity", "SensorFrequency", "SensorFlow", "SensorStatus", "SensorHumidity", "SensorVoltage", "SensorCurrent", "SensorParticleMonitor", "TrackerTrips", "TrackerInTrip", "TrackerTamperAlert", "TrackerRecoveryModeActive", "HolykellLevel", "HolykellTemperature", "SurvalentAnalog", "SurvalentStatus", "SurvalentText"]
        if known is not None and known not in allowed_values:
            raise ValueError(
                "Invalid value for `known` ({0}), must be one of {1}"
                .format(known, allowed_values)
            )

        self._known = known

    @property
    def custom(self):
        """
        Gets the custom of this WellKnownPointClassOrCustom.

        :return: The custom of this WellKnownPointClassOrCustom.
        :rtype: int
        """
        return self._custom

    @custom.setter
    def custom(self, custom):
        """
        Sets the custom of this WellKnownPointClassOrCustom.

        :param custom: The custom of this WellKnownPointClassOrCustom.
        :type: int
        """

        self._custom = custom

    def to_dict(self):
        """
        Returns the model properties as a dict
        """
        result = {}

        for attr, _ in iteritems(self.swagger_types):
            value = getattr(self, attr)
            if isinstance(value, list):
                result[attr] = list(map(
                    lambda x: x.to_dict() if hasattr(x, "to_dict") else x,
                    value
                ))
            elif hasattr(value, "to_dict"):
                result[attr] = value.to_dict()
            elif isinstance(value, dict):
                result[attr] = dict(map(
                    lambda item: (item[0], item[1].to_dict())
                    if hasattr(item[1], "to_dict") else item,
                    value.items()
                ))
            else:
                result[attr] = value

        return result

    def to_str(self):
        """
        Returns the string representation of the model
        """
        return pformat(self.to_dict())

    def __repr__(self):
        """
        For `print` and `pprint`
        """
        return self.to_str()

    def __eq__(self, other):
        """
        Returns true if both objects are equal
        """
        if not isinstance(other, WellKnownPointClassOrCustom):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

