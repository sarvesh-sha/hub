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

class TimeSeriesSinglePropertyRequest(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, max_samples=None, max_gap_between_samples=None, skip_missing=None, range_start=None, range_end=None, spec=None, delta_encode=None):
        """
        TimeSeriesSinglePropertyRequest - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'max_samples': 'int',
            'max_gap_between_samples': 'int',
            'skip_missing': 'bool',
            'range_start': 'datetime',
            'range_end': 'datetime',
            'spec': 'TimeSeriesPropertyRequest',
            'delta_encode': 'bool'
        }

        self.attribute_map = {
            'max_samples': 'maxSamples',
            'max_gap_between_samples': 'maxGapBetweenSamples',
            'skip_missing': 'skipMissing',
            'range_start': 'rangeStart',
            'range_end': 'rangeEnd',
            'spec': 'spec',
            'delta_encode': 'deltaEncode'
        }

        self._max_samples = max_samples
        self._max_gap_between_samples = max_gap_between_samples
        self._skip_missing = skip_missing
        self._range_start = range_start
        self._range_end = range_end
        self._spec = spec
        self._delta_encode = delta_encode


    @property
    def max_samples(self):
        """
        Gets the max_samples of this TimeSeriesSinglePropertyRequest.

        :return: The max_samples of this TimeSeriesSinglePropertyRequest.
        :rtype: int
        """
        return self._max_samples

    @max_samples.setter
    def max_samples(self, max_samples):
        """
        Sets the max_samples of this TimeSeriesSinglePropertyRequest.

        :param max_samples: The max_samples of this TimeSeriesSinglePropertyRequest.
        :type: int
        """

        self._max_samples = max_samples

    @property
    def max_gap_between_samples(self):
        """
        Gets the max_gap_between_samples of this TimeSeriesSinglePropertyRequest.

        :return: The max_gap_between_samples of this TimeSeriesSinglePropertyRequest.
        :rtype: int
        """
        return self._max_gap_between_samples

    @max_gap_between_samples.setter
    def max_gap_between_samples(self, max_gap_between_samples):
        """
        Sets the max_gap_between_samples of this TimeSeriesSinglePropertyRequest.

        :param max_gap_between_samples: The max_gap_between_samples of this TimeSeriesSinglePropertyRequest.
        :type: int
        """

        self._max_gap_between_samples = max_gap_between_samples

    @property
    def skip_missing(self):
        """
        Gets the skip_missing of this TimeSeriesSinglePropertyRequest.

        :return: The skip_missing of this TimeSeriesSinglePropertyRequest.
        :rtype: bool
        """
        return self._skip_missing

    @skip_missing.setter
    def skip_missing(self, skip_missing):
        """
        Sets the skip_missing of this TimeSeriesSinglePropertyRequest.

        :param skip_missing: The skip_missing of this TimeSeriesSinglePropertyRequest.
        :type: bool
        """

        self._skip_missing = skip_missing

    @property
    def range_start(self):
        """
        Gets the range_start of this TimeSeriesSinglePropertyRequest.

        :return: The range_start of this TimeSeriesSinglePropertyRequest.
        :rtype: datetime
        """
        return self._range_start

    @range_start.setter
    def range_start(self, range_start):
        """
        Sets the range_start of this TimeSeriesSinglePropertyRequest.

        :param range_start: The range_start of this TimeSeriesSinglePropertyRequest.
        :type: datetime
        """

        self._range_start = range_start

    @property
    def range_end(self):
        """
        Gets the range_end of this TimeSeriesSinglePropertyRequest.

        :return: The range_end of this TimeSeriesSinglePropertyRequest.
        :rtype: datetime
        """
        return self._range_end

    @range_end.setter
    def range_end(self, range_end):
        """
        Sets the range_end of this TimeSeriesSinglePropertyRequest.

        :param range_end: The range_end of this TimeSeriesSinglePropertyRequest.
        :type: datetime
        """

        self._range_end = range_end

    @property
    def spec(self):
        """
        Gets the spec of this TimeSeriesSinglePropertyRequest.

        :return: The spec of this TimeSeriesSinglePropertyRequest.
        :rtype: TimeSeriesPropertyRequest
        """
        return self._spec

    @spec.setter
    def spec(self, spec):
        """
        Sets the spec of this TimeSeriesSinglePropertyRequest.

        :param spec: The spec of this TimeSeriesSinglePropertyRequest.
        :type: TimeSeriesPropertyRequest
        """

        self._spec = spec

    @property
    def delta_encode(self):
        """
        Gets the delta_encode of this TimeSeriesSinglePropertyRequest.

        :return: The delta_encode of this TimeSeriesSinglePropertyRequest.
        :rtype: bool
        """
        return self._delta_encode

    @delta_encode.setter
    def delta_encode(self, delta_encode):
        """
        Sets the delta_encode of this TimeSeriesSinglePropertyRequest.

        :param delta_encode: The delta_encode of this TimeSeriesSinglePropertyRequest.
        :type: bool
        """

        self._delta_encode = delta_encode

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
        if not isinstance(other, TimeSeriesSinglePropertyRequest):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
