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

class ProberOperationForBACnetToDiscoverRoutersRouterNetwork(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, device=None, networks=None):
        """
        ProberOperationForBACnetToDiscoverRoutersRouterNetwork - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'device': 'BACnetDeviceDescriptor',
            'networks': 'list[int]'
        }

        self.attribute_map = {
            'device': 'device',
            'networks': 'networks'
        }

        self._device = device
        self._networks = networks


    @property
    def device(self):
        """
        Gets the device of this ProberOperationForBACnetToDiscoverRoutersRouterNetwork.

        :return: The device of this ProberOperationForBACnetToDiscoverRoutersRouterNetwork.
        :rtype: BACnetDeviceDescriptor
        """
        return self._device

    @device.setter
    def device(self, device):
        """
        Sets the device of this ProberOperationForBACnetToDiscoverRoutersRouterNetwork.

        :param device: The device of this ProberOperationForBACnetToDiscoverRoutersRouterNetwork.
        :type: BACnetDeviceDescriptor
        """

        self._device = device

    @property
    def networks(self):
        """
        Gets the networks of this ProberOperationForBACnetToDiscoverRoutersRouterNetwork.

        :return: The networks of this ProberOperationForBACnetToDiscoverRoutersRouterNetwork.
        :rtype: list[int]
        """
        return self._networks

    @networks.setter
    def networks(self, networks):
        """
        Sets the networks of this ProberOperationForBACnetToDiscoverRoutersRouterNetwork.

        :param networks: The networks of this ProberOperationForBACnetToDiscoverRoutersRouterNetwork.
        :type: list[int]
        """

        self._networks = networks

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
        if not isinstance(other, ProberOperationForBACnetToDiscoverRoutersRouterNetwork):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
