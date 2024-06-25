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

class EngineeringUnitsDescriptor(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, units=None, display_name=None, description=None, family=None, factors=None):
        """
        EngineeringUnitsDescriptor - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'units': 'str',
            'display_name': 'str',
            'description': 'str',
            'family': 'str',
            'factors': 'EngineeringUnitsFactors'
        }

        self.attribute_map = {
            'units': 'units',
            'display_name': 'displayName',
            'description': 'description',
            'family': 'family',
            'factors': 'factors'
        }

        self._units = units
        self._display_name = display_name
        self._description = description
        self._family = family
        self._factors = factors


    @property
    def units(self):
        """
        Gets the units of this EngineeringUnitsDescriptor.

        :return: The units of this EngineeringUnitsDescriptor.
        :rtype: str
        """
        return self._units

    @units.setter
    def units(self, units):
        """
        Sets the units of this EngineeringUnitsDescriptor.

        :param units: The units of this EngineeringUnitsDescriptor.
        :type: str
        """
        allowed_values = ["enumerated", "onOff", "activeInactive", "constant", "log", "meters", "nanometers", "micrometers", "millimeters", "centimeters", "kilometers", "inches", "feet", "miles", "seconds", "hundredths_seconds", "milliseconds", "minutes", "hours", "days", "weeks", "months", "years", "grams", "nanograms", "micrograms", "milligrams", "kilograms", "pounds_mass", "tons", "meters_per_second", "meters_per_minute", "meters_per_hour", "millimeters_per_second", "millimeters_per_minute", "millimeters_per_hour", "kilometers_per_hour", "feet_per_second", "feet_per_minute", "miles_per_hour", "knots", "longitude", "latitude", "meters_per_second_per_second", "millig", "cubic_meters", "cubic_feet", "liters", "milliliters", "imperial_gallons", "us_gallons", "cubic_feet_per_minute", "cubic_feet_per_second", "cubic_feet_per_hour", "cubic_feet_per_day", "thousand_cubic_feet_per_day", "standard_cubic_feet_per_day", "million_standard_cubic_feet_per_minute", "million_standard_cubic_feet_per_day", "thousand_standard_cubic_feet_per_day", "cubic_meters_per_minute", "cubic_meters_per_second", "cubic_meters_per_hour", "cubic_meters_per_day", "liters_per_second", "liters_per_minute", "liters_per_hour", "milliliters_per_second", "pounds_mass_per_day", "imperial_gallons_per_minute", "us_gallons_per_minute", "us_gallons_per_hour", "square_meters", "square_centimeters", "square_feet", "square_inches", "currency_dollar_US", "currency_dollar_Canadian", "currency_euro", "currency_generic", "amperes", "milliamperes", "kiloamperes", "megaamperes", "volts", "millivolts", "kilovolts", "megavolts", "watts", "milliwatts", "kilowatts", "megawatts", "amperes_per_meter", "amperes_per_square_meter", "ampere_square_meters", "ohms", "milliohms", "kilohms", "megohms", "ohm_meters", "ohm_meter_squared_per_meter", "siemens", "millisiemens", "microsiemens", "siemens_per_meter", "microsiemens_per_millimeter", "decibels", "decibels_milliwatts", "decibels_millivolt", "decibels_volt", "farads", "henrys", "teslas", "webers", "power_factor", "volt_amperes", "kilovolt_amperes", "megavolt_amperes", "volt_amperes_reactive", "kilovolt_amperes_reactive", "megavolt_amperes_reactive", "volts_per_meter", "volts_per_degree_kelvin", "degrees_phase", "ampere_seconds", "ampere_hours", "volt_ampere_hours", "kilovolt_ampere_hours", "megavolt_ampere_hours", "volt_ampere_hours_reactive", "kilovolt_ampere_hours_reactive", "megavolt_ampere_hours_reactive", "volt_square_hours", "ampere_square_hours", "joules", "kilojoules", "megajoules", "kilojoules_per_kilogram", "watt_hours", "kilowatt_hours", "megawatt_hours", "watt_hours_reactive", "kilowatt_hours_reactive", "megawatt_hours_reactive", "btus", "kilo_btus", "mega_btus", "therms", "ton_hours", "joules_per_kilogram_dry_air", "kilojoules_per_kilogram_dry_air", "megajoules_per_kilogram_dry_air", "btus_per_pound_dry_air", "btus_per_pound", "joules_per_degree_kelvin", "kilojoules_per_degree_kelvin", "megajoules_per_degree_kelvin", "joules_per_kilogram_degree_kelvin", "newton", "newton_meters", "hertz", "kilohertz", "megahertz", "cycles_per_minute", "cycles_per_hour", "per_hour", "per_minute", "per_second", "grams_of_water_per_kilogram_dry_air", "percent_relative_humidity", "watts_per_square_foot", "watts_per_square_meter", "milliwatts_per_square_centimeter", "candelas", "candelas_per_square_meter", "lumens", "luxes", "foot_candles", "grams_per_second", "grams_per_minute", "kilograms_per_second", "kilograms_per_minute", "kilograms_per_hour", "pounds_mass_per_second", "pounds_mass_per_minute", "pounds_mass_per_hour", "tons_per_hour", "btus_per_hour", "kilo_btus_per_hour", "joule_per_hours", "horsepower", "tons_refrigeration", "pascals", "hectopascals", "kilopascals", "megapascals", "millibars", "bars", "pounds_force_per_square_inch", "millimeters_of_water", "centimeters_of_water", "inches_of_water", "millimeters_of_mercury", "centimeters_of_mercury", "inches_of_mercury", "degrees_celsius", "degrees_kelvin", "degrees_fahrenheit", "degrees_kelvin_per_hour", "degrees_kelvin_per_minute", "degrees_celsius_per_hour", "degrees_celsius_per_minute", "degrees_fahrenheit_per_hour", "degrees_fahrenheit_per_minute", "degree_days_celsius", "degree_days_fahrenheit", "delta_degrees_kelvin", "delta_degrees_fahrenheit", "no_units", "ticks", "counts", "bytes", "kilo_bytes", "mega_bytes", "giga_bytes", "tera_bytes", "degrees_angular", "radians", "degrees_angular_per_second", "radians_per_second", "mole_percent", "percent", "per_mille", "percent_per_second", "percent_obscuration_per_meter", "percent_obscuration_per_foot", "parts_per_million", "parts_per_billion", "revolutions_per_minute", "joule_seconds", "newton_seconds", "newtons_per_meter", "pascal_seconds", "psi_per_degree_fahrenheit", "kilograms_per_cubic_meter", "kilometers_per_liter", "miles_per_us_gallon", "kilowatts_per_ton", "kilowatt_hours_per_square_meter", "kilowatt_hours_per_square_foot", "watt_hours_per_cubic_meter", "joules_per_cubic_meter", "megajoules_per_square_meter", "megajoules_per_square_foot", "square_meters_per_newton", "watts_per_meter_per_degree_kelvin", "watts_per_square_meter_degree_kelvin", "grams_per_gram", "kilograms_per_kilogram", "grams_per_kilogram", "milligrams_per_gram", "milligrams_per_kilogram", "grams_per_milliliter", "grams_per_liter", "grams_per_square_meter", "milligrams_per_liter", "micrograms_per_liter", "grams_per_cubic_meter", "milligrams_per_cubic_meter", "micrograms_per_cubic_meter", "nanograms_per_cubic_meter", "grams_per_cubic_centimeter", "becquerels", "kilobecquerels", "megabecquerels", "gray", "milligray", "microgray", "sieverts", "millisieverts", "microsieverts", "microsieverts_per_hour", "millirems", "millirems_per_hour", "decibels_a", "nephelometric_turbidity_unit", "pH", "minutes_per_degree_kelvin"]
        if units is not None and units not in allowed_values:
            raise ValueError(
                "Invalid value for `units` ({0}), must be one of {1}"
                .format(units, allowed_values)
            )

        self._units = units

    @property
    def display_name(self):
        """
        Gets the display_name of this EngineeringUnitsDescriptor.

        :return: The display_name of this EngineeringUnitsDescriptor.
        :rtype: str
        """
        return self._display_name

    @display_name.setter
    def display_name(self, display_name):
        """
        Sets the display_name of this EngineeringUnitsDescriptor.

        :param display_name: The display_name of this EngineeringUnitsDescriptor.
        :type: str
        """

        self._display_name = display_name

    @property
    def description(self):
        """
        Gets the description of this EngineeringUnitsDescriptor.

        :return: The description of this EngineeringUnitsDescriptor.
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """
        Sets the description of this EngineeringUnitsDescriptor.

        :param description: The description of this EngineeringUnitsDescriptor.
        :type: str
        """

        self._description = description

    @property
    def family(self):
        """
        Gets the family of this EngineeringUnitsDescriptor.

        :return: The family of this EngineeringUnitsDescriptor.
        :rtype: str
        """
        return self._family

    @family.setter
    def family(self, family):
        """
        Sets the family of this EngineeringUnitsDescriptor.

        :param family: The family of this EngineeringUnitsDescriptor.
        :type: str
        """
        allowed_values = ["Acceleration", "Area", "Currency", "Electrical", "Energy", "Enthalpy", "Entropy", "Force", "Frequency", "Humidity", "Length", "Light", "Mass", "MassFlow", "Power", "Pressure", "Temperature", "Time", "Torque", "Velocity", "Volume", "VolumetricFlow", "Other", "Constant", "Enumerated", "Binary"]
        if family is not None and family not in allowed_values:
            raise ValueError(
                "Invalid value for `family` ({0}), must be one of {1}"
                .format(family, allowed_values)
            )

        self._family = family

    @property
    def factors(self):
        """
        Gets the factors of this EngineeringUnitsDescriptor.

        :return: The factors of this EngineeringUnitsDescriptor.
        :rtype: EngineeringUnitsFactors
        """
        return self._factors

    @factors.setter
    def factors(self, factors):
        """
        Sets the factors of this EngineeringUnitsDescriptor.

        :param factors: The factors of this EngineeringUnitsDescriptor.
        :type: EngineeringUnitsFactors
        """

        self._factors = factors

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
        if not isinstance(other, EngineeringUnitsDescriptor):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
