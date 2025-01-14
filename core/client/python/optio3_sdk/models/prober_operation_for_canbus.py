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

class ProberOperationForCANbus(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, port=None, frequency=None, no_termination=None, invert=None):
        """
        ProberOperationForCANbus - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'port': 'str',
            'frequency': 'int',
            'no_termination': 'bool',
            'invert': 'bool',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'port': 'port',
            'frequency': 'frequency',
            'no_termination': 'noTermination',
            'invert': 'invert',
            'discriminator___type': '__type'
        }

        self._port = port
        self._frequency = frequency
        self._no_termination = no_termination
        self._invert = invert


    @property
    def port(self):
        """
        Gets the port of this ProberOperationForCANbus.

        :return: The port of this ProberOperationForCANbus.
        :rtype: str
        """
        return self._port

    @port.setter
    def port(self, port):
        """
        Sets the port of this ProberOperationForCANbus.

        :param port: The port of this ProberOperationForCANbus.
        :type: str
        """

        self._port = port

    @property
    def frequency(self):
        """
        Gets the frequency of this ProberOperationForCANbus.

        :return: The frequency of this ProberOperationForCANbus.
        :rtype: int
        """
        return self._frequency

    @frequency.setter
    def frequency(self, frequency):
        """
        Sets the frequency of this ProberOperationForCANbus.

        :param frequency: The frequency of this ProberOperationForCANbus.
        :type: int
        """

        self._frequency = frequency

    @property
    def no_termination(self):
        """
        Gets the no_termination of this ProberOperationForCANbus.

        :return: The no_termination of this ProberOperationForCANbus.
        :rtype: bool
        """
        return self._no_termination

    @no_termination.setter
    def no_termination(self, no_termination):
        """
        Sets the no_termination of this ProberOperationForCANbus.

        :param no_termination: The no_termination of this ProberOperationForCANbus.
        :type: bool
        """

        self._no_termination = no_termination

    @property
    def invert(self):
        """
        Gets the invert of this ProberOperationForCANbus.

        :return: The invert of this ProberOperationForCANbus.
        :rtype: bool
        """
        return self._invert

    @invert.setter
    def invert(self, invert):
        """
        Sets the invert of this ProberOperationForCANbus.

        :param invert: The invert of this ProberOperationForCANbus.
        :type: bool
        """

        self._invert = invert

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
        if not isinstance(other, ProberOperationForCANbus):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
