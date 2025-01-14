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

class WidgetOutline(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, left=None, top=None, width=None, height=None):
        """
        WidgetOutline - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'left': 'int',
            'top': 'int',
            'width': 'int',
            'height': 'int'
        }

        self.attribute_map = {
            'left': 'left',
            'top': 'top',
            'width': 'width',
            'height': 'height'
        }

        self._left = left
        self._top = top
        self._width = width
        self._height = height


    @property
    def left(self):
        """
        Gets the left of this WidgetOutline.

        :return: The left of this WidgetOutline.
        :rtype: int
        """
        return self._left

    @left.setter
    def left(self, left):
        """
        Sets the left of this WidgetOutline.

        :param left: The left of this WidgetOutline.
        :type: int
        """

        self._left = left

    @property
    def top(self):
        """
        Gets the top of this WidgetOutline.

        :return: The top of this WidgetOutline.
        :rtype: int
        """
        return self._top

    @top.setter
    def top(self, top):
        """
        Sets the top of this WidgetOutline.

        :param top: The top of this WidgetOutline.
        :type: int
        """

        self._top = top

    @property
    def width(self):
        """
        Gets the width of this WidgetOutline.

        :return: The width of this WidgetOutline.
        :rtype: int
        """
        return self._width

    @width.setter
    def width(self, width):
        """
        Sets the width of this WidgetOutline.

        :param width: The width of this WidgetOutline.
        :type: int
        """

        self._width = width

    @property
    def height(self):
        """
        Gets the height of this WidgetOutline.

        :return: The height of this WidgetOutline.
        :rtype: int
        """
        return self._height

    @height.setter
    def height(self, height):
        """
        Sets the height of this WidgetOutline.

        :param height: The height of this WidgetOutline.
        :type: int
        """

        self._height = height

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
        if not isinstance(other, WidgetOutline):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
