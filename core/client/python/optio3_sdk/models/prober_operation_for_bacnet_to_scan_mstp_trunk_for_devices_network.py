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

class ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, transport=None, network_number=None):
        """
        ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'transport': 'TransportAddress',
            'network_number': 'int'
        }

        self.attribute_map = {
            'transport': 'transport',
            'network_number': 'networkNumber'
        }

        self._transport = transport
        self._network_number = network_number


    @property
    def transport(self):
        """
        Gets the transport of this ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.

        :return: The transport of this ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.
        :rtype: TransportAddress
        """
        return self._transport

    @transport.setter
    def transport(self, transport):
        """
        Sets the transport of this ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.

        :param transport: The transport of this ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.
        :type: TransportAddress
        """

        self._transport = transport

    @property
    def network_number(self):
        """
        Gets the network_number of this ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.

        :return: The network_number of this ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.
        :rtype: int
        """
        return self._network_number

    @network_number.setter
    def network_number(self, network_number):
        """
        Sets the network_number of this ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.

        :param network_number: The network_number of this ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork.
        :type: int
        """

        self._network_number = network_number

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
        if not isinstance(other, ProberOperationForBACnetToScanMstpTrunkForDevicesNetwork):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other