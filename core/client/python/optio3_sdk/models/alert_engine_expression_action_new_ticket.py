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

class AlertEngineExpressionActionNewTicket(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, alert=None, delivery_options=None):
        """
        AlertEngineExpressionActionNewTicket - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'x': 'int',
            'y': 'int',
            'alert': 'EngineExpression',
            'delivery_options': 'EngineExpression',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'alert': 'alert',
            'delivery_options': 'deliveryOptions',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._alert = alert
        self._delivery_options = delivery_options

    @property
    def discriminator___type(self):
        return "AlertEngineExpressionActionNewTicket"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this AlertEngineExpressionActionNewTicket.

        :return: The id of this AlertEngineExpressionActionNewTicket.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this AlertEngineExpressionActionNewTicket.

        :param id: The id of this AlertEngineExpressionActionNewTicket.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this AlertEngineExpressionActionNewTicket.

        :return: The x of this AlertEngineExpressionActionNewTicket.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this AlertEngineExpressionActionNewTicket.

        :param x: The x of this AlertEngineExpressionActionNewTicket.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this AlertEngineExpressionActionNewTicket.

        :return: The y of this AlertEngineExpressionActionNewTicket.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this AlertEngineExpressionActionNewTicket.

        :param y: The y of this AlertEngineExpressionActionNewTicket.
        :type: int
        """

        self._y = y

    @property
    def alert(self):
        """
        Gets the alert of this AlertEngineExpressionActionNewTicket.

        :return: The alert of this AlertEngineExpressionActionNewTicket.
        :rtype: EngineExpression
        """
        return self._alert

    @alert.setter
    def alert(self, alert):
        """
        Sets the alert of this AlertEngineExpressionActionNewTicket.

        :param alert: The alert of this AlertEngineExpressionActionNewTicket.
        :type: EngineExpression
        """

        self._alert = alert

    @property
    def delivery_options(self):
        """
        Gets the delivery_options of this AlertEngineExpressionActionNewTicket.

        :return: The delivery_options of this AlertEngineExpressionActionNewTicket.
        :rtype: EngineExpression
        """
        return self._delivery_options

    @delivery_options.setter
    def delivery_options(self, delivery_options):
        """
        Sets the delivery_options of this AlertEngineExpressionActionNewTicket.

        :param delivery_options: The delivery_options of this AlertEngineExpressionActionNewTicket.
        :type: EngineExpression
        """

        self._delivery_options = delivery_options

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
        if not isinstance(other, AlertEngineExpressionActionNewTicket):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
