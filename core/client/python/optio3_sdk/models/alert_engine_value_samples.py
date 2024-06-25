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

class AlertEngineValueSamples(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, length=None, control_point=None, timestamps=None):
        """
        AlertEngineValueSamples - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'length': 'int',
            'control_point': 'RecordIdentity',
            'timestamps': 'list[datetime]',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'length': 'length',
            'control_point': 'controlPoint',
            'timestamps': 'timestamps',
            'discriminator___type': '__type'
        }

        self._length = length
        self._control_point = control_point
        self._timestamps = timestamps

    @property
    def discriminator___type(self):
        return "AlertEngineValueSamples"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def length(self):
        """
        Gets the length of this AlertEngineValueSamples.

        :return: The length of this AlertEngineValueSamples.
        :rtype: int
        """
        return self._length

    @length.setter
    def length(self, length):
        """
        Sets the length of this AlertEngineValueSamples.

        :param length: The length of this AlertEngineValueSamples.
        :type: int
        """

        self._length = length

    @property
    def control_point(self):
        """
        Gets the control_point of this AlertEngineValueSamples.

        :return: The control_point of this AlertEngineValueSamples.
        :rtype: RecordIdentity
        """
        return self._control_point

    @control_point.setter
    def control_point(self, control_point):
        """
        Sets the control_point of this AlertEngineValueSamples.

        :param control_point: The control_point of this AlertEngineValueSamples.
        :type: RecordIdentity
        """

        self._control_point = control_point

    @property
    def timestamps(self):
        """
        Gets the timestamps of this AlertEngineValueSamples.

        :return: The timestamps of this AlertEngineValueSamples.
        :rtype: list[datetime]
        """
        return self._timestamps

    @timestamps.setter
    def timestamps(self, timestamps):
        """
        Sets the timestamps of this AlertEngineValueSamples.

        :param timestamps: The timestamps of this AlertEngineValueSamples.
        :type: list[datetime]
        """

        self._timestamps = timestamps

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
        if not isinstance(other, AlertEngineValueSamples):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other