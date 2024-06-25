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

class DigineousVibrationMonitorDetails(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, plant_id=None, label=None, device_name=None):
        """
        DigineousVibrationMonitorDetails - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'int',
            'plant_id': 'int',
            'label': 'str',
            'device_name': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'plant_id': 'plantId',
            'label': 'label',
            'device_name': 'deviceName'
        }

        self._id = id
        self._plant_id = plant_id
        self._label = label
        self._device_name = device_name


    @property
    def id(self):
        """
        Gets the id of this DigineousVibrationMonitorDetails.

        :return: The id of this DigineousVibrationMonitorDetails.
        :rtype: int
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this DigineousVibrationMonitorDetails.

        :param id: The id of this DigineousVibrationMonitorDetails.
        :type: int
        """

        self._id = id

    @property
    def plant_id(self):
        """
        Gets the plant_id of this DigineousVibrationMonitorDetails.

        :return: The plant_id of this DigineousVibrationMonitorDetails.
        :rtype: int
        """
        return self._plant_id

    @plant_id.setter
    def plant_id(self, plant_id):
        """
        Sets the plant_id of this DigineousVibrationMonitorDetails.

        :param plant_id: The plant_id of this DigineousVibrationMonitorDetails.
        :type: int
        """

        self._plant_id = plant_id

    @property
    def label(self):
        """
        Gets the label of this DigineousVibrationMonitorDetails.

        :return: The label of this DigineousVibrationMonitorDetails.
        :rtype: str
        """
        return self._label

    @label.setter
    def label(self, label):
        """
        Sets the label of this DigineousVibrationMonitorDetails.

        :param label: The label of this DigineousVibrationMonitorDetails.
        :type: str
        """

        self._label = label

    @property
    def device_name(self):
        """
        Gets the device_name of this DigineousVibrationMonitorDetails.

        :return: The device_name of this DigineousVibrationMonitorDetails.
        :rtype: str
        """
        return self._device_name

    @device_name.setter
    def device_name(self, device_name):
        """
        Sets the device_name of this DigineousVibrationMonitorDetails.

        :param device_name: The device_name of this DigineousVibrationMonitorDetails.
        :type: str
        """

        self._device_name = device_name

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
        if not isinstance(other, DigineousVibrationMonitorDetails):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
