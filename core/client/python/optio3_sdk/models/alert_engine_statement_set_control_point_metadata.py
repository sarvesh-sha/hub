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

class AlertEngineStatementSetControlPointMetadata(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, control_point=None, key=None, value=None):
        """
        AlertEngineStatementSetControlPointMetadata - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'x': 'int',
            'y': 'int',
            'control_point': 'EngineExpression',
            'key': 'str',
            'value': 'EngineExpression',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'control_point': 'controlPoint',
            'key': 'key',
            'value': 'value',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._control_point = control_point
        self._key = key
        self._value = value

    @property
    def discriminator___type(self):
        return "AlertEngineStatementSetControlPointMetadata"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this AlertEngineStatementSetControlPointMetadata.

        :return: The id of this AlertEngineStatementSetControlPointMetadata.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this AlertEngineStatementSetControlPointMetadata.

        :param id: The id of this AlertEngineStatementSetControlPointMetadata.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this AlertEngineStatementSetControlPointMetadata.

        :return: The x of this AlertEngineStatementSetControlPointMetadata.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this AlertEngineStatementSetControlPointMetadata.

        :param x: The x of this AlertEngineStatementSetControlPointMetadata.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this AlertEngineStatementSetControlPointMetadata.

        :return: The y of this AlertEngineStatementSetControlPointMetadata.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this AlertEngineStatementSetControlPointMetadata.

        :param y: The y of this AlertEngineStatementSetControlPointMetadata.
        :type: int
        """

        self._y = y

    @property
    def control_point(self):
        """
        Gets the control_point of this AlertEngineStatementSetControlPointMetadata.

        :return: The control_point of this AlertEngineStatementSetControlPointMetadata.
        :rtype: EngineExpression
        """
        return self._control_point

    @control_point.setter
    def control_point(self, control_point):
        """
        Sets the control_point of this AlertEngineStatementSetControlPointMetadata.

        :param control_point: The control_point of this AlertEngineStatementSetControlPointMetadata.
        :type: EngineExpression
        """

        self._control_point = control_point

    @property
    def key(self):
        """
        Gets the key of this AlertEngineStatementSetControlPointMetadata.

        :return: The key of this AlertEngineStatementSetControlPointMetadata.
        :rtype: str
        """
        return self._key

    @key.setter
    def key(self, key):
        """
        Sets the key of this AlertEngineStatementSetControlPointMetadata.

        :param key: The key of this AlertEngineStatementSetControlPointMetadata.
        :type: str
        """

        self._key = key

    @property
    def value(self):
        """
        Gets the value of this AlertEngineStatementSetControlPointMetadata.

        :return: The value of this AlertEngineStatementSetControlPointMetadata.
        :rtype: EngineExpression
        """
        return self._value

    @value.setter
    def value(self, value):
        """
        Sets the value of this AlertEngineStatementSetControlPointMetadata.

        :param value: The value of this AlertEngineStatementSetControlPointMetadata.
        :type: EngineExpression
        """

        self._value = value

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
        if not isinstance(other, AlertEngineStatementSetControlPointMetadata):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
