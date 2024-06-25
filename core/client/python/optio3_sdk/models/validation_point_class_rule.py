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

class ValidationPointClassRule(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, point_class_id=None, is_binary=None, min_value=None, max_value=None, allowable_object_types=None):
        """
        ValidationPointClassRule - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'point_class_id': 'str',
            'is_binary': 'bool',
            'min_value': 'float',
            'max_value': 'float',
            'allowable_object_types': 'list[str]'
        }

        self.attribute_map = {
            'point_class_id': 'pointClassId',
            'is_binary': 'isBinary',
            'min_value': 'minValue',
            'max_value': 'maxValue',
            'allowable_object_types': 'allowableObjectTypes'
        }

        self._point_class_id = point_class_id
        self._is_binary = is_binary
        self._min_value = min_value
        self._max_value = max_value
        self._allowable_object_types = allowable_object_types


    @property
    def point_class_id(self):
        """
        Gets the point_class_id of this ValidationPointClassRule.

        :return: The point_class_id of this ValidationPointClassRule.
        :rtype: str
        """
        return self._point_class_id

    @point_class_id.setter
    def point_class_id(self, point_class_id):
        """
        Sets the point_class_id of this ValidationPointClassRule.

        :param point_class_id: The point_class_id of this ValidationPointClassRule.
        :type: str
        """

        self._point_class_id = point_class_id

    @property
    def is_binary(self):
        """
        Gets the is_binary of this ValidationPointClassRule.

        :return: The is_binary of this ValidationPointClassRule.
        :rtype: bool
        """
        return self._is_binary

    @is_binary.setter
    def is_binary(self, is_binary):
        """
        Sets the is_binary of this ValidationPointClassRule.

        :param is_binary: The is_binary of this ValidationPointClassRule.
        :type: bool
        """

        self._is_binary = is_binary

    @property
    def min_value(self):
        """
        Gets the min_value of this ValidationPointClassRule.

        :return: The min_value of this ValidationPointClassRule.
        :rtype: float
        """
        return self._min_value

    @min_value.setter
    def min_value(self, min_value):
        """
        Sets the min_value of this ValidationPointClassRule.

        :param min_value: The min_value of this ValidationPointClassRule.
        :type: float
        """

        self._min_value = min_value

    @property
    def max_value(self):
        """
        Gets the max_value of this ValidationPointClassRule.

        :return: The max_value of this ValidationPointClassRule.
        :rtype: float
        """
        return self._max_value

    @max_value.setter
    def max_value(self, max_value):
        """
        Sets the max_value of this ValidationPointClassRule.

        :param max_value: The max_value of this ValidationPointClassRule.
        :type: float
        """

        self._max_value = max_value

    @property
    def allowable_object_types(self):
        """
        Gets the allowable_object_types of this ValidationPointClassRule.

        :return: The allowable_object_types of this ValidationPointClassRule.
        :rtype: list[str]
        """
        return self._allowable_object_types

    @allowable_object_types.setter
    def allowable_object_types(self, allowable_object_types):
        """
        Sets the allowable_object_types of this ValidationPointClassRule.

        :param allowable_object_types: The allowable_object_types of this ValidationPointClassRule.
        :type: list[str]
        """

        self._allowable_object_types = allowable_object_types

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
        if not isinstance(other, ValidationPointClassRule):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other