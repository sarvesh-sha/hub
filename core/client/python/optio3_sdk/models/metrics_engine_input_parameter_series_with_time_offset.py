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

class MetricsEngineInputParameterSeriesWithTimeOffset(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, title=None, description=None, node_id=None, time_shift=None, time_shift_unit=None):
        """
        MetricsEngineInputParameterSeriesWithTimeOffset - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'x': 'int',
            'y': 'int',
            'title': 'str',
            'description': 'str',
            'node_id': 'str',
            'time_shift': 'int',
            'time_shift_unit': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'title': 'title',
            'description': 'description',
            'node_id': 'nodeId',
            'time_shift': 'timeShift',
            'time_shift_unit': 'timeShiftUnit',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._title = title
        self._description = description
        self._node_id = node_id
        self._time_shift = time_shift
        self._time_shift_unit = time_shift_unit

    @property
    def discriminator___type(self):
        return "MetricsEngineInputParameterSeriesWithTimeOffset"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :return: The id of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :param id: The id of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :return: The x of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :param x: The x of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :return: The y of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :param y: The y of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :type: int
        """

        self._y = y

    @property
    def title(self):
        """
        Gets the title of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :return: The title of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :rtype: str
        """
        return self._title

    @title.setter
    def title(self, title):
        """
        Sets the title of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :param title: The title of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :type: str
        """

        self._title = title

    @property
    def description(self):
        """
        Gets the description of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :return: The description of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """
        Sets the description of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :param description: The description of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :type: str
        """

        self._description = description

    @property
    def node_id(self):
        """
        Gets the node_id of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :return: The node_id of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :rtype: str
        """
        return self._node_id

    @node_id.setter
    def node_id(self, node_id):
        """
        Sets the node_id of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :param node_id: The node_id of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :type: str
        """

        self._node_id = node_id

    @property
    def time_shift(self):
        """
        Gets the time_shift of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :return: The time_shift of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :rtype: int
        """
        return self._time_shift

    @time_shift.setter
    def time_shift(self, time_shift):
        """
        Sets the time_shift of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :param time_shift: The time_shift of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :type: int
        """

        self._time_shift = time_shift

    @property
    def time_shift_unit(self):
        """
        Gets the time_shift_unit of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :return: The time_shift_unit of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :rtype: str
        """
        return self._time_shift_unit

    @time_shift_unit.setter
    def time_shift_unit(self, time_shift_unit):
        """
        Sets the time_shift_unit of this MetricsEngineInputParameterSeriesWithTimeOffset.

        :param time_shift_unit: The time_shift_unit of this MetricsEngineInputParameterSeriesWithTimeOffset.
        :type: str
        """
        allowed_values = ["NANOS", "MICROS", "MILLIS", "SECONDS", "MINUTES", "HOURS", "HALF_DAYS", "DAYS", "WEEKS", "MONTHS", "YEARS", "DECADES", "CENTURIES", "MILLENNIA", "ERAS", "FOREVER"]
        if time_shift_unit is not None and time_shift_unit not in allowed_values:
            raise ValueError(
                "Invalid value for `time_shift_unit` ({0}), must be one of {1}"
                .format(time_shift_unit, allowed_values)
            )

        self._time_shift_unit = time_shift_unit

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
        if not isinstance(other, MetricsEngineInputParameterSeriesWithTimeOffset):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
