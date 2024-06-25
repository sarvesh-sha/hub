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

class RpcMessageCallback(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, call_id=None, callback_id=None, descriptor=None):
        """
        RpcMessageCallback - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'call_id': 'str',
            'callback_id': 'str',
            'descriptor': 'RemoteCallDescriptor',
            'discriminator_type': 'str'
        }

        self.attribute_map = {
            'call_id': 'callId',
            'callback_id': 'callbackId',
            'descriptor': 'descriptor',
            'discriminator_type': 'type'
        }

        self._call_id = call_id
        self._callback_id = callback_id
        self._descriptor = descriptor

    @property
    def discriminator_type(self):
        return "RpcMessageCallback"

    @discriminator_type.setter
    def discriminator_type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def call_id(self):
        """
        Gets the call_id of this RpcMessageCallback.

        :return: The call_id of this RpcMessageCallback.
        :rtype: str
        """
        return self._call_id

    @call_id.setter
    def call_id(self, call_id):
        """
        Sets the call_id of this RpcMessageCallback.

        :param call_id: The call_id of this RpcMessageCallback.
        :type: str
        """

        self._call_id = call_id

    @property
    def callback_id(self):
        """
        Gets the callback_id of this RpcMessageCallback.

        :return: The callback_id of this RpcMessageCallback.
        :rtype: str
        """
        return self._callback_id

    @callback_id.setter
    def callback_id(self, callback_id):
        """
        Sets the callback_id of this RpcMessageCallback.

        :param callback_id: The callback_id of this RpcMessageCallback.
        :type: str
        """

        self._callback_id = callback_id

    @property
    def descriptor(self):
        """
        Gets the descriptor of this RpcMessageCallback.

        :return: The descriptor of this RpcMessageCallback.
        :rtype: RemoteCallDescriptor
        """
        return self._descriptor

    @descriptor.setter
    def descriptor(self, descriptor):
        """
        Sets the descriptor of this RpcMessageCallback.

        :param descriptor: The descriptor of this RpcMessageCallback.
        :type: RemoteCallDescriptor
        """

        self._descriptor = descriptor

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
        if not isinstance(other, RpcMessageCallback):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
