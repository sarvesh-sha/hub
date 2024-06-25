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

class ReportSchedulingOptions(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, range=None, schedule=None, delivery_options=None):
        """
        ReportSchedulingOptions - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'range': 'str',
            'schedule': 'ReportSchedule',
            'delivery_options': 'DeliveryOptions'
        }

        self.attribute_map = {
            'range': 'range',
            'schedule': 'schedule',
            'delivery_options': 'deliveryOptions'
        }

        self._range = range
        self._schedule = schedule
        self._delivery_options = delivery_options


    @property
    def range(self):
        """
        Gets the range of this ReportSchedulingOptions.

        :return: The range of this ReportSchedulingOptions.
        :rtype: str
        """
        return self._range

    @range.setter
    def range(self, range):
        """
        Sets the range of this ReportSchedulingOptions.

        :param range: The range of this ReportSchedulingOptions.
        :type: str
        """
        allowed_values = ["Last15Minutes", "Last30Minutes", "Last60Minutes", "Hour", "PreviousHour", "Last3Hours", "Last6Hours", "Last12Hours", "Last24Hours", "Today", "Yesterday", "Last2Days", "Last3Days", "Last7Days", "Week", "PreviousWeek", "Month", "PreviousMonth", "Last30Days", "Quarter", "PreviousQuarter", "Last3Months", "Year", "PreviousYear", "Last365Days", "All"]
        if range is not None and range not in allowed_values:
            raise ValueError(
                "Invalid value for `range` ({0}), must be one of {1}"
                .format(range, allowed_values)
            )

        self._range = range

    @property
    def schedule(self):
        """
        Gets the schedule of this ReportSchedulingOptions.

        :return: The schedule of this ReportSchedulingOptions.
        :rtype: ReportSchedule
        """
        return self._schedule

    @schedule.setter
    def schedule(self, schedule):
        """
        Sets the schedule of this ReportSchedulingOptions.

        :param schedule: The schedule of this ReportSchedulingOptions.
        :type: ReportSchedule
        """

        self._schedule = schedule

    @property
    def delivery_options(self):
        """
        Gets the delivery_options of this ReportSchedulingOptions.

        :return: The delivery_options of this ReportSchedulingOptions.
        :rtype: DeliveryOptions
        """
        return self._delivery_options

    @delivery_options.setter
    def delivery_options(self, delivery_options):
        """
        Sets the delivery_options of this ReportSchedulingOptions.

        :param delivery_options: The delivery_options of this ReportSchedulingOptions.
        :type: DeliveryOptions
        """

        self._delivery_options = delivery_options

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
        if not isinstance(other, ReportSchedulingOptions):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

