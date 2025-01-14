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

class DbMessageConfigReply(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, success=None, last_update=None):
        """
        DbMessageConfigReply - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'success': 'bool',
            'last_update': 'datetime',
            'discriminator_type': 'str'
        }

        self.attribute_map = {
            'success': 'success',
            'last_update': 'lastUpdate',
            'discriminator_type': 'type'
        }

        self._success = success
        self._last_update = last_update

    @property
    def discriminator_type(self):
        return "DbMessageConfigReply"

    @discriminator_type.setter
    def discriminator_type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def success(self):
        """
        Gets the success of this DbMessageConfigReply.

        :return: The success of this DbMessageConfigReply.
        :rtype: bool
        """
        return self._success

    @success.setter
    def success(self, success):
        """
        Sets the success of this DbMessageConfigReply.

        :param success: The success of this DbMessageConfigReply.
        :type: bool
        """

        self._success = success

    @property
    def last_update(self):
        """
        Gets the last_update of this DbMessageConfigReply.

        :return: The last_update of this DbMessageConfigReply.
        :rtype: datetime
        """
        return self._last_update

    @last_update.setter
    def last_update(self, last_update):
        """
        Sets the last_update of this DbMessageConfigReply.

        :param last_update: The last_update of this DbMessageConfigReply.
        :type: datetime
        """

        self._last_update = last_update

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
        if not isinstance(other, DbMessageConfigReply):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
