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

class TimeSeriesRangeResponse(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, first_timestamp=None, last_timestamp=None, number_of_samples=None, number_of_missing_samples=None, min_value=None, max_value=None, average_value=None):
        """
        TimeSeriesRangeResponse - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'first_timestamp': 'datetime',
            'last_timestamp': 'datetime',
            'number_of_samples': 'int',
            'number_of_missing_samples': 'int',
            'min_value': 'float',
            'max_value': 'float',
            'average_value': 'float'
        }

        self.attribute_map = {
            'first_timestamp': 'firstTimestamp',
            'last_timestamp': 'lastTimestamp',
            'number_of_samples': 'numberOfSamples',
            'number_of_missing_samples': 'numberOfMissingSamples',
            'min_value': 'minValue',
            'max_value': 'maxValue',
            'average_value': 'averageValue'
        }

        self._first_timestamp = first_timestamp
        self._last_timestamp = last_timestamp
        self._number_of_samples = number_of_samples
        self._number_of_missing_samples = number_of_missing_samples
        self._min_value = min_value
        self._max_value = max_value
        self._average_value = average_value


    @property
    def first_timestamp(self):
        """
        Gets the first_timestamp of this TimeSeriesRangeResponse.

        :return: The first_timestamp of this TimeSeriesRangeResponse.
        :rtype: datetime
        """
        return self._first_timestamp

    @first_timestamp.setter
    def first_timestamp(self, first_timestamp):
        """
        Sets the first_timestamp of this TimeSeriesRangeResponse.

        :param first_timestamp: The first_timestamp of this TimeSeriesRangeResponse.
        :type: datetime
        """

        self._first_timestamp = first_timestamp

    @property
    def last_timestamp(self):
        """
        Gets the last_timestamp of this TimeSeriesRangeResponse.

        :return: The last_timestamp of this TimeSeriesRangeResponse.
        :rtype: datetime
        """
        return self._last_timestamp

    @last_timestamp.setter
    def last_timestamp(self, last_timestamp):
        """
        Sets the last_timestamp of this TimeSeriesRangeResponse.

        :param last_timestamp: The last_timestamp of this TimeSeriesRangeResponse.
        :type: datetime
        """

        self._last_timestamp = last_timestamp

    @property
    def number_of_samples(self):
        """
        Gets the number_of_samples of this TimeSeriesRangeResponse.

        :return: The number_of_samples of this TimeSeriesRangeResponse.
        :rtype: int
        """
        return self._number_of_samples

    @number_of_samples.setter
    def number_of_samples(self, number_of_samples):
        """
        Sets the number_of_samples of this TimeSeriesRangeResponse.

        :param number_of_samples: The number_of_samples of this TimeSeriesRangeResponse.
        :type: int
        """

        self._number_of_samples = number_of_samples

    @property
    def number_of_missing_samples(self):
        """
        Gets the number_of_missing_samples of this TimeSeriesRangeResponse.

        :return: The number_of_missing_samples of this TimeSeriesRangeResponse.
        :rtype: int
        """
        return self._number_of_missing_samples

    @number_of_missing_samples.setter
    def number_of_missing_samples(self, number_of_missing_samples):
        """
        Sets the number_of_missing_samples of this TimeSeriesRangeResponse.

        :param number_of_missing_samples: The number_of_missing_samples of this TimeSeriesRangeResponse.
        :type: int
        """

        self._number_of_missing_samples = number_of_missing_samples

    @property
    def min_value(self):
        """
        Gets the min_value of this TimeSeriesRangeResponse.

        :return: The min_value of this TimeSeriesRangeResponse.
        :rtype: float
        """
        return self._min_value

    @min_value.setter
    def min_value(self, min_value):
        """
        Sets the min_value of this TimeSeriesRangeResponse.

        :param min_value: The min_value of this TimeSeriesRangeResponse.
        :type: float
        """

        self._min_value = min_value

    @property
    def max_value(self):
        """
        Gets the max_value of this TimeSeriesRangeResponse.

        :return: The max_value of this TimeSeriesRangeResponse.
        :rtype: float
        """
        return self._max_value

    @max_value.setter
    def max_value(self, max_value):
        """
        Sets the max_value of this TimeSeriesRangeResponse.

        :param max_value: The max_value of this TimeSeriesRangeResponse.
        :type: float
        """

        self._max_value = max_value

    @property
    def average_value(self):
        """
        Gets the average_value of this TimeSeriesRangeResponse.

        :return: The average_value of this TimeSeriesRangeResponse.
        :rtype: float
        """
        return self._average_value

    @average_value.setter
    def average_value(self, average_value):
        """
        Sets the average_value of this TimeSeriesRangeResponse.

        :param average_value: The average_value of this TimeSeriesRangeResponse.
        :type: float
        """

        self._average_value = average_value

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
        if not isinstance(other, TimeSeriesRangeResponse):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
