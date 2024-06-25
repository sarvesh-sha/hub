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

class ProberOperationForCANbusToRawReadResults(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, frames=None):
        """
        ProberOperationForCANbusToRawReadResults - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'frames': 'list[ProberObjectCANbus]',
            'discriminator___type_result': 'str'
        }

        self.attribute_map = {
            'frames': 'frames',
            'discriminator___type_result': '__type_result'
        }

        self._frames = frames

    @property
    def discriminator___type_result(self):
        return "ProberOperationForCANbusToRawReadResults"

    @discriminator___type_result.setter
    def discriminator___type_result(self, discriminator):
        # Don't need to do anything
        return

    @property
    def frames(self):
        """
        Gets the frames of this ProberOperationForCANbusToRawReadResults.

        :return: The frames of this ProberOperationForCANbusToRawReadResults.
        :rtype: list[ProberObjectCANbus]
        """
        return self._frames

    @frames.setter
    def frames(self, frames):
        """
        Sets the frames of this ProberOperationForCANbusToRawReadResults.

        :param frames: The frames of this ProberOperationForCANbusToRawReadResults.
        :type: list[ProberObjectCANbus]
        """

        self._frames = frames

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
        if not isinstance(other, ProberOperationForCANbusToRawReadResults):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
