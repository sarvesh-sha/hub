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

class HierarchicalVisualizationBinding(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, leaf_node_id=None, options=None, color=None):
        """
        HierarchicalVisualizationBinding - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'leaf_node_id': 'str',
            'options': 'HierarchicalVisualizationConfiguration',
            'color': 'ColorConfiguration'
        }

        self.attribute_map = {
            'leaf_node_id': 'leafNodeId',
            'options': 'options',
            'color': 'color'
        }

        self._leaf_node_id = leaf_node_id
        self._options = options
        self._color = color


    @property
    def leaf_node_id(self):
        """
        Gets the leaf_node_id of this HierarchicalVisualizationBinding.

        :return: The leaf_node_id of this HierarchicalVisualizationBinding.
        :rtype: str
        """
        return self._leaf_node_id

    @leaf_node_id.setter
    def leaf_node_id(self, leaf_node_id):
        """
        Sets the leaf_node_id of this HierarchicalVisualizationBinding.

        :param leaf_node_id: The leaf_node_id of this HierarchicalVisualizationBinding.
        :type: str
        """

        self._leaf_node_id = leaf_node_id

    @property
    def options(self):
        """
        Gets the options of this HierarchicalVisualizationBinding.

        :return: The options of this HierarchicalVisualizationBinding.
        :rtype: HierarchicalVisualizationConfiguration
        """
        return self._options

    @options.setter
    def options(self, options):
        """
        Sets the options of this HierarchicalVisualizationBinding.

        :param options: The options of this HierarchicalVisualizationBinding.
        :type: HierarchicalVisualizationConfiguration
        """

        self._options = options

    @property
    def color(self):
        """
        Gets the color of this HierarchicalVisualizationBinding.

        :return: The color of this HierarchicalVisualizationBinding.
        :rtype: ColorConfiguration
        """
        return self._color

    @color.setter
    def color(self, color):
        """
        Sets the color of this HierarchicalVisualizationBinding.

        :param color: The color of this HierarchicalVisualizationBinding.
        :type: ColorConfiguration
        """

        self._color = color

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
        if not isinstance(other, HierarchicalVisualizationBinding):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
