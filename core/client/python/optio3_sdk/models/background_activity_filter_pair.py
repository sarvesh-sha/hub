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

class BackgroundActivityFilterPair(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, filter=None, targets=None):
        """
        BackgroundActivityFilterPair - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'filter': 'str',
            'targets': 'list[str]'
        }

        self.attribute_map = {
            'filter': 'filter',
            'targets': 'targets'
        }

        self._filter = filter
        self._targets = targets


    @property
    def filter(self):
        """
        Gets the filter of this BackgroundActivityFilterPair.

        :return: The filter of this BackgroundActivityFilterPair.
        :rtype: str
        """
        return self._filter

    @filter.setter
    def filter(self, filter):
        """
        Sets the filter of this BackgroundActivityFilterPair.

        :param filter: The filter of this BackgroundActivityFilterPair.
        :type: str
        """
        allowed_values = ["all", "hideCompleted", "running", "completed", "matchingStatus"]
        if filter is not None and filter not in allowed_values:
            raise ValueError(
                "Invalid value for `filter` ({0}), must be one of {1}"
                .format(filter, allowed_values)
            )

        self._filter = filter

    @property
    def targets(self):
        """
        Gets the targets of this BackgroundActivityFilterPair.

        :return: The targets of this BackgroundActivityFilterPair.
        :rtype: list[str]
        """
        return self._targets

    @targets.setter
    def targets(self, targets):
        """
        Sets the targets of this BackgroundActivityFilterPair.

        :param targets: The targets of this BackgroundActivityFilterPair.
        :type: list[str]
        """
        allowed_values = ["ACTIVE", "ACTIVE_BUT_CANCELLING", "PAUSED", "PAUSED_BUT_CANCELLING", "WAITING", "WAITING_BUT_CANCELLING", "SLEEPING", "SLEEPING_BUT_CANCELLIN", "EXECUTING", "EXECUTING_BUT_CANCELLING", "CANCELLED", "COMPLETED", "FAILED"]
        if not set(targets).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `targets` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(targets)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._targets = targets

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
        if not isinstance(other, BackgroundActivityFilterPair):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

