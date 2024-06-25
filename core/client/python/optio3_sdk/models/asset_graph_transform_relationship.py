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

class AssetGraphTransformRelationship(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, input_id=None, output_id=None, relationship=None):
        """
        AssetGraphTransformRelationship - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'input_id': 'str',
            'output_id': 'str',
            'relationship': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'input_id': 'inputId',
            'output_id': 'outputId',
            'relationship': 'relationship',
            'discriminator___type': '__type'
        }

        self._input_id = input_id
        self._output_id = output_id
        self._relationship = relationship

    @property
    def discriminator___type(self):
        return "AssetGraphTransformRelationship"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def input_id(self):
        """
        Gets the input_id of this AssetGraphTransformRelationship.

        :return: The input_id of this AssetGraphTransformRelationship.
        :rtype: str
        """
        return self._input_id

    @input_id.setter
    def input_id(self, input_id):
        """
        Sets the input_id of this AssetGraphTransformRelationship.

        :param input_id: The input_id of this AssetGraphTransformRelationship.
        :type: str
        """

        self._input_id = input_id

    @property
    def output_id(self):
        """
        Gets the output_id of this AssetGraphTransformRelationship.

        :return: The output_id of this AssetGraphTransformRelationship.
        :rtype: str
        """
        return self._output_id

    @output_id.setter
    def output_id(self, output_id):
        """
        Sets the output_id of this AssetGraphTransformRelationship.

        :param output_id: The output_id of this AssetGraphTransformRelationship.
        :type: str
        """

        self._output_id = output_id

    @property
    def relationship(self):
        """
        Gets the relationship of this AssetGraphTransformRelationship.

        :return: The relationship of this AssetGraphTransformRelationship.
        :rtype: str
        """
        return self._relationship

    @relationship.setter
    def relationship(self, relationship):
        """
        Sets the relationship of this AssetGraphTransformRelationship.

        :param relationship: The relationship of this AssetGraphTransformRelationship.
        :type: str
        """
        allowed_values = ["structural", "controls"]
        if relationship is not None and relationship not in allowed_values:
            raise ValueError(
                "Invalid value for `relationship` ({0}), must be one of {1}"
                .format(relationship, allowed_values)
            )

        self._relationship = relationship

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
        if not isinstance(other, AssetGraphTransformRelationship):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
