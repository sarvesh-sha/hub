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

class CustomReportElementAlertFeed(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, label=None, alert_types=None, locations=None):
        """
        CustomReportElementAlertFeed - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'label': 'str',
            'alert_types': 'list[str]',
            'locations': 'list[str]',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'label': 'label',
            'alert_types': 'alertTypes',
            'locations': 'locations',
            'discriminator___type': '__type'
        }

        self._label = label
        self._alert_types = alert_types
        self._locations = locations

    @property
    def discriminator___type(self):
        return "CustomReportElementAlertFeed"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def label(self):
        """
        Gets the label of this CustomReportElementAlertFeed.

        :return: The label of this CustomReportElementAlertFeed.
        :rtype: str
        """
        return self._label

    @label.setter
    def label(self, label):
        """
        Sets the label of this CustomReportElementAlertFeed.

        :param label: The label of this CustomReportElementAlertFeed.
        :type: str
        """

        self._label = label

    @property
    def alert_types(self):
        """
        Gets the alert_types of this CustomReportElementAlertFeed.

        :return: The alert_types of this CustomReportElementAlertFeed.
        :rtype: list[str]
        """
        return self._alert_types

    @alert_types.setter
    def alert_types(self, alert_types):
        """
        Sets the alert_types of this CustomReportElementAlertFeed.

        :param alert_types: The alert_types of this CustomReportElementAlertFeed.
        :type: list[str]
        """
        allowed_values = ["ALARM", "COMMUNICATION_PROBLEM", "DEVICE_FAILURE", "END_OF_LIFE", "INFORMATIONAL", "OPERATOR_SUMMARY", "RECALL", "THRESHOLD_EXCEEDED", "WARNING", "WARRANTY"]
        if not set(alert_types).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `alert_types` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(alert_types)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._alert_types = alert_types

    @property
    def locations(self):
        """
        Gets the locations of this CustomReportElementAlertFeed.

        :return: The locations of this CustomReportElementAlertFeed.
        :rtype: list[str]
        """
        return self._locations

    @locations.setter
    def locations(self, locations):
        """
        Sets the locations of this CustomReportElementAlertFeed.

        :param locations: The locations of this CustomReportElementAlertFeed.
        :type: list[str]
        """

        self._locations = locations

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
        if not isinstance(other, CustomReportElementAlertFeed):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

