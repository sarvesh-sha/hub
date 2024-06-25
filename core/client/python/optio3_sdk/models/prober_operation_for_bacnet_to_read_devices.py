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

class ProberOperationForBACnetToReadDevices(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, cidr=None, static_address=None, network_interface=None, use_udp=None, udp_port=None, use_ethernet=None, bbmds=None, limit_scan=None, default_timeout=None, max_parallel_requests_per_host=None, max_parallel_requests_per_network=None, limit_packet_rate=None, target_devices=None):
        """
        ProberOperationForBACnetToReadDevices - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'cidr': 'str',
            'static_address': 'str',
            'network_interface': 'str',
            'use_udp': 'bool',
            'udp_port': 'int',
            'use_ethernet': 'bool',
            'bbmds': 'list[BACnetBBMD]',
            'limit_scan': 'WhoIsRange',
            'default_timeout': 'int',
            'max_parallel_requests_per_host': 'int',
            'max_parallel_requests_per_network': 'int',
            'limit_packet_rate': 'int',
            'target_devices': 'list[BACnetDeviceDescriptor]',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'cidr': 'cidr',
            'static_address': 'staticAddress',
            'network_interface': 'networkInterface',
            'use_udp': 'useUDP',
            'udp_port': 'udpPort',
            'use_ethernet': 'useEthernet',
            'bbmds': 'bbmds',
            'limit_scan': 'limitScan',
            'default_timeout': 'defaultTimeout',
            'max_parallel_requests_per_host': 'maxParallelRequestsPerHost',
            'max_parallel_requests_per_network': 'maxParallelRequestsPerNetwork',
            'limit_packet_rate': 'limitPacketRate',
            'target_devices': 'targetDevices',
            'discriminator___type': '__type'
        }

        self._cidr = cidr
        self._static_address = static_address
        self._network_interface = network_interface
        self._use_udp = use_udp
        self._udp_port = udp_port
        self._use_ethernet = use_ethernet
        self._bbmds = bbmds
        self._limit_scan = limit_scan
        self._default_timeout = default_timeout
        self._max_parallel_requests_per_host = max_parallel_requests_per_host
        self._max_parallel_requests_per_network = max_parallel_requests_per_network
        self._limit_packet_rate = limit_packet_rate
        self._target_devices = target_devices

    @property
    def discriminator___type(self):
        return "ProberOperationForBACnetToReadDevices"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def cidr(self):
        """
        Gets the cidr of this ProberOperationForBACnetToReadDevices.

        :return: The cidr of this ProberOperationForBACnetToReadDevices.
        :rtype: str
        """
        return self._cidr

    @cidr.setter
    def cidr(self, cidr):
        """
        Sets the cidr of this ProberOperationForBACnetToReadDevices.

        :param cidr: The cidr of this ProberOperationForBACnetToReadDevices.
        :type: str
        """

        self._cidr = cidr

    @property
    def static_address(self):
        """
        Gets the static_address of this ProberOperationForBACnetToReadDevices.

        :return: The static_address of this ProberOperationForBACnetToReadDevices.
        :rtype: str
        """
        return self._static_address

    @static_address.setter
    def static_address(self, static_address):
        """
        Sets the static_address of this ProberOperationForBACnetToReadDevices.

        :param static_address: The static_address of this ProberOperationForBACnetToReadDevices.
        :type: str
        """

        self._static_address = static_address

    @property
    def network_interface(self):
        """
        Gets the network_interface of this ProberOperationForBACnetToReadDevices.

        :return: The network_interface of this ProberOperationForBACnetToReadDevices.
        :rtype: str
        """
        return self._network_interface

    @network_interface.setter
    def network_interface(self, network_interface):
        """
        Sets the network_interface of this ProberOperationForBACnetToReadDevices.

        :param network_interface: The network_interface of this ProberOperationForBACnetToReadDevices.
        :type: str
        """

        self._network_interface = network_interface

    @property
    def use_udp(self):
        """
        Gets the use_udp of this ProberOperationForBACnetToReadDevices.

        :return: The use_udp of this ProberOperationForBACnetToReadDevices.
        :rtype: bool
        """
        return self._use_udp

    @use_udp.setter
    def use_udp(self, use_udp):
        """
        Sets the use_udp of this ProberOperationForBACnetToReadDevices.

        :param use_udp: The use_udp of this ProberOperationForBACnetToReadDevices.
        :type: bool
        """

        self._use_udp = use_udp

    @property
    def udp_port(self):
        """
        Gets the udp_port of this ProberOperationForBACnetToReadDevices.

        :return: The udp_port of this ProberOperationForBACnetToReadDevices.
        :rtype: int
        """
        return self._udp_port

    @udp_port.setter
    def udp_port(self, udp_port):
        """
        Sets the udp_port of this ProberOperationForBACnetToReadDevices.

        :param udp_port: The udp_port of this ProberOperationForBACnetToReadDevices.
        :type: int
        """

        self._udp_port = udp_port

    @property
    def use_ethernet(self):
        """
        Gets the use_ethernet of this ProberOperationForBACnetToReadDevices.

        :return: The use_ethernet of this ProberOperationForBACnetToReadDevices.
        :rtype: bool
        """
        return self._use_ethernet

    @use_ethernet.setter
    def use_ethernet(self, use_ethernet):
        """
        Sets the use_ethernet of this ProberOperationForBACnetToReadDevices.

        :param use_ethernet: The use_ethernet of this ProberOperationForBACnetToReadDevices.
        :type: bool
        """

        self._use_ethernet = use_ethernet

    @property
    def bbmds(self):
        """
        Gets the bbmds of this ProberOperationForBACnetToReadDevices.

        :return: The bbmds of this ProberOperationForBACnetToReadDevices.
        :rtype: list[BACnetBBMD]
        """
        return self._bbmds

    @bbmds.setter
    def bbmds(self, bbmds):
        """
        Sets the bbmds of this ProberOperationForBACnetToReadDevices.

        :param bbmds: The bbmds of this ProberOperationForBACnetToReadDevices.
        :type: list[BACnetBBMD]
        """

        self._bbmds = bbmds

    @property
    def limit_scan(self):
        """
        Gets the limit_scan of this ProberOperationForBACnetToReadDevices.

        :return: The limit_scan of this ProberOperationForBACnetToReadDevices.
        :rtype: WhoIsRange
        """
        return self._limit_scan

    @limit_scan.setter
    def limit_scan(self, limit_scan):
        """
        Sets the limit_scan of this ProberOperationForBACnetToReadDevices.

        :param limit_scan: The limit_scan of this ProberOperationForBACnetToReadDevices.
        :type: WhoIsRange
        """

        self._limit_scan = limit_scan

    @property
    def default_timeout(self):
        """
        Gets the default_timeout of this ProberOperationForBACnetToReadDevices.

        :return: The default_timeout of this ProberOperationForBACnetToReadDevices.
        :rtype: int
        """
        return self._default_timeout

    @default_timeout.setter
    def default_timeout(self, default_timeout):
        """
        Sets the default_timeout of this ProberOperationForBACnetToReadDevices.

        :param default_timeout: The default_timeout of this ProberOperationForBACnetToReadDevices.
        :type: int
        """

        self._default_timeout = default_timeout

    @property
    def max_parallel_requests_per_host(self):
        """
        Gets the max_parallel_requests_per_host of this ProberOperationForBACnetToReadDevices.

        :return: The max_parallel_requests_per_host of this ProberOperationForBACnetToReadDevices.
        :rtype: int
        """
        return self._max_parallel_requests_per_host

    @max_parallel_requests_per_host.setter
    def max_parallel_requests_per_host(self, max_parallel_requests_per_host):
        """
        Sets the max_parallel_requests_per_host of this ProberOperationForBACnetToReadDevices.

        :param max_parallel_requests_per_host: The max_parallel_requests_per_host of this ProberOperationForBACnetToReadDevices.
        :type: int
        """

        self._max_parallel_requests_per_host = max_parallel_requests_per_host

    @property
    def max_parallel_requests_per_network(self):
        """
        Gets the max_parallel_requests_per_network of this ProberOperationForBACnetToReadDevices.

        :return: The max_parallel_requests_per_network of this ProberOperationForBACnetToReadDevices.
        :rtype: int
        """
        return self._max_parallel_requests_per_network

    @max_parallel_requests_per_network.setter
    def max_parallel_requests_per_network(self, max_parallel_requests_per_network):
        """
        Sets the max_parallel_requests_per_network of this ProberOperationForBACnetToReadDevices.

        :param max_parallel_requests_per_network: The max_parallel_requests_per_network of this ProberOperationForBACnetToReadDevices.
        :type: int
        """

        self._max_parallel_requests_per_network = max_parallel_requests_per_network

    @property
    def limit_packet_rate(self):
        """
        Gets the limit_packet_rate of this ProberOperationForBACnetToReadDevices.

        :return: The limit_packet_rate of this ProberOperationForBACnetToReadDevices.
        :rtype: int
        """
        return self._limit_packet_rate

    @limit_packet_rate.setter
    def limit_packet_rate(self, limit_packet_rate):
        """
        Sets the limit_packet_rate of this ProberOperationForBACnetToReadDevices.

        :param limit_packet_rate: The limit_packet_rate of this ProberOperationForBACnetToReadDevices.
        :type: int
        """

        self._limit_packet_rate = limit_packet_rate

    @property
    def target_devices(self):
        """
        Gets the target_devices of this ProberOperationForBACnetToReadDevices.

        :return: The target_devices of this ProberOperationForBACnetToReadDevices.
        :rtype: list[BACnetDeviceDescriptor]
        """
        return self._target_devices

    @target_devices.setter
    def target_devices(self, target_devices):
        """
        Sets the target_devices of this ProberOperationForBACnetToReadDevices.

        :param target_devices: The target_devices of this ProberOperationForBACnetToReadDevices.
        :type: list[BACnetDeviceDescriptor]
        """

        self._target_devices = target_devices

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
        if not isinstance(other, ProberOperationForBACnetToReadDevices):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other