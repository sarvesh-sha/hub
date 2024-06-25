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

class AlertMapSeverityColor(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, severity=None, color=None):
        """
        AlertMapSeverityColor - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'severity': 'str',
            'color': 'str'
        }

        self.attribute_map = {
            'severity': 'severity',
            'color': 'color'
        }

        self._severity = severity
        self._color = color


    @property
    def severity(self):
        """
        Gets the severity of this AlertMapSeverityColor.

        :return: The severity of this AlertMapSeverityColor.
        :rtype: str
        """
        return self._severity

    @severity.setter
    def severity(self, severity):
        """
        Sets the severity of this AlertMapSeverityColor.

        :param severity: The severity of this AlertMapSeverityColor.
        :type: str
        """
        allowed_values = ["CRITICAL", "SIGNIFICANT", "NORMAL", "LOW"]
        if severity is not None and severity not in allowed_values:
            raise ValueError(
                "Invalid value for `severity` ({0}), must be one of {1}"
                .format(severity, allowed_values)
            )

        self._severity = severity

    @property
    def color(self):
        """
        Gets the color of this AlertMapSeverityColor.

        :return: The color of this AlertMapSeverityColor.
        :rtype: str
        """
        return self._color

    @color.setter
    def color(self, color):
        """
        Sets the color of this AlertMapSeverityColor.

        :param color: The color of this AlertMapSeverityColor.
        :type: str
        """

        self._color = color

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
        if not isinstance(other, AlertMapSeverityColor):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
