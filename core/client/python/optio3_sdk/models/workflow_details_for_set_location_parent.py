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

class WorkflowDetailsForSetLocationParent(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, parent_location_sys_id=None, child_location_sys_ids=None):
        """
        WorkflowDetailsForSetLocationParent - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'parent_location_sys_id': 'str',
            'child_location_sys_ids': 'list[str]',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'parent_location_sys_id': 'parentLocationSysId',
            'child_location_sys_ids': 'childLocationSysIds',
            'discriminator___type': '__type'
        }

        self._parent_location_sys_id = parent_location_sys_id
        self._child_location_sys_ids = child_location_sys_ids

    @property
    def discriminator___type(self):
        return "WorkflowDetailsForSetLocationParent"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def parent_location_sys_id(self):
        """
        Gets the parent_location_sys_id of this WorkflowDetailsForSetLocationParent.

        :return: The parent_location_sys_id of this WorkflowDetailsForSetLocationParent.
        :rtype: str
        """
        return self._parent_location_sys_id

    @parent_location_sys_id.setter
    def parent_location_sys_id(self, parent_location_sys_id):
        """
        Sets the parent_location_sys_id of this WorkflowDetailsForSetLocationParent.

        :param parent_location_sys_id: The parent_location_sys_id of this WorkflowDetailsForSetLocationParent.
        :type: str
        """

        self._parent_location_sys_id = parent_location_sys_id

    @property
    def child_location_sys_ids(self):
        """
        Gets the child_location_sys_ids of this WorkflowDetailsForSetLocationParent.

        :return: The child_location_sys_ids of this WorkflowDetailsForSetLocationParent.
        :rtype: list[str]
        """
        return self._child_location_sys_ids

    @child_location_sys_ids.setter
    def child_location_sys_ids(self, child_location_sys_ids):
        """
        Sets the child_location_sys_ids of this WorkflowDetailsForSetLocationParent.

        :param child_location_sys_ids: The child_location_sys_ids of this WorkflowDetailsForSetLocationParent.
        :type: list[str]
        """

        self._child_location_sys_ids = child_location_sys_ids

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
        if not isinstance(other, WorkflowDetailsForSetLocationParent):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
