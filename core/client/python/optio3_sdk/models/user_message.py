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

class UserMessage(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, sys_id=None, created_on=None, updated_on=None, user=None, subject=None, body=None, flag_new=None, flag_read=None, flag_active=None):
        """
        UserMessage - a model defined in Swagger

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

    @property
    def discriminator___type(self):
        return "UserMessage"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @staticmethod
    def fixup_prototype(data):
        className = data["__type"]
        from .. import models
        switcher = {
        "UserMessageAlert": models.UserMessageAlert,
        "UserMessageDevice": models.UserMessageDevice,
        "UserMessageGeneric": models.UserMessageGeneric,
        "UserMessageReport": models.UserMessageReport,
        "UserMessageRoleManagement": models.UserMessageRoleManagement,
        "UserMessageWorkflow": models.UserMessageWorkflow,
        "UserMessage": UserMessage
        }

        klass = switcher.get(className)
        if klass:
            return klass()
        else:
            raise Exception("Unable to deserialize unknown type")

    @property
    def sys_id(self):
        """
        Gets the sys_id of this UserMessage.

        :return: The sys_id of this UserMessage.
        :rtype: str
        """
        return self._sys_id

    @sys_id.setter
    def sys_id(self, sys_id):
        """
        Sets the sys_id of this UserMessage.

        :param sys_id: The sys_id of this UserMessage.
        :type: str
        """

        self._sys_id = sys_id

    @property
    def created_on(self):
        """
        Gets the created_on of this UserMessage.

        :return: The created_on of this UserMessage.
        :rtype: datetime
        """
        return self._created_on

    @created_on.setter
    def created_on(self, created_on):
        """
        Sets the created_on of this UserMessage.

        :param created_on: The created_on of this UserMessage.
        :type: datetime
        """

        self._created_on = created_on

    @property
    def updated_on(self):
        """
        Gets the updated_on of this UserMessage.

        :return: The updated_on of this UserMessage.
        :rtype: datetime
        """
        return self._updated_on

    @updated_on.setter
    def updated_on(self, updated_on):
        """
        Sets the updated_on of this UserMessage.

        :param updated_on: The updated_on of this UserMessage.
        :type: datetime
        """

        self._updated_on = updated_on

    @property
    def user(self):
        """
        Gets the user of this UserMessage.

        :return: The user of this UserMessage.
        :rtype: RecordIdentity
        """
        return self._user

    @user.setter
    def user(self, user):
        """
        Sets the user of this UserMessage.

        :param user: The user of this UserMessage.
        :type: RecordIdentity
        """

        self._user = user

    @property
    def subject(self):
        """
        Gets the subject of this UserMessage.

        :return: The subject of this UserMessage.
        :rtype: str
        """
        return self._subject

    @subject.setter
    def subject(self, subject):
        """
        Sets the subject of this UserMessage.

        :param subject: The subject of this UserMessage.
        :type: str
        """

        self._subject = subject

    @property
    def body(self):
        """
        Gets the body of this UserMessage.

        :return: The body of this UserMessage.
        :rtype: str
        """
        return self._body

    @body.setter
    def body(self, body):
        """
        Sets the body of this UserMessage.

        :param body: The body of this UserMessage.
        :type: str
        """

        self._body = body

    @property
    def flag_new(self):
        """
        Gets the flag_new of this UserMessage.

        :return: The flag_new of this UserMessage.
        :rtype: bool
        """
        return self._flag_new

    @flag_new.setter
    def flag_new(self, flag_new):
        """
        Sets the flag_new of this UserMessage.

        :param flag_new: The flag_new of this UserMessage.
        :type: bool
        """

        self._flag_new = flag_new

    @property
    def flag_read(self):
        """
        Gets the flag_read of this UserMessage.

        :return: The flag_read of this UserMessage.
        :rtype: bool
        """
        return self._flag_read

    @flag_read.setter
    def flag_read(self, flag_read):
        """
        Sets the flag_read of this UserMessage.

        :param flag_read: The flag_read of this UserMessage.
        :type: bool
        """

        self._flag_read = flag_read

    @property
    def flag_active(self):
        """
        Gets the flag_active of this UserMessage.

        :return: The flag_active of this UserMessage.
        :rtype: bool
        """
        return self._flag_active

    @flag_active.setter
    def flag_active(self, flag_active):
        """
        Sets the flag_active of this UserMessage.

        :param flag_active: The flag_active of this UserMessage.
        :type: bool
        """

        self._flag_active = flag_active

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
        if not isinstance(other, UserMessage):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
