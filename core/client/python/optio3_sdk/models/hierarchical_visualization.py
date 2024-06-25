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

class HierarchicalVisualization(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, virtual_nodes=None, interaction_behavior=None, bindings=None):
        """
        HierarchicalVisualization - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'virtual_nodes': 'list[VirtualAssetGraphNode]',
            'interaction_behavior': 'InteractionBehavior',
            'bindings': 'list[HierarchicalVisualizationBinding]'
        }

        self.attribute_map = {
            'virtual_nodes': 'virtualNodes',
            'interaction_behavior': 'interactionBehavior',
            'bindings': 'bindings'
        }

        self._virtual_nodes = virtual_nodes
        self._interaction_behavior = interaction_behavior
        self._bindings = bindings


    @property
    def virtual_nodes(self):
        """
        Gets the virtual_nodes of this HierarchicalVisualization.

        :return: The virtual_nodes of this HierarchicalVisualization.
        :rtype: list[VirtualAssetGraphNode]
        """
        return self._virtual_nodes

    @virtual_nodes.setter
    def virtual_nodes(self, virtual_nodes):
        """
        Sets the virtual_nodes of this HierarchicalVisualization.

        :param virtual_nodes: The virtual_nodes of this HierarchicalVisualization.
        :type: list[VirtualAssetGraphNode]
        """

        self._virtual_nodes = virtual_nodes

    @property
    def interaction_behavior(self):
        """
        Gets the interaction_behavior of this HierarchicalVisualization.

        :return: The interaction_behavior of this HierarchicalVisualization.
        :rtype: InteractionBehavior
        """
        return self._interaction_behavior

    @interaction_behavior.setter
    def interaction_behavior(self, interaction_behavior):
        """
        Sets the interaction_behavior of this HierarchicalVisualization.

        :param interaction_behavior: The interaction_behavior of this HierarchicalVisualization.
        :type: InteractionBehavior
        """

        self._interaction_behavior = interaction_behavior

    @property
    def bindings(self):
        """
        Gets the bindings of this HierarchicalVisualization.

        :return: The bindings of this HierarchicalVisualization.
        :rtype: list[HierarchicalVisualizationBinding]
        """
        return self._bindings

    @bindings.setter
    def bindings(self, bindings):
        """
        Sets the bindings of this HierarchicalVisualization.

        :param bindings: The bindings of this HierarchicalVisualization.
        :type: list[HierarchicalVisualizationBinding]
        """

        self._bindings = bindings

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
        if not isinstance(other, HierarchicalVisualization):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
