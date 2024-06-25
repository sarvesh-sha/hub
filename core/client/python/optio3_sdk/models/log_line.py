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

class LogLine(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, fd=None, host=None, thread=None, selector=None, level=None, timestamp=None, line=None, line_number=None):
        """
        LogLine - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'fd': 'int',
            'host': 'str',
            'thread': 'str',
            'selector': 'str',
            'level': 'str',
            'timestamp': 'datetime',
            'line': 'str',
            'line_number': 'int'
        }

        self.attribute_map = {
            'fd': 'fd',
            'host': 'host',
            'thread': 'thread',
            'selector': 'selector',
            'level': 'level',
            'timestamp': 'timestamp',
            'line': 'line',
            'line_number': 'lineNumber'
        }

        self._fd = fd
        self._host = host
        self._thread = thread
        self._selector = selector
        self._level = level
        self._timestamp = timestamp
        self._line = line
        self._line_number = line_number


    @property
    def fd(self):
        """
        Gets the fd of this LogLine.

        :return: The fd of this LogLine.
        :rtype: int
        """
        return self._fd

    @fd.setter
    def fd(self, fd):
        """
        Sets the fd of this LogLine.

        :param fd: The fd of this LogLine.
        :type: int
        """

        self._fd = fd

    @property
    def host(self):
        """
        Gets the host of this LogLine.

        :return: The host of this LogLine.
        :rtype: str
        """
        return self._host

    @host.setter
    def host(self, host):
        """
        Sets the host of this LogLine.

        :param host: The host of this LogLine.
        :type: str
        """

        self._host = host

    @property
    def thread(self):
        """
        Gets the thread of this LogLine.

        :return: The thread of this LogLine.
        :rtype: str
        """
        return self._thread

    @thread.setter
    def thread(self, thread):
        """
        Sets the thread of this LogLine.

        :param thread: The thread of this LogLine.
        :type: str
        """

        self._thread = thread

    @property
    def selector(self):
        """
        Gets the selector of this LogLine.

        :return: The selector of this LogLine.
        :rtype: str
        """
        return self._selector

    @selector.setter
    def selector(self, selector):
        """
        Sets the selector of this LogLine.

        :param selector: The selector of this LogLine.
        :type: str
        """

        self._selector = selector

    @property
    def level(self):
        """
        Gets the level of this LogLine.

        :return: The level of this LogLine.
        :rtype: str
        """
        return self._level

    @level.setter
    def level(self, level):
        """
        Sets the level of this LogLine.

        :param level: The level of this LogLine.
        :type: str
        """
        allowed_values = ["Error", "Warn", "Info", "Debug", "DebugVerbose", "DebugObnoxious"]
        if level is not None and level not in allowed_values:
            raise ValueError(
                "Invalid value for `level` ({0}), must be one of {1}"
                .format(level, allowed_values)
            )

        self._level = level

    @property
    def timestamp(self):
        """
        Gets the timestamp of this LogLine.

        :return: The timestamp of this LogLine.
        :rtype: datetime
        """
        return self._timestamp

    @timestamp.setter
    def timestamp(self, timestamp):
        """
        Sets the timestamp of this LogLine.

        :param timestamp: The timestamp of this LogLine.
        :type: datetime
        """

        self._timestamp = timestamp

    @property
    def line(self):
        """
        Gets the line of this LogLine.

        :return: The line of this LogLine.
        :rtype: str
        """
        return self._line

    @line.setter
    def line(self, line):
        """
        Sets the line of this LogLine.

        :param line: The line of this LogLine.
        :type: str
        """

        self._line = line

    @property
    def line_number(self):
        """
        Gets the line_number of this LogLine.

        :return: The line_number of this LogLine.
        :rtype: int
        """
        return self._line_number

    @line_number.setter
    def line_number(self, line_number):
        """
        Sets the line_number of this LogLine.

        :param line_number: The line_number of this LogLine.
        :type: int
        """

        self._line_number = line_number

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
        if not isinstance(other, LogLine):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
