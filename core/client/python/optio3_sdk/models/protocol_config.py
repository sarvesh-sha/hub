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

class ProtocolConfig(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sampling_configuration_id=None):
        """
        ProtocolConfig - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'sampling_configuration_id': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'sampling_configuration_id': 'samplingConfigurationId',
            'discriminator___type': '__type'
        }

        self._sampling_configuration_id = sampling_configuration_id

    @property
    def discriminator___type(self):
        return "ProtocolConfig"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @staticmethod
    def fixup_prototype(data):
        className = data["__type"]
        from .. import models
        switcher = {
        "ProtocolConfigForBACnet": models.ProtocolConfigForBACnet,
        "ProtocolConfigForIpn": models.ProtocolConfigForIpn,
        "ProtocolConfig": ProtocolConfig
        }

        klass = switcher.get(className)
        if klass:
            return klass()
        else:
            raise Exception("Unable to deserialize unknown type")

    @property
    def sampling_configuration_id(self):
        """
        Gets the sampling_configuration_id of this ProtocolConfig.

        :return: The sampling_configuration_id of this ProtocolConfig.
        :rtype: str
        """
        return self._sampling_configuration_id

    @sampling_configuration_id.setter
    def sampling_configuration_id(self, sampling_configuration_id):
        """
        Sets the sampling_configuration_id of this ProtocolConfig.

        :param sampling_configuration_id: The sampling_configuration_id of this ProtocolConfig.
        :type: str
        """

        self._sampling_configuration_id = sampling_configuration_id

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
        if not isinstance(other, ProtocolConfig):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
