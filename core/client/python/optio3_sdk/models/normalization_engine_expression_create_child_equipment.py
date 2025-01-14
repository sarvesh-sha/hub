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

class NormalizationEngineExpressionCreateChildEquipment(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, parent=None, name=None, equipment_class_id=None):
        """
        NormalizationEngineExpressionCreateChildEquipment - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'x': 'int',
            'y': 'int',
            'parent': 'EngineExpression',
            'name': 'EngineExpression',
            'equipment_class_id': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'parent': 'parent',
            'name': 'name',
            'equipment_class_id': 'equipmentClassId',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._parent = parent
        self._name = name
        self._equipment_class_id = equipment_class_id

    @property
    def discriminator___type(self):
        return "NormalizationEngineExpressionCreateChildEquipment"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this NormalizationEngineExpressionCreateChildEquipment.

        :return: The id of this NormalizationEngineExpressionCreateChildEquipment.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this NormalizationEngineExpressionCreateChildEquipment.

        :param id: The id of this NormalizationEngineExpressionCreateChildEquipment.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this NormalizationEngineExpressionCreateChildEquipment.

        :return: The x of this NormalizationEngineExpressionCreateChildEquipment.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this NormalizationEngineExpressionCreateChildEquipment.

        :param x: The x of this NormalizationEngineExpressionCreateChildEquipment.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this NormalizationEngineExpressionCreateChildEquipment.

        :return: The y of this NormalizationEngineExpressionCreateChildEquipment.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this NormalizationEngineExpressionCreateChildEquipment.

        :param y: The y of this NormalizationEngineExpressionCreateChildEquipment.
        :type: int
        """

        self._y = y

    @property
    def parent(self):
        """
        Gets the parent of this NormalizationEngineExpressionCreateChildEquipment.

        :return: The parent of this NormalizationEngineExpressionCreateChildEquipment.
        :rtype: EngineExpression
        """
        return self._parent

    @parent.setter
    def parent(self, parent):
        """
        Sets the parent of this NormalizationEngineExpressionCreateChildEquipment.

        :param parent: The parent of this NormalizationEngineExpressionCreateChildEquipment.
        :type: EngineExpression
        """

        self._parent = parent

    @property
    def name(self):
        """
        Gets the name of this NormalizationEngineExpressionCreateChildEquipment.

        :return: The name of this NormalizationEngineExpressionCreateChildEquipment.
        :rtype: EngineExpression
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this NormalizationEngineExpressionCreateChildEquipment.

        :param name: The name of this NormalizationEngineExpressionCreateChildEquipment.
        :type: EngineExpression
        """

        self._name = name

    @property
    def equipment_class_id(self):
        """
        Gets the equipment_class_id of this NormalizationEngineExpressionCreateChildEquipment.

        :return: The equipment_class_id of this NormalizationEngineExpressionCreateChildEquipment.
        :rtype: str
        """
        return self._equipment_class_id

    @equipment_class_id.setter
    def equipment_class_id(self, equipment_class_id):
        """
        Sets the equipment_class_id of this NormalizationEngineExpressionCreateChildEquipment.

        :param equipment_class_id: The equipment_class_id of this NormalizationEngineExpressionCreateChildEquipment.
        :type: str
        """

        self._equipment_class_id = equipment_class_id

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
        if not isinstance(other, NormalizationEngineExpressionCreateChildEquipment):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
