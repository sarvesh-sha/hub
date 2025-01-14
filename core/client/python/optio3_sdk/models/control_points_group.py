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

class ControlPointsGroup(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, name=None, units_factors=None, units_display=None, aggregation_type=None, group_aggregation_type=None, granularity=None, limit_mode=None, limit_value=None, value_precision=None, selections=None, color_config=None, range=None, graph=None, point_input=None):
        """
        ControlPointsGroup - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'name': 'str',
            'units_factors': 'EngineeringUnitsFactors',
            'units_display': 'str',
            'aggregation_type': 'str',
            'group_aggregation_type': 'str',
            'granularity': 'str',
            'limit_mode': 'str',
            'limit_value': 'int',
            'value_precision': 'int',
            'selections': 'ControlPointsSelection',
            'color_config': 'ColorConfiguration',
            'range': 'ToggleableNumericRange',
            'graph': 'AssetGraph',
            'point_input': 'AssetGraphBinding'
        }

        self.attribute_map = {
            'name': 'name',
            'units_factors': 'unitsFactors',
            'units_display': 'unitsDisplay',
            'aggregation_type': 'aggregationType',
            'group_aggregation_type': 'groupAggregationType',
            'granularity': 'granularity',
            'limit_mode': 'limitMode',
            'limit_value': 'limitValue',
            'value_precision': 'valuePrecision',
            'selections': 'selections',
            'color_config': 'colorConfig',
            'range': 'range',
            'graph': 'graph',
            'point_input': 'pointInput'
        }

        self._name = name
        self._units_factors = units_factors
        self._units_display = units_display
        self._aggregation_type = aggregation_type
        self._group_aggregation_type = group_aggregation_type
        self._granularity = granularity
        self._limit_mode = limit_mode
        self._limit_value = limit_value
        self._value_precision = value_precision
        self._selections = selections
        self._color_config = color_config
        self._range = range
        self._graph = graph
        self._point_input = point_input


    @property
    def name(self):
        """
        Gets the name of this ControlPointsGroup.

        :return: The name of this ControlPointsGroup.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this ControlPointsGroup.

        :param name: The name of this ControlPointsGroup.
        :type: str
        """

        self._name = name

    @property
    def units_factors(self):
        """
        Gets the units_factors of this ControlPointsGroup.

        :return: The units_factors of this ControlPointsGroup.
        :rtype: EngineeringUnitsFactors
        """
        return self._units_factors

    @units_factors.setter
    def units_factors(self, units_factors):
        """
        Sets the units_factors of this ControlPointsGroup.

        :param units_factors: The units_factors of this ControlPointsGroup.
        :type: EngineeringUnitsFactors
        """

        self._units_factors = units_factors

    @property
    def units_display(self):
        """
        Gets the units_display of this ControlPointsGroup.

        :return: The units_display of this ControlPointsGroup.
        :rtype: str
        """
        return self._units_display

    @units_display.setter
    def units_display(self, units_display):
        """
        Sets the units_display of this ControlPointsGroup.

        :param units_display: The units_display of this ControlPointsGroup.
        :type: str
        """

        self._units_display = units_display

    @property
    def aggregation_type(self):
        """
        Gets the aggregation_type of this ControlPointsGroup.

        :return: The aggregation_type of this ControlPointsGroup.
        :rtype: str
        """
        return self._aggregation_type

    @aggregation_type.setter
    def aggregation_type(self, aggregation_type):
        """
        Sets the aggregation_type of this ControlPointsGroup.

        :param aggregation_type: The aggregation_type of this ControlPointsGroup.
        :type: str
        """
        allowed_values = ["NONE", "SUM", "MEAN", "MIN", "MAX", "DELTA", "AVGDELTA", "INCREASE", "DECREASE", "FIRST", "LAST"]
        if aggregation_type is not None and aggregation_type not in allowed_values:
            raise ValueError(
                "Invalid value for `aggregation_type` ({0}), must be one of {1}"
                .format(aggregation_type, allowed_values)
            )

        self._aggregation_type = aggregation_type

    @property
    def group_aggregation_type(self):
        """
        Gets the group_aggregation_type of this ControlPointsGroup.

        :return: The group_aggregation_type of this ControlPointsGroup.
        :rtype: str
        """
        return self._group_aggregation_type

    @group_aggregation_type.setter
    def group_aggregation_type(self, group_aggregation_type):
        """
        Sets the group_aggregation_type of this ControlPointsGroup.

        :param group_aggregation_type: The group_aggregation_type of this ControlPointsGroup.
        :type: str
        """
        allowed_values = ["NONE", "SUM", "MEAN", "MIN", "MAX", "DELTA", "AVGDELTA", "INCREASE", "DECREASE", "FIRST", "LAST"]
        if group_aggregation_type is not None and group_aggregation_type not in allowed_values:
            raise ValueError(
                "Invalid value for `group_aggregation_type` ({0}), must be one of {1}"
                .format(group_aggregation_type, allowed_values)
            )

        self._group_aggregation_type = group_aggregation_type

    @property
    def granularity(self):
        """
        Gets the granularity of this ControlPointsGroup.

        :return: The granularity of this ControlPointsGroup.
        :rtype: str
        """
        return self._granularity

    @granularity.setter
    def granularity(self, granularity):
        """
        Sets the granularity of this ControlPointsGroup.

        :param granularity: The granularity of this ControlPointsGroup.
        :type: str
        """
        allowed_values = ["None", "Hour", "Day", "Week", "Month", "Quarter", "Year"]
        if granularity is not None and granularity not in allowed_values:
            raise ValueError(
                "Invalid value for `granularity` ({0}), must be one of {1}"
                .format(granularity, allowed_values)
            )

        self._granularity = granularity

    @property
    def limit_mode(self):
        """
        Gets the limit_mode of this ControlPointsGroup.

        :return: The limit_mode of this ControlPointsGroup.
        :rtype: str
        """
        return self._limit_mode

    @limit_mode.setter
    def limit_mode(self, limit_mode):
        """
        Sets the limit_mode of this ControlPointsGroup.

        :param limit_mode: The limit_mode of this ControlPointsGroup.
        :type: str
        """
        allowed_values = ["None", "TopN", "BottomN", "TopNPercent", "BottomNPercent"]
        if limit_mode is not None and limit_mode not in allowed_values:
            raise ValueError(
                "Invalid value for `limit_mode` ({0}), must be one of {1}"
                .format(limit_mode, allowed_values)
            )

        self._limit_mode = limit_mode

    @property
    def limit_value(self):
        """
        Gets the limit_value of this ControlPointsGroup.

        :return: The limit_value of this ControlPointsGroup.
        :rtype: int
        """
        return self._limit_value

    @limit_value.setter
    def limit_value(self, limit_value):
        """
        Sets the limit_value of this ControlPointsGroup.

        :param limit_value: The limit_value of this ControlPointsGroup.
        :type: int
        """

        self._limit_value = limit_value

    @property
    def value_precision(self):
        """
        Gets the value_precision of this ControlPointsGroup.

        :return: The value_precision of this ControlPointsGroup.
        :rtype: int
        """
        return self._value_precision

    @value_precision.setter
    def value_precision(self, value_precision):
        """
        Sets the value_precision of this ControlPointsGroup.

        :param value_precision: The value_precision of this ControlPointsGroup.
        :type: int
        """

        self._value_precision = value_precision

    @property
    def selections(self):
        """
        Gets the selections of this ControlPointsGroup.

        :return: The selections of this ControlPointsGroup.
        :rtype: ControlPointsSelection
        """
        return self._selections

    @selections.setter
    def selections(self, selections):
        """
        Sets the selections of this ControlPointsGroup.

        :param selections: The selections of this ControlPointsGroup.
        :type: ControlPointsSelection
        """

        self._selections = selections

    @property
    def color_config(self):
        """
        Gets the color_config of this ControlPointsGroup.

        :return: The color_config of this ControlPointsGroup.
        :rtype: ColorConfiguration
        """
        return self._color_config

    @color_config.setter
    def color_config(self, color_config):
        """
        Sets the color_config of this ControlPointsGroup.

        :param color_config: The color_config of this ControlPointsGroup.
        :type: ColorConfiguration
        """

        self._color_config = color_config

    @property
    def range(self):
        """
        Gets the range of this ControlPointsGroup.

        :return: The range of this ControlPointsGroup.
        :rtype: ToggleableNumericRange
        """
        return self._range

    @range.setter
    def range(self, range):
        """
        Sets the range of this ControlPointsGroup.

        :param range: The range of this ControlPointsGroup.
        :type: ToggleableNumericRange
        """

        self._range = range

    @property
    def graph(self):
        """
        Gets the graph of this ControlPointsGroup.

        :return: The graph of this ControlPointsGroup.
        :rtype: AssetGraph
        """
        return self._graph

    @graph.setter
    def graph(self, graph):
        """
        Sets the graph of this ControlPointsGroup.

        :param graph: The graph of this ControlPointsGroup.
        :type: AssetGraph
        """

        self._graph = graph

    @property
    def point_input(self):
        """
        Gets the point_input of this ControlPointsGroup.

        :return: The point_input of this ControlPointsGroup.
        :rtype: AssetGraphBinding
        """
        return self._point_input

    @point_input.setter
    def point_input(self, point_input):
        """
        Sets the point_input of this ControlPointsGroup.

        :param point_input: The point_input of this ControlPointsGroup.
        :type: AssetGraphBinding
        """

        self._point_input = point_input

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
        if not isinstance(other, ControlPointsGroup):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

