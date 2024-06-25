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

class AlertEngineExecutionStepSetAlertSeverity(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, entering_block_id=None, leaving_block_id=None, assignment=None, not_implemented=None, failure=None, timestamp=None, record=None, severity=None):
        """
        AlertEngineExecutionStepSetAlertSeverity - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'entering_block_id': 'str',
            'leaving_block_id': 'str',
            'assignment': 'EngineExecutionAssignment',
            'not_implemented': 'str',
            'failure': 'str',
            'timestamp': 'datetime',
            'record': 'RecordIdentity',
            'severity': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'entering_block_id': 'enteringBlockId',
            'leaving_block_id': 'leavingBlockId',
            'assignment': 'assignment',
            'not_implemented': 'notImplemented',
            'failure': 'failure',
            'timestamp': 'timestamp',
            'record': 'record',
            'severity': 'severity',
            'discriminator___type': '__type'
        }

        self._entering_block_id = entering_block_id
        self._leaving_block_id = leaving_block_id
        self._assignment = assignment
        self._not_implemented = not_implemented
        self._failure = failure
        self._timestamp = timestamp
        self._record = record
        self._severity = severity

    @property
    def discriminator___type(self):
        return "AlertEngineExecutionStepSetAlertSeverity"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def entering_block_id(self):
        """
        Gets the entering_block_id of this AlertEngineExecutionStepSetAlertSeverity.

        :return: The entering_block_id of this AlertEngineExecutionStepSetAlertSeverity.
        :rtype: str
        """
        return self._entering_block_id

    @entering_block_id.setter
    def entering_block_id(self, entering_block_id):
        """
        Sets the entering_block_id of this AlertEngineExecutionStepSetAlertSeverity.

        :param entering_block_id: The entering_block_id of this AlertEngineExecutionStepSetAlertSeverity.
        :type: str
        """

        self._entering_block_id = entering_block_id

    @property
    def leaving_block_id(self):
        """
        Gets the leaving_block_id of this AlertEngineExecutionStepSetAlertSeverity.

        :return: The leaving_block_id of this AlertEngineExecutionStepSetAlertSeverity.
        :rtype: str
        """
        return self._leaving_block_id

    @leaving_block_id.setter
    def leaving_block_id(self, leaving_block_id):
        """
        Sets the leaving_block_id of this AlertEngineExecutionStepSetAlertSeverity.

        :param leaving_block_id: The leaving_block_id of this AlertEngineExecutionStepSetAlertSeverity.
        :type: str
        """

        self._leaving_block_id = leaving_block_id

    @property
    def assignment(self):
        """
        Gets the assignment of this AlertEngineExecutionStepSetAlertSeverity.

        :return: The assignment of this AlertEngineExecutionStepSetAlertSeverity.
        :rtype: EngineExecutionAssignment
        """
        return self._assignment

    @assignment.setter
    def assignment(self, assignment):
        """
        Sets the assignment of this AlertEngineExecutionStepSetAlertSeverity.

        :param assignment: The assignment of this AlertEngineExecutionStepSetAlertSeverity.
        :type: EngineExecutionAssignment
        """

        self._assignment = assignment

    @property
    def not_implemented(self):
        """
        Gets the not_implemented of this AlertEngineExecutionStepSetAlertSeverity.

        :return: The not_implemented of this AlertEngineExecutionStepSetAlertSeverity.
        :rtype: str
        """
        return self._not_implemented

    @not_implemented.setter
    def not_implemented(self, not_implemented):
        """
        Sets the not_implemented of this AlertEngineExecutionStepSetAlertSeverity.

        :param not_implemented: The not_implemented of this AlertEngineExecutionStepSetAlertSeverity.
        :type: str
        """

        self._not_implemented = not_implemented

    @property
    def failure(self):
        """
        Gets the failure of this AlertEngineExecutionStepSetAlertSeverity.

        :return: The failure of this AlertEngineExecutionStepSetAlertSeverity.
        :rtype: str
        """
        return self._failure

    @failure.setter
    def failure(self, failure):
        """
        Sets the failure of this AlertEngineExecutionStepSetAlertSeverity.

        :param failure: The failure of this AlertEngineExecutionStepSetAlertSeverity.
        :type: str
        """

        self._failure = failure

    @property
    def timestamp(self):
        """
        Gets the timestamp of this AlertEngineExecutionStepSetAlertSeverity.

        :return: The timestamp of this AlertEngineExecutionStepSetAlertSeverity.
        :rtype: datetime
        """
        return self._timestamp

    @timestamp.setter
    def timestamp(self, timestamp):
        """
        Sets the timestamp of this AlertEngineExecutionStepSetAlertSeverity.

        :param timestamp: The timestamp of this AlertEngineExecutionStepSetAlertSeverity.
        :type: datetime
        """

        self._timestamp = timestamp

    @property
    def record(self):
        """
        Gets the record of this AlertEngineExecutionStepSetAlertSeverity.

        :return: The record of this AlertEngineExecutionStepSetAlertSeverity.
        :rtype: RecordIdentity
        """
        return self._record

    @record.setter
    def record(self, record):
        """
        Sets the record of this AlertEngineExecutionStepSetAlertSeverity.

        :param record: The record of this AlertEngineExecutionStepSetAlertSeverity.
        :type: RecordIdentity
        """

        self._record = record

    @property
    def severity(self):
        """
        Gets the severity of this AlertEngineExecutionStepSetAlertSeverity.

        :return: The severity of this AlertEngineExecutionStepSetAlertSeverity.
        :rtype: str
        """
        return self._severity

    @severity.setter
    def severity(self, severity):
        """
        Sets the severity of this AlertEngineExecutionStepSetAlertSeverity.

        :param severity: The severity of this AlertEngineExecutionStepSetAlertSeverity.
        :type: str
        """
        allowed_values = ["CRITICAL", "SIGNIFICANT", "NORMAL", "LOW"]
        if severity is not None and severity not in allowed_values:
            raise ValueError(
                "Invalid value for `severity` ({0}), must be one of {1}"
                .format(severity, allowed_values)
            )

        self._severity = severity

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
        if not isinstance(other, AlertEngineExecutionStepSetAlertSeverity):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
