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

class BookmarkConfiguration(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, created_on=None, id=None, parent_id=None, record_id=None, parent_record_id=None, name=None, description=None, type=None, url=None, state_serialized=None):
        """
        BookmarkConfiguration - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'created_on': 'datetime',
            'id': 'str',
            'parent_id': 'str',
            'record_id': 'str',
            'parent_record_id': 'str',
            'name': 'str',
            'description': 'str',
            'type': 'str',
            'url': 'str',
            'state_serialized': 'ViewStateSerialized'
        }

        self.attribute_map = {
            'created_on': 'createdOn',
            'id': 'id',
            'parent_id': 'parentID',
            'record_id': 'recordID',
            'parent_record_id': 'parentRecordID',
            'name': 'name',
            'description': 'description',
            'type': 'type',
            'url': 'url',
            'state_serialized': 'stateSerialized'
        }

        self._created_on = created_on
        self._id = id
        self._parent_id = parent_id
        self._record_id = record_id
        self._parent_record_id = parent_record_id
        self._name = name
        self._description = description
        self._type = type
        self._url = url
        self._state_serialized = state_serialized


    @property
    def created_on(self):
        """
        Gets the created_on of this BookmarkConfiguration.

        :return: The created_on of this BookmarkConfiguration.
        :rtype: datetime
        """
        return self._created_on

    @created_on.setter
    def created_on(self, created_on):
        """
        Sets the created_on of this BookmarkConfiguration.

        :param created_on: The created_on of this BookmarkConfiguration.
        :type: datetime
        """

        self._created_on = created_on

    @property
    def id(self):
        """
        Gets the id of this BookmarkConfiguration.

        :return: The id of this BookmarkConfiguration.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this BookmarkConfiguration.

        :param id: The id of this BookmarkConfiguration.
        :type: str
        """

        self._id = id

    @property
    def parent_id(self):
        """
        Gets the parent_id of this BookmarkConfiguration.

        :return: The parent_id of this BookmarkConfiguration.
        :rtype: str
        """
        return self._parent_id

    @parent_id.setter
    def parent_id(self, parent_id):
        """
        Sets the parent_id of this BookmarkConfiguration.

        :param parent_id: The parent_id of this BookmarkConfiguration.
        :type: str
        """

        self._parent_id = parent_id

    @property
    def record_id(self):
        """
        Gets the record_id of this BookmarkConfiguration.

        :return: The record_id of this BookmarkConfiguration.
        :rtype: str
        """
        return self._record_id

    @record_id.setter
    def record_id(self, record_id):
        """
        Sets the record_id of this BookmarkConfiguration.

        :param record_id: The record_id of this BookmarkConfiguration.
        :type: str
        """

        self._record_id = record_id

    @property
    def parent_record_id(self):
        """
        Gets the parent_record_id of this BookmarkConfiguration.

        :return: The parent_record_id of this BookmarkConfiguration.
        :rtype: str
        """
        return self._parent_record_id

    @parent_record_id.setter
    def parent_record_id(self, parent_record_id):
        """
        Sets the parent_record_id of this BookmarkConfiguration.

        :param parent_record_id: The parent_record_id of this BookmarkConfiguration.
        :type: str
        """

        self._parent_record_id = parent_record_id

    @property
    def name(self):
        """
        Gets the name of this BookmarkConfiguration.

        :return: The name of this BookmarkConfiguration.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this BookmarkConfiguration.

        :param name: The name of this BookmarkConfiguration.
        :type: str
        """

        self._name = name

    @property
    def description(self):
        """
        Gets the description of this BookmarkConfiguration.

        :return: The description of this BookmarkConfiguration.
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """
        Sets the description of this BookmarkConfiguration.

        :param description: The description of this BookmarkConfiguration.
        :type: str
        """

        self._description = description

    @property
    def type(self):
        """
        Gets the type of this BookmarkConfiguration.

        :return: The type of this BookmarkConfiguration.
        :rtype: str
        """
        return self._type

    @type.setter
    def type(self, type):
        """
        Sets the type of this BookmarkConfiguration.

        :param type: The type of this BookmarkConfiguration.
        :type: str
        """
        allowed_values = ["ALERT", "DEVICE", "EQUIPMENT", "DEVICE_ELEMENT", "DATA_EXPLORER"]
        if type is not None and type not in allowed_values:
            raise ValueError(
                "Invalid value for `type` ({0}), must be one of {1}"
                .format(type, allowed_values)
            )

        self._type = type

    @property
    def url(self):
        """
        Gets the url of this BookmarkConfiguration.

        :return: The url of this BookmarkConfiguration.
        :rtype: str
        """
        return self._url

    @url.setter
    def url(self, url):
        """
        Sets the url of this BookmarkConfiguration.

        :param url: The url of this BookmarkConfiguration.
        :type: str
        """

        self._url = url

    @property
    def state_serialized(self):
        """
        Gets the state_serialized of this BookmarkConfiguration.

        :return: The state_serialized of this BookmarkConfiguration.
        :rtype: ViewStateSerialized
        """
        return self._state_serialized

    @state_serialized.setter
    def state_serialized(self, state_serialized):
        """
        Sets the state_serialized of this BookmarkConfiguration.

        :param state_serialized: The state_serialized of this BookmarkConfiguration.
        :type: ViewStateSerialized
        """

        self._state_serialized = state_serialized

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
        if not isinstance(other, BookmarkConfiguration):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

