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

class TimeSeriesTooltipEntry(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, binding=None, units_factors=None):
        """
        TimeSeriesTooltipEntry - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'binding': 'AssetGraphBinding',
            'units_factors': 'EngineeringUnitsFactors'
        }

        self.attribute_map = {
            'binding': 'binding',
            'units_factors': 'unitsFactors'
        }

        self._binding = binding
        self._units_factors = units_factors


    @property
    def binding(self):
        """
        Gets the binding of this TimeSeriesTooltipEntry.

        :return: The binding of this TimeSeriesTooltipEntry.
        :rtype: AssetGraphBinding
        """
        return self._binding

    @binding.setter
    def binding(self, binding):
        """
        Sets the binding of this TimeSeriesTooltipEntry.

        :param binding: The binding of this TimeSeriesTooltipEntry.
        :type: AssetGraphBinding
        """

        self._binding = binding

    @property
    def units_factors(self):
        """
        Gets the units_factors of this TimeSeriesTooltipEntry.

        :return: The units_factors of this TimeSeriesTooltipEntry.
        :rtype: EngineeringUnitsFactors
        """
        return self._units_factors

    @units_factors.setter
    def units_factors(self, units_factors):
        """
        Sets the units_factors of this TimeSeriesTooltipEntry.

        :param units_factors: The units_factors of this TimeSeriesTooltipEntry.
        :type: EngineeringUnitsFactors
        """

        self._units_factors = units_factors

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
        if not isinstance(other, TimeSeriesTooltipEntry):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
