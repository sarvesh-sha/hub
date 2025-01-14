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

class PaneFieldConfigurationAggregatedValue(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, label=None, control_point_group=None):
        """
        PaneFieldConfigurationAggregatedValue - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'label': 'str',
            'control_point_group': 'ControlPointsGroup',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'label': 'label',
            'control_point_group': 'controlPointGroup',
            'discriminator___type': '__type'
        }

        self._label = label
        self._control_point_group = control_point_group

    @property
    def discriminator___type(self):
        return "PaneFieldConfigurationAggregatedValue"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def label(self):
        """
        Gets the label of this PaneFieldConfigurationAggregatedValue.

        :return: The label of this PaneFieldConfigurationAggregatedValue.
        :rtype: str
        """
        return self._label

    @label.setter
    def label(self, label):
        """
        Sets the label of this PaneFieldConfigurationAggregatedValue.

        :param label: The label of this PaneFieldConfigurationAggregatedValue.
        :type: str
        """

        self._label = label

    @property
    def control_point_group(self):
        """
        Gets the control_point_group of this PaneFieldConfigurationAggregatedValue.

        :return: The control_point_group of this PaneFieldConfigurationAggregatedValue.
        :rtype: ControlPointsGroup
        """
        return self._control_point_group

    @control_point_group.setter
    def control_point_group(self, control_point_group):
        """
        Sets the control_point_group of this PaneFieldConfigurationAggregatedValue.

        :param control_point_group: The control_point_group of this PaneFieldConfigurationAggregatedValue.
        :type: ControlPointsGroup
        """

        self._control_point_group = control_point_group

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
        if not isinstance(other, PaneFieldConfigurationAggregatedValue):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
