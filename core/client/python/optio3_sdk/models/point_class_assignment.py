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

class PointClassAssignment(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, regex=None, point_class_id=None, case_sensitive=None, comment=None):
        """
        PointClassAssignment - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'regex': 'str',
            'point_class_id': 'str',
            'case_sensitive': 'bool',
            'comment': 'str'
        }

        self.attribute_map = {
            'regex': 'regex',
            'point_class_id': 'pointClassId',
            'case_sensitive': 'caseSensitive',
            'comment': 'comment'
        }

        self._regex = regex
        self._point_class_id = point_class_id
        self._case_sensitive = case_sensitive
        self._comment = comment


    @property
    def regex(self):
        """
        Gets the regex of this PointClassAssignment.

        :return: The regex of this PointClassAssignment.
        :rtype: str
        """
        return self._regex

    @regex.setter
    def regex(self, regex):
        """
        Sets the regex of this PointClassAssignment.

        :param regex: The regex of this PointClassAssignment.
        :type: str
        """

        self._regex = regex

    @property
    def point_class_id(self):
        """
        Gets the point_class_id of this PointClassAssignment.

        :return: The point_class_id of this PointClassAssignment.
        :rtype: str
        """
        return self._point_class_id

    @point_class_id.setter
    def point_class_id(self, point_class_id):
        """
        Sets the point_class_id of this PointClassAssignment.

        :param point_class_id: The point_class_id of this PointClassAssignment.
        :type: str
        """

        self._point_class_id = point_class_id

    @property
    def case_sensitive(self):
        """
        Gets the case_sensitive of this PointClassAssignment.

        :return: The case_sensitive of this PointClassAssignment.
        :rtype: bool
        """
        return self._case_sensitive

    @case_sensitive.setter
    def case_sensitive(self, case_sensitive):
        """
        Sets the case_sensitive of this PointClassAssignment.

        :param case_sensitive: The case_sensitive of this PointClassAssignment.
        :type: bool
        """

        self._case_sensitive = case_sensitive

    @property
    def comment(self):
        """
        Gets the comment of this PointClassAssignment.

        :return: The comment of this PointClassAssignment.
        :rtype: str
        """
        return self._comment

    @comment.setter
    def comment(self, comment):
        """
        Sets the comment of this PointClassAssignment.

        :param comment: The comment of this PointClassAssignment.
        :type: str
        """

        self._comment = comment

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
        if not isinstance(other, PointClassAssignment):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
