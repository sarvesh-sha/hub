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

class SkippedBACnetDevice(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, network_number=None, instance_number=None, transport_address=None, transport_port=None, notes=None):
        """
        SkippedBACnetDevice - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'network_number': 'int',
            'instance_number': 'int',
            'transport_address': 'str',
            'transport_port': 'int',
            'notes': 'str'
        }

        self.attribute_map = {
            'network_number': 'networkNumber',
            'instance_number': 'instanceNumber',
            'transport_address': 'transportAddress',
            'transport_port': 'transportPort',
            'notes': 'notes'
        }

        self._network_number = network_number
        self._instance_number = instance_number
        self._transport_address = transport_address
        self._transport_port = transport_port
        self._notes = notes


    @property
    def network_number(self):
        """
        Gets the network_number of this SkippedBACnetDevice.

        :return: The network_number of this SkippedBACnetDevice.
        :rtype: int
        """
        return self._network_number

    @network_number.setter
    def network_number(self, network_number):
        """
        Sets the network_number of this SkippedBACnetDevice.

        :param network_number: The network_number of this SkippedBACnetDevice.
        :type: int
        """

        self._network_number = network_number

    @property
    def instance_number(self):
        """
        Gets the instance_number of this SkippedBACnetDevice.

        :return: The instance_number of this SkippedBACnetDevice.
        :rtype: int
        """
        return self._instance_number

    @instance_number.setter
    def instance_number(self, instance_number):
        """
        Sets the instance_number of this SkippedBACnetDevice.

        :param instance_number: The instance_number of this SkippedBACnetDevice.
        :type: int
        """

        self._instance_number = instance_number

    @property
    def transport_address(self):
        """
        Gets the transport_address of this SkippedBACnetDevice.

        :return: The transport_address of this SkippedBACnetDevice.
        :rtype: str
        """
        return self._transport_address

    @transport_address.setter
    def transport_address(self, transport_address):
        """
        Sets the transport_address of this SkippedBACnetDevice.

        :param transport_address: The transport_address of this SkippedBACnetDevice.
        :type: str
        """

        self._transport_address = transport_address

    @property
    def transport_port(self):
        """
        Gets the transport_port of this SkippedBACnetDevice.

        :return: The transport_port of this SkippedBACnetDevice.
        :rtype: int
        """
        return self._transport_port

    @transport_port.setter
    def transport_port(self, transport_port):
        """
        Sets the transport_port of this SkippedBACnetDevice.

        :param transport_port: The transport_port of this SkippedBACnetDevice.
        :type: int
        """

        self._transport_port = transport_port

    @property
    def notes(self):
        """
        Gets the notes of this SkippedBACnetDevice.

        :return: The notes of this SkippedBACnetDevice.
        :rtype: str
        """
        return self._notes

    @notes.setter
    def notes(self, notes):
        """
        Sets the notes of this SkippedBACnetDevice.

        :param notes: The notes of this SkippedBACnetDevice.
        :type: str
        """

        self._notes = notes

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
        if not isinstance(other, SkippedBACnetDevice):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
