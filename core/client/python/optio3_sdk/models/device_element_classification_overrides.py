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

class DeviceElementClassificationOverrides(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, equipments=None, point_name=None, point_class_id=None, locations_with_type=None):
        """
        DeviceElementClassificationOverrides - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'equipments': 'list[NormalizationEquipment]',
            'point_name': 'str',
            'point_class_id': 'str',
            'locations_with_type': 'list[NormalizationEquipmentLocation]'
        }

        self.attribute_map = {
            'equipments': 'equipments',
            'point_name': 'pointName',
            'point_class_id': 'pointClassId',
            'locations_with_type': 'locationsWithType'
        }

        self._equipments = equipments
        self._point_name = point_name
        self._point_class_id = point_class_id
        self._locations_with_type = locations_with_type


    @property
    def equipments(self):
        """
        Gets the equipments of this DeviceElementClassificationOverrides.

        :return: The equipments of this DeviceElementClassificationOverrides.
        :rtype: list[NormalizationEquipment]
        """
        return self._equipments

    @equipments.setter
    def equipments(self, equipments):
        """
        Sets the equipments of this DeviceElementClassificationOverrides.

        :param equipments: The equipments of this DeviceElementClassificationOverrides.
        :type: list[NormalizationEquipment]
        """

        self._equipments = equipments

    @property
    def point_name(self):
        """
        Gets the point_name of this DeviceElementClassificationOverrides.

        :return: The point_name of this DeviceElementClassificationOverrides.
        :rtype: str
        """
        return self._point_name

    @point_name.setter
    def point_name(self, point_name):
        """
        Sets the point_name of this DeviceElementClassificationOverrides.

        :param point_name: The point_name of this DeviceElementClassificationOverrides.
        :type: str
        """

        self._point_name = point_name

    @property
    def point_class_id(self):
        """
        Gets the point_class_id of this DeviceElementClassificationOverrides.

        :return: The point_class_id of this DeviceElementClassificationOverrides.
        :rtype: str
        """
        return self._point_class_id

    @point_class_id.setter
    def point_class_id(self, point_class_id):
        """
        Sets the point_class_id of this DeviceElementClassificationOverrides.

        :param point_class_id: The point_class_id of this DeviceElementClassificationOverrides.
        :type: str
        """

        self._point_class_id = point_class_id

    @property
    def locations_with_type(self):
        """
        Gets the locations_with_type of this DeviceElementClassificationOverrides.

        :return: The locations_with_type of this DeviceElementClassificationOverrides.
        :rtype: list[NormalizationEquipmentLocation]
        """
        return self._locations_with_type

    @locations_with_type.setter
    def locations_with_type(self, locations_with_type):
        """
        Sets the locations_with_type of this DeviceElementClassificationOverrides.

        :param locations_with_type: The locations_with_type of this DeviceElementClassificationOverrides.
        :type: list[NormalizationEquipmentLocation]
        """

        self._locations_with_type = locations_with_type

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
        if not isinstance(other, DeviceElementClassificationOverrides):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
