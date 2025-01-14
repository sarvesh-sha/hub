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

class TimeSeriesAxisGroupConfiguration(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, key_factors=None, selected_factors=None, override=None):
        """
        TimeSeriesAxisGroupConfiguration - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'key_factors': 'EngineeringUnitsFactors',
            'selected_factors': 'EngineeringUnitsFactors',
            'override': 'ToggleableNumericRange'
        }

        self.attribute_map = {
            'key_factors': 'keyFactors',
            'selected_factors': 'selectedFactors',
            'override': 'override'
        }

        self._key_factors = key_factors
        self._selected_factors = selected_factors
        self._override = override


    @property
    def key_factors(self):
        """
        Gets the key_factors of this TimeSeriesAxisGroupConfiguration.

        :return: The key_factors of this TimeSeriesAxisGroupConfiguration.
        :rtype: EngineeringUnitsFactors
        """
        return self._key_factors

    @key_factors.setter
    def key_factors(self, key_factors):
        """
        Sets the key_factors of this TimeSeriesAxisGroupConfiguration.

        :param key_factors: The key_factors of this TimeSeriesAxisGroupConfiguration.
        :type: EngineeringUnitsFactors
        """

        self._key_factors = key_factors

    @property
    def selected_factors(self):
        """
        Gets the selected_factors of this TimeSeriesAxisGroupConfiguration.

        :return: The selected_factors of this TimeSeriesAxisGroupConfiguration.
        :rtype: EngineeringUnitsFactors
        """
        return self._selected_factors

    @selected_factors.setter
    def selected_factors(self, selected_factors):
        """
        Sets the selected_factors of this TimeSeriesAxisGroupConfiguration.

        :param selected_factors: The selected_factors of this TimeSeriesAxisGroupConfiguration.
        :type: EngineeringUnitsFactors
        """

        self._selected_factors = selected_factors

    @property
    def override(self):
        """
        Gets the override of this TimeSeriesAxisGroupConfiguration.

        :return: The override of this TimeSeriesAxisGroupConfiguration.
        :rtype: ToggleableNumericRange
        """
        return self._override

    @override.setter
    def override(self, override):
        """
        Sets the override of this TimeSeriesAxisGroupConfiguration.

        :param override: The override of this TimeSeriesAxisGroupConfiguration.
        :type: ToggleableNumericRange
        """

        self._override = override

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
        if not isinstance(other, TimeSeriesAxisGroupConfiguration):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
