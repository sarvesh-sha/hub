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

class ControllerMetadataAggregation(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sys_id=None, name=None, network_number=None, instance_number=None, points=None):
        """
        ControllerMetadataAggregation - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'sys_id': 'str',
            'name': 'str',
            'network_number': 'int',
            'instance_number': 'int',
            'points': 'list[MetadataAggregationPoint]'
        }

        self.attribute_map = {
            'sys_id': 'sysId',
            'name': 'name',
            'network_number': 'networkNumber',
            'instance_number': 'instanceNumber',
            'points': 'points'
        }

        self._sys_id = sys_id
        self._name = name
        self._network_number = network_number
        self._instance_number = instance_number
        self._points = points


    @property
    def sys_id(self):
        """
        Gets the sys_id of this ControllerMetadataAggregation.

        :return: The sys_id of this ControllerMetadataAggregation.
        :rtype: str
        """
        return self._sys_id

    @sys_id.setter
    def sys_id(self, sys_id):
        """
        Sets the sys_id of this ControllerMetadataAggregation.

        :param sys_id: The sys_id of this ControllerMetadataAggregation.
        :type: str
        """

        self._sys_id = sys_id

    @property
    def name(self):
        """
        Gets the name of this ControllerMetadataAggregation.

        :return: The name of this ControllerMetadataAggregation.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this ControllerMetadataAggregation.

        :param name: The name of this ControllerMetadataAggregation.
        :type: str
        """

        self._name = name

    @property
    def network_number(self):
        """
        Gets the network_number of this ControllerMetadataAggregation.

        :return: The network_number of this ControllerMetadataAggregation.
        :rtype: int
        """
        return self._network_number

    @network_number.setter
    def network_number(self, network_number):
        """
        Sets the network_number of this ControllerMetadataAggregation.

        :param network_number: The network_number of this ControllerMetadataAggregation.
        :type: int
        """

        self._network_number = network_number

    @property
    def instance_number(self):
        """
        Gets the instance_number of this ControllerMetadataAggregation.

        :return: The instance_number of this ControllerMetadataAggregation.
        :rtype: int
        """
        return self._instance_number

    @instance_number.setter
    def instance_number(self, instance_number):
        """
        Sets the instance_number of this ControllerMetadataAggregation.

        :param instance_number: The instance_number of this ControllerMetadataAggregation.
        :type: int
        """

        self._instance_number = instance_number

    @property
    def points(self):
        """
        Gets the points of this ControllerMetadataAggregation.

        :return: The points of this ControllerMetadataAggregation.
        :rtype: list[MetadataAggregationPoint]
        """
        return self._points

    @points.setter
    def points(self, points):
        """
        Sets the points of this ControllerMetadataAggregation.

        :param points: The points of this ControllerMetadataAggregation.
        :type: list[MetadataAggregationPoint]
        """

        self._points = points

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
        if not isinstance(other, ControllerMetadataAggregation):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
