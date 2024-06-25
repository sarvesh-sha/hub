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

class HierarchicalVisualizationConfiguration(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, type=None, sizing=None, size=None, axis_sizing=None, axis_range=None):
        """
        HierarchicalVisualizationConfiguration - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'type': 'str',
            'sizing': 'str',
            'size': 'int',
            'axis_sizing': 'str',
            'axis_range': 'NumericRange'
        }

        self.attribute_map = {
            'type': 'type',
            'sizing': 'sizing',
            'size': 'size',
            'axis_sizing': 'axisSizing',
            'axis_range': 'axisRange'
        }

        self._type = type
        self._sizing = sizing
        self._size = size
        self._axis_sizing = axis_sizing
        self._axis_range = axis_range


    @property
    def type(self):
        """
        Gets the type of this HierarchicalVisualizationConfiguration.

        :return: The type of this HierarchicalVisualizationConfiguration.
        :rtype: str
        """
        return self._type

    @type.setter
    def type(self, type):
        """
        Sets the type of this HierarchicalVisualizationConfiguration.

        :param type: The type of this HierarchicalVisualizationConfiguration.
        :type: str
        """
        allowed_values = ["HEATMAP", "LINE", "TABLE", "TABLE_WITH_BAR", "BUBBLEMAP", "TREEMAP", "SUNBURST", "PIEBURST", "DONUT", "PIE"]
        if type is not None and type not in allowed_values:
            raise ValueError(
                "Invalid value for `type` ({0}), must be one of {1}"
                .format(type, allowed_values)
            )

        self._type = type

    @property
    def sizing(self):
        """
        Gets the sizing of this HierarchicalVisualizationConfiguration.

        :return: The sizing of this HierarchicalVisualizationConfiguration.
        :rtype: str
        """
        return self._sizing

    @sizing.setter
    def sizing(self, sizing):
        """
        Sets the sizing of this HierarchicalVisualizationConfiguration.

        :param sizing: The sizing of this HierarchicalVisualizationConfiguration.
        :type: str
        """
        allowed_values = ["FIT", "FIXED"]
        if sizing is not None and sizing not in allowed_values:
            raise ValueError(
                "Invalid value for `sizing` ({0}), must be one of {1}"
                .format(sizing, allowed_values)
            )

        self._sizing = sizing

    @property
    def size(self):
        """
        Gets the size of this HierarchicalVisualizationConfiguration.

        :return: The size of this HierarchicalVisualizationConfiguration.
        :rtype: int
        """
        return self._size

    @size.setter
    def size(self, size):
        """
        Sets the size of this HierarchicalVisualizationConfiguration.

        :param size: The size of this HierarchicalVisualizationConfiguration.
        :type: int
        """

        self._size = size

    @property
    def axis_sizing(self):
        """
        Gets the axis_sizing of this HierarchicalVisualizationConfiguration.

        :return: The axis_sizing of this HierarchicalVisualizationConfiguration.
        :rtype: str
        """
        return self._axis_sizing

    @axis_sizing.setter
    def axis_sizing(self, axis_sizing):
        """
        Sets the axis_sizing of this HierarchicalVisualizationConfiguration.

        :param axis_sizing: The axis_sizing of this HierarchicalVisualizationConfiguration.
        :type: str
        """
        allowed_values = ["INDIVIDUAL", "SHARED", "FIXED"]
        if axis_sizing is not None and axis_sizing not in allowed_values:
            raise ValueError(
                "Invalid value for `axis_sizing` ({0}), must be one of {1}"
                .format(axis_sizing, allowed_values)
            )

        self._axis_sizing = axis_sizing

    @property
    def axis_range(self):
        """
        Gets the axis_range of this HierarchicalVisualizationConfiguration.

        :return: The axis_range of this HierarchicalVisualizationConfiguration.
        :rtype: NumericRange
        """
        return self._axis_range

    @axis_range.setter
    def axis_range(self, axis_range):
        """
        Sets the axis_range of this HierarchicalVisualizationConfiguration.

        :param axis_range: The axis_range of this HierarchicalVisualizationConfiguration.
        :type: NumericRange
        """

        self._axis_range = axis_range

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
        if not isinstance(other, HierarchicalVisualizationConfiguration):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

