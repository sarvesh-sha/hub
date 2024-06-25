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

class UserGroup(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sys_id=None, created_on=None, updated_on=None, name=None, description=None, roles=None, sub_groups=None):
        """
        UserGroup - a model defined in Swagger

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
            'description': 'str',
            'roles': 'list[RecordIdentity]',
            'sub_groups': 'list[RecordIdentity]'
        }

        self.attribute_map = {
            'sys_id': 'sysId',
            'created_on': 'createdOn',
            'updated_on': 'updatedOn',
            'name': 'name',
            'description': 'description',
            'roles': 'roles',
            'sub_groups': 'subGroups'
        }

        self._sys_id = sys_id
        self._created_on = created_on
        self._updated_on = updated_on
        self._name = name
        self._description = description
        self._roles = roles
        self._sub_groups = sub_groups


    @property
    def sys_id(self):
        """
        Gets the sys_id of this UserGroup.

        :return: The sys_id of this UserGroup.
        :rtype: str
        """
        return self._sys_id

    @sys_id.setter
    def sys_id(self, sys_id):
        """
        Sets the sys_id of this UserGroup.

        :param sys_id: The sys_id of this UserGroup.
        :type: str
        """

        self._sys_id = sys_id

    @property
    def created_on(self):
        """
        Gets the created_on of this UserGroup.

        :return: The created_on of this UserGroup.
        :rtype: datetime
        """
        return self._created_on

    @created_on.setter
    def created_on(self, created_on):
        """
        Sets the created_on of this UserGroup.

        :param created_on: The created_on of this UserGroup.
        :type: datetime
        """

        self._created_on = created_on

    @property
    def updated_on(self):
        """
        Gets the updated_on of this UserGroup.

        :return: The updated_on of this UserGroup.
        :rtype: datetime
        """
        return self._updated_on

    @updated_on.setter
    def updated_on(self, updated_on):
        """
        Sets the updated_on of this UserGroup.

        :param updated_on: The updated_on of this UserGroup.
        :type: datetime
        """

        self._updated_on = updated_on

    @property
    def name(self):
        """
        Gets the name of this UserGroup.

        :return: The name of this UserGroup.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this UserGroup.

        :param name: The name of this UserGroup.
        :type: str
        """

        self._name = name

    @property
    def description(self):
        """
        Gets the description of this UserGroup.

        :return: The description of this UserGroup.
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """
        Sets the description of this UserGroup.

        :param description: The description of this UserGroup.
        :type: str
        """

        self._description = description

    @property
    def roles(self):
        """
        Gets the roles of this UserGroup.

        :return: The roles of this UserGroup.
        :rtype: list[RecordIdentity]
        """
        return self._roles

    @roles.setter
    def roles(self, roles):
        """
        Sets the roles of this UserGroup.

        :param roles: The roles of this UserGroup.
        :type: list[RecordIdentity]
        """

        self._roles = roles

    @property
    def sub_groups(self):
        """
        Gets the sub_groups of this UserGroup.

        :return: The sub_groups of this UserGroup.
        :rtype: list[RecordIdentity]
        """
        return self._sub_groups

    @sub_groups.setter
    def sub_groups(self, sub_groups):
        """
        Sets the sub_groups of this UserGroup.

        :param sub_groups: The sub_groups of this UserGroup.
        :type: list[RecordIdentity]
        """

        self._sub_groups = sub_groups

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
        if not isinstance(other, UserGroup):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other