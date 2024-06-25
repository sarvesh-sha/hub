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

class Workflow(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sys_id=None, created_on=None, updated_on=None, asset=None, location=None, sequence_number=None, description=None, extended_description=None, created_by=None, assigned_to=None, status=None, type=None, priority=None, details=None):
        """
        Workflow - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'sys_id': 'str',
            'created_on': 'datetime',
            'updated_on': 'datetime',
            'asset': 'RecordIdentity',
            'location': 'RecordIdentity',
            'sequence_number': 'int',
            'description': 'str',
            'extended_description': 'str',
            'created_by': 'RecordIdentity',
            'assigned_to': 'RecordIdentity',
            'status': 'str',
            'type': 'str',
            'priority': 'str',
            'details': 'WorkflowDetails',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'sys_id': 'sysId',
            'created_on': 'createdOn',
            'updated_on': 'updatedOn',
            'asset': 'asset',
            'location': 'location',
            'sequence_number': 'sequenceNumber',
            'description': 'description',
            'extended_description': 'extendedDescription',
            'created_by': 'createdBy',
            'assigned_to': 'assignedTo',
            'status': 'status',
            'type': 'type',
            'priority': 'priority',
            'details': 'details',
            'discriminator___type': '__type'
        }

        self._sys_id = sys_id
        self._created_on = created_on
        self._updated_on = updated_on
        self._asset = asset
        self._location = location
        self._sequence_number = sequence_number
        self._description = description
        self._extended_description = extended_description
        self._created_by = created_by
        self._assigned_to = assigned_to
        self._status = status
        self._type = type
        self._priority = priority
        self._details = details

    @property
    def discriminator___type(self):
        return "Workflow"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def sys_id(self):
        """
        Gets the sys_id of this Workflow.

        :return: The sys_id of this Workflow.
        :rtype: str
        """
        return self._sys_id

    @sys_id.setter
    def sys_id(self, sys_id):
        """
        Sets the sys_id of this Workflow.

        :param sys_id: The sys_id of this Workflow.
        :type: str
        """

        self._sys_id = sys_id

    @property
    def created_on(self):
        """
        Gets the created_on of this Workflow.

        :return: The created_on of this Workflow.
        :rtype: datetime
        """
        return self._created_on

    @created_on.setter
    def created_on(self, created_on):
        """
        Sets the created_on of this Workflow.

        :param created_on: The created_on of this Workflow.
        :type: datetime
        """

        self._created_on = created_on

    @property
    def updated_on(self):
        """
        Gets the updated_on of this Workflow.

        :return: The updated_on of this Workflow.
        :rtype: datetime
        """
        return self._updated_on

    @updated_on.setter
    def updated_on(self, updated_on):
        """
        Sets the updated_on of this Workflow.

        :param updated_on: The updated_on of this Workflow.
        :type: datetime
        """

        self._updated_on = updated_on

    @property
    def asset(self):
        """
        Gets the asset of this Workflow.

        :return: The asset of this Workflow.
        :rtype: RecordIdentity
        """
        return self._asset

    @asset.setter
    def asset(self, asset):
        """
        Sets the asset of this Workflow.

        :param asset: The asset of this Workflow.
        :type: RecordIdentity
        """

        self._asset = asset

    @property
    def location(self):
        """
        Gets the location of this Workflow.

        :return: The location of this Workflow.
        :rtype: RecordIdentity
        """
        return self._location

    @location.setter
    def location(self, location):
        """
        Sets the location of this Workflow.

        :param location: The location of this Workflow.
        :type: RecordIdentity
        """

        self._location = location

    @property
    def sequence_number(self):
        """
        Gets the sequence_number of this Workflow.

        :return: The sequence_number of this Workflow.
        :rtype: int
        """
        return self._sequence_number

    @sequence_number.setter
    def sequence_number(self, sequence_number):
        """
        Sets the sequence_number of this Workflow.

        :param sequence_number: The sequence_number of this Workflow.
        :type: int
        """

        self._sequence_number = sequence_number

    @property
    def description(self):
        """
        Gets the description of this Workflow.

        :return: The description of this Workflow.
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """
        Sets the description of this Workflow.

        :param description: The description of this Workflow.
        :type: str
        """

        self._description = description

    @property
    def extended_description(self):
        """
        Gets the extended_description of this Workflow.

        :return: The extended_description of this Workflow.
        :rtype: str
        """
        return self._extended_description

    @extended_description.setter
    def extended_description(self, extended_description):
        """
        Sets the extended_description of this Workflow.

        :param extended_description: The extended_description of this Workflow.
        :type: str
        """

        self._extended_description = extended_description

    @property
    def created_by(self):
        """
        Gets the created_by of this Workflow.

        :return: The created_by of this Workflow.
        :rtype: RecordIdentity
        """
        return self._created_by

    @created_by.setter
    def created_by(self, created_by):
        """
        Sets the created_by of this Workflow.

        :param created_by: The created_by of this Workflow.
        :type: RecordIdentity
        """

        self._created_by = created_by

    @property
    def assigned_to(self):
        """
        Gets the assigned_to of this Workflow.

        :return: The assigned_to of this Workflow.
        :rtype: RecordIdentity
        """
        return self._assigned_to

    @assigned_to.setter
    def assigned_to(self, assigned_to):
        """
        Sets the assigned_to of this Workflow.

        :param assigned_to: The assigned_to of this Workflow.
        :type: RecordIdentity
        """

        self._assigned_to = assigned_to

    @property
    def status(self):
        """
        Gets the status of this Workflow.

        :return: The status of this Workflow.
        :rtype: str
        """
        return self._status

    @status.setter
    def status(self, status):
        """
        Sets the status of this Workflow.

        :param status: The status of this Workflow.
        :type: str
        """
        allowed_values = ["Active", "Resolved", "Closed", "Disabling", "Disabled"]
        if status is not None and status not in allowed_values:
            raise ValueError(
                "Invalid value for `status` ({0}), must be one of {1}"
                .format(status, allowed_values)
            )

        self._status = status

    @property
    def type(self):
        """
        Gets the type of this Workflow.

        :return: The type of this Workflow.
        :rtype: str
        """
        return self._type

    @type.setter
    def type(self, type):
        """
        Sets the type of this Workflow.

        :param type: The type of this Workflow.
        :type: str
        """
        allowed_values = ["RenameControlPoint", "SamplingControlPoint", "SamplingPeriod", "HidingControlPoint", "AssignControlPointsToEquipment", "SetControlPointsClass", "IgnoreDevice", "RenameDevice", "SetDeviceLocation", "RenameEquipment", "RemoveEquipment", "MergeEquipments", "NewEquipment", "SetEquipmentClass", "SetEquipmentParent", "SetEquipmentLocation", "SetLocationParent"]
        if type is not None and type not in allowed_values:
            raise ValueError(
                "Invalid value for `type` ({0}), must be one of {1}"
                .format(type, allowed_values)
            )

        self._type = type

    @property
    def priority(self):
        """
        Gets the priority of this Workflow.

        :return: The priority of this Workflow.
        :rtype: str
        """
        return self._priority

    @priority.setter
    def priority(self, priority):
        """
        Sets the priority of this Workflow.

        :param priority: The priority of this Workflow.
        :type: str
        """
        allowed_values = ["Urgent", "High", "Normal", "Low"]
        if priority is not None and priority not in allowed_values:
            raise ValueError(
                "Invalid value for `priority` ({0}), must be one of {1}"
                .format(priority, allowed_values)
            )

        self._priority = priority

    @property
    def details(self):
        """
        Gets the details of this Workflow.

        :return: The details of this Workflow.
        :rtype: WorkflowDetails
        """
        return self._details

    @details.setter
    def details(self, details):
        """
        Sets the details of this Workflow.

        :param details: The details of this Workflow.
        :type: WorkflowDetails
        """

        self._details = details

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
        if not isinstance(other, Workflow):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
