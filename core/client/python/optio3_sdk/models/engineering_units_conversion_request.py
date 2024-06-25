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

class EngineeringUnitsConversionRequest(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, convert_from=None, convert_to=None, value=None):
        """
        EngineeringUnitsConversionRequest - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'convert_from': 'EngineeringUnitsFactors',
            'convert_to': 'EngineeringUnitsFactors',
            'value': 'float'
        }

        self.attribute_map = {
            'convert_from': 'convertFrom',
            'convert_to': 'convertTo',
            'value': 'value'
        }

        self._convert_from = convert_from
        self._convert_to = convert_to
        self._value = value


    @property
    def convert_from(self):
        """
        Gets the convert_from of this EngineeringUnitsConversionRequest.

        :return: The convert_from of this EngineeringUnitsConversionRequest.
        :rtype: EngineeringUnitsFactors
        """
        return self._convert_from

    @convert_from.setter
    def convert_from(self, convert_from):
        """
        Sets the convert_from of this EngineeringUnitsConversionRequest.

        :param convert_from: The convert_from of this EngineeringUnitsConversionRequest.
        :type: EngineeringUnitsFactors
        """

        self._convert_from = convert_from

    @property
    def convert_to(self):
        """
        Gets the convert_to of this EngineeringUnitsConversionRequest.

        :return: The convert_to of this EngineeringUnitsConversionRequest.
        :rtype: EngineeringUnitsFactors
        """
        return self._convert_to

    @convert_to.setter
    def convert_to(self, convert_to):
        """
        Sets the convert_to of this EngineeringUnitsConversionRequest.

        :param convert_to: The convert_to of this EngineeringUnitsConversionRequest.
        :type: EngineeringUnitsFactors
        """

        self._convert_to = convert_to

    @property
    def value(self):
        """
        Gets the value of this EngineeringUnitsConversionRequest.

        :return: The value of this EngineeringUnitsConversionRequest.
        :rtype: float
        """
        return self._value

    @value.setter
    def value(self, value):
        """
        Sets the value of this EngineeringUnitsConversionRequest.

        :param value: The value of this EngineeringUnitsConversionRequest.
        :type: float
        """

        self._value = value

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
        if not isinstance(other, EngineeringUnitsConversionRequest):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other