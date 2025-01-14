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

class ColorSegment(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, color=None, stop_point=None, stop_point_value=None):
        """
        ColorSegment - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'color': 'str',
            'stop_point': 'str',
            'stop_point_value': 'float'
        }

        self.attribute_map = {
            'color': 'color',
            'stop_point': 'stopPoint',
            'stop_point_value': 'stopPointValue'
        }

        self._color = color
        self._stop_point = stop_point
        self._stop_point_value = stop_point_value


    @property
    def color(self):
        """
        Gets the color of this ColorSegment.

        :return: The color of this ColorSegment.
        :rtype: str
        """
        return self._color

    @color.setter
    def color(self, color):
        """
        Sets the color of this ColorSegment.

        :param color: The color of this ColorSegment.
        :type: str
        """

        self._color = color

    @property
    def stop_point(self):
        """
        Gets the stop_point of this ColorSegment.

        :return: The stop_point of this ColorSegment.
        :rtype: str
        """
        return self._stop_point

    @stop_point.setter
    def stop_point(self, stop_point):
        """
        Sets the stop_point of this ColorSegment.

        :param stop_point: The stop_point of this ColorSegment.
        :type: str
        """
        allowed_values = ["MIN", "MIDPOINT", "MAX", "CUSTOM"]
        if stop_point is not None and stop_point not in allowed_values:
            raise ValueError(
                "Invalid value for `stop_point` ({0}), must be one of {1}"
                .format(stop_point, allowed_values)
            )

        self._stop_point = stop_point

    @property
    def stop_point_value(self):
        """
        Gets the stop_point_value of this ColorSegment.

        :return: The stop_point_value of this ColorSegment.
        :rtype: float
        """
        return self._stop_point_value

    @stop_point_value.setter
    def stop_point_value(self, stop_point_value):
        """
        Sets the stop_point_value of this ColorSegment.

        :param stop_point_value: The stop_point_value of this ColorSegment.
        :type: float
        """

        self._stop_point_value = stop_point_value

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
        if not isinstance(other, ColorSegment):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

