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

class EngineExpressionBinaryStringIndexOf(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, a=None, b=None):
        """
        EngineExpressionBinaryStringIndexOf - a model defined in Swagger

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
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'a': 'a',
            'b': 'b',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._a = a
        self._b = b

    @property
    def discriminator___type(self):
        return "EngineExpressionBinaryStringIndexOf"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this EngineExpressionBinaryStringIndexOf.

        :return: The id of this EngineExpressionBinaryStringIndexOf.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this EngineExpressionBinaryStringIndexOf.

        :param id: The id of this EngineExpressionBinaryStringIndexOf.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this EngineExpressionBinaryStringIndexOf.

        :return: The x of this EngineExpressionBinaryStringIndexOf.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this EngineExpressionBinaryStringIndexOf.

        :param x: The x of this EngineExpressionBinaryStringIndexOf.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this EngineExpressionBinaryStringIndexOf.

        :return: The y of this EngineExpressionBinaryStringIndexOf.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this EngineExpressionBinaryStringIndexOf.

        :param y: The y of this EngineExpressionBinaryStringIndexOf.
        :type: int
        """

        self._y = y

    @property
    def a(self):
        """
        Gets the a of this EngineExpressionBinaryStringIndexOf.

        :return: The a of this EngineExpressionBinaryStringIndexOf.
        :rtype: EngineExpression
        """
        return self._a

    @a.setter
    def a(self, a):
        """
        Sets the a of this EngineExpressionBinaryStringIndexOf.

        :param a: The a of this EngineExpressionBinaryStringIndexOf.
        :type: EngineExpression
        """

        self._a = a

    @property
    def b(self):
        """
        Gets the b of this EngineExpressionBinaryStringIndexOf.

        :return: The b of this EngineExpressionBinaryStringIndexOf.
        :rtype: EngineExpression
        """
        return self._b

    @b.setter
    def b(self, b):
        """
        Sets the b of this EngineExpressionBinaryStringIndexOf.

        :param b: The b of this EngineExpressionBinaryStringIndexOf.
        :type: EngineExpression
        """

        self._b = b

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
        if not isinstance(other, EngineExpressionBinaryStringIndexOf):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
