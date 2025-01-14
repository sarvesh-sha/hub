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

class AlertEngineStatementSetAlertStatus(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, alert=None, status=None):
        """
        AlertEngineStatementSetAlertStatus - a model defined in Swagger

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
            'status': 'EngineExpression',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'alert': 'alert',
            'status': 'status',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._alert = alert
        self._status = status

    @property
    def discriminator___type(self):
        return "AlertEngineStatementSetAlertStatus"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this AlertEngineStatementSetAlertStatus.

        :return: The id of this AlertEngineStatementSetAlertStatus.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this AlertEngineStatementSetAlertStatus.

        :param id: The id of this AlertEngineStatementSetAlertStatus.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this AlertEngineStatementSetAlertStatus.

        :return: The x of this AlertEngineStatementSetAlertStatus.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this AlertEngineStatementSetAlertStatus.

        :param x: The x of this AlertEngineStatementSetAlertStatus.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this AlertEngineStatementSetAlertStatus.

        :return: The y of this AlertEngineStatementSetAlertStatus.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this AlertEngineStatementSetAlertStatus.

        :param y: The y of this AlertEngineStatementSetAlertStatus.
        :type: int
        """

        self._y = y

    @property
    def alert(self):
        """
        Gets the alert of this AlertEngineStatementSetAlertStatus.

        :return: The alert of this AlertEngineStatementSetAlertStatus.
        :rtype: EngineExpression
        """
        return self._alert

    @alert.setter
    def alert(self, alert):
        """
        Sets the alert of this AlertEngineStatementSetAlertStatus.

        :param alert: The alert of this AlertEngineStatementSetAlertStatus.
        :type: EngineExpression
        """

        self._alert = alert

    @property
    def status(self):
        """
        Gets the status of this AlertEngineStatementSetAlertStatus.

        :return: The status of this AlertEngineStatementSetAlertStatus.
        :rtype: EngineExpression
        """
        return self._status

    @status.setter
    def status(self, status):
        """
        Sets the status of this AlertEngineStatementSetAlertStatus.

        :param status: The status of this AlertEngineStatementSetAlertStatus.
        :type: EngineExpression
        """

        self._status = status

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
        if not isinstance(other, AlertEngineStatementSetAlertStatus):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
