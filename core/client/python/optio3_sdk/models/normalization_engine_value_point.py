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

class NormalizationEngineValuePoint(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, locator=None, object_id=None, name=None, backup_name=None, description=None, location=None):
        """
        NormalizationEngineValuePoint - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'locator': 'RecordLocator',
            'object_id': 'str',
            'name': 'str',
            'backup_name': 'str',
            'description': 'str',
            'location': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'locator': 'locator',
            'object_id': 'objectId',
            'name': 'name',
            'backup_name': 'backupName',
            'description': 'description',
            'location': 'location',
            'discriminator___type': '__type'
        }

        self._locator = locator
        self._object_id = object_id
        self._name = name
        self._backup_name = backup_name
        self._description = description
        self._location = location

    @property
    def discriminator___type(self):
        return "NormalizationEngineValuePoint"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def locator(self):
        """
        Gets the locator of this NormalizationEngineValuePoint.

        :return: The locator of this NormalizationEngineValuePoint.
        :rtype: RecordLocator
        """
        return self._locator

    @locator.setter
    def locator(self, locator):
        """
        Sets the locator of this NormalizationEngineValuePoint.

        :param locator: The locator of this NormalizationEngineValuePoint.
        :type: RecordLocator
        """

        self._locator = locator

    @property
    def object_id(self):
        """
        Gets the object_id of this NormalizationEngineValuePoint.

        :return: The object_id of this NormalizationEngineValuePoint.
        :rtype: str
        """
        return self._object_id

    @object_id.setter
    def object_id(self, object_id):
        """
        Sets the object_id of this NormalizationEngineValuePoint.

        :param object_id: The object_id of this NormalizationEngineValuePoint.
        :type: str
        """

        self._object_id = object_id

    @property
    def name(self):
        """
        Gets the name of this NormalizationEngineValuePoint.

        :return: The name of this NormalizationEngineValuePoint.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this NormalizationEngineValuePoint.

        :param name: The name of this NormalizationEngineValuePoint.
        :type: str
        """

        self._name = name

    @property
    def backup_name(self):
        """
        Gets the backup_name of this NormalizationEngineValuePoint.

        :return: The backup_name of this NormalizationEngineValuePoint.
        :rtype: str
        """
        return self._backup_name

    @backup_name.setter
    def backup_name(self, backup_name):
        """
        Sets the backup_name of this NormalizationEngineValuePoint.

        :param backup_name: The backup_name of this NormalizationEngineValuePoint.
        :type: str
        """

        self._backup_name = backup_name

    @property
    def description(self):
        """
        Gets the description of this NormalizationEngineValuePoint.

        :return: The description of this NormalizationEngineValuePoint.
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """
        Sets the description of this NormalizationEngineValuePoint.

        :param description: The description of this NormalizationEngineValuePoint.
        :type: str
        """

        self._description = description

    @property
    def location(self):
        """
        Gets the location of this NormalizationEngineValuePoint.

        :return: The location of this NormalizationEngineValuePoint.
        :rtype: str
        """
        return self._location

    @location.setter
    def location(self, location):
        """
        Sets the location of this NormalizationEngineValuePoint.

        :param location: The location of this NormalizationEngineValuePoint.
        :type: str
        """

        self._location = location

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
        if not isinstance(other, NormalizationEngineValuePoint):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other