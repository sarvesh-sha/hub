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

class SearchResultSet(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, total_users=None, users=None, total_alerts=None, alerts=None, total_devices=None, devices=None, total_locations=None, locations=None, total_networks=None, networks=None, total_gateways=None, gateways=None, total_device_elements=None, device_elements=None, total_logical_groups=None, logical_groups=None):
        """
        SearchResultSet - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'total_users': 'int',
            'users': 'list[RecordIdentity]',
            'total_alerts': 'int',
            'alerts': 'list[RecordIdentity]',
            'total_devices': 'int',
            'devices': 'list[RecordIdentity]',
            'total_locations': 'int',
            'locations': 'list[RecordIdentity]',
            'total_networks': 'int',
            'networks': 'list[RecordIdentity]',
            'total_gateways': 'int',
            'gateways': 'list[RecordIdentity]',
            'total_device_elements': 'int',
            'device_elements': 'list[RecordIdentity]',
            'total_logical_groups': 'int',
            'logical_groups': 'list[RecordIdentity]'
        }

        self.attribute_map = {
            'total_users': 'totalUsers',
            'users': 'users',
            'total_alerts': 'totalAlerts',
            'alerts': 'alerts',
            'total_devices': 'totalDevices',
            'devices': 'devices',
            'total_locations': 'totalLocations',
            'locations': 'locations',
            'total_networks': 'totalNetworks',
            'networks': 'networks',
            'total_gateways': 'totalGateways',
            'gateways': 'gateways',
            'total_device_elements': 'totalDeviceElements',
            'device_elements': 'deviceElements',
            'total_logical_groups': 'totalLogicalGroups',
            'logical_groups': 'logicalGroups'
        }

        self._total_users = total_users
        self._users = users
        self._total_alerts = total_alerts
        self._alerts = alerts
        self._total_devices = total_devices
        self._devices = devices
        self._total_locations = total_locations
        self._locations = locations
        self._total_networks = total_networks
        self._networks = networks
        self._total_gateways = total_gateways
        self._gateways = gateways
        self._total_device_elements = total_device_elements
        self._device_elements = device_elements
        self._total_logical_groups = total_logical_groups
        self._logical_groups = logical_groups


    @property
    def total_users(self):
        """
        Gets the total_users of this SearchResultSet.

        :return: The total_users of this SearchResultSet.
        :rtype: int
        """
        return self._total_users

    @total_users.setter
    def total_users(self, total_users):
        """
        Sets the total_users of this SearchResultSet.

        :param total_users: The total_users of this SearchResultSet.
        :type: int
        """

        self._total_users = total_users

    @property
    def users(self):
        """
        Gets the users of this SearchResultSet.

        :return: The users of this SearchResultSet.
        :rtype: list[RecordIdentity]
        """
        return self._users

    @users.setter
    def users(self, users):
        """
        Sets the users of this SearchResultSet.

        :param users: The users of this SearchResultSet.
        :type: list[RecordIdentity]
        """

        self._users = users

    @property
    def total_alerts(self):
        """
        Gets the total_alerts of this SearchResultSet.

        :return: The total_alerts of this SearchResultSet.
        :rtype: int
        """
        return self._total_alerts

    @total_alerts.setter
    def total_alerts(self, total_alerts):
        """
        Sets the total_alerts of this SearchResultSet.

        :param total_alerts: The total_alerts of this SearchResultSet.
        :type: int
        """

        self._total_alerts = total_alerts

    @property
    def alerts(self):
        """
        Gets the alerts of this SearchResultSet.

        :return: The alerts of this SearchResultSet.
        :rtype: list[RecordIdentity]
        """
        return self._alerts

    @alerts.setter
    def alerts(self, alerts):
        """
        Sets the alerts of this SearchResultSet.

        :param alerts: The alerts of this SearchResultSet.
        :type: list[RecordIdentity]
        """

        self._alerts = alerts

    @property
    def total_devices(self):
        """
        Gets the total_devices of this SearchResultSet.

        :return: The total_devices of this SearchResultSet.
        :rtype: int
        """
        return self._total_devices

    @total_devices.setter
    def total_devices(self, total_devices):
        """
        Sets the total_devices of this SearchResultSet.

        :param total_devices: The total_devices of this SearchResultSet.
        :type: int
        """

        self._total_devices = total_devices

    @property
    def devices(self):
        """
        Gets the devices of this SearchResultSet.

        :return: The devices of this SearchResultSet.
        :rtype: list[RecordIdentity]
        """
        return self._devices

    @devices.setter
    def devices(self, devices):
        """
        Sets the devices of this SearchResultSet.

        :param devices: The devices of this SearchResultSet.
        :type: list[RecordIdentity]
        """

        self._devices = devices

    @property
    def total_locations(self):
        """
        Gets the total_locations of this SearchResultSet.

        :return: The total_locations of this SearchResultSet.
        :rtype: int
        """
        return self._total_locations

    @total_locations.setter
    def total_locations(self, total_locations):
        """
        Sets the total_locations of this SearchResultSet.

        :param total_locations: The total_locations of this SearchResultSet.
        :type: int
        """

        self._total_locations = total_locations

    @property
    def locations(self):
        """
        Gets the locations of this SearchResultSet.

        :return: The locations of this SearchResultSet.
        :rtype: list[RecordIdentity]
        """
        return self._locations

    @locations.setter
    def locations(self, locations):
        """
        Sets the locations of this SearchResultSet.

        :param locations: The locations of this SearchResultSet.
        :type: list[RecordIdentity]
        """

        self._locations = locations

    @property
    def total_networks(self):
        """
        Gets the total_networks of this SearchResultSet.

        :return: The total_networks of this SearchResultSet.
        :rtype: int
        """
        return self._total_networks

    @total_networks.setter
    def total_networks(self, total_networks):
        """
        Sets the total_networks of this SearchResultSet.

        :param total_networks: The total_networks of this SearchResultSet.
        :type: int
        """

        self._total_networks = total_networks

    @property
    def networks(self):
        """
        Gets the networks of this SearchResultSet.

        :return: The networks of this SearchResultSet.
        :rtype: list[RecordIdentity]
        """
        return self._networks

    @networks.setter
    def networks(self, networks):
        """
        Sets the networks of this SearchResultSet.

        :param networks: The networks of this SearchResultSet.
        :type: list[RecordIdentity]
        """

        self._networks = networks

    @property
    def total_gateways(self):
        """
        Gets the total_gateways of this SearchResultSet.

        :return: The total_gateways of this SearchResultSet.
        :rtype: int
        """
        return self._total_gateways

    @total_gateways.setter
    def total_gateways(self, total_gateways):
        """
        Sets the total_gateways of this SearchResultSet.

        :param total_gateways: The total_gateways of this SearchResultSet.
        :type: int
        """

        self._total_gateways = total_gateways

    @property
    def gateways(self):
        """
        Gets the gateways of this SearchResultSet.

        :return: The gateways of this SearchResultSet.
        :rtype: list[RecordIdentity]
        """
        return self._gateways

    @gateways.setter
    def gateways(self, gateways):
        """
        Sets the gateways of this SearchResultSet.

        :param gateways: The gateways of this SearchResultSet.
        :type: list[RecordIdentity]
        """

        self._gateways = gateways

    @property
    def total_device_elements(self):
        """
        Gets the total_device_elements of this SearchResultSet.

        :return: The total_device_elements of this SearchResultSet.
        :rtype: int
        """
        return self._total_device_elements

    @total_device_elements.setter
    def total_device_elements(self, total_device_elements):
        """
        Sets the total_device_elements of this SearchResultSet.

        :param total_device_elements: The total_device_elements of this SearchResultSet.
        :type: int
        """

        self._total_device_elements = total_device_elements

    @property
    def device_elements(self):
        """
        Gets the device_elements of this SearchResultSet.

        :return: The device_elements of this SearchResultSet.
        :rtype: list[RecordIdentity]
        """
        return self._device_elements

    @device_elements.setter
    def device_elements(self, device_elements):
        """
        Sets the device_elements of this SearchResultSet.

        :param device_elements: The device_elements of this SearchResultSet.
        :type: list[RecordIdentity]
        """

        self._device_elements = device_elements

    @property
    def total_logical_groups(self):
        """
        Gets the total_logical_groups of this SearchResultSet.

        :return: The total_logical_groups of this SearchResultSet.
        :rtype: int
        """
        return self._total_logical_groups

    @total_logical_groups.setter
    def total_logical_groups(self, total_logical_groups):
        """
        Sets the total_logical_groups of this SearchResultSet.

        :param total_logical_groups: The total_logical_groups of this SearchResultSet.
        :type: int
        """

        self._total_logical_groups = total_logical_groups

    @property
    def logical_groups(self):
        """
        Gets the logical_groups of this SearchResultSet.

        :return: The logical_groups of this SearchResultSet.
        :rtype: list[RecordIdentity]
        """
        return self._logical_groups

    @logical_groups.setter
    def logical_groups(self, logical_groups):
        """
        Sets the logical_groups of this SearchResultSet.

        :param logical_groups: The logical_groups of this SearchResultSet.
        :type: list[RecordIdentity]
        """

        self._logical_groups = logical_groups

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
        if not isinstance(other, SearchResultSet):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
