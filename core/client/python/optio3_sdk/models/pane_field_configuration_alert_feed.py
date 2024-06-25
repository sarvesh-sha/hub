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

class PaneFieldConfigurationAlertFeed(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, label=None, location_input=None):
        """
        PaneFieldConfigurationAlertFeed - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'label': 'str',
            'location_input': 'AssetGraphBinding',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'label': 'label',
            'location_input': 'locationInput',
            'discriminator___type': '__type'
        }

        self._label = label
        self._location_input = location_input

    @property
    def discriminator___type(self):
        return "PaneFieldConfigurationAlertFeed"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def label(self):
        """
        Gets the label of this PaneFieldConfigurationAlertFeed.

        :return: The label of this PaneFieldConfigurationAlertFeed.
        :rtype: str
        """
        return self._label

    @label.setter
    def label(self, label):
        """
        Sets the label of this PaneFieldConfigurationAlertFeed.

        :param label: The label of this PaneFieldConfigurationAlertFeed.
        :type: str
        """

        self._label = label

    @property
    def location_input(self):
        """
        Gets the location_input of this PaneFieldConfigurationAlertFeed.

        :return: The location_input of this PaneFieldConfigurationAlertFeed.
        :rtype: AssetGraphBinding
        """
        return self._location_input

    @location_input.setter
    def location_input(self, location_input):
        """
        Sets the location_input of this PaneFieldConfigurationAlertFeed.

        :param location_input: The location_input of this PaneFieldConfigurationAlertFeed.
        :type: AssetGraphBinding
        """

        self._location_input = location_input

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
        if not isinstance(other, PaneFieldConfigurationAlertFeed):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
