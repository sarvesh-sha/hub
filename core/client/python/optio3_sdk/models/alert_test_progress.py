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

class AlertTestProgress(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, status=None, start=None, end=None, current=None, results=None, log_entries=None):
        """
        AlertTestProgress - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'status': 'str',
            'start': 'datetime',
            'end': 'datetime',
            'current': 'datetime',
            'results': 'AlertEngineExecutionStepsOverRange',
            'log_entries': 'list[LogLine]'
        }

        self.attribute_map = {
            'status': 'status',
            'start': 'start',
            'end': 'end',
            'current': 'current',
            'results': 'results',
            'log_entries': 'logEntries'
        }

        self._status = status
        self._start = start
        self._end = end
        self._current = current
        self._results = results
        self._log_entries = log_entries


    @property
    def status(self):
        """
        Gets the status of this AlertTestProgress.

        :return: The status of this AlertTestProgress.
        :rtype: str
        """
        return self._status

    @status.setter
    def status(self, status):
        """
        Sets the status of this AlertTestProgress.

        :param status: The status of this AlertTestProgress.
        :type: str
        """
        allowed_values = ["ACTIVE", "ACTIVE_BUT_CANCELLING", "PAUSED", "PAUSED_BUT_CANCELLING", "WAITING", "WAITING_BUT_CANCELLING", "SLEEPING", "SLEEPING_BUT_CANCELLIN", "EXECUTING", "EXECUTING_BUT_CANCELLING", "CANCELLED", "COMPLETED", "FAILED"]
        if status is not None and status not in allowed_values:
            raise ValueError(
                "Invalid value for `status` ({0}), must be one of {1}"
                .format(status, allowed_values)
            )

        self._status = status

    @property
    def start(self):
        """
        Gets the start of this AlertTestProgress.

        :return: The start of this AlertTestProgress.
        :rtype: datetime
        """
        return self._start

    @start.setter
    def start(self, start):
        """
        Sets the start of this AlertTestProgress.

        :param start: The start of this AlertTestProgress.
        :type: datetime
        """

        self._start = start

    @property
    def end(self):
        """
        Gets the end of this AlertTestProgress.

        :return: The end of this AlertTestProgress.
        :rtype: datetime
        """
        return self._end

    @end.setter
    def end(self, end):
        """
        Sets the end of this AlertTestProgress.

        :param end: The end of this AlertTestProgress.
        :type: datetime
        """

        self._end = end

    @property
    def current(self):
        """
        Gets the current of this AlertTestProgress.

        :return: The current of this AlertTestProgress.
        :rtype: datetime
        """
        return self._current

    @current.setter
    def current(self, current):
        """
        Sets the current of this AlertTestProgress.

        :param current: The current of this AlertTestProgress.
        :type: datetime
        """

        self._current = current

    @property
    def results(self):
        """
        Gets the results of this AlertTestProgress.

        :return: The results of this AlertTestProgress.
        :rtype: AlertEngineExecutionStepsOverRange
        """
        return self._results

    @results.setter
    def results(self, results):
        """
        Sets the results of this AlertTestProgress.

        :param results: The results of this AlertTestProgress.
        :type: AlertEngineExecutionStepsOverRange
        """

        self._results = results

    @property
    def log_entries(self):
        """
        Gets the log_entries of this AlertTestProgress.

        :return: The log_entries of this AlertTestProgress.
        :rtype: list[LogLine]
        """
        return self._log_entries

    @log_entries.setter
    def log_entries(self, log_entries):
        """
        Sets the log_entries of this AlertTestProgress.

        :param log_entries: The log_entries of this AlertTestProgress.
        :type: list[LogLine]
        """

        self._log_entries = log_entries

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
        if not isinstance(other, AlertTestProgress):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
