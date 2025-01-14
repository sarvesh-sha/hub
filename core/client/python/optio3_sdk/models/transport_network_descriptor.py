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

class TransportNetworkDescriptor(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, transport_address=None, network_number=None, device_identifier=None):
        """
        TransportNetworkDescriptor - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'transport_address': 'str',
            'network_number': 'int',
            'device_identifier': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'transport_address': 'transportAddress',
            'network_number': 'networkNumber',
            'device_identifier': 'deviceIdentifier',
            'discriminator___type': '__type'
        }

        self._transport_address = transport_address
        self._network_number = network_number
        self._device_identifier = device_identifier

    @property
    def discriminator___type(self):
        return "TransportNetworkDescriptor"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def transport_address(self):
        """
        Gets the transport_address of this TransportNetworkDescriptor.

        :return: The transport_address of this TransportNetworkDescriptor.
        :rtype: str
        """
        return self._transport_address

    @transport_address.setter
    def transport_address(self, transport_address):
        """
        Sets the transport_address of this TransportNetworkDescriptor.

        :param transport_address: The transport_address of this TransportNetworkDescriptor.
        :type: str
        """

        self._transport_address = transport_address

    @property
    def network_number(self):
        """
        Gets the network_number of this TransportNetworkDescriptor.

        :return: The network_number of this TransportNetworkDescriptor.
        :rtype: int
        """
        return self._network_number

    @network_number.setter
    def network_number(self, network_number):
        """
        Sets the network_number of this TransportNetworkDescriptor.

        :param network_number: The network_number of this TransportNetworkDescriptor.
        :type: int
        """

        self._network_number = network_number

    @property
    def device_identifier(self):
        """
        Gets the device_identifier of this TransportNetworkDescriptor.

        :return: The device_identifier of this TransportNetworkDescriptor.
        :rtype: str
        """
        return self._device_identifier

    @device_identifier.setter
    def device_identifier(self, device_identifier):
        """
        Sets the device_identifier of this TransportNetworkDescriptor.

        :param device_identifier: The device_identifier of this TransportNetworkDescriptor.
        :type: str
        """

        self._device_identifier = device_identifier

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
        if not isinstance(other, TransportNetworkDescriptor):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
