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

class ValidationEquipmentRulePointClassCriteria(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, point_class_id=None, min_number=None, max_number=None):
        """
        ValidationEquipmentRulePointClassCriteria - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'point_class_id': 'str',
            'min_number': 'int',
            'max_number': 'int'
        }

        self.attribute_map = {
            'point_class_id': 'pointClassId',
            'min_number': 'minNumber',
            'max_number': 'maxNumber'
        }

        self._point_class_id = point_class_id
        self._min_number = min_number
        self._max_number = max_number


    @property
    def point_class_id(self):
        """
        Gets the point_class_id of this ValidationEquipmentRulePointClassCriteria.

        :return: The point_class_id of this ValidationEquipmentRulePointClassCriteria.
        :rtype: str
        """
        return self._point_class_id

    @point_class_id.setter
    def point_class_id(self, point_class_id):
        """
        Sets the point_class_id of this ValidationEquipmentRulePointClassCriteria.

        :param point_class_id: The point_class_id of this ValidationEquipmentRulePointClassCriteria.
        :type: str
        """

        self._point_class_id = point_class_id

    @property
    def min_number(self):
        """
        Gets the min_number of this ValidationEquipmentRulePointClassCriteria.

        :return: The min_number of this ValidationEquipmentRulePointClassCriteria.
        :rtype: int
        """
        return self._min_number

    @min_number.setter
    def min_number(self, min_number):
        """
        Sets the min_number of this ValidationEquipmentRulePointClassCriteria.

        :param min_number: The min_number of this ValidationEquipmentRulePointClassCriteria.
        :type: int
        """

        self._min_number = min_number

    @property
    def max_number(self):
        """
        Gets the max_number of this ValidationEquipmentRulePointClassCriteria.

        :return: The max_number of this ValidationEquipmentRulePointClassCriteria.
        :rtype: int
        """
        return self._max_number

    @max_number.setter
    def max_number(self, max_number):
        """
        Sets the max_number of this ValidationEquipmentRulePointClassCriteria.

        :param max_number: The max_number of this ValidationEquipmentRulePointClassCriteria.
        :type: int
        """

        self._max_number = max_number

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
        if not isinstance(other, ValidationEquipmentRulePointClassCriteria):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other