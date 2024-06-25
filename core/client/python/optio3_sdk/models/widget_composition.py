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

class WidgetComposition(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, config=None, outline=None):
        """
        WidgetComposition - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'config': 'WidgetConfiguration',
            'outline': 'WidgetOutline'
        }

        self.attribute_map = {
            'config': 'config',
            'outline': 'outline'
        }

        self._config = config
        self._outline = outline


    @property
    def config(self):
        """
        Gets the config of this WidgetComposition.

        :return: The config of this WidgetComposition.
        :rtype: WidgetConfiguration
        """
        return self._config

    @config.setter
    def config(self, config):
        """
        Sets the config of this WidgetComposition.

        :param config: The config of this WidgetComposition.
        :type: WidgetConfiguration
        """

        self._config = config

    @property
    def outline(self):
        """
        Gets the outline of this WidgetComposition.

        :return: The outline of this WidgetComposition.
        :rtype: WidgetOutline
        """
        return self._outline

    @outline.setter
    def outline(self, outline):
        """
        Sets the outline of this WidgetComposition.

        :param outline: The outline of this WidgetComposition.
        :type: WidgetOutline
        """

        self._outline = outline

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
        if not isinstance(other, WidgetComposition):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
