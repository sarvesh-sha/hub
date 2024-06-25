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

class AlertEngineOperatorUnaryAssetQueryRelation(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, a=None, relation=None, from_child=None):
        """
        AlertEngineOperatorUnaryAssetQueryRelation - a model defined in Swagger

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
            'relation': 'str',
            'from_child': 'bool',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'a': 'a',
            'relation': 'relation',
            'from_child': 'fromChild',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._a = a
        self._relation = relation
        self._from_child = from_child

    @property
    def discriminator___type(self):
        return "AlertEngineOperatorUnaryAssetQueryRelation"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this AlertEngineOperatorUnaryAssetQueryRelation.

        :return: The id of this AlertEngineOperatorUnaryAssetQueryRelation.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this AlertEngineOperatorUnaryAssetQueryRelation.

        :param id: The id of this AlertEngineOperatorUnaryAssetQueryRelation.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this AlertEngineOperatorUnaryAssetQueryRelation.

        :return: The x of this AlertEngineOperatorUnaryAssetQueryRelation.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this AlertEngineOperatorUnaryAssetQueryRelation.

        :param x: The x of this AlertEngineOperatorUnaryAssetQueryRelation.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this AlertEngineOperatorUnaryAssetQueryRelation.

        :return: The y of this AlertEngineOperatorUnaryAssetQueryRelation.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this AlertEngineOperatorUnaryAssetQueryRelation.

        :param y: The y of this AlertEngineOperatorUnaryAssetQueryRelation.
        :type: int
        """

        self._y = y

    @property
    def a(self):
        """
        Gets the a of this AlertEngineOperatorUnaryAssetQueryRelation.

        :return: The a of this AlertEngineOperatorUnaryAssetQueryRelation.
        :rtype: EngineExpression
        """
        return self._a

    @a.setter
    def a(self, a):
        """
        Sets the a of this AlertEngineOperatorUnaryAssetQueryRelation.

        :param a: The a of this AlertEngineOperatorUnaryAssetQueryRelation.
        :type: EngineExpression
        """

        self._a = a

    @property
    def relation(self):
        """
        Gets the relation of this AlertEngineOperatorUnaryAssetQueryRelation.

        :return: The relation of this AlertEngineOperatorUnaryAssetQueryRelation.
        :rtype: str
        """
        return self._relation

    @relation.setter
    def relation(self, relation):
        """
        Sets the relation of this AlertEngineOperatorUnaryAssetQueryRelation.

        :param relation: The relation of this AlertEngineOperatorUnaryAssetQueryRelation.
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
        Gets the from_child of this AlertEngineOperatorUnaryAssetQueryRelation.

        :return: The from_child of this AlertEngineOperatorUnaryAssetQueryRelation.
        :rtype: bool
        """
        return self._from_child

    @from_child.setter
    def from_child(self, from_child):
        """
        Sets the from_child of this AlertEngineOperatorUnaryAssetQueryRelation.

        :param from_child: The from_child of this AlertEngineOperatorUnaryAssetQueryRelation.
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
        if not isinstance(other, AlertEngineOperatorUnaryAssetQueryRelation):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
