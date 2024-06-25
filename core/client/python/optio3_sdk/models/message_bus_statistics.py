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

class MessageBusStatistics(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sessions=None, packet_tx=None, packet_tx_bytes=None, packet_tx_bytes_resent=None, message_tx=None, packet_rx=None, packet_rx_bytes=None, packet_rx_bytes_resent=None, message_rx=None):
        """
        MessageBusStatistics - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'sessions': 'int',
            'packet_tx': 'int',
            'packet_tx_bytes': 'int',
            'packet_tx_bytes_resent': 'int',
            'message_tx': 'int',
            'packet_rx': 'int',
            'packet_rx_bytes': 'int',
            'packet_rx_bytes_resent': 'int',
            'message_rx': 'int'
        }

        self.attribute_map = {
            'sessions': 'sessions',
            'packet_tx': 'packetTx',
            'packet_tx_bytes': 'packetTxBytes',
            'packet_tx_bytes_resent': 'packetTxBytesResent',
            'message_tx': 'messageTx',
            'packet_rx': 'packetRx',
            'packet_rx_bytes': 'packetRxBytes',
            'packet_rx_bytes_resent': 'packetRxBytesResent',
            'message_rx': 'messageRx'
        }

        self._sessions = sessions
        self._packet_tx = packet_tx
        self._packet_tx_bytes = packet_tx_bytes
        self._packet_tx_bytes_resent = packet_tx_bytes_resent
        self._message_tx = message_tx
        self._packet_rx = packet_rx
        self._packet_rx_bytes = packet_rx_bytes
        self._packet_rx_bytes_resent = packet_rx_bytes_resent
        self._message_rx = message_rx


    @property
    def sessions(self):
        """
        Gets the sessions of this MessageBusStatistics.

        :return: The sessions of this MessageBusStatistics.
        :rtype: int
        """
        return self._sessions

    @sessions.setter
    def sessions(self, sessions):
        """
        Sets the sessions of this MessageBusStatistics.

        :param sessions: The sessions of this MessageBusStatistics.
        :type: int
        """

        self._sessions = sessions

    @property
    def packet_tx(self):
        """
        Gets the packet_tx of this MessageBusStatistics.

        :return: The packet_tx of this MessageBusStatistics.
        :rtype: int
        """
        return self._packet_tx

    @packet_tx.setter
    def packet_tx(self, packet_tx):
        """
        Sets the packet_tx of this MessageBusStatistics.

        :param packet_tx: The packet_tx of this MessageBusStatistics.
        :type: int
        """

        self._packet_tx = packet_tx

    @property
    def packet_tx_bytes(self):
        """
        Gets the packet_tx_bytes of this MessageBusStatistics.

        :return: The packet_tx_bytes of this MessageBusStatistics.
        :rtype: int
        """
        return self._packet_tx_bytes

    @packet_tx_bytes.setter
    def packet_tx_bytes(self, packet_tx_bytes):
        """
        Sets the packet_tx_bytes of this MessageBusStatistics.

        :param packet_tx_bytes: The packet_tx_bytes of this MessageBusStatistics.
        :type: int
        """

        self._packet_tx_bytes = packet_tx_bytes

    @property
    def packet_tx_bytes_resent(self):
        """
        Gets the packet_tx_bytes_resent of this MessageBusStatistics.

        :return: The packet_tx_bytes_resent of this MessageBusStatistics.
        :rtype: int
        """
        return self._packet_tx_bytes_resent

    @packet_tx_bytes_resent.setter
    def packet_tx_bytes_resent(self, packet_tx_bytes_resent):
        """
        Sets the packet_tx_bytes_resent of this MessageBusStatistics.

        :param packet_tx_bytes_resent: The packet_tx_bytes_resent of this MessageBusStatistics.
        :type: int
        """

        self._packet_tx_bytes_resent = packet_tx_bytes_resent

    @property
    def message_tx(self):
        """
        Gets the message_tx of this MessageBusStatistics.

        :return: The message_tx of this MessageBusStatistics.
        :rtype: int
        """
        return self._message_tx

    @message_tx.setter
    def message_tx(self, message_tx):
        """
        Sets the message_tx of this MessageBusStatistics.

        :param message_tx: The message_tx of this MessageBusStatistics.
        :type: int
        """

        self._message_tx = message_tx

    @property
    def packet_rx(self):
        """
        Gets the packet_rx of this MessageBusStatistics.

        :return: The packet_rx of this MessageBusStatistics.
        :rtype: int
        """
        return self._packet_rx

    @packet_rx.setter
    def packet_rx(self, packet_rx):
        """
        Sets the packet_rx of this MessageBusStatistics.

        :param packet_rx: The packet_rx of this MessageBusStatistics.
        :type: int
        """

        self._packet_rx = packet_rx

    @property
    def packet_rx_bytes(self):
        """
        Gets the packet_rx_bytes of this MessageBusStatistics.

        :return: The packet_rx_bytes of this MessageBusStatistics.
        :rtype: int
        """
        return self._packet_rx_bytes

    @packet_rx_bytes.setter
    def packet_rx_bytes(self, packet_rx_bytes):
        """
        Sets the packet_rx_bytes of this MessageBusStatistics.

        :param packet_rx_bytes: The packet_rx_bytes of this MessageBusStatistics.
        :type: int
        """

        self._packet_rx_bytes = packet_rx_bytes

    @property
    def packet_rx_bytes_resent(self):
        """
        Gets the packet_rx_bytes_resent of this MessageBusStatistics.

        :return: The packet_rx_bytes_resent of this MessageBusStatistics.
        :rtype: int
        """
        return self._packet_rx_bytes_resent

    @packet_rx_bytes_resent.setter
    def packet_rx_bytes_resent(self, packet_rx_bytes_resent):
        """
        Sets the packet_rx_bytes_resent of this MessageBusStatistics.

        :param packet_rx_bytes_resent: The packet_rx_bytes_resent of this MessageBusStatistics.
        :type: int
        """

        self._packet_rx_bytes_resent = packet_rx_bytes_resent

    @property
    def message_rx(self):
        """
        Gets the message_rx of this MessageBusStatistics.

        :return: The message_rx of this MessageBusStatistics.
        :rtype: int
        """
        return self._message_rx

    @message_rx.setter
    def message_rx(self, message_rx):
        """
        Sets the message_rx of this MessageBusStatistics.

        :param message_rx: The message_rx of this MessageBusStatistics.
        :type: int
        """

        self._message_rx = message_rx

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
        if not isinstance(other, MessageBusStatistics):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other