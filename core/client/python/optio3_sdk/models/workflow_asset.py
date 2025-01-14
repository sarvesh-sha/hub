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

class WorkflowAsset(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sys_id=None, key=None, name=None):
        """
        WorkflowAsset - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'sys_id': 'str',
            'key': 'str',
            'name': 'str'
        }

        self.attribute_map = {
            'sys_id': 'sysId',
            'key': 'key',
            'name': 'name'
        }

        self._sys_id = sys_id
        self._key = key
        self._name = name


    @property
    def sys_id(self):
        """
        Gets the sys_id of this WorkflowAsset.

        :return: The sys_id of this WorkflowAsset.
        :rtype: str
        """
        return self._sys_id

    @sys_id.setter
    def sys_id(self, sys_id):
        """
        Sets the sys_id of this WorkflowAsset.

        :param sys_id: The sys_id of this WorkflowAsset.
        :type: str
        """

        self._sys_id = sys_id

    @property
    def key(self):
        """
        Gets the key of this WorkflowAsset.

        :return: The key of this WorkflowAsset.
        :rtype: str
        """
        return self._key

    @key.setter
    def key(self, key):
        """
        Sets the key of this WorkflowAsset.

        :param key: The key of this WorkflowAsset.
        :type: str
        """

        self._key = key

    @property
    def name(self):
        """
        Gets the name of this WorkflowAsset.

        :return: The name of this WorkflowAsset.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this WorkflowAsset.

        :param name: The name of this WorkflowAsset.
        :type: str
        """

        self._name = name

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
        if not isinstance(other, WorkflowAsset):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
