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

class RegexReplacement(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, regex=None, replacement=None, case_sensitive=None, comment=None):
        """
        RegexReplacement - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'regex': 'str',
            'replacement': 'str',
            'case_sensitive': 'bool',
            'comment': 'str'
        }

        self.attribute_map = {
            'regex': 'regex',
            'replacement': 'replacement',
            'case_sensitive': 'caseSensitive',
            'comment': 'comment'
        }

        self._regex = regex
        self._replacement = replacement
        self._case_sensitive = case_sensitive
        self._comment = comment


    @property
    def regex(self):
        """
        Gets the regex of this RegexReplacement.

        :return: The regex of this RegexReplacement.
        :rtype: str
        """
        return self._regex

    @regex.setter
    def regex(self, regex):
        """
        Sets the regex of this RegexReplacement.

        :param regex: The regex of this RegexReplacement.
        :type: str
        """

        self._regex = regex

    @property
    def replacement(self):
        """
        Gets the replacement of this RegexReplacement.

        :return: The replacement of this RegexReplacement.
        :rtype: str
        """
        return self._replacement

    @replacement.setter
    def replacement(self, replacement):
        """
        Sets the replacement of this RegexReplacement.

        :param replacement: The replacement of this RegexReplacement.
        :type: str
        """

        self._replacement = replacement

    @property
    def case_sensitive(self):
        """
        Gets the case_sensitive of this RegexReplacement.

        :return: The case_sensitive of this RegexReplacement.
        :rtype: bool
        """
        return self._case_sensitive

    @case_sensitive.setter
    def case_sensitive(self, case_sensitive):
        """
        Sets the case_sensitive of this RegexReplacement.

        :param case_sensitive: The case_sensitive of this RegexReplacement.
        :type: bool
        """

        self._case_sensitive = case_sensitive

    @property
    def comment(self):
        """
        Gets the comment of this RegexReplacement.

        :return: The comment of this RegexReplacement.
        :rtype: str
        """
        return self._comment

    @comment.setter
    def comment(self, comment):
        """
        Sets the comment of this RegexReplacement.

        :param comment: The comment of this RegexReplacement.
        :type: str
        """

        self._comment = comment

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
        if not isinstance(other, RegexReplacement):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
