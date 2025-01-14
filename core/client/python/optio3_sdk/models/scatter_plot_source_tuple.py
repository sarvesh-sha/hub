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

class ScatterPlotSourceTuple(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, source_x=None, source_y=None, source_z=None, name=None, color_override=None, panel=None):
        """
        ScatterPlotSourceTuple - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'source_x': 'ScatterPlotSource',
            'source_y': 'ScatterPlotSource',
            'source_z': 'ScatterPlotSource',
            'name': 'str',
            'color_override': 'str',
            'panel': 'int'
        }

        self.attribute_map = {
            'source_x': 'sourceX',
            'source_y': 'sourceY',
            'source_z': 'sourceZ',
            'name': 'name',
            'color_override': 'colorOverride',
            'panel': 'panel'
        }

        self._source_x = source_x
        self._source_y = source_y
        self._source_z = source_z
        self._name = name
        self._color_override = color_override
        self._panel = panel


    @property
    def source_x(self):
        """
        Gets the source_x of this ScatterPlotSourceTuple.

        :return: The source_x of this ScatterPlotSourceTuple.
        :rtype: ScatterPlotSource
        """
        return self._source_x

    @source_x.setter
    def source_x(self, source_x):
        """
        Sets the source_x of this ScatterPlotSourceTuple.

        :param source_x: The source_x of this ScatterPlotSourceTuple.
        :type: ScatterPlotSource
        """

        self._source_x = source_x

    @property
    def source_y(self):
        """
        Gets the source_y of this ScatterPlotSourceTuple.

        :return: The source_y of this ScatterPlotSourceTuple.
        :rtype: ScatterPlotSource
        """
        return self._source_y

    @source_y.setter
    def source_y(self, source_y):
        """
        Sets the source_y of this ScatterPlotSourceTuple.

        :param source_y: The source_y of this ScatterPlotSourceTuple.
        :type: ScatterPlotSource
        """

        self._source_y = source_y

    @property
    def source_z(self):
        """
        Gets the source_z of this ScatterPlotSourceTuple.

        :return: The source_z of this ScatterPlotSourceTuple.
        :rtype: ScatterPlotSource
        """
        return self._source_z

    @source_z.setter
    def source_z(self, source_z):
        """
        Sets the source_z of this ScatterPlotSourceTuple.

        :param source_z: The source_z of this ScatterPlotSourceTuple.
        :type: ScatterPlotSource
        """

        self._source_z = source_z

    @property
    def name(self):
        """
        Gets the name of this ScatterPlotSourceTuple.

        :return: The name of this ScatterPlotSourceTuple.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this ScatterPlotSourceTuple.

        :param name: The name of this ScatterPlotSourceTuple.
        :type: str
        """

        self._name = name

    @property
    def color_override(self):
        """
        Gets the color_override of this ScatterPlotSourceTuple.

        :return: The color_override of this ScatterPlotSourceTuple.
        :rtype: str
        """
        return self._color_override

    @color_override.setter
    def color_override(self, color_override):
        """
        Sets the color_override of this ScatterPlotSourceTuple.

        :param color_override: The color_override of this ScatterPlotSourceTuple.
        :type: str
        """

        self._color_override = color_override

    @property
    def panel(self):
        """
        Gets the panel of this ScatterPlotSourceTuple.

        :return: The panel of this ScatterPlotSourceTuple.
        :rtype: int
        """
        return self._panel

    @panel.setter
    def panel(self, panel):
        """
        Sets the panel of this ScatterPlotSourceTuple.

        :param panel: The panel of this ScatterPlotSourceTuple.
        :type: int
        """

        self._panel = panel

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
        if not isinstance(other, ScatterPlotSourceTuple):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
