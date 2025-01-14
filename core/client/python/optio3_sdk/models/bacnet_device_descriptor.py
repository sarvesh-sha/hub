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

class BACnetDeviceDescriptor(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, address=None, bacnet_address=None, transport=None, segmentation=None, max_adpu=None):
        """
        BACnetDeviceDescriptor - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'address': 'BACnetDeviceAddress',
            'bacnet_address': 'BACnetAddress',
            'transport': 'TransportAddress',
            'segmentation': 'str',
            'max_adpu': 'int',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'address': 'address',
            'bacnet_address': 'bacnetAddress',
            'transport': 'transport',
            'segmentation': 'segmentation',
            'max_adpu': 'maxAdpu',
            'discriminator___type': '__type'
        }

        self._address = address
        self._bacnet_address = bacnet_address
        self._transport = transport
        self._segmentation = segmentation
        self._max_adpu = max_adpu

    @property
    def discriminator___type(self):
        return "BACnetDeviceDescriptor"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def address(self):
        """
        Gets the address of this BACnetDeviceDescriptor.

        :return: The address of this BACnetDeviceDescriptor.
        :rtype: BACnetDeviceAddress
        """
        return self._address

    @address.setter
    def address(self, address):
        """
        Sets the address of this BACnetDeviceDescriptor.

        :param address: The address of this BACnetDeviceDescriptor.
        :type: BACnetDeviceAddress
        """

        self._address = address

    @property
    def bacnet_address(self):
        """
        Gets the bacnet_address of this BACnetDeviceDescriptor.

        :return: The bacnet_address of this BACnetDeviceDescriptor.
        :rtype: BACnetAddress
        """
        return self._bacnet_address

    @bacnet_address.setter
    def bacnet_address(self, bacnet_address):
        """
        Sets the bacnet_address of this BACnetDeviceDescriptor.

        :param bacnet_address: The bacnet_address of this BACnetDeviceDescriptor.
        :type: BACnetAddress
        """

        self._bacnet_address = bacnet_address

    @property
    def transport(self):
        """
        Gets the transport of this BACnetDeviceDescriptor.

        :return: The transport of this BACnetDeviceDescriptor.
        :rtype: TransportAddress
        """
        return self._transport

    @transport.setter
    def transport(self, transport):
        """
        Sets the transport of this BACnetDeviceDescriptor.

        :param transport: The transport of this BACnetDeviceDescriptor.
        :type: TransportAddress
        """

        self._transport = transport

    @property
    def segmentation(self):
        """
        Gets the segmentation of this BACnetDeviceDescriptor.

        :return: The segmentation of this BACnetDeviceDescriptor.
        :rtype: str
        """
        return self._segmentation

    @segmentation.setter
    def segmentation(self, segmentation):
        """
        Sets the segmentation of this BACnetDeviceDescriptor.

        :param segmentation: The segmentation of this BACnetDeviceDescriptor.
        :type: str
        """
        allowed_values = ["segmented_both", "segmented_transmit", "segmented_receive", "no_segmentation"]
        if segmentation is not None and segmentation not in allowed_values:
            raise ValueError(
                "Invalid value for `segmentation` ({0}), must be one of {1}"
                .format(segmentation, allowed_values)
            )

        self._segmentation = segmentation

    @property
    def max_adpu(self):
        """
        Gets the max_adpu of this BACnetDeviceDescriptor.

        :return: The max_adpu of this BACnetDeviceDescriptor.
        :rtype: int
        """
        return self._max_adpu

    @max_adpu.setter
    def max_adpu(self, max_adpu):
        """
        Sets the max_adpu of this BACnetDeviceDescriptor.

        :param max_adpu: The max_adpu of this BACnetDeviceDescriptor.
        :type: int
        """

        self._max_adpu = max_adpu

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
        if not isinstance(other, BACnetDeviceDescriptor):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

