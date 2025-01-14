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

class ScatterPlotSource(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, device_element_id=None, binding=None):
        """
        ScatterPlotSource - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'device_element_id': 'str',
            'binding': 'AssetGraphBinding'
        }

        self.attribute_map = {
            'device_element_id': 'deviceElementId',
            'binding': 'binding'
        }

        self._device_element_id = device_element_id
        self._binding = binding


    @property
    def device_element_id(self):
        """
        Gets the device_element_id of this ScatterPlotSource.

        :return: The device_element_id of this ScatterPlotSource.
        :rtype: str
        """
        return self._device_element_id

    @device_element_id.setter
    def device_element_id(self, device_element_id):
        """
        Sets the device_element_id of this ScatterPlotSource.

        :param device_element_id: The device_element_id of this ScatterPlotSource.
        :type: str
        """

        self._device_element_id = device_element_id

    @property
    def binding(self):
        """
        Gets the binding of this ScatterPlotSource.

        :return: The binding of this ScatterPlotSource.
        :rtype: AssetGraphBinding
        """
        return self._binding

    @binding.setter
    def binding(self, binding):
        """
        Sets the binding of this ScatterPlotSource.

        :param binding: The binding of this ScatterPlotSource.
        :type: AssetGraphBinding
        """

        self._binding = binding

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
        if not isinstance(other, ScatterPlotSource):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
