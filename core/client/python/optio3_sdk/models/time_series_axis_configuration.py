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

class TimeSeriesAxisConfiguration(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, label=None, color=None, display_factors=None, override=None, grouped_factors=None):
        """
        TimeSeriesAxisConfiguration - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'label': 'str',
            'color': 'str',
            'display_factors': 'EngineeringUnitsFactors',
            'override': 'ToggleableNumericRange',
            'grouped_factors': 'list[TimeSeriesAxisGroupConfiguration]'
        }

        self.attribute_map = {
            'label': 'label',
            'color': 'color',
            'display_factors': 'displayFactors',
            'override': 'override',
            'grouped_factors': 'groupedFactors'
        }

        self._label = label
        self._color = color
        self._display_factors = display_factors
        self._override = override
        self._grouped_factors = grouped_factors


    @property
    def label(self):
        """
        Gets the label of this TimeSeriesAxisConfiguration.

        :return: The label of this TimeSeriesAxisConfiguration.
        :rtype: str
        """
        return self._label

    @label.setter
    def label(self, label):
        """
        Sets the label of this TimeSeriesAxisConfiguration.

        :param label: The label of this TimeSeriesAxisConfiguration.
        :type: str
        """

        self._label = label

    @property
    def color(self):
        """
        Gets the color of this TimeSeriesAxisConfiguration.

        :return: The color of this TimeSeriesAxisConfiguration.
        :rtype: str
        """
        return self._color

    @color.setter
    def color(self, color):
        """
        Sets the color of this TimeSeriesAxisConfiguration.

        :param color: The color of this TimeSeriesAxisConfiguration.
        :type: str
        """

        self._color = color

    @property
    def display_factors(self):
        """
        Gets the display_factors of this TimeSeriesAxisConfiguration.

        :return: The display_factors of this TimeSeriesAxisConfiguration.
        :rtype: EngineeringUnitsFactors
        """
        return self._display_factors

    @display_factors.setter
    def display_factors(self, display_factors):
        """
        Sets the display_factors of this TimeSeriesAxisConfiguration.

        :param display_factors: The display_factors of this TimeSeriesAxisConfiguration.
        :type: EngineeringUnitsFactors
        """

        self._display_factors = display_factors

    @property
    def override(self):
        """
        Gets the override of this TimeSeriesAxisConfiguration.

        :return: The override of this TimeSeriesAxisConfiguration.
        :rtype: ToggleableNumericRange
        """
        return self._override

    @override.setter
    def override(self, override):
        """
        Sets the override of this TimeSeriesAxisConfiguration.

        :param override: The override of this TimeSeriesAxisConfiguration.
        :type: ToggleableNumericRange
        """

        self._override = override

    @property
    def grouped_factors(self):
        """
        Gets the grouped_factors of this TimeSeriesAxisConfiguration.

        :return: The grouped_factors of this TimeSeriesAxisConfiguration.
        :rtype: list[TimeSeriesAxisGroupConfiguration]
        """
        return self._grouped_factors

    @grouped_factors.setter
    def grouped_factors(self, grouped_factors):
        """
        Sets the grouped_factors of this TimeSeriesAxisConfiguration.

        :param grouped_factors: The grouped_factors of this TimeSeriesAxisConfiguration.
        :type: list[TimeSeriesAxisGroupConfiguration]
        """

        self._grouped_factors = grouped_factors

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
        if not isinstance(other, TimeSeriesAxisConfiguration):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
