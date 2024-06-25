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

class LookupEntry(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, key=None, value=None, case_sensitive=None):
        """
        LookupEntry - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'key': 'str',
            'value': 'str',
            'case_sensitive': 'bool'
        }

        self.attribute_map = {
            'key': 'key',
            'value': 'value',
            'case_sensitive': 'caseSensitive'
        }

        self._key = key
        self._value = value
        self._case_sensitive = case_sensitive


    @property
    def key(self):
        """
        Gets the key of this LookupEntry.

        :return: The key of this LookupEntry.
        :rtype: str
        """
        return self._key

    @key.setter
    def key(self, key):
        """
        Sets the key of this LookupEntry.

        :param key: The key of this LookupEntry.
        :type: str
        """

        self._key = key

    @property
    def value(self):
        """
        Gets the value of this LookupEntry.

        :return: The value of this LookupEntry.
        :rtype: str
        """
        return self._value

    @value.setter
    def value(self, value):
        """
        Sets the value of this LookupEntry.

        :param value: The value of this LookupEntry.
        :type: str
        """

        self._value = value

    @property
    def case_sensitive(self):
        """
        Gets the case_sensitive of this LookupEntry.

        :return: The case_sensitive of this LookupEntry.
        :rtype: bool
        """
        return self._case_sensitive

    @case_sensitive.setter
    def case_sensitive(self, case_sensitive):
        """
        Sets the case_sensitive of this LookupEntry.

        :param case_sensitive: The case_sensitive of this LookupEntry.
        :type: bool
        """

        self._case_sensitive = case_sensitive

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
        if not isinstance(other, LookupEntry):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other