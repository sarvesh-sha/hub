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

class MetricsDefinitionVersion(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sys_id=None, created_on=None, updated_on=None, version=None, definition=None, details=None, predecessor=None, successors=None):
        """
        MetricsDefinitionVersion - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'sys_id': 'str',
            'created_on': 'datetime',
            'updated_on': 'datetime',
            'version': 'int',
            'definition': 'RecordIdentity',
            'details': 'MetricsDefinitionDetails',
            'predecessor': 'RecordIdentity',
            'successors': 'list[RecordIdentity]'
        }

        self.attribute_map = {
            'sys_id': 'sysId',
            'created_on': 'createdOn',
            'updated_on': 'updatedOn',
            'version': 'version',
            'definition': 'definition',
            'details': 'details',
            'predecessor': 'predecessor',
            'successors': 'successors'
        }

        self._sys_id = sys_id
        self._created_on = created_on
        self._updated_on = updated_on
        self._version = version
        self._definition = definition
        self._details = details
        self._predecessor = predecessor
        self._successors = successors


    @property
    def sys_id(self):
        """
        Gets the sys_id of this MetricsDefinitionVersion.

        :return: The sys_id of this MetricsDefinitionVersion.
        :rtype: str
        """
        return self._sys_id

    @sys_id.setter
    def sys_id(self, sys_id):
        """
        Sets the sys_id of this MetricsDefinitionVersion.

        :param sys_id: The sys_id of this MetricsDefinitionVersion.
        :type: str
        """

        self._sys_id = sys_id

    @property
    def created_on(self):
        """
        Gets the created_on of this MetricsDefinitionVersion.

        :return: The created_on of this MetricsDefinitionVersion.
        :rtype: datetime
        """
        return self._created_on

    @created_on.setter
    def created_on(self, created_on):
        """
        Sets the created_on of this MetricsDefinitionVersion.

        :param created_on: The created_on of this MetricsDefinitionVersion.
        :type: datetime
        """

        self._created_on = created_on

    @property
    def updated_on(self):
        """
        Gets the updated_on of this MetricsDefinitionVersion.

        :return: The updated_on of this MetricsDefinitionVersion.
        :rtype: datetime
        """
        return self._updated_on

    @updated_on.setter
    def updated_on(self, updated_on):
        """
        Sets the updated_on of this MetricsDefinitionVersion.

        :param updated_on: The updated_on of this MetricsDefinitionVersion.
        :type: datetime
        """

        self._updated_on = updated_on

    @property
    def version(self):
        """
        Gets the version of this MetricsDefinitionVersion.

        :return: The version of this MetricsDefinitionVersion.
        :rtype: int
        """
        return self._version

    @version.setter
    def version(self, version):
        """
        Sets the version of this MetricsDefinitionVersion.

        :param version: The version of this MetricsDefinitionVersion.
        :type: int
        """

        self._version = version

    @property
    def definition(self):
        """
        Gets the definition of this MetricsDefinitionVersion.

        :return: The definition of this MetricsDefinitionVersion.
        :rtype: RecordIdentity
        """
        return self._definition

    @definition.setter
    def definition(self, definition):
        """
        Sets the definition of this MetricsDefinitionVersion.

        :param definition: The definition of this MetricsDefinitionVersion.
        :type: RecordIdentity
        """

        self._definition = definition

    @property
    def details(self):
        """
        Gets the details of this MetricsDefinitionVersion.

        :return: The details of this MetricsDefinitionVersion.
        :rtype: MetricsDefinitionDetails
        """
        return self._details

    @details.setter
    def details(self, details):
        """
        Sets the details of this MetricsDefinitionVersion.

        :param details: The details of this MetricsDefinitionVersion.
        :type: MetricsDefinitionDetails
        """

        self._details = details

    @property
    def predecessor(self):
        """
        Gets the predecessor of this MetricsDefinitionVersion.

        :return: The predecessor of this MetricsDefinitionVersion.
        :rtype: RecordIdentity
        """
        return self._predecessor

    @predecessor.setter
    def predecessor(self, predecessor):
        """
        Sets the predecessor of this MetricsDefinitionVersion.

        :param predecessor: The predecessor of this MetricsDefinitionVersion.
        :type: RecordIdentity
        """

        self._predecessor = predecessor

    @property
    def successors(self):
        """
        Gets the successors of this MetricsDefinitionVersion.

        :return: The successors of this MetricsDefinitionVersion.
        :rtype: list[RecordIdentity]
        """
        return self._successors

    @successors.setter
    def successors(self, successors):
        """
        Sets the successors of this MetricsDefinitionVersion.

        :param successors: The successors of this MetricsDefinitionVersion.
        :type: list[RecordIdentity]
        """

        self._successors = successors

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
        if not isinstance(other, MetricsDefinitionVersion):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
