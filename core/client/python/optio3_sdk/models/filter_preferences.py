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

class FilterPreferences(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, name=None, location_ids=None, alert_rules=None, alert_status_ids=None, alert_severity_ids=None, alert_type_ids=None, like_device_manufacturer_name=None, like_device_product_name=None, like_device_model_name=None, equipment_ids=None, equipment_class_ids=None, device_ids=None, point_class_ids=None, is_sampling=None, is_classified=None, assigned_to_ids=None, created_by_ids=None, workflow_type_ids=None, workflow_status_ids=None, workflow_priority_ids=None):
        """
        FilterPreferences - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'name': 'str',
            'location_ids': 'list[str]',
            'alert_rules': 'list[RecordIdentity]',
            'alert_status_ids': 'list[str]',
            'alert_severity_ids': 'list[str]',
            'alert_type_ids': 'list[str]',
            'like_device_manufacturer_name': 'str',
            'like_device_product_name': 'str',
            'like_device_model_name': 'str',
            'equipment_ids': 'list[str]',
            'equipment_class_ids': 'list[str]',
            'device_ids': 'list[str]',
            'point_class_ids': 'list[str]',
            'is_sampling': 'str',
            'is_classified': 'str',
            'assigned_to_ids': 'list[str]',
            'created_by_ids': 'list[str]',
            'workflow_type_ids': 'list[str]',
            'workflow_status_ids': 'list[str]',
            'workflow_priority_ids': 'list[str]'
        }

        self.attribute_map = {
            'id': 'id',
            'name': 'name',
            'location_ids': 'locationIDs',
            'alert_rules': 'alertRules',
            'alert_status_ids': 'alertStatusIDs',
            'alert_severity_ids': 'alertSeverityIDs',
            'alert_type_ids': 'alertTypeIDs',
            'like_device_manufacturer_name': 'likeDeviceManufacturerName',
            'like_device_product_name': 'likeDeviceProductName',
            'like_device_model_name': 'likeDeviceModelName',
            'equipment_ids': 'equipmentIDs',
            'equipment_class_ids': 'equipmentClassIDs',
            'device_ids': 'deviceIDs',
            'point_class_ids': 'pointClassIDs',
            'is_sampling': 'isSampling',
            'is_classified': 'isClassified',
            'assigned_to_ids': 'assignedToIDs',
            'created_by_ids': 'createdByIDs',
            'workflow_type_ids': 'workflowTypeIDs',
            'workflow_status_ids': 'workflowStatusIDs',
            'workflow_priority_ids': 'workflowPriorityIDs'
        }

        self._id = id
        self._name = name
        self._location_ids = location_ids
        self._alert_rules = alert_rules
        self._alert_status_ids = alert_status_ids
        self._alert_severity_ids = alert_severity_ids
        self._alert_type_ids = alert_type_ids
        self._like_device_manufacturer_name = like_device_manufacturer_name
        self._like_device_product_name = like_device_product_name
        self._like_device_model_name = like_device_model_name
        self._equipment_ids = equipment_ids
        self._equipment_class_ids = equipment_class_ids
        self._device_ids = device_ids
        self._point_class_ids = point_class_ids
        self._is_sampling = is_sampling
        self._is_classified = is_classified
        self._assigned_to_ids = assigned_to_ids
        self._created_by_ids = created_by_ids
        self._workflow_type_ids = workflow_type_ids
        self._workflow_status_ids = workflow_status_ids
        self._workflow_priority_ids = workflow_priority_ids


    @property
    def id(self):
        """
        Gets the id of this FilterPreferences.

        :return: The id of this FilterPreferences.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this FilterPreferences.

        :param id: The id of this FilterPreferences.
        :type: str
        """

        self._id = id

    @property
    def name(self):
        """
        Gets the name of this FilterPreferences.

        :return: The name of this FilterPreferences.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this FilterPreferences.

        :param name: The name of this FilterPreferences.
        :type: str
        """

        self._name = name

    @property
    def location_ids(self):
        """
        Gets the location_ids of this FilterPreferences.

        :return: The location_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._location_ids

    @location_ids.setter
    def location_ids(self, location_ids):
        """
        Sets the location_ids of this FilterPreferences.

        :param location_ids: The location_ids of this FilterPreferences.
        :type: list[str]
        """

        self._location_ids = location_ids

    @property
    def alert_rules(self):
        """
        Gets the alert_rules of this FilterPreferences.

        :return: The alert_rules of this FilterPreferences.
        :rtype: list[RecordIdentity]
        """
        return self._alert_rules

    @alert_rules.setter
    def alert_rules(self, alert_rules):
        """
        Sets the alert_rules of this FilterPreferences.

        :param alert_rules: The alert_rules of this FilterPreferences.
        :type: list[RecordIdentity]
        """

        self._alert_rules = alert_rules

    @property
    def alert_status_ids(self):
        """
        Gets the alert_status_ids of this FilterPreferences.

        :return: The alert_status_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._alert_status_ids

    @alert_status_ids.setter
    def alert_status_ids(self, alert_status_ids):
        """
        Sets the alert_status_ids of this FilterPreferences.

        :param alert_status_ids: The alert_status_ids of this FilterPreferences.
        :type: list[str]
        """
        allowed_values = ["active", "muted", "resolved", "closed"]
        if not set(alert_status_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `alert_status_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(alert_status_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._alert_status_ids = alert_status_ids

    @property
    def alert_severity_ids(self):
        """
        Gets the alert_severity_ids of this FilterPreferences.

        :return: The alert_severity_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._alert_severity_ids

    @alert_severity_ids.setter
    def alert_severity_ids(self, alert_severity_ids):
        """
        Sets the alert_severity_ids of this FilterPreferences.

        :param alert_severity_ids: The alert_severity_ids of this FilterPreferences.
        :type: list[str]
        """
        allowed_values = ["CRITICAL", "SIGNIFICANT", "NORMAL", "LOW"]
        if not set(alert_severity_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `alert_severity_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(alert_severity_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._alert_severity_ids = alert_severity_ids

    @property
    def alert_type_ids(self):
        """
        Gets the alert_type_ids of this FilterPreferences.

        :return: The alert_type_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._alert_type_ids

    @alert_type_ids.setter
    def alert_type_ids(self, alert_type_ids):
        """
        Sets the alert_type_ids of this FilterPreferences.

        :param alert_type_ids: The alert_type_ids of this FilterPreferences.
        :type: list[str]
        """
        allowed_values = ["ALARM", "COMMUNICATION_PROBLEM", "DEVICE_FAILURE", "END_OF_LIFE", "INFORMATIONAL", "OPERATOR_SUMMARY", "RECALL", "THRESHOLD_EXCEEDED", "WARNING", "WARRANTY"]
        if not set(alert_type_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `alert_type_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(alert_type_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._alert_type_ids = alert_type_ids

    @property
    def like_device_manufacturer_name(self):
        """
        Gets the like_device_manufacturer_name of this FilterPreferences.

        :return: The like_device_manufacturer_name of this FilterPreferences.
        :rtype: str
        """
        return self._like_device_manufacturer_name

    @like_device_manufacturer_name.setter
    def like_device_manufacturer_name(self, like_device_manufacturer_name):
        """
        Sets the like_device_manufacturer_name of this FilterPreferences.

        :param like_device_manufacturer_name: The like_device_manufacturer_name of this FilterPreferences.
        :type: str
        """

        self._like_device_manufacturer_name = like_device_manufacturer_name

    @property
    def like_device_product_name(self):
        """
        Gets the like_device_product_name of this FilterPreferences.

        :return: The like_device_product_name of this FilterPreferences.
        :rtype: str
        """
        return self._like_device_product_name

    @like_device_product_name.setter
    def like_device_product_name(self, like_device_product_name):
        """
        Sets the like_device_product_name of this FilterPreferences.

        :param like_device_product_name: The like_device_product_name of this FilterPreferences.
        :type: str
        """

        self._like_device_product_name = like_device_product_name

    @property
    def like_device_model_name(self):
        """
        Gets the like_device_model_name of this FilterPreferences.

        :return: The like_device_model_name of this FilterPreferences.
        :rtype: str
        """
        return self._like_device_model_name

    @like_device_model_name.setter
    def like_device_model_name(self, like_device_model_name):
        """
        Sets the like_device_model_name of this FilterPreferences.

        :param like_device_model_name: The like_device_model_name of this FilterPreferences.
        :type: str
        """

        self._like_device_model_name = like_device_model_name

    @property
    def equipment_ids(self):
        """
        Gets the equipment_ids of this FilterPreferences.

        :return: The equipment_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._equipment_ids

    @equipment_ids.setter
    def equipment_ids(self, equipment_ids):
        """
        Sets the equipment_ids of this FilterPreferences.

        :param equipment_ids: The equipment_ids of this FilterPreferences.
        :type: list[str]
        """

        self._equipment_ids = equipment_ids

    @property
    def equipment_class_ids(self):
        """
        Gets the equipment_class_ids of this FilterPreferences.

        :return: The equipment_class_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._equipment_class_ids

    @equipment_class_ids.setter
    def equipment_class_ids(self, equipment_class_ids):
        """
        Sets the equipment_class_ids of this FilterPreferences.

        :param equipment_class_ids: The equipment_class_ids of this FilterPreferences.
        :type: list[str]
        """

        self._equipment_class_ids = equipment_class_ids

    @property
    def device_ids(self):
        """
        Gets the device_ids of this FilterPreferences.

        :return: The device_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._device_ids

    @device_ids.setter
    def device_ids(self, device_ids):
        """
        Sets the device_ids of this FilterPreferences.

        :param device_ids: The device_ids of this FilterPreferences.
        :type: list[str]
        """

        self._device_ids = device_ids

    @property
    def point_class_ids(self):
        """
        Gets the point_class_ids of this FilterPreferences.

        :return: The point_class_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._point_class_ids

    @point_class_ids.setter
    def point_class_ids(self, point_class_ids):
        """
        Sets the point_class_ids of this FilterPreferences.

        :param point_class_ids: The point_class_ids of this FilterPreferences.
        :type: list[str]
        """

        self._point_class_ids = point_class_ids

    @property
    def is_sampling(self):
        """
        Gets the is_sampling of this FilterPreferences.

        :return: The is_sampling of this FilterPreferences.
        :rtype: str
        """
        return self._is_sampling

    @is_sampling.setter
    def is_sampling(self, is_sampling):
        """
        Sets the is_sampling of this FilterPreferences.

        :param is_sampling: The is_sampling of this FilterPreferences.
        :type: str
        """
        allowed_values = ["Yes", "No"]
        if is_sampling is not None and is_sampling not in allowed_values:
            raise ValueError(
                "Invalid value for `is_sampling` ({0}), must be one of {1}"
                .format(is_sampling, allowed_values)
            )

        self._is_sampling = is_sampling

    @property
    def is_classified(self):
        """
        Gets the is_classified of this FilterPreferences.

        :return: The is_classified of this FilterPreferences.
        :rtype: str
        """
        return self._is_classified

    @is_classified.setter
    def is_classified(self, is_classified):
        """
        Sets the is_classified of this FilterPreferences.

        :param is_classified: The is_classified of this FilterPreferences.
        :type: str
        """
        allowed_values = ["Yes", "No"]
        if is_classified is not None and is_classified not in allowed_values:
            raise ValueError(
                "Invalid value for `is_classified` ({0}), must be one of {1}"
                .format(is_classified, allowed_values)
            )

        self._is_classified = is_classified

    @property
    def assigned_to_ids(self):
        """
        Gets the assigned_to_ids of this FilterPreferences.

        :return: The assigned_to_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._assigned_to_ids

    @assigned_to_ids.setter
    def assigned_to_ids(self, assigned_to_ids):
        """
        Sets the assigned_to_ids of this FilterPreferences.

        :param assigned_to_ids: The assigned_to_ids of this FilterPreferences.
        :type: list[str]
        """

        self._assigned_to_ids = assigned_to_ids

    @property
    def created_by_ids(self):
        """
        Gets the created_by_ids of this FilterPreferences.

        :return: The created_by_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._created_by_ids

    @created_by_ids.setter
    def created_by_ids(self, created_by_ids):
        """
        Sets the created_by_ids of this FilterPreferences.

        :param created_by_ids: The created_by_ids of this FilterPreferences.
        :type: list[str]
        """

        self._created_by_ids = created_by_ids

    @property
    def workflow_type_ids(self):
        """
        Gets the workflow_type_ids of this FilterPreferences.

        :return: The workflow_type_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._workflow_type_ids

    @workflow_type_ids.setter
    def workflow_type_ids(self, workflow_type_ids):
        """
        Sets the workflow_type_ids of this FilterPreferences.

        :param workflow_type_ids: The workflow_type_ids of this FilterPreferences.
        :type: list[str]
        """
        allowed_values = ["RenameControlPoint", "SamplingControlPoint", "SamplingPeriod", "HidingControlPoint", "AssignControlPointsToEquipment", "SetControlPointsClass", "IgnoreDevice", "RenameDevice", "SetDeviceLocation", "RenameEquipment", "RemoveEquipment", "MergeEquipments", "NewEquipment", "SetEquipmentClass", "SetEquipmentParent", "SetEquipmentLocation", "SetLocationParent"]
        if not set(workflow_type_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `workflow_type_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(workflow_type_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._workflow_type_ids = workflow_type_ids

    @property
    def workflow_status_ids(self):
        """
        Gets the workflow_status_ids of this FilterPreferences.

        :return: The workflow_status_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._workflow_status_ids

    @workflow_status_ids.setter
    def workflow_status_ids(self, workflow_status_ids):
        """
        Sets the workflow_status_ids of this FilterPreferences.

        :param workflow_status_ids: The workflow_status_ids of this FilterPreferences.
        :type: list[str]
        """
        allowed_values = ["Active", "Resolved", "Closed", "Disabling", "Disabled"]
        if not set(workflow_status_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `workflow_status_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(workflow_status_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._workflow_status_ids = workflow_status_ids

    @property
    def workflow_priority_ids(self):
        """
        Gets the workflow_priority_ids of this FilterPreferences.

        :return: The workflow_priority_ids of this FilterPreferences.
        :rtype: list[str]
        """
        return self._workflow_priority_ids

    @workflow_priority_ids.setter
    def workflow_priority_ids(self, workflow_priority_ids):
        """
        Sets the workflow_priority_ids of this FilterPreferences.

        :param workflow_priority_ids: The workflow_priority_ids of this FilterPreferences.
        :type: list[str]
        """
        allowed_values = ["Urgent", "High", "Normal", "Low"]
        if not set(workflow_priority_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `workflow_priority_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(workflow_priority_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._workflow_priority_ids = workflow_priority_ids

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
        if not isinstance(other, FilterPreferences):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

