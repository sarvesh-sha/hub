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

class ViewStateItem(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, value=None, save_in_bookmark=None):
        """
        ViewStateItem - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'value': 'object',
            'save_in_bookmark': 'bool'
        }

        self.attribute_map = {
            'value': 'value',
            'save_in_bookmark': 'saveInBookmark'
        }

        self._value = value
        self._save_in_bookmark = save_in_bookmark


    @property
    def value(self):
        """
        Gets the value of this ViewStateItem.

        :return: The value of this ViewStateItem.
        :rtype: object
        """
        return self._value

    @value.setter
    def value(self, value):
        """
        Sets the value of this ViewStateItem.

        :param value: The value of this ViewStateItem.
        :type: object
        """

        self._value = value

    @property
    def save_in_bookmark(self):
        """
        Gets the save_in_bookmark of this ViewStateItem.

        :return: The save_in_bookmark of this ViewStateItem.
        :rtype: bool
        """
        return self._save_in_bookmark

    @save_in_bookmark.setter
    def save_in_bookmark(self, save_in_bookmark):
        """
        Sets the save_in_bookmark of this ViewStateItem.

        :param save_in_bookmark: The save_in_bookmark of this ViewStateItem.
        :type: bool
        """

        self._save_in_bookmark = save_in_bookmark

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
        if not isinstance(other, ViewStateItem):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
