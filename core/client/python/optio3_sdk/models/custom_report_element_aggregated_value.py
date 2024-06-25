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

class CustomReportElementAggregatedValue(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, label=None, control_point_group=None, is_filter_applied=None, filter=None):
        """
        CustomReportElementAggregatedValue - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'label': 'str',
            'control_point_group': 'ControlPointsGroup',
            'is_filter_applied': 'bool',
            'filter': 'RecurringWeeklySchedule',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'label': 'label',
            'control_point_group': 'controlPointGroup',
            'is_filter_applied': 'isFilterApplied',
            'filter': 'filter',
            'discriminator___type': '__type'
        }

        self._label = label
        self._control_point_group = control_point_group
        self._is_filter_applied = is_filter_applied
        self._filter = filter

    @property
    def discriminator___type(self):
        return "CustomReportElementAggregatedValue"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def label(self):
        """
        Gets the label of this CustomReportElementAggregatedValue.

        :return: The label of this CustomReportElementAggregatedValue.
        :rtype: str
        """
        return self._label

    @label.setter
    def label(self, label):
        """
        Sets the label of this CustomReportElementAggregatedValue.

        :param label: The label of this CustomReportElementAggregatedValue.
        :type: str
        """

        self._label = label

    @property
    def control_point_group(self):
        """
        Gets the control_point_group of this CustomReportElementAggregatedValue.

        :return: The control_point_group of this CustomReportElementAggregatedValue.
        :rtype: ControlPointsGroup
        """
        return self._control_point_group

    @control_point_group.setter
    def control_point_group(self, control_point_group):
        """
        Sets the control_point_group of this CustomReportElementAggregatedValue.

        :param control_point_group: The control_point_group of this CustomReportElementAggregatedValue.
        :type: ControlPointsGroup
        """

        self._control_point_group = control_point_group

    @property
    def is_filter_applied(self):
        """
        Gets the is_filter_applied of this CustomReportElementAggregatedValue.

        :return: The is_filter_applied of this CustomReportElementAggregatedValue.
        :rtype: bool
        """
        return self._is_filter_applied

    @is_filter_applied.setter
    def is_filter_applied(self, is_filter_applied):
        """
        Sets the is_filter_applied of this CustomReportElementAggregatedValue.

        :param is_filter_applied: The is_filter_applied of this CustomReportElementAggregatedValue.
        :type: bool
        """

        self._is_filter_applied = is_filter_applied

    @property
    def filter(self):
        """
        Gets the filter of this CustomReportElementAggregatedValue.

        :return: The filter of this CustomReportElementAggregatedValue.
        :rtype: RecurringWeeklySchedule
        """
        return self._filter

    @filter.setter
    def filter(self, filter):
        """
        Sets the filter of this CustomReportElementAggregatedValue.

        :param filter: The filter of this CustomReportElementAggregatedValue.
        :type: RecurringWeeklySchedule
        """

        self._filter = filter

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
        if not isinstance(other, CustomReportElementAggregatedValue):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
