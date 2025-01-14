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

class AzureDigitalTwinSyncProgress(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, status=None, report=None, networks_to_process=None, devices_processed=None, elements_processed=None, twins_found=None, relationships_found=None, twins_processed=None, relationships_processed=None):
        """
        AzureDigitalTwinSyncProgress - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'status': 'str',
            'report': 'list[str]',
            'networks_to_process': 'int',
            'devices_processed': 'int',
            'elements_processed': 'int',
            'twins_found': 'int',
            'relationships_found': 'int',
            'twins_processed': 'int',
            'relationships_processed': 'int'
        }

        self.attribute_map = {
            'status': 'status',
            'report': 'report',
            'networks_to_process': 'networksToProcess',
            'devices_processed': 'devicesProcessed',
            'elements_processed': 'elementsProcessed',
            'twins_found': 'twinsFound',
            'relationships_found': 'relationshipsFound',
            'twins_processed': 'twinsProcessed',
            'relationships_processed': 'relationshipsProcessed'
        }

        self._status = status
        self._report = report
        self._networks_to_process = networks_to_process
        self._devices_processed = devices_processed
        self._elements_processed = elements_processed
        self._twins_found = twins_found
        self._relationships_found = relationships_found
        self._twins_processed = twins_processed
        self._relationships_processed = relationships_processed


    @property
    def status(self):
        """
        Gets the status of this AzureDigitalTwinSyncProgress.

        :return: The status of this AzureDigitalTwinSyncProgress.
        :rtype: str
        """
        return self._status

    @status.setter
    def status(self, status):
        """
        Sets the status of this AzureDigitalTwinSyncProgress.

        :param status: The status of this AzureDigitalTwinSyncProgress.
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
    def report(self):
        """
        Gets the report of this AzureDigitalTwinSyncProgress.

        :return: The report of this AzureDigitalTwinSyncProgress.
        :rtype: list[str]
        """
        return self._report

    @report.setter
    def report(self, report):
        """
        Sets the report of this AzureDigitalTwinSyncProgress.

        :param report: The report of this AzureDigitalTwinSyncProgress.
        :type: list[str]
        """

        self._report = report

    @property
    def networks_to_process(self):
        """
        Gets the networks_to_process of this AzureDigitalTwinSyncProgress.

        :return: The networks_to_process of this AzureDigitalTwinSyncProgress.
        :rtype: int
        """
        return self._networks_to_process

    @networks_to_process.setter
    def networks_to_process(self, networks_to_process):
        """
        Sets the networks_to_process of this AzureDigitalTwinSyncProgress.

        :param networks_to_process: The networks_to_process of this AzureDigitalTwinSyncProgress.
        :type: int
        """

        self._networks_to_process = networks_to_process

    @property
    def devices_processed(self):
        """
        Gets the devices_processed of this AzureDigitalTwinSyncProgress.

        :return: The devices_processed of this AzureDigitalTwinSyncProgress.
        :rtype: int
        """
        return self._devices_processed

    @devices_processed.setter
    def devices_processed(self, devices_processed):
        """
        Sets the devices_processed of this AzureDigitalTwinSyncProgress.

        :param devices_processed: The devices_processed of this AzureDigitalTwinSyncProgress.
        :type: int
        """

        self._devices_processed = devices_processed

    @property
    def elements_processed(self):
        """
        Gets the elements_processed of this AzureDigitalTwinSyncProgress.

        :return: The elements_processed of this AzureDigitalTwinSyncProgress.
        :rtype: int
        """
        return self._elements_processed

    @elements_processed.setter
    def elements_processed(self, elements_processed):
        """
        Sets the elements_processed of this AzureDigitalTwinSyncProgress.

        :param elements_processed: The elements_processed of this AzureDigitalTwinSyncProgress.
        :type: int
        """

        self._elements_processed = elements_processed

    @property
    def twins_found(self):
        """
        Gets the twins_found of this AzureDigitalTwinSyncProgress.

        :return: The twins_found of this AzureDigitalTwinSyncProgress.
        :rtype: int
        """
        return self._twins_found

    @twins_found.setter
    def twins_found(self, twins_found):
        """
        Sets the twins_found of this AzureDigitalTwinSyncProgress.

        :param twins_found: The twins_found of this AzureDigitalTwinSyncProgress.
        :type: int
        """

        self._twins_found = twins_found

    @property
    def relationships_found(self):
        """
        Gets the relationships_found of this AzureDigitalTwinSyncProgress.

        :return: The relationships_found of this AzureDigitalTwinSyncProgress.
        :rtype: int
        """
        return self._relationships_found

    @relationships_found.setter
    def relationships_found(self, relationships_found):
        """
        Sets the relationships_found of this AzureDigitalTwinSyncProgress.

        :param relationships_found: The relationships_found of this AzureDigitalTwinSyncProgress.
        :type: int
        """

        self._relationships_found = relationships_found

    @property
    def twins_processed(self):
        """
        Gets the twins_processed of this AzureDigitalTwinSyncProgress.

        :return: The twins_processed of this AzureDigitalTwinSyncProgress.
        :rtype: int
        """
        return self._twins_processed

    @twins_processed.setter
    def twins_processed(self, twins_processed):
        """
        Sets the twins_processed of this AzureDigitalTwinSyncProgress.

        :param twins_processed: The twins_processed of this AzureDigitalTwinSyncProgress.
        :type: int
        """

        self._twins_processed = twins_processed

    @property
    def relationships_processed(self):
        """
        Gets the relationships_processed of this AzureDigitalTwinSyncProgress.

        :return: The relationships_processed of this AzureDigitalTwinSyncProgress.
        :rtype: int
        """
        return self._relationships_processed

    @relationships_processed.setter
    def relationships_processed(self, relationships_processed):
        """
        Sets the relationships_processed of this AzureDigitalTwinSyncProgress.

        :param relationships_processed: The relationships_processed of this AzureDigitalTwinSyncProgress.
        :type: int
        """

        self._relationships_processed = relationships_processed

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
        if not isinstance(other, AzureDigitalTwinSyncProgress):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

