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

class CustomReportElementAggregationTrend(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, label=None, groups=None, granularity=None, visualization_mode=None, show_y=None, show_legend=None):
        """
        CustomReportElementAggregationTrend - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'label': 'str',
            'groups': 'list[ControlPointsGroup]',
            'granularity': 'str',
            'visualization_mode': 'str',
            'show_y': 'bool',
            'show_legend': 'bool',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'label': 'label',
            'groups': 'groups',
            'granularity': 'granularity',
            'visualization_mode': 'visualizationMode',
            'show_y': 'showY',
            'show_legend': 'showLegend',
            'discriminator___type': '__type'
        }

        self._label = label
        self._groups = groups
        self._granularity = granularity
        self._visualization_mode = visualization_mode
        self._show_y = show_y
        self._show_legend = show_legend

    @property
    def discriminator___type(self):
        return "CustomReportElementAggregationTrend"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def label(self):
        """
        Gets the label of this CustomReportElementAggregationTrend.

        :return: The label of this CustomReportElementAggregationTrend.
        :rtype: str
        """
        return self._label

    @label.setter
    def label(self, label):
        """
        Sets the label of this CustomReportElementAggregationTrend.

        :param label: The label of this CustomReportElementAggregationTrend.
        :type: str
        """

        self._label = label

    @property
    def groups(self):
        """
        Gets the groups of this CustomReportElementAggregationTrend.

        :return: The groups of this CustomReportElementAggregationTrend.
        :rtype: list[ControlPointsGroup]
        """
        return self._groups

    @groups.setter
    def groups(self, groups):
        """
        Sets the groups of this CustomReportElementAggregationTrend.

        :param groups: The groups of this CustomReportElementAggregationTrend.
        :type: list[ControlPointsGroup]
        """

        self._groups = groups

    @property
    def granularity(self):
        """
        Gets the granularity of this CustomReportElementAggregationTrend.

        :return: The granularity of this CustomReportElementAggregationTrend.
        :rtype: str
        """
        return self._granularity

    @granularity.setter
    def granularity(self, granularity):
        """
        Sets the granularity of this CustomReportElementAggregationTrend.

        :param granularity: The granularity of this CustomReportElementAggregationTrend.
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
    def visualization_mode(self):
        """
        Gets the visualization_mode of this CustomReportElementAggregationTrend.

        :return: The visualization_mode of this CustomReportElementAggregationTrend.
        :rtype: str
        """
        return self._visualization_mode

    @visualization_mode.setter
    def visualization_mode(self, visualization_mode):
        """
        Sets the visualization_mode of this CustomReportElementAggregationTrend.

        :param visualization_mode: The visualization_mode of this CustomReportElementAggregationTrend.
        :type: str
        """
        allowed_values = ["Line", "Bar"]
        if visualization_mode is not None and visualization_mode not in allowed_values:
            raise ValueError(
                "Invalid value for `visualization_mode` ({0}), must be one of {1}"
                .format(visualization_mode, allowed_values)
            )

        self._visualization_mode = visualization_mode

    @property
    def show_y(self):
        """
        Gets the show_y of this CustomReportElementAggregationTrend.

        :return: The show_y of this CustomReportElementAggregationTrend.
        :rtype: bool
        """
        return self._show_y

    @show_y.setter
    def show_y(self, show_y):
        """
        Sets the show_y of this CustomReportElementAggregationTrend.

        :param show_y: The show_y of this CustomReportElementAggregationTrend.
        :type: bool
        """

        self._show_y = show_y

    @property
    def show_legend(self):
        """
        Gets the show_legend of this CustomReportElementAggregationTrend.

        :return: The show_legend of this CustomReportElementAggregationTrend.
        :rtype: bool
        """
        return self._show_legend

    @show_legend.setter
    def show_legend(self, show_legend):
        """
        Sets the show_legend of this CustomReportElementAggregationTrend.

        :param show_legend: The show_legend of this CustomReportElementAggregationTrend.
        :type: bool
        """

        self._show_legend = show_legend

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
        if not isinstance(other, CustomReportElementAggregationTrend):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

