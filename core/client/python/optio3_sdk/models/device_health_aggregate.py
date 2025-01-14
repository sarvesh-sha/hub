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

class DeviceHealthAggregate(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, type=None, max_severity=None, count=None):
        """
        DeviceHealthAggregate - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'type': 'str',
            'max_severity': 'str',
            'count': 'int'
        }

        self.attribute_map = {
            'type': 'type',
            'max_severity': 'maxSeverity',
            'count': 'count'
        }

        self._type = type
        self._max_severity = max_severity
        self._count = count


    @property
    def type(self):
        """
        Gets the type of this DeviceHealthAggregate.

        :return: The type of this DeviceHealthAggregate.
        :rtype: str
        """
        return self._type

    @type.setter
    def type(self, type):
        """
        Sets the type of this DeviceHealthAggregate.

        :param type: The type of this DeviceHealthAggregate.
        :type: str
        """
        allowed_values = ["ALARM", "COMMUNICATION_PROBLEM", "DEVICE_FAILURE", "END_OF_LIFE", "INFORMATIONAL", "OPERATOR_SUMMARY", "RECALL", "THRESHOLD_EXCEEDED", "WARNING", "WARRANTY"]
        if type is not None and type not in allowed_values:
            raise ValueError(
                "Invalid value for `type` ({0}), must be one of {1}"
                .format(type, allowed_values)
            )

        self._type = type

    @property
    def max_severity(self):
        """
        Gets the max_severity of this DeviceHealthAggregate.

        :return: The max_severity of this DeviceHealthAggregate.
        :rtype: str
        """
        return self._max_severity

    @max_severity.setter
    def max_severity(self, max_severity):
        """
        Sets the max_severity of this DeviceHealthAggregate.

        :param max_severity: The max_severity of this DeviceHealthAggregate.
        :type: str
        """
        allowed_values = ["CRITICAL", "SIGNIFICANT", "NORMAL", "LOW"]
        if max_severity is not None and max_severity not in allowed_values:
            raise ValueError(
                "Invalid value for `max_severity` ({0}), must be one of {1}"
                .format(max_severity, allowed_values)
            )

        self._max_severity = max_severity

    @property
    def count(self):
        """
        Gets the count of this DeviceHealthAggregate.

        :return: The count of this DeviceHealthAggregate.
        :rtype: int
        """
        return self._count

    @count.setter
    def count(self, count):
        """
        Sets the count of this DeviceHealthAggregate.

        :param count: The count of this DeviceHealthAggregate.
        :type: int
        """

        self._count = count

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
        if not isinstance(other, DeviceHealthAggregate):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

