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

class RemoteArgument(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, type_id=None, value=None, callback_id=None):
        """
        RemoteArgument - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'type_id': 'str',
            'value': 'object',
            'callback_id': 'str'
        }

        self.attribute_map = {
            'type_id': 'typeId',
            'value': 'value',
            'callback_id': 'callbackId'
        }

        self._type_id = type_id
        self._value = value
        self._callback_id = callback_id


    @property
    def type_id(self):
        """
        Gets the type_id of this RemoteArgument.

        :return: The type_id of this RemoteArgument.
        :rtype: str
        """
        return self._type_id

    @type_id.setter
    def type_id(self, type_id):
        """
        Sets the type_id of this RemoteArgument.

        :param type_id: The type_id of this RemoteArgument.
        :type: str
        """

        self._type_id = type_id

    @property
    def value(self):
        """
        Gets the value of this RemoteArgument.

        :return: The value of this RemoteArgument.
        :rtype: object
        """
        return self._value

    @value.setter
    def value(self, value):
        """
        Sets the value of this RemoteArgument.

        :param value: The value of this RemoteArgument.
        :type: object
        """

        self._value = value

    @property
    def callback_id(self):
        """
        Gets the callback_id of this RemoteArgument.

        :return: The callback_id of this RemoteArgument.
        :rtype: str
        """
        return self._callback_id

    @callback_id.setter
    def callback_id(self, callback_id):
        """
        Sets the callback_id of this RemoteArgument.

        :param callback_id: The callback_id of this RemoteArgument.
        :type: str
        """

        self._callback_id = callback_id

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
        if not isinstance(other, RemoteArgument):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
