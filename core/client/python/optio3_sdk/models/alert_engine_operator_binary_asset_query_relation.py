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

class AlertEngineOperatorBinaryAssetQueryRelation(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, a=None, b=None, relation=None, from_child=None):
        """
        AlertEngineOperatorBinaryAssetQueryRelation - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'x': 'int',
            'y': 'int',
            'a': 'EngineExpression',
            'b': 'EngineExpression',
            'relation': 'str',
            'from_child': 'bool',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'a': 'a',
            'b': 'b',
            'relation': 'relation',
            'from_child': 'fromChild',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._a = a
        self._b = b
        self._relation = relation
        self._from_child = from_child

    @property
    def discriminator___type(self):
        return "AlertEngineOperatorBinaryAssetQueryRelation"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this AlertEngineOperatorBinaryAssetQueryRelation.

        :return: The id of this AlertEngineOperatorBinaryAssetQueryRelation.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this AlertEngineOperatorBinaryAssetQueryRelation.

        :param id: The id of this AlertEngineOperatorBinaryAssetQueryRelation.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this AlertEngineOperatorBinaryAssetQueryRelation.

        :return: The x of this AlertEngineOperatorBinaryAssetQueryRelation.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this AlertEngineOperatorBinaryAssetQueryRelation.

        :param x: The x of this AlertEngineOperatorBinaryAssetQueryRelation.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this AlertEngineOperatorBinaryAssetQueryRelation.

        :return: The y of this AlertEngineOperatorBinaryAssetQueryRelation.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this AlertEngineOperatorBinaryAssetQueryRelation.

        :param y: The y of this AlertEngineOperatorBinaryAssetQueryRelation.
        :type: int
        """

        self._y = y

    @property
    def a(self):
        """
        Gets the a of this AlertEngineOperatorBinaryAssetQueryRelation.

        :return: The a of this AlertEngineOperatorBinaryAssetQueryRelation.
        :rtype: EngineExpression
        """
        return self._a

    @a.setter
    def a(self, a):
        """
        Sets the a of this AlertEngineOperatorBinaryAssetQueryRelation.

        :param a: The a of this AlertEngineOperatorBinaryAssetQueryRelation.
        :type: EngineExpression
        """

        self._a = a

    @property
    def b(self):
        """
        Gets the b of this AlertEngineOperatorBinaryAssetQueryRelation.

        :return: The b of this AlertEngineOperatorBinaryAssetQueryRelation.
        :rtype: EngineExpression
        """
        return self._b

    @b.setter
    def b(self, b):
        """
        Sets the b of this AlertEngineOperatorBinaryAssetQueryRelation.

        :param b: The b of this AlertEngineOperatorBinaryAssetQueryRelation.
        :type: EngineExpression
        """

        self._b = b

    @property
    def relation(self):
        """
        Gets the relation of this AlertEngineOperatorBinaryAssetQueryRelation.

        :return: The relation of this AlertEngineOperatorBinaryAssetQueryRelation.
        :rtype: str
        """
        return self._relation

    @relation.setter
    def relation(self, relation):
        """
        Sets the relation of this AlertEngineOperatorBinaryAssetQueryRelation.

        :param relation: The relation of this AlertEngineOperatorBinaryAssetQueryRelation.
        :type: str
        """
        allowed_values = ["structural", "controls"]
        if relation is not None and relation not in allowed_values:
            raise ValueError(
                "Invalid value for `relation` ({0}), must be one of {1}"
                .format(relation, allowed_values)
            )

        self._relation = relation

    @property
    def from_child(self):
        """
        Gets the from_child of this AlertEngineOperatorBinaryAssetQueryRelation.

        :return: The from_child of this AlertEngineOperatorBinaryAssetQueryRelation.
        :rtype: bool
        """
        return self._from_child

    @from_child.setter
    def from_child(self, from_child):
        """
        Sets the from_child of this AlertEngineOperatorBinaryAssetQueryRelation.

        :param from_child: The from_child of this AlertEngineOperatorBinaryAssetQueryRelation.
        :type: bool
        """

        self._from_child = from_child

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
        if not isinstance(other, AlertEngineOperatorBinaryAssetQueryRelation):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

