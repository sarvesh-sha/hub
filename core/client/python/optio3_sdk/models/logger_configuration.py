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

class LoggerConfiguration(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, parent=None, name=None, levels=None):
        """
        LoggerConfiguration - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'parent': 'str',
            'name': 'str',
            'levels': 'dict(str, bool)'
        }

        self.attribute_map = {
            'parent': 'parent',
            'name': 'name',
            'levels': 'levels'
        }

        self._parent = parent
        self._name = name
        self._levels = levels


    @property
    def parent(self):
        """
        Gets the parent of this LoggerConfiguration.

        :return: The parent of this LoggerConfiguration.
        :rtype: str
        """
        return self._parent

    @parent.setter
    def parent(self, parent):
        """
        Sets the parent of this LoggerConfiguration.

        :param parent: The parent of this LoggerConfiguration.
        :type: str
        """

        self._parent = parent

    @property
    def name(self):
        """
        Gets the name of this LoggerConfiguration.

        :return: The name of this LoggerConfiguration.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this LoggerConfiguration.

        :param name: The name of this LoggerConfiguration.
        :type: str
        """

        self._name = name

    @property
    def levels(self):
        """
        Gets the levels of this LoggerConfiguration.

        :return: The levels of this LoggerConfiguration.
        :rtype: dict(str, bool)
        """
        return self._levels

    @levels.setter
    def levels(self, levels):
        """
        Sets the levels of this LoggerConfiguration.

        :param levels: The levels of this LoggerConfiguration.
        :type: dict(str, bool)
        """

        self._levels = levels

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
        if not isinstance(other, LoggerConfiguration):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other