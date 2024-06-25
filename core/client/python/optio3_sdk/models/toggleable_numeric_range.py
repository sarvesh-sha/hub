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

class ToggleableNumericRange(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, min=None, max=None, min_invalid=None, max_invalid=None, active=None):
        """
        ToggleableNumericRange - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'min': 'float',
            'max': 'float',
            'min_invalid': 'bool',
            'max_invalid': 'bool',
            'active': 'bool'
        }

        self.attribute_map = {
            'min': 'min',
            'max': 'max',
            'min_invalid': 'minInvalid',
            'max_invalid': 'maxInvalid',
            'active': 'active'
        }

        self._min = min
        self._max = max
        self._min_invalid = min_invalid
        self._max_invalid = max_invalid
        self._active = active


    @property
    def min(self):
        """
        Gets the min of this ToggleableNumericRange.

        :return: The min of this ToggleableNumericRange.
        :rtype: float
        """
        return self._min

    @min.setter
    def min(self, min):
        """
        Sets the min of this ToggleableNumericRange.

        :param min: The min of this ToggleableNumericRange.
        :type: float
        """

        self._min = min

    @property
    def max(self):
        """
        Gets the max of this ToggleableNumericRange.

        :return: The max of this ToggleableNumericRange.
        :rtype: float
        """
        return self._max

    @max.setter
    def max(self, max):
        """
        Sets the max of this ToggleableNumericRange.

        :param max: The max of this ToggleableNumericRange.
        :type: float
        """

        self._max = max

    @property
    def min_invalid(self):
        """
        Gets the min_invalid of this ToggleableNumericRange.

        :return: The min_invalid of this ToggleableNumericRange.
        :rtype: bool
        """
        return self._min_invalid

    @min_invalid.setter
    def min_invalid(self, min_invalid):
        """
        Sets the min_invalid of this ToggleableNumericRange.

        :param min_invalid: The min_invalid of this ToggleableNumericRange.
        :type: bool
        """

        self._min_invalid = min_invalid

    @property
    def max_invalid(self):
        """
        Gets the max_invalid of this ToggleableNumericRange.

        :return: The max_invalid of this ToggleableNumericRange.
        :rtype: bool
        """
        return self._max_invalid

    @max_invalid.setter
    def max_invalid(self, max_invalid):
        """
        Sets the max_invalid of this ToggleableNumericRange.

        :param max_invalid: The max_invalid of this ToggleableNumericRange.
        :type: bool
        """

        self._max_invalid = max_invalid

    @property
    def active(self):
        """
        Gets the active of this ToggleableNumericRange.

        :return: The active of this ToggleableNumericRange.
        :rtype: bool
        """
        return self._active

    @active.setter
    def active(self, active):
        """
        Sets the active of this ToggleableNumericRange.

        :param active: The active of this ToggleableNumericRange.
        :type: bool
        """

        self._active = active

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
        if not isinstance(other, ToggleableNumericRange):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
