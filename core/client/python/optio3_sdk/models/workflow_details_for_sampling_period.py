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

class WorkflowDetailsForSamplingPeriod(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, control_points=None, sampling_period=None):
        """
        WorkflowDetailsForSamplingPeriod - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'control_points': 'list[str]',
            'sampling_period': 'int',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'control_points': 'controlPoints',
            'sampling_period': 'samplingPeriod',
            'discriminator___type': '__type'
        }

        self._control_points = control_points
        self._sampling_period = sampling_period

    @property
    def discriminator___type(self):
        return "WorkflowDetailsForSamplingPeriod"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def control_points(self):
        """
        Gets the control_points of this WorkflowDetailsForSamplingPeriod.

        :return: The control_points of this WorkflowDetailsForSamplingPeriod.
        :rtype: list[str]
        """
        return self._control_points

    @control_points.setter
    def control_points(self, control_points):
        """
        Sets the control_points of this WorkflowDetailsForSamplingPeriod.

        :param control_points: The control_points of this WorkflowDetailsForSamplingPeriod.
        :type: list[str]
        """

        self._control_points = control_points

    @property
    def sampling_period(self):
        """
        Gets the sampling_period of this WorkflowDetailsForSamplingPeriod.

        :return: The sampling_period of this WorkflowDetailsForSamplingPeriod.
        :rtype: int
        """
        return self._sampling_period

    @sampling_period.setter
    def sampling_period(self, sampling_period):
        """
        Sets the sampling_period of this WorkflowDetailsForSamplingPeriod.

        :param sampling_period: The sampling_period of this WorkflowDetailsForSamplingPeriod.
        :type: int
        """

        self._sampling_period = sampling_period

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
        if not isinstance(other, WorkflowDetailsForSamplingPeriod):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
