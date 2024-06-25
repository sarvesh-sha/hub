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

class GroupingWidgetConfiguration(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, size=None, name=None, description=None, locations=None, refresh_rate_in_seconds=None, manual_font_scaling=None, font_multiplier=None, toolbar_behavior=None, num_cols=None, num_rows=None, widgets=None):
        """
        GroupingWidgetConfiguration - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'size': 'int',
            'name': 'str',
            'description': 'str',
            'locations': 'list[str]',
            'refresh_rate_in_seconds': 'int',
            'manual_font_scaling': 'bool',
            'font_multiplier': 'float',
            'toolbar_behavior': 'str',
            'num_cols': 'int',
            'num_rows': 'int',
            'widgets': 'list[WidgetComposition]',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'size': 'size',
            'name': 'name',
            'description': 'description',
            'locations': 'locations',
            'refresh_rate_in_seconds': 'refreshRateInSeconds',
            'manual_font_scaling': 'manualFontScaling',
            'font_multiplier': 'fontMultiplier',
            'toolbar_behavior': 'toolbarBehavior',
            'num_cols': 'numCols',
            'num_rows': 'numRows',
            'widgets': 'widgets',
            'discriminator___type': '__type'
        }

        self._id = id
        self._size = size
        self._name = name
        self._description = description
        self._locations = locations
        self._refresh_rate_in_seconds = refresh_rate_in_seconds
        self._manual_font_scaling = manual_font_scaling
        self._font_multiplier = font_multiplier
        self._toolbar_behavior = toolbar_behavior
        self._num_cols = num_cols
        self._num_rows = num_rows
        self._widgets = widgets

    @property
    def discriminator___type(self):
        return "GroupingWidgetConfiguration"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this GroupingWidgetConfiguration.

        :return: The id of this GroupingWidgetConfiguration.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this GroupingWidgetConfiguration.

        :param id: The id of this GroupingWidgetConfiguration.
        :type: str
        """

        self._id = id

    @property
    def size(self):
        """
        Gets the size of this GroupingWidgetConfiguration.

        :return: The size of this GroupingWidgetConfiguration.
        :rtype: int
        """
        return self._size

    @size.setter
    def size(self, size):
        """
        Sets the size of this GroupingWidgetConfiguration.

        :param size: The size of this GroupingWidgetConfiguration.
        :type: int
        """

        self._size = size

    @property
    def name(self):
        """
        Gets the name of this GroupingWidgetConfiguration.

        :return: The name of this GroupingWidgetConfiguration.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this GroupingWidgetConfiguration.

        :param name: The name of this GroupingWidgetConfiguration.
        :type: str
        """

        self._name = name

    @property
    def description(self):
        """
        Gets the description of this GroupingWidgetConfiguration.

        :return: The description of this GroupingWidgetConfiguration.
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """
        Sets the description of this GroupingWidgetConfiguration.

        :param description: The description of this GroupingWidgetConfiguration.
        :type: str
        """

        self._description = description

    @property
    def locations(self):
        """
        Gets the locations of this GroupingWidgetConfiguration.

        :return: The locations of this GroupingWidgetConfiguration.
        :rtype: list[str]
        """
        return self._locations

    @locations.setter
    def locations(self, locations):
        """
        Sets the locations of this GroupingWidgetConfiguration.

        :param locations: The locations of this GroupingWidgetConfiguration.
        :type: list[str]
        """

        self._locations = locations

    @property
    def refresh_rate_in_seconds(self):
        """
        Gets the refresh_rate_in_seconds of this GroupingWidgetConfiguration.

        :return: The refresh_rate_in_seconds of this GroupingWidgetConfiguration.
        :rtype: int
        """
        return self._refresh_rate_in_seconds

    @refresh_rate_in_seconds.setter
    def refresh_rate_in_seconds(self, refresh_rate_in_seconds):
        """
        Sets the refresh_rate_in_seconds of this GroupingWidgetConfiguration.

        :param refresh_rate_in_seconds: The refresh_rate_in_seconds of this GroupingWidgetConfiguration.
        :type: int
        """

        self._refresh_rate_in_seconds = refresh_rate_in_seconds

    @property
    def manual_font_scaling(self):
        """
        Gets the manual_font_scaling of this GroupingWidgetConfiguration.

        :return: The manual_font_scaling of this GroupingWidgetConfiguration.
        :rtype: bool
        """
        return self._manual_font_scaling

    @manual_font_scaling.setter
    def manual_font_scaling(self, manual_font_scaling):
        """
        Sets the manual_font_scaling of this GroupingWidgetConfiguration.

        :param manual_font_scaling: The manual_font_scaling of this GroupingWidgetConfiguration.
        :type: bool
        """

        self._manual_font_scaling = manual_font_scaling

    @property
    def font_multiplier(self):
        """
        Gets the font_multiplier of this GroupingWidgetConfiguration.

        :return: The font_multiplier of this GroupingWidgetConfiguration.
        :rtype: float
        """
        return self._font_multiplier

    @font_multiplier.setter
    def font_multiplier(self, font_multiplier):
        """
        Sets the font_multiplier of this GroupingWidgetConfiguration.

        :param font_multiplier: The font_multiplier of this GroupingWidgetConfiguration.
        :type: float
        """

        self._font_multiplier = font_multiplier

    @property
    def toolbar_behavior(self):
        """
        Gets the toolbar_behavior of this GroupingWidgetConfiguration.

        :return: The toolbar_behavior of this GroupingWidgetConfiguration.
        :rtype: str
        """
        return self._toolbar_behavior

    @toolbar_behavior.setter
    def toolbar_behavior(self, toolbar_behavior):
        """
        Sets the toolbar_behavior of this GroupingWidgetConfiguration.

        :param toolbar_behavior: The toolbar_behavior of this GroupingWidgetConfiguration.
        :type: str
        """
        allowed_values = ["AlwaysShow", "AutoHide", "Collapsible", "Hide"]
        if toolbar_behavior is not None and toolbar_behavior not in allowed_values:
            raise ValueError(
                "Invalid value for `toolbar_behavior` ({0}), must be one of {1}"
                .format(toolbar_behavior, allowed_values)
            )

        self._toolbar_behavior = toolbar_behavior

    @property
    def num_cols(self):
        """
        Gets the num_cols of this GroupingWidgetConfiguration.

        :return: The num_cols of this GroupingWidgetConfiguration.
        :rtype: int
        """
        return self._num_cols

    @num_cols.setter
    def num_cols(self, num_cols):
        """
        Sets the num_cols of this GroupingWidgetConfiguration.

        :param num_cols: The num_cols of this GroupingWidgetConfiguration.
        :type: int
        """

        self._num_cols = num_cols

    @property
    def num_rows(self):
        """
        Gets the num_rows of this GroupingWidgetConfiguration.

        :return: The num_rows of this GroupingWidgetConfiguration.
        :rtype: int
        """
        return self._num_rows

    @num_rows.setter
    def num_rows(self, num_rows):
        """
        Sets the num_rows of this GroupingWidgetConfiguration.

        :param num_rows: The num_rows of this GroupingWidgetConfiguration.
        :type: int
        """

        self._num_rows = num_rows

    @property
    def widgets(self):
        """
        Gets the widgets of this GroupingWidgetConfiguration.

        :return: The widgets of this GroupingWidgetConfiguration.
        :rtype: list[WidgetComposition]
        """
        return self._widgets

    @widgets.setter
    def widgets(self, widgets):
        """
        Sets the widgets of this GroupingWidgetConfiguration.

        :param widgets: The widgets of this GroupingWidgetConfiguration.
        :type: list[WidgetComposition]
        """

        self._widgets = widgets

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
        if not isinstance(other, GroupingWidgetConfiguration):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

