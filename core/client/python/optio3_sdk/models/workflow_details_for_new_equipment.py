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

class WorkflowDetailsForNewEquipment(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, equipment_key=None, equipment_name=None, equipment_class_id=None, location_sys_id=None, parent_equipment=None):
        """
        WorkflowDetailsForNewEquipment - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'equipment_key': 'str',
            'equipment_name': 'str',
            'equipment_class_id': 'str',
            'location_sys_id': 'str',
            'parent_equipment': 'WorkflowAsset',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'equipment_key': 'equipmentKey',
            'equipment_name': 'equipmentName',
            'equipment_class_id': 'equipmentClassId',
            'location_sys_id': 'locationSysId',
            'parent_equipment': 'parentEquipment',
            'discriminator___type': '__type'
        }

        self._equipment_key = equipment_key
        self._equipment_name = equipment_name
        self._equipment_class_id = equipment_class_id
        self._location_sys_id = location_sys_id
        self._parent_equipment = parent_equipment

    @property
    def discriminator___type(self):
        return "WorkflowDetailsForNewEquipment"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def equipment_key(self):
        """
        Gets the equipment_key of this WorkflowDetailsForNewEquipment.

        :return: The equipment_key of this WorkflowDetailsForNewEquipment.
        :rtype: str
        """
        return self._equipment_key

    @equipment_key.setter
    def equipment_key(self, equipment_key):
        """
        Sets the equipment_key of this WorkflowDetailsForNewEquipment.

        :param equipment_key: The equipment_key of this WorkflowDetailsForNewEquipment.
        :type: str
        """

        self._equipment_key = equipment_key

    @property
    def equipment_name(self):
        """
        Gets the equipment_name of this WorkflowDetailsForNewEquipment.

        :return: The equipment_name of this WorkflowDetailsForNewEquipment.
        :rtype: str
        """
        return self._equipment_name

    @equipment_name.setter
    def equipment_name(self, equipment_name):
        """
        Sets the equipment_name of this WorkflowDetailsForNewEquipment.

        :param equipment_name: The equipment_name of this WorkflowDetailsForNewEquipment.
        :type: str
        """

        self._equipment_name = equipment_name

    @property
    def equipment_class_id(self):
        """
        Gets the equipment_class_id of this WorkflowDetailsForNewEquipment.

        :return: The equipment_class_id of this WorkflowDetailsForNewEquipment.
        :rtype: str
        """
        return self._equipment_class_id

    @equipment_class_id.setter
    def equipment_class_id(self, equipment_class_id):
        """
        Sets the equipment_class_id of this WorkflowDetailsForNewEquipment.

        :param equipment_class_id: The equipment_class_id of this WorkflowDetailsForNewEquipment.
        :type: str
        """

        self._equipment_class_id = equipment_class_id

    @property
    def location_sys_id(self):
        """
        Gets the location_sys_id of this WorkflowDetailsForNewEquipment.

        :return: The location_sys_id of this WorkflowDetailsForNewEquipment.
        :rtype: str
        """
        return self._location_sys_id

    @location_sys_id.setter
    def location_sys_id(self, location_sys_id):
        """
        Sets the location_sys_id of this WorkflowDetailsForNewEquipment.

        :param location_sys_id: The location_sys_id of this WorkflowDetailsForNewEquipment.
        :type: str
        """

        self._location_sys_id = location_sys_id

    @property
    def parent_equipment(self):
        """
        Gets the parent_equipment of this WorkflowDetailsForNewEquipment.

        :return: The parent_equipment of this WorkflowDetailsForNewEquipment.
        :rtype: WorkflowAsset
        """
        return self._parent_equipment

    @parent_equipment.setter
    def parent_equipment(self, parent_equipment):
        """
        Sets the parent_equipment of this WorkflowDetailsForNewEquipment.

        :param parent_equipment: The parent_equipment of this WorkflowDetailsForNewEquipment.
        :type: WorkflowAsset
        """

        self._parent_equipment = parent_equipment

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
        if not isinstance(other, WorkflowDetailsForNewEquipment):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other