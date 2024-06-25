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

class TimeSeriesSinglePropertyResponse(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, timestamps=None, results=None, delta_encoded=None):
        """
        TimeSeriesSinglePropertyResponse - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'timestamps': 'list[float]',
            'results': 'TimeSeriesPropertyResponse',
            'delta_encoded': 'bool'
        }

        self.attribute_map = {
            'timestamps': 'timestamps',
            'results': 'results',
            'delta_encoded': 'deltaEncoded'
        }

        self._timestamps = timestamps
        self._results = results
        self._delta_encoded = delta_encoded


    @property
    def timestamps(self):
        """
        Gets the timestamps of this TimeSeriesSinglePropertyResponse.

        :return: The timestamps of this TimeSeriesSinglePropertyResponse.
        :rtype: list[float]
        """
        return self._timestamps

    @timestamps.setter
    def timestamps(self, timestamps):
        """
        Sets the timestamps of this TimeSeriesSinglePropertyResponse.

        :param timestamps: The timestamps of this TimeSeriesSinglePropertyResponse.
        :type: list[float]
        """

        self._timestamps = timestamps

    @property
    def results(self):
        """
        Gets the results of this TimeSeriesSinglePropertyResponse.

        :return: The results of this TimeSeriesSinglePropertyResponse.
        :rtype: TimeSeriesPropertyResponse
        """
        return self._results

    @results.setter
    def results(self, results):
        """
        Sets the results of this TimeSeriesSinglePropertyResponse.

        :param results: The results of this TimeSeriesSinglePropertyResponse.
        :type: TimeSeriesPropertyResponse
        """

        self._results = results

    @property
    def delta_encoded(self):
        """
        Gets the delta_encoded of this TimeSeriesSinglePropertyResponse.

        :return: The delta_encoded of this TimeSeriesSinglePropertyResponse.
        :rtype: bool
        """
        return self._delta_encoded

    @delta_encoded.setter
    def delta_encoded(self, delta_encoded):
        """
        Sets the delta_encoded of this TimeSeriesSinglePropertyResponse.

        :param delta_encoded: The delta_encoded of this TimeSeriesSinglePropertyResponse.
        :type: bool
        """

        self._delta_encoded = delta_encoded

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
        if not isinstance(other, TimeSeriesSinglePropertyResponse):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
