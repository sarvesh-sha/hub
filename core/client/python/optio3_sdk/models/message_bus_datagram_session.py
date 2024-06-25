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

class MessageBusDatagramSession(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, session_id=None, context_sys_id=None, display_name=None, udp_address=None, last_packet=None, rpc_id=None, statistics=None):
        """
        MessageBusDatagramSession - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'session_id': 'str',
            'context_sys_id': 'str',
            'display_name': 'str',
            'udp_address': 'str',
            'last_packet': 'datetime',
            'rpc_id': 'str',
            'statistics': 'MessageBusStatistics'
        }

        self.attribute_map = {
            'session_id': 'sessionId',
            'context_sys_id': 'contextSysId',
            'display_name': 'displayName',
            'udp_address': 'udpAddress',
            'last_packet': 'lastPacket',
            'rpc_id': 'rpcId',
            'statistics': 'statistics'
        }

        self._session_id = session_id
        self._context_sys_id = context_sys_id
        self._display_name = display_name
        self._udp_address = udp_address
        self._last_packet = last_packet
        self._rpc_id = rpc_id
        self._statistics = statistics


    @property
    def session_id(self):
        """
        Gets the session_id of this MessageBusDatagramSession.

        :return: The session_id of this MessageBusDatagramSession.
        :rtype: str
        """
        return self._session_id

    @session_id.setter
    def session_id(self, session_id):
        """
        Sets the session_id of this MessageBusDatagramSession.

        :param session_id: The session_id of this MessageBusDatagramSession.
        :type: str
        """

        self._session_id = session_id

    @property
    def context_sys_id(self):
        """
        Gets the context_sys_id of this MessageBusDatagramSession.

        :return: The context_sys_id of this MessageBusDatagramSession.
        :rtype: str
        """
        return self._context_sys_id

    @context_sys_id.setter
    def context_sys_id(self, context_sys_id):
        """
        Sets the context_sys_id of this MessageBusDatagramSession.

        :param context_sys_id: The context_sys_id of this MessageBusDatagramSession.
        :type: str
        """

        self._context_sys_id = context_sys_id

    @property
    def display_name(self):
        """
        Gets the display_name of this MessageBusDatagramSession.

        :return: The display_name of this MessageBusDatagramSession.
        :rtype: str
        """
        return self._display_name

    @display_name.setter
    def display_name(self, display_name):
        """
        Sets the display_name of this MessageBusDatagramSession.

        :param display_name: The display_name of this MessageBusDatagramSession.
        :type: str
        """

        self._display_name = display_name

    @property
    def udp_address(self):
        """
        Gets the udp_address of this MessageBusDatagramSession.

        :return: The udp_address of this MessageBusDatagramSession.
        :rtype: str
        """
        return self._udp_address

    @udp_address.setter
    def udp_address(self, udp_address):
        """
        Sets the udp_address of this MessageBusDatagramSession.

        :param udp_address: The udp_address of this MessageBusDatagramSession.
        :type: str
        """

        self._udp_address = udp_address

    @property
    def last_packet(self):
        """
        Gets the last_packet of this MessageBusDatagramSession.

        :return: The last_packet of this MessageBusDatagramSession.
        :rtype: datetime
        """
        return self._last_packet

    @last_packet.setter
    def last_packet(self, last_packet):
        """
        Sets the last_packet of this MessageBusDatagramSession.

        :param last_packet: The last_packet of this MessageBusDatagramSession.
        :type: datetime
        """

        self._last_packet = last_packet

    @property
    def rpc_id(self):
        """
        Gets the rpc_id of this MessageBusDatagramSession.

        :return: The rpc_id of this MessageBusDatagramSession.
        :rtype: str
        """
        return self._rpc_id

    @rpc_id.setter
    def rpc_id(self, rpc_id):
        """
        Sets the rpc_id of this MessageBusDatagramSession.

        :param rpc_id: The rpc_id of this MessageBusDatagramSession.
        :type: str
        """

        self._rpc_id = rpc_id

    @property
    def statistics(self):
        """
        Gets the statistics of this MessageBusDatagramSession.

        :return: The statistics of this MessageBusDatagramSession.
        :rtype: MessageBusStatistics
        """
        return self._statistics

    @statistics.setter
    def statistics(self, statistics):
        """
        Sets the statistics of this MessageBusDatagramSession.

        :param statistics: The statistics of this MessageBusDatagramSession.
        :type: MessageBusStatistics
        """

        self._statistics = statistics

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
        if not isinstance(other, MessageBusDatagramSession):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other