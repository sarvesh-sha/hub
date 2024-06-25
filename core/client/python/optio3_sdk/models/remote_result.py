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

class RemoteResult(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, type_id=None, value=None, exception=None):
        """
        RemoteResult - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'type_id': 'str',
            'value': 'object',
            'exception': 'RemoteExceptionResult'
        }

        self.attribute_map = {
            'type_id': 'typeId',
            'value': 'value',
            'exception': 'exception'
        }

        self._type_id = type_id
        self._value = value
        self._exception = exception


    @property
    def type_id(self):
        """
        Gets the type_id of this RemoteResult.

        :return: The type_id of this RemoteResult.
        :rtype: str
        """
        return self._type_id

    @type_id.setter
    def type_id(self, type_id):
        """
        Sets the type_id of this RemoteResult.

        :param type_id: The type_id of this RemoteResult.
        :type: str
        """

        self._type_id = type_id

    @property
    def value(self):
        """
        Gets the value of this RemoteResult.

        :return: The value of this RemoteResult.
        :rtype: object
        """
        return self._value

    @value.setter
    def value(self, value):
        """
        Sets the value of this RemoteResult.

        :param value: The value of this RemoteResult.
        :type: object
        """

        self._value = value

    @property
    def exception(self):
        """
        Gets the exception of this RemoteResult.

        :return: The exception of this RemoteResult.
        :rtype: RemoteExceptionResult
        """
        return self._exception

    @exception.setter
    def exception(self, exception):
        """
        Sets the exception of this RemoteResult.

        :param exception: The exception of this RemoteResult.
        :type: RemoteExceptionResult
        """

        self._exception = exception

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
        if not isinstance(other, RemoteResult):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
