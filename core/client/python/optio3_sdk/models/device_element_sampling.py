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

class DeviceElementSampling(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, property_name=None, sampling_period=None):
        """
        DeviceElementSampling - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'property_name': 'str',
            'sampling_period': 'int'
        }

        self.attribute_map = {
            'property_name': 'propertyName',
            'sampling_period': 'samplingPeriod'
        }

        self._property_name = property_name
        self._sampling_period = sampling_period


    @property
    def property_name(self):
        """
        Gets the property_name of this DeviceElementSampling.

        :return: The property_name of this DeviceElementSampling.
        :rtype: str
        """
        return self._property_name

    @property_name.setter
    def property_name(self, property_name):
        """
        Sets the property_name of this DeviceElementSampling.

        :param property_name: The property_name of this DeviceElementSampling.
        :type: str
        """

        self._property_name = property_name

    @property
    def sampling_period(self):
        """
        Gets the sampling_period of this DeviceElementSampling.

        :return: The sampling_period of this DeviceElementSampling.
        :rtype: int
        """
        return self._sampling_period

    @sampling_period.setter
    def sampling_period(self, sampling_period):
        """
        Sets the sampling_period of this DeviceElementSampling.

        :param sampling_period: The sampling_period of this DeviceElementSampling.
        :type: int
        """

        self._sampling_period = sampling_period

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
        if not isinstance(other, DeviceElementSampling):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
