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

class EngineValueDateTimeList(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, length=None, elements=None):
        """
        EngineValueDateTimeList - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'length': 'int',
            'elements': 'list[EngineValueDateTime]',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'length': 'length',
            'elements': 'elements',
            'discriminator___type': '__type'
        }

        self._length = length
        self._elements = elements

    @property
    def discriminator___type(self):
        return "EngineValueDateTimeList"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def length(self):
        """
        Gets the length of this EngineValueDateTimeList.

        :return: The length of this EngineValueDateTimeList.
        :rtype: int
        """
        return self._length

    @length.setter
    def length(self, length):
        """
        Sets the length of this EngineValueDateTimeList.

        :param length: The length of this EngineValueDateTimeList.
        :type: int
        """

        self._length = length

    @property
    def elements(self):
        """
        Gets the elements of this EngineValueDateTimeList.

        :return: The elements of this EngineValueDateTimeList.
        :rtype: list[EngineValueDateTime]
        """
        return self._elements

    @elements.setter
    def elements(self, elements):
        """
        Sets the elements of this EngineValueDateTimeList.

        :param elements: The elements of this EngineValueDateTimeList.
        :type: list[EngineValueDateTime]
        """

        self._elements = elements

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
        if not isinstance(other, EngineValueDateTimeList):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
