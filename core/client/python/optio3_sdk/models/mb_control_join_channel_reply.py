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

class MbControlJoinChannelReply(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, message_id=None, success=None):
        """
        MbControlJoinChannelReply - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'message_id': 'str',
            'success': 'bool',
            'discriminator_type': 'str'
        }

        self.attribute_map = {
            'message_id': 'messageId',
            'success': 'success',
            'discriminator_type': 'type'
        }

        self._message_id = message_id
        self._success = success

    @property
    def discriminator_type(self):
        return "MbControlJoinChannelReply"

    @discriminator_type.setter
    def discriminator_type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def message_id(self):
        """
        Gets the message_id of this MbControlJoinChannelReply.

        :return: The message_id of this MbControlJoinChannelReply.
        :rtype: str
        """
        return self._message_id

    @message_id.setter
    def message_id(self, message_id):
        """
        Sets the message_id of this MbControlJoinChannelReply.

        :param message_id: The message_id of this MbControlJoinChannelReply.
        :type: str
        """

        self._message_id = message_id

    @property
    def success(self):
        """
        Gets the success of this MbControlJoinChannelReply.

        :return: The success of this MbControlJoinChannelReply.
        :rtype: bool
        """
        return self._success

    @success.setter
    def success(self, success):
        """
        Sets the success of this MbControlJoinChannelReply.

        :param success: The success of this MbControlJoinChannelReply.
        :type: bool
        """

        self._success = success

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
        if not isinstance(other, MbControlJoinChannelReply):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
