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

class InteractionBehavior(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, type=None, pane_config_id=None):
        """
        InteractionBehavior - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'type': 'str',
            'pane_config_id': 'str'
        }

        self.attribute_map = {
            'type': 'type',
            'pane_config_id': 'paneConfigId'
        }

        self._type = type
        self._pane_config_id = pane_config_id


    @property
    def type(self):
        """
        Gets the type of this InteractionBehavior.

        :return: The type of this InteractionBehavior.
        :rtype: str
        """
        return self._type

    @type.setter
    def type(self, type):
        """
        Sets the type of this InteractionBehavior.

        :param type: The type of this InteractionBehavior.
        :type: str
        """
        allowed_values = ["NavigateDataExplorer", "NavigateDeviceElem", "None", "Pane", "Standard"]
        if type is not None and type not in allowed_values:
            raise ValueError(
                "Invalid value for `type` ({0}), must be one of {1}"
                .format(type, allowed_values)
            )

        self._type = type

    @property
    def pane_config_id(self):
        """
        Gets the pane_config_id of this InteractionBehavior.

        :return: The pane_config_id of this InteractionBehavior.
        :rtype: str
        """
        return self._pane_config_id

    @pane_config_id.setter
    def pane_config_id(self, pane_config_id):
        """
        Sets the pane_config_id of this InteractionBehavior.

        :param pane_config_id: The pane_config_id of this InteractionBehavior.
        :type: str
        """

        self._pane_config_id = pane_config_id

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
        if not isinstance(other, InteractionBehavior):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
