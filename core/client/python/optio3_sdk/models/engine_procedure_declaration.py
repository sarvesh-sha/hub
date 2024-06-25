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

class EngineProcedureDeclaration(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, statements=None, function_id=None, name=None, arguments=None):
        """
        EngineProcedureDeclaration - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'x': 'int',
            'y': 'int',
            'statements': 'list[EngineStatement]',
            'function_id': 'str',
            'name': 'str',
            'arguments': 'list[EngineVariableReference]',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'statements': 'statements',
            'function_id': 'functionId',
            'name': 'name',
            'arguments': 'arguments',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._statements = statements
        self._function_id = function_id
        self._name = name
        self._arguments = arguments

    @property
    def discriminator___type(self):
        return "EngineProcedureDeclaration"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this EngineProcedureDeclaration.

        :return: The id of this EngineProcedureDeclaration.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this EngineProcedureDeclaration.

        :param id: The id of this EngineProcedureDeclaration.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this EngineProcedureDeclaration.

        :return: The x of this EngineProcedureDeclaration.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this EngineProcedureDeclaration.

        :param x: The x of this EngineProcedureDeclaration.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this EngineProcedureDeclaration.

        :return: The y of this EngineProcedureDeclaration.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this EngineProcedureDeclaration.

        :param y: The y of this EngineProcedureDeclaration.
        :type: int
        """

        self._y = y

    @property
    def statements(self):
        """
        Gets the statements of this EngineProcedureDeclaration.

        :return: The statements of this EngineProcedureDeclaration.
        :rtype: list[EngineStatement]
        """
        return self._statements

    @statements.setter
    def statements(self, statements):
        """
        Sets the statements of this EngineProcedureDeclaration.

        :param statements: The statements of this EngineProcedureDeclaration.
        :type: list[EngineStatement]
        """

        self._statements = statements

    @property
    def function_id(self):
        """
        Gets the function_id of this EngineProcedureDeclaration.

        :return: The function_id of this EngineProcedureDeclaration.
        :rtype: str
        """
        return self._function_id

    @function_id.setter
    def function_id(self, function_id):
        """
        Sets the function_id of this EngineProcedureDeclaration.

        :param function_id: The function_id of this EngineProcedureDeclaration.
        :type: str
        """

        self._function_id = function_id

    @property
    def name(self):
        """
        Gets the name of this EngineProcedureDeclaration.

        :return: The name of this EngineProcedureDeclaration.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this EngineProcedureDeclaration.

        :param name: The name of this EngineProcedureDeclaration.
        :type: str
        """

        self._name = name

    @property
    def arguments(self):
        """
        Gets the arguments of this EngineProcedureDeclaration.

        :return: The arguments of this EngineProcedureDeclaration.
        :rtype: list[EngineVariableReference]
        """
        return self._arguments

    @arguments.setter
    def arguments(self, arguments):
        """
        Sets the arguments of this EngineProcedureDeclaration.

        :param arguments: The arguments of this EngineProcedureDeclaration.
        :type: list[EngineVariableReference]
        """

        self._arguments = arguments

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
        if not isinstance(other, EngineProcedureDeclaration):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other