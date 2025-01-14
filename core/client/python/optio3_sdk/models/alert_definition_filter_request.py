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

class AlertDefinitionFilterRequest(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, purposes=None, sort_by=None):
        """
        AlertDefinitionFilterRequest - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'purposes': 'list[str]',
            'sort_by': 'list[SortCriteria]'
        }

        self.attribute_map = {
            'purposes': 'purposes',
            'sort_by': 'sortBy'
        }

        self._purposes = purposes
        self._sort_by = sort_by


    @property
    def purposes(self):
        """
        Gets the purposes of this AlertDefinitionFilterRequest.

        :return: The purposes of this AlertDefinitionFilterRequest.
        :rtype: list[str]
        """
        return self._purposes

    @purposes.setter
    def purposes(self, purposes):
        """
        Sets the purposes of this AlertDefinitionFilterRequest.

        :param purposes: The purposes of this AlertDefinitionFilterRequest.
        :type: list[str]
        """
        allowed_values = ["Definition", "Library"]
        if not set(purposes).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `purposes` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(purposes)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._purposes = purposes

    @property
    def sort_by(self):
        """
        Gets the sort_by of this AlertDefinitionFilterRequest.

        :return: The sort_by of this AlertDefinitionFilterRequest.
        :rtype: list[SortCriteria]
        """
        return self._sort_by

    @sort_by.setter
    def sort_by(self, sort_by):
        """
        Sets the sort_by of this AlertDefinitionFilterRequest.

        :param sort_by: The sort_by of this AlertDefinitionFilterRequest.
        :type: list[SortCriteria]
        """

        self._sort_by = sort_by

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
        if not isinstance(other, AlertDefinitionFilterRequest):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

