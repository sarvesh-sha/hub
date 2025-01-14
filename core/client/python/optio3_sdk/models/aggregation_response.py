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

class AggregationResponse(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, records=None, results_per_range=None):
        """
        AggregationResponse - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'records': 'list[list[str]]',
            'results_per_range': 'list[list[float]]'
        }

        self.attribute_map = {
            'records': 'records',
            'results_per_range': 'resultsPerRange'
        }

        self._records = records
        self._results_per_range = results_per_range


    @property
    def records(self):
        """
        Gets the records of this AggregationResponse.

        :return: The records of this AggregationResponse.
        :rtype: list[list[str]]
        """
        return self._records

    @records.setter
    def records(self, records):
        """
        Sets the records of this AggregationResponse.

        :param records: The records of this AggregationResponse.
        :type: list[list[str]]
        """

        self._records = records

    @property
    def results_per_range(self):
        """
        Gets the results_per_range of this AggregationResponse.

        :return: The results_per_range of this AggregationResponse.
        :rtype: list[list[float]]
        """
        return self._results_per_range

    @results_per_range.setter
    def results_per_range(self, results_per_range):
        """
        Sets the results_per_range of this AggregationResponse.

        :param results_per_range: The results_per_range of this AggregationResponse.
        :type: list[list[float]]
        """

        self._results_per_range = results_per_range

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
        if not isinstance(other, AggregationResponse):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
