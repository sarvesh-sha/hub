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

class AssetGraphContextLocation(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, graph_id=None, node_id=None, location_sys_id=None):
        """
        AssetGraphContextLocation - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'graph_id': 'str',
            'node_id': 'str',
            'location_sys_id': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'graph_id': 'graphId',
            'node_id': 'nodeId',
            'location_sys_id': 'locationSysId',
            'discriminator___type': '__type'
        }

        self._graph_id = graph_id
        self._node_id = node_id
        self._location_sys_id = location_sys_id

    @property
    def discriminator___type(self):
        return "AssetGraphContextLocation"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def graph_id(self):
        """
        Gets the graph_id of this AssetGraphContextLocation.

        :return: The graph_id of this AssetGraphContextLocation.
        :rtype: str
        """
        return self._graph_id

    @graph_id.setter
    def graph_id(self, graph_id):
        """
        Sets the graph_id of this AssetGraphContextLocation.

        :param graph_id: The graph_id of this AssetGraphContextLocation.
        :type: str
        """

        self._graph_id = graph_id

    @property
    def node_id(self):
        """
        Gets the node_id of this AssetGraphContextLocation.

        :return: The node_id of this AssetGraphContextLocation.
        :rtype: str
        """
        return self._node_id

    @node_id.setter
    def node_id(self, node_id):
        """
        Sets the node_id of this AssetGraphContextLocation.

        :param node_id: The node_id of this AssetGraphContextLocation.
        :type: str
        """

        self._node_id = node_id

    @property
    def location_sys_id(self):
        """
        Gets the location_sys_id of this AssetGraphContextLocation.

        :return: The location_sys_id of this AssetGraphContextLocation.
        :rtype: str
        """
        return self._location_sys_id

    @location_sys_id.setter
    def location_sys_id(self, location_sys_id):
        """
        Sets the location_sys_id of this AssetGraphContextLocation.

        :param location_sys_id: The location_sys_id of this AssetGraphContextLocation.
        :type: str
        """

        self._location_sys_id = location_sys_id

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
        if not isinstance(other, AssetGraphContextLocation):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other