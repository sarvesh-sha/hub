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

class RemoteExceptionResult(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, type_id=None, message=None, declaring_class=None, method_name=None, file_name=None, line_number=None, cause=None):
        """
        RemoteExceptionResult - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'type_id': 'str',
            'message': 'str',
            'declaring_class': 'list[str]',
            'method_name': 'list[str]',
            'file_name': 'list[str]',
            'line_number': 'list[int]',
            'cause': 'RemoteExceptionResult'
        }

        self.attribute_map = {
            'type_id': 'typeId',
            'message': 'message',
            'declaring_class': 'declaringClass',
            'method_name': 'methodName',
            'file_name': 'fileName',
            'line_number': 'lineNumber',
            'cause': 'cause'
        }

        self._type_id = type_id
        self._message = message
        self._declaring_class = declaring_class
        self._method_name = method_name
        self._file_name = file_name
        self._line_number = line_number
        self._cause = cause


    @property
    def type_id(self):
        """
        Gets the type_id of this RemoteExceptionResult.

        :return: The type_id of this RemoteExceptionResult.
        :rtype: str
        """
        return self._type_id

    @type_id.setter
    def type_id(self, type_id):
        """
        Sets the type_id of this RemoteExceptionResult.

        :param type_id: The type_id of this RemoteExceptionResult.
        :type: str
        """

        self._type_id = type_id

    @property
    def message(self):
        """
        Gets the message of this RemoteExceptionResult.

        :return: The message of this RemoteExceptionResult.
        :rtype: str
        """
        return self._message

    @message.setter
    def message(self, message):
        """
        Sets the message of this RemoteExceptionResult.

        :param message: The message of this RemoteExceptionResult.
        :type: str
        """

        self._message = message

    @property
    def declaring_class(self):
        """
        Gets the declaring_class of this RemoteExceptionResult.

        :return: The declaring_class of this RemoteExceptionResult.
        :rtype: list[str]
        """
        return self._declaring_class

    @declaring_class.setter
    def declaring_class(self, declaring_class):
        """
        Sets the declaring_class of this RemoteExceptionResult.

        :param declaring_class: The declaring_class of this RemoteExceptionResult.
        :type: list[str]
        """

        self._declaring_class = declaring_class

    @property
    def method_name(self):
        """
        Gets the method_name of this RemoteExceptionResult.

        :return: The method_name of this RemoteExceptionResult.
        :rtype: list[str]
        """
        return self._method_name

    @method_name.setter
    def method_name(self, method_name):
        """
        Sets the method_name of this RemoteExceptionResult.

        :param method_name: The method_name of this RemoteExceptionResult.
        :type: list[str]
        """

        self._method_name = method_name

    @property
    def file_name(self):
        """
        Gets the file_name of this RemoteExceptionResult.

        :return: The file_name of this RemoteExceptionResult.
        :rtype: list[str]
        """
        return self._file_name

    @file_name.setter
    def file_name(self, file_name):
        """
        Sets the file_name of this RemoteExceptionResult.

        :param file_name: The file_name of this RemoteExceptionResult.
        :type: list[str]
        """

        self._file_name = file_name

    @property
    def line_number(self):
        """
        Gets the line_number of this RemoteExceptionResult.

        :return: The line_number of this RemoteExceptionResult.
        :rtype: list[int]
        """
        return self._line_number

    @line_number.setter
    def line_number(self, line_number):
        """
        Sets the line_number of this RemoteExceptionResult.

        :param line_number: The line_number of this RemoteExceptionResult.
        :type: list[int]
        """

        self._line_number = line_number

    @property
    def cause(self):
        """
        Gets the cause of this RemoteExceptionResult.

        :return: The cause of this RemoteExceptionResult.
        :rtype: RemoteExceptionResult
        """
        return self._cause

    @cause.setter
    def cause(self, cause):
        """
        Sets the cause of this RemoteExceptionResult.

        :param cause: The cause of this RemoteExceptionResult.
        :type: RemoteExceptionResult
        """

        self._cause = cause

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
        if not isinstance(other, RemoteExceptionResult):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
