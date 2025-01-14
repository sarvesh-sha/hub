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

class UserMessageDevice(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sys_id=None, created_on=None, updated_on=None, user=None, subject=None, body=None, flag_new=None, flag_read=None, flag_active=None, device=None):
        """
        UserMessageDevice - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'sys_id': 'str',
            'created_on': 'datetime',
            'updated_on': 'datetime',
            'user': 'RecordIdentity',
            'subject': 'str',
            'body': 'str',
            'flag_new': 'bool',
            'flag_read': 'bool',
            'flag_active': 'bool',
            'device': 'RecordIdentity',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'sys_id': 'sysId',
            'created_on': 'createdOn',
            'updated_on': 'updatedOn',
            'user': 'user',
            'subject': 'subject',
            'body': 'body',
            'flag_new': 'flagNew',
            'flag_read': 'flagRead',
            'flag_active': 'flagActive',
            'device': 'device',
            'discriminator___type': '__type'
        }

        self._sys_id = sys_id
        self._created_on = created_on
        self._updated_on = updated_on
        self._user = user
        self._subject = subject
        self._body = body
        self._flag_new = flag_new
        self._flag_read = flag_read
        self._flag_active = flag_active
        self._device = device

    @property
    def discriminator___type(self):
        return "UserMessageDevice"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def sys_id(self):
        """
        Gets the sys_id of this UserMessageDevice.

        :return: The sys_id of this UserMessageDevice.
        :rtype: str
        """
        return self._sys_id

    @sys_id.setter
    def sys_id(self, sys_id):
        """
        Sets the sys_id of this UserMessageDevice.

        :param sys_id: The sys_id of this UserMessageDevice.
        :type: str
        """

        self._sys_id = sys_id

    @property
    def created_on(self):
        """
        Gets the created_on of this UserMessageDevice.

        :return: The created_on of this UserMessageDevice.
        :rtype: datetime
        """
        return self._created_on

    @created_on.setter
    def created_on(self, created_on):
        """
        Sets the created_on of this UserMessageDevice.

        :param created_on: The created_on of this UserMessageDevice.
        :type: datetime
        """

        self._created_on = created_on

    @property
    def updated_on(self):
        """
        Gets the updated_on of this UserMessageDevice.

        :return: The updated_on of this UserMessageDevice.
        :rtype: datetime
        """
        return self._updated_on

    @updated_on.setter
    def updated_on(self, updated_on):
        """
        Sets the updated_on of this UserMessageDevice.

        :param updated_on: The updated_on of this UserMessageDevice.
        :type: datetime
        """

        self._updated_on = updated_on

    @property
    def user(self):
        """
        Gets the user of this UserMessageDevice.

        :return: The user of this UserMessageDevice.
        :rtype: RecordIdentity
        """
        return self._user

    @user.setter
    def user(self, user):
        """
        Sets the user of this UserMessageDevice.

        :param user: The user of this UserMessageDevice.
        :type: RecordIdentity
        """

        self._user = user

    @property
    def subject(self):
        """
        Gets the subject of this UserMessageDevice.

        :return: The subject of this UserMessageDevice.
        :rtype: str
        """
        return self._subject

    @subject.setter
    def subject(self, subject):
        """
        Sets the subject of this UserMessageDevice.

        :param subject: The subject of this UserMessageDevice.
        :type: str
        """

        self._subject = subject

    @property
    def body(self):
        """
        Gets the body of this UserMessageDevice.

        :return: The body of this UserMessageDevice.
        :rtype: str
        """
        return self._body

    @body.setter
    def body(self, body):
        """
        Sets the body of this UserMessageDevice.

        :param body: The body of this UserMessageDevice.
        :type: str
        """

        self._body = body

    @property
    def flag_new(self):
        """
        Gets the flag_new of this UserMessageDevice.

        :return: The flag_new of this UserMessageDevice.
        :rtype: bool
        """
        return self._flag_new

    @flag_new.setter
    def flag_new(self, flag_new):
        """
        Sets the flag_new of this UserMessageDevice.

        :param flag_new: The flag_new of this UserMessageDevice.
        :type: bool
        """

        self._flag_new = flag_new

    @property
    def flag_read(self):
        """
        Gets the flag_read of this UserMessageDevice.

        :return: The flag_read of this UserMessageDevice.
        :rtype: bool
        """
        return self._flag_read

    @flag_read.setter
    def flag_read(self, flag_read):
        """
        Sets the flag_read of this UserMessageDevice.

        :param flag_read: The flag_read of this UserMessageDevice.
        :type: bool
        """

        self._flag_read = flag_read

    @property
    def flag_active(self):
        """
        Gets the flag_active of this UserMessageDevice.

        :return: The flag_active of this UserMessageDevice.
        :rtype: bool
        """
        return self._flag_active

    @flag_active.setter
    def flag_active(self, flag_active):
        """
        Sets the flag_active of this UserMessageDevice.

        :param flag_active: The flag_active of this UserMessageDevice.
        :type: bool
        """

        self._flag_active = flag_active

    @property
    def device(self):
        """
        Gets the device of this UserMessageDevice.

        :return: The device of this UserMessageDevice.
        :rtype: RecordIdentity
        """
        return self._device

    @device.setter
    def device(self, device):
        """
        Sets the device of this UserMessageDevice.

        :param device: The device of this UserMessageDevice.
        :type: RecordIdentity
        """

        self._device = device

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
        if not isinstance(other, UserMessageDevice):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
