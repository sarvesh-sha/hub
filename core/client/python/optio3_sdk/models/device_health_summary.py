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

class DeviceHealthSummary(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, overall_status=None, counts_by_type=None):
        """
        DeviceHealthSummary - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'overall_status': 'str',
            'counts_by_type': 'list[DeviceHealthAggregate]'
        }

        self.attribute_map = {
            'overall_status': 'overallStatus',
            'counts_by_type': 'countsByType'
        }

        self._overall_status = overall_status
        self._counts_by_type = counts_by_type


    @property
    def overall_status(self):
        """
        Gets the overall_status of this DeviceHealthSummary.

        :return: The overall_status of this DeviceHealthSummary.
        :rtype: str
        """
        return self._overall_status

    @overall_status.setter
    def overall_status(self, overall_status):
        """
        Sets the overall_status of this DeviceHealthSummary.

        :param overall_status: The overall_status of this DeviceHealthSummary.
        :type: str
        """
        allowed_values = ["CRITICAL", "SIGNIFICANT", "NORMAL", "LOW"]
        if overall_status is not None and overall_status not in allowed_values:
            raise ValueError(
                "Invalid value for `overall_status` ({0}), must be one of {1}"
                .format(overall_status, allowed_values)
            )

        self._overall_status = overall_status

    @property
    def counts_by_type(self):
        """
        Gets the counts_by_type of this DeviceHealthSummary.

        :return: The counts_by_type of this DeviceHealthSummary.
        :rtype: list[DeviceHealthAggregate]
        """
        return self._counts_by_type

    @counts_by_type.setter
    def counts_by_type(self, counts_by_type):
        """
        Sets the counts_by_type of this DeviceHealthSummary.

        :param counts_by_type: The counts_by_type of this DeviceHealthSummary.
        :type: list[DeviceHealthAggregate]
        """

        self._counts_by_type = counts_by_type

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
        if not isinstance(other, DeviceHealthSummary):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

