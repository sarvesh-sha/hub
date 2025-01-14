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

class Asset(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sys_id=None, created_on=None, updated_on=None, name=None, physical_name=None, logical_name=None, normalized_name=None, display_name=None, state=None, asset_id=None, serial_number=None, customer_notes=None, last_checked_date=None, last_updated_date=None, hidden=None, location=None, point_class_id=None, equipment_class_id=None, azure_digital_twin_model=None, is_equipment=None, classification_tags=None, manual_tags=None, parent_asset=None, identity_descriptor=None):
        """
        Asset - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'sys_id': 'str',
            'created_on': 'datetime',
            'updated_on': 'datetime',
            'name': 'str',
            'physical_name': 'str',
            'logical_name': 'str',
            'normalized_name': 'str',
            'display_name': 'str',
            'state': 'str',
            'asset_id': 'str',
            'serial_number': 'str',
            'customer_notes': 'str',
            'last_checked_date': 'datetime',
            'last_updated_date': 'datetime',
            'hidden': 'bool',
            'location': 'RecordIdentity',
            'point_class_id': 'str',
            'equipment_class_id': 'str',
            'azure_digital_twin_model': 'str',
            'is_equipment': 'bool',
            'classification_tags': 'list[str]',
            'manual_tags': 'list[str]',
            'parent_asset': 'RecordIdentity',
            'identity_descriptor': 'BaseAssetDescriptor',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'sys_id': 'sysId',
            'created_on': 'createdOn',
            'updated_on': 'updatedOn',
            'name': 'name',
            'physical_name': 'physicalName',
            'logical_name': 'logicalName',
            'normalized_name': 'normalizedName',
            'display_name': 'displayName',
            'state': 'state',
            'asset_id': 'assetId',
            'serial_number': 'serialNumber',
            'customer_notes': 'customerNotes',
            'last_checked_date': 'lastCheckedDate',
            'last_updated_date': 'lastUpdatedDate',
            'hidden': 'hidden',
            'location': 'location',
            'point_class_id': 'pointClassId',
            'equipment_class_id': 'equipmentClassId',
            'azure_digital_twin_model': 'azureDigitalTwinModel',
            'is_equipment': 'isEquipment',
            'classification_tags': 'classificationTags',
            'manual_tags': 'manualTags',
            'parent_asset': 'parentAsset',
            'identity_descriptor': 'identityDescriptor',
            'discriminator___type': '__type'
        }

        self._sys_id = sys_id
        self._created_on = created_on
        self._updated_on = updated_on
        self._name = name
        self._physical_name = physical_name
        self._logical_name = logical_name
        self._normalized_name = normalized_name
        self._display_name = display_name
        self._state = state
        self._asset_id = asset_id
        self._serial_number = serial_number
        self._customer_notes = customer_notes
        self._last_checked_date = last_checked_date
        self._last_updated_date = last_updated_date
        self._hidden = hidden
        self._location = location
        self._point_class_id = point_class_id
        self._equipment_class_id = equipment_class_id
        self._azure_digital_twin_model = azure_digital_twin_model
        self._is_equipment = is_equipment
        self._classification_tags = classification_tags
        self._manual_tags = manual_tags
        self._parent_asset = parent_asset
        self._identity_descriptor = identity_descriptor

    @property
    def discriminator___type(self):
        return "Asset"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @staticmethod
    def fixup_prototype(data):
        className = data["__type"]
        from .. import models
        switcher = {
        "BACnetDevice": models.BACnetDevice,
        "Device": models.Device,
        "DeviceElement": models.DeviceElement,
        "GatewayAsset": models.GatewayAsset,
        "HostAsset": models.HostAsset,
        "IpnDevice": models.IpnDevice,
        "Location": models.Location,
        "LogicalAsset": models.LogicalAsset,
        "MetricsDeviceElement": models.MetricsDeviceElement,
        "NetworkAsset": models.NetworkAsset,
        "Asset": Asset
        }

        klass = switcher.get(className)
        if klass:
            return klass()
        else:
            raise Exception("Unable to deserialize unknown type")

    @property
    def sys_id(self):
        """
        Gets the sys_id of this Asset.

        :return: The sys_id of this Asset.
        :rtype: str
        """
        return self._sys_id

    @sys_id.setter
    def sys_id(self, sys_id):
        """
        Sets the sys_id of this Asset.

        :param sys_id: The sys_id of this Asset.
        :type: str
        """

        self._sys_id = sys_id

    @property
    def created_on(self):
        """
        Gets the created_on of this Asset.

        :return: The created_on of this Asset.
        :rtype: datetime
        """
        return self._created_on

    @created_on.setter
    def created_on(self, created_on):
        """
        Sets the created_on of this Asset.

        :param created_on: The created_on of this Asset.
        :type: datetime
        """

        self._created_on = created_on

    @property
    def updated_on(self):
        """
        Gets the updated_on of this Asset.

        :return: The updated_on of this Asset.
        :rtype: datetime
        """
        return self._updated_on

    @updated_on.setter
    def updated_on(self, updated_on):
        """
        Sets the updated_on of this Asset.

        :param updated_on: The updated_on of this Asset.
        :type: datetime
        """

        self._updated_on = updated_on

    @property
    def name(self):
        """
        Gets the name of this Asset.

        :return: The name of this Asset.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this Asset.

        :param name: The name of this Asset.
        :type: str
        """

        self._name = name

    @property
    def physical_name(self):
        """
        Gets the physical_name of this Asset.

        :return: The physical_name of this Asset.
        :rtype: str
        """
        return self._physical_name

    @physical_name.setter
    def physical_name(self, physical_name):
        """
        Sets the physical_name of this Asset.

        :param physical_name: The physical_name of this Asset.
        :type: str
        """

        self._physical_name = physical_name

    @property
    def logical_name(self):
        """
        Gets the logical_name of this Asset.

        :return: The logical_name of this Asset.
        :rtype: str
        """
        return self._logical_name

    @logical_name.setter
    def logical_name(self, logical_name):
        """
        Sets the logical_name of this Asset.

        :param logical_name: The logical_name of this Asset.
        :type: str
        """

        self._logical_name = logical_name

    @property
    def normalized_name(self):
        """
        Gets the normalized_name of this Asset.

        :return: The normalized_name of this Asset.
        :rtype: str
        """
        return self._normalized_name

    @normalized_name.setter
    def normalized_name(self, normalized_name):
        """
        Sets the normalized_name of this Asset.

        :param normalized_name: The normalized_name of this Asset.
        :type: str
        """

        self._normalized_name = normalized_name

    @property
    def display_name(self):
        """
        Gets the display_name of this Asset.

        :return: The display_name of this Asset.
        :rtype: str
        """
        return self._display_name

    @display_name.setter
    def display_name(self, display_name):
        """
        Sets the display_name of this Asset.

        :param display_name: The display_name of this Asset.
        :type: str
        """

        self._display_name = display_name

    @property
    def state(self):
        """
        Gets the state of this Asset.

        :return: The state of this Asset.
        :rtype: str
        """
        return self._state

    @state.setter
    def state(self, state):
        """
        Sets the state of this Asset.

        :param state: The state of this Asset.
        :type: str
        """
        allowed_values = ["provisioned", "offline", "passive", "operational", "maintenance", "retired"]
        if state is not None and state not in allowed_values:
            raise ValueError(
                "Invalid value for `state` ({0}), must be one of {1}"
                .format(state, allowed_values)
            )

        self._state = state

    @property
    def asset_id(self):
        """
        Gets the asset_id of this Asset.

        :return: The asset_id of this Asset.
        :rtype: str
        """
        return self._asset_id

    @asset_id.setter
    def asset_id(self, asset_id):
        """
        Sets the asset_id of this Asset.

        :param asset_id: The asset_id of this Asset.
        :type: str
        """

        self._asset_id = asset_id

    @property
    def serial_number(self):
        """
        Gets the serial_number of this Asset.

        :return: The serial_number of this Asset.
        :rtype: str
        """
        return self._serial_number

    @serial_number.setter
    def serial_number(self, serial_number):
        """
        Sets the serial_number of this Asset.

        :param serial_number: The serial_number of this Asset.
        :type: str
        """

        self._serial_number = serial_number

    @property
    def customer_notes(self):
        """
        Gets the customer_notes of this Asset.

        :return: The customer_notes of this Asset.
        :rtype: str
        """
        return self._customer_notes

    @customer_notes.setter
    def customer_notes(self, customer_notes):
        """
        Sets the customer_notes of this Asset.

        :param customer_notes: The customer_notes of this Asset.
        :type: str
        """

        self._customer_notes = customer_notes

    @property
    def last_checked_date(self):
        """
        Gets the last_checked_date of this Asset.

        :return: The last_checked_date of this Asset.
        :rtype: datetime
        """
        return self._last_checked_date

    @last_checked_date.setter
    def last_checked_date(self, last_checked_date):
        """
        Sets the last_checked_date of this Asset.

        :param last_checked_date: The last_checked_date of this Asset.
        :type: datetime
        """

        self._last_checked_date = last_checked_date

    @property
    def last_updated_date(self):
        """
        Gets the last_updated_date of this Asset.

        :return: The last_updated_date of this Asset.
        :rtype: datetime
        """
        return self._last_updated_date

    @last_updated_date.setter
    def last_updated_date(self, last_updated_date):
        """
        Sets the last_updated_date of this Asset.

        :param last_updated_date: The last_updated_date of this Asset.
        :type: datetime
        """

        self._last_updated_date = last_updated_date

    @property
    def hidden(self):
        """
        Gets the hidden of this Asset.

        :return: The hidden of this Asset.
        :rtype: bool
        """
        return self._hidden

    @hidden.setter
    def hidden(self, hidden):
        """
        Sets the hidden of this Asset.

        :param hidden: The hidden of this Asset.
        :type: bool
        """

        self._hidden = hidden

    @property
    def location(self):
        """
        Gets the location of this Asset.

        :return: The location of this Asset.
        :rtype: RecordIdentity
        """
        return self._location

    @location.setter
    def location(self, location):
        """
        Sets the location of this Asset.

        :param location: The location of this Asset.
        :type: RecordIdentity
        """

        self._location = location

    @property
    def point_class_id(self):
        """
        Gets the point_class_id of this Asset.

        :return: The point_class_id of this Asset.
        :rtype: str
        """
        return self._point_class_id

    @point_class_id.setter
    def point_class_id(self, point_class_id):
        """
        Sets the point_class_id of this Asset.

        :param point_class_id: The point_class_id of this Asset.
        :type: str
        """

        self._point_class_id = point_class_id

    @property
    def equipment_class_id(self):
        """
        Gets the equipment_class_id of this Asset.

        :return: The equipment_class_id of this Asset.
        :rtype: str
        """
        return self._equipment_class_id

    @equipment_class_id.setter
    def equipment_class_id(self, equipment_class_id):
        """
        Sets the equipment_class_id of this Asset.

        :param equipment_class_id: The equipment_class_id of this Asset.
        :type: str
        """

        self._equipment_class_id = equipment_class_id

    @property
    def azure_digital_twin_model(self):
        """
        Gets the azure_digital_twin_model of this Asset.

        :return: The azure_digital_twin_model of this Asset.
        :rtype: str
        """
        return self._azure_digital_twin_model

    @azure_digital_twin_model.setter
    def azure_digital_twin_model(self, azure_digital_twin_model):
        """
        Sets the azure_digital_twin_model of this Asset.

        :param azure_digital_twin_model: The azure_digital_twin_model of this Asset.
        :type: str
        """

        self._azure_digital_twin_model = azure_digital_twin_model

    @property
    def is_equipment(self):
        """
        Gets the is_equipment of this Asset.

        :return: The is_equipment of this Asset.
        :rtype: bool
        """
        return self._is_equipment

    @is_equipment.setter
    def is_equipment(self, is_equipment):
        """
        Sets the is_equipment of this Asset.

        :param is_equipment: The is_equipment of this Asset.
        :type: bool
        """

        self._is_equipment = is_equipment

    @property
    def classification_tags(self):
        """
        Gets the classification_tags of this Asset.

        :return: The classification_tags of this Asset.
        :rtype: list[str]
        """
        return self._classification_tags

    @classification_tags.setter
    def classification_tags(self, classification_tags):
        """
        Sets the classification_tags of this Asset.

        :param classification_tags: The classification_tags of this Asset.
        :type: list[str]
        """

        self._classification_tags = classification_tags

    @property
    def manual_tags(self):
        """
        Gets the manual_tags of this Asset.

        :return: The manual_tags of this Asset.
        :rtype: list[str]
        """
        return self._manual_tags

    @manual_tags.setter
    def manual_tags(self, manual_tags):
        """
        Sets the manual_tags of this Asset.

        :param manual_tags: The manual_tags of this Asset.
        :type: list[str]
        """

        self._manual_tags = manual_tags

    @property
    def parent_asset(self):
        """
        Gets the parent_asset of this Asset.

        :return: The parent_asset of this Asset.
        :rtype: RecordIdentity
        """
        return self._parent_asset

    @parent_asset.setter
    def parent_asset(self, parent_asset):
        """
        Sets the parent_asset of this Asset.

        :param parent_asset: The parent_asset of this Asset.
        :type: RecordIdentity
        """

        self._parent_asset = parent_asset

    @property
    def identity_descriptor(self):
        """
        Gets the identity_descriptor of this Asset.

        :return: The identity_descriptor of this Asset.
        :rtype: BaseAssetDescriptor
        """
        return self._identity_descriptor

    @identity_descriptor.setter
    def identity_descriptor(self, identity_descriptor):
        """
        Sets the identity_descriptor of this Asset.

        :param identity_descriptor: The identity_descriptor of this Asset.
        :type: BaseAssetDescriptor
        """

        self._identity_descriptor = identity_descriptor

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
        if not isinstance(other, Asset):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

