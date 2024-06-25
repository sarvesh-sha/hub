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

class TimeSeriesPropertyResponse(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, values=None, enum_lookup=None, enum_set_lookup=None, next_timestamp=None):
        """
        TimeSeriesPropertyResponse - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'values': 'list[float]',
            'enum_lookup': 'list[str]',
            'enum_set_lookup': 'list[list[str]]',
            'next_timestamp': 'datetime'
        }

        self.attribute_map = {
            'values': 'values',
            'enum_lookup': 'enumLookup',
            'enum_set_lookup': 'enumSetLookup',
            'next_timestamp': 'nextTimestamp'
        }

        self._values = values
        self._enum_lookup = enum_lookup
        self._enum_set_lookup = enum_set_lookup
        self._next_timestamp = next_timestamp


    @property
    def values(self):
        """
        Gets the values of this TimeSeriesPropertyResponse.

        :return: The values of this TimeSeriesPropertyResponse.
        :rtype: list[float]
        """
        return self._values

    @values.setter
    def values(self, values):
        """
        Sets the values of this TimeSeriesPropertyResponse.

        :param values: The values of this TimeSeriesPropertyResponse.
        :type: list[float]
        """

        self._values = values

    @property
    def enum_lookup(self):
        """
        Gets the enum_lookup of this TimeSeriesPropertyResponse.

        :return: The enum_lookup of this TimeSeriesPropertyResponse.
        :rtype: list[str]
        """
        return self._enum_lookup

    @enum_lookup.setter
    def enum_lookup(self, enum_lookup):
        """
        Sets the enum_lookup of this TimeSeriesPropertyResponse.

        :param enum_lookup: The enum_lookup of this TimeSeriesPropertyResponse.
        :type: list[str]
        """

        self._enum_lookup = enum_lookup

    @property
    def enum_set_lookup(self):
        """
        Gets the enum_set_lookup of this TimeSeriesPropertyResponse.

        :return: The enum_set_lookup of this TimeSeriesPropertyResponse.
        :rtype: list[list[str]]
        """
        return self._enum_set_lookup

    @enum_set_lookup.setter
    def enum_set_lookup(self, enum_set_lookup):
        """
        Sets the enum_set_lookup of this TimeSeriesPropertyResponse.

        :param enum_set_lookup: The enum_set_lookup of this TimeSeriesPropertyResponse.
        :type: list[list[str]]
        """

        self._enum_set_lookup = enum_set_lookup

    @property
    def next_timestamp(self):
        """
        Gets the next_timestamp of this TimeSeriesPropertyResponse.

        :return: The next_timestamp of this TimeSeriesPropertyResponse.
        :rtype: datetime
        """
        return self._next_timestamp

    @next_timestamp.setter
    def next_timestamp(self, next_timestamp):
        """
        Sets the next_timestamp of this TimeSeriesPropertyResponse.

        :param next_timestamp: The next_timestamp of this TimeSeriesPropertyResponse.
        :type: datetime
        """

        self._next_timestamp = next_timestamp

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
        if not isinstance(other, TimeSeriesPropertyResponse):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
