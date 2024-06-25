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

class AlertFilterRequest(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, version=None, start_offset=None, max_results=None, asset_ids=None, location_ids=None, location_inclusive=None, like_device_manufacturer_name=None, like_device_product_name=None, like_device_model_name=None, sort_by=None, evaluate_updated_on=None, range_start=None, range_end=None, alert_status_ids=None, alert_type_ids=None, alert_severity_ids=None, alert_rules=None):
        """
        AlertFilterRequest - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'version': 'int',
            'start_offset': 'int',
            'max_results': 'int',
            'asset_ids': 'list[str]',
            'location_ids': 'list[str]',
            'location_inclusive': 'bool',
            'like_device_manufacturer_name': 'str',
            'like_device_product_name': 'str',
            'like_device_model_name': 'str',
            'sort_by': 'list[SortCriteria]',
            'evaluate_updated_on': 'bool',
            'range_start': 'datetime',
            'range_end': 'datetime',
            'alert_status_ids': 'list[str]',
            'alert_type_ids': 'list[str]',
            'alert_severity_ids': 'list[str]',
            'alert_rules': 'list[RecordIdentity]',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'version': 'version',
            'start_offset': 'startOffset',
            'max_results': 'maxResults',
            'asset_ids': 'assetIDs',
            'location_ids': 'locationIDs',
            'location_inclusive': 'locationInclusive',
            'like_device_manufacturer_name': 'likeDeviceManufacturerName',
            'like_device_product_name': 'likeDeviceProductName',
            'like_device_model_name': 'likeDeviceModelName',
            'sort_by': 'sortBy',
            'evaluate_updated_on': 'evaluateUpdatedOn',
            'range_start': 'rangeStart',
            'range_end': 'rangeEnd',
            'alert_status_ids': 'alertStatusIDs',
            'alert_type_ids': 'alertTypeIDs',
            'alert_severity_ids': 'alertSeverityIDs',
            'alert_rules': 'alertRules',
            'discriminator___type': '__type'
        }

        self._version = version
        self._start_offset = start_offset
        self._max_results = max_results
        self._asset_ids = asset_ids
        self._location_ids = location_ids
        self._location_inclusive = location_inclusive
        self._like_device_manufacturer_name = like_device_manufacturer_name
        self._like_device_product_name = like_device_product_name
        self._like_device_model_name = like_device_model_name
        self._sort_by = sort_by
        self._evaluate_updated_on = evaluate_updated_on
        self._range_start = range_start
        self._range_end = range_end
        self._alert_status_ids = alert_status_ids
        self._alert_type_ids = alert_type_ids
        self._alert_severity_ids = alert_severity_ids
        self._alert_rules = alert_rules

    @property
    def discriminator___type(self):
        return "AlertFilterRequest"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def version(self):
        """
        Gets the version of this AlertFilterRequest.

        :return: The version of this AlertFilterRequest.
        :rtype: int
        """
        return self._version

    @version.setter
    def version(self, version):
        """
        Sets the version of this AlertFilterRequest.

        :param version: The version of this AlertFilterRequest.
        :type: int
        """

        self._version = version

    @property
    def start_offset(self):
        """
        Gets the start_offset of this AlertFilterRequest.

        :return: The start_offset of this AlertFilterRequest.
        :rtype: int
        """
        return self._start_offset

    @start_offset.setter
    def start_offset(self, start_offset):
        """
        Sets the start_offset of this AlertFilterRequest.

        :param start_offset: The start_offset of this AlertFilterRequest.
        :type: int
        """

        self._start_offset = start_offset

    @property
    def max_results(self):
        """
        Gets the max_results of this AlertFilterRequest.

        :return: The max_results of this AlertFilterRequest.
        :rtype: int
        """
        return self._max_results

    @max_results.setter
    def max_results(self, max_results):
        """
        Sets the max_results of this AlertFilterRequest.

        :param max_results: The max_results of this AlertFilterRequest.
        :type: int
        """

        self._max_results = max_results

    @property
    def asset_ids(self):
        """
        Gets the asset_ids of this AlertFilterRequest.

        :return: The asset_ids of this AlertFilterRequest.
        :rtype: list[str]
        """
        return self._asset_ids

    @asset_ids.setter
    def asset_ids(self, asset_ids):
        """
        Sets the asset_ids of this AlertFilterRequest.

        :param asset_ids: The asset_ids of this AlertFilterRequest.
        :type: list[str]
        """

        self._asset_ids = asset_ids

    @property
    def location_ids(self):
        """
        Gets the location_ids of this AlertFilterRequest.

        :return: The location_ids of this AlertFilterRequest.
        :rtype: list[str]
        """
        return self._location_ids

    @location_ids.setter
    def location_ids(self, location_ids):
        """
        Sets the location_ids of this AlertFilterRequest.

        :param location_ids: The location_ids of this AlertFilterRequest.
        :type: list[str]
        """

        self._location_ids = location_ids

    @property
    def location_inclusive(self):
        """
        Gets the location_inclusive of this AlertFilterRequest.

        :return: The location_inclusive of this AlertFilterRequest.
        :rtype: bool
        """
        return self._location_inclusive

    @location_inclusive.setter
    def location_inclusive(self, location_inclusive):
        """
        Sets the location_inclusive of this AlertFilterRequest.

        :param location_inclusive: The location_inclusive of this AlertFilterRequest.
        :type: bool
        """

        self._location_inclusive = location_inclusive

    @property
    def like_device_manufacturer_name(self):
        """
        Gets the like_device_manufacturer_name of this AlertFilterRequest.

        :return: The like_device_manufacturer_name of this AlertFilterRequest.
        :rtype: str
        """
        return self._like_device_manufacturer_name

    @like_device_manufacturer_name.setter
    def like_device_manufacturer_name(self, like_device_manufacturer_name):
        """
        Sets the like_device_manufacturer_name of this AlertFilterRequest.

        :param like_device_manufacturer_name: The like_device_manufacturer_name of this AlertFilterRequest.
        :type: str
        """

        self._like_device_manufacturer_name = like_device_manufacturer_name

    @property
    def like_device_product_name(self):
        """
        Gets the like_device_product_name of this AlertFilterRequest.

        :return: The like_device_product_name of this AlertFilterRequest.
        :rtype: str
        """
        return self._like_device_product_name

    @like_device_product_name.setter
    def like_device_product_name(self, like_device_product_name):
        """
        Sets the like_device_product_name of this AlertFilterRequest.

        :param like_device_product_name: The like_device_product_name of this AlertFilterRequest.
        :type: str
        """

        self._like_device_product_name = like_device_product_name

    @property
    def like_device_model_name(self):
        """
        Gets the like_device_model_name of this AlertFilterRequest.

        :return: The like_device_model_name of this AlertFilterRequest.
        :rtype: str
        """
        return self._like_device_model_name

    @like_device_model_name.setter
    def like_device_model_name(self, like_device_model_name):
        """
        Sets the like_device_model_name of this AlertFilterRequest.

        :param like_device_model_name: The like_device_model_name of this AlertFilterRequest.
        :type: str
        """

        self._like_device_model_name = like_device_model_name

    @property
    def sort_by(self):
        """
        Gets the sort_by of this AlertFilterRequest.

        :return: The sort_by of this AlertFilterRequest.
        :rtype: list[SortCriteria]
        """
        return self._sort_by

    @sort_by.setter
    def sort_by(self, sort_by):
        """
        Sets the sort_by of this AlertFilterRequest.

        :param sort_by: The sort_by of this AlertFilterRequest.
        :type: list[SortCriteria]
        """

        self._sort_by = sort_by

    @property
    def evaluate_updated_on(self):
        """
        Gets the evaluate_updated_on of this AlertFilterRequest.

        :return: The evaluate_updated_on of this AlertFilterRequest.
        :rtype: bool
        """
        return self._evaluate_updated_on

    @evaluate_updated_on.setter
    def evaluate_updated_on(self, evaluate_updated_on):
        """
        Sets the evaluate_updated_on of this AlertFilterRequest.

        :param evaluate_updated_on: The evaluate_updated_on of this AlertFilterRequest.
        :type: bool
        """

        self._evaluate_updated_on = evaluate_updated_on

    @property
    def range_start(self):
        """
        Gets the range_start of this AlertFilterRequest.

        :return: The range_start of this AlertFilterRequest.
        :rtype: datetime
        """
        return self._range_start

    @range_start.setter
    def range_start(self, range_start):
        """
        Sets the range_start of this AlertFilterRequest.

        :param range_start: The range_start of this AlertFilterRequest.
        :type: datetime
        """

        self._range_start = range_start

    @property
    def range_end(self):
        """
        Gets the range_end of this AlertFilterRequest.

        :return: The range_end of this AlertFilterRequest.
        :rtype: datetime
        """
        return self._range_end

    @range_end.setter
    def range_end(self, range_end):
        """
        Sets the range_end of this AlertFilterRequest.

        :param range_end: The range_end of this AlertFilterRequest.
        :type: datetime
        """

        self._range_end = range_end

    @property
    def alert_status_ids(self):
        """
        Gets the alert_status_ids of this AlertFilterRequest.

        :return: The alert_status_ids of this AlertFilterRequest.
        :rtype: list[str]
        """
        return self._alert_status_ids

    @alert_status_ids.setter
    def alert_status_ids(self, alert_status_ids):
        """
        Sets the alert_status_ids of this AlertFilterRequest.

        :param alert_status_ids: The alert_status_ids of this AlertFilterRequest.
        :type: list[str]
        """
        allowed_values = ["active", "muted", "resolved", "closed"]
        if not set(alert_status_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `alert_status_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(alert_status_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._alert_status_ids = alert_status_ids

    @property
    def alert_type_ids(self):
        """
        Gets the alert_type_ids of this AlertFilterRequest.

        :return: The alert_type_ids of this AlertFilterRequest.
        :rtype: list[str]
        """
        return self._alert_type_ids

    @alert_type_ids.setter
    def alert_type_ids(self, alert_type_ids):
        """
        Sets the alert_type_ids of this AlertFilterRequest.

        :param alert_type_ids: The alert_type_ids of this AlertFilterRequest.
        :type: list[str]
        """
        allowed_values = ["ALARM", "COMMUNICATION_PROBLEM", "DEVICE_FAILURE", "END_OF_LIFE", "INFORMATIONAL", "OPERATOR_SUMMARY", "RECALL", "THRESHOLD_EXCEEDED", "WARNING", "WARRANTY"]
        if not set(alert_type_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `alert_type_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(alert_type_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._alert_type_ids = alert_type_ids

    @property
    def alert_severity_ids(self):
        """
        Gets the alert_severity_ids of this AlertFilterRequest.

        :return: The alert_severity_ids of this AlertFilterRequest.
        :rtype: list[str]
        """
        return self._alert_severity_ids

    @alert_severity_ids.setter
    def alert_severity_ids(self, alert_severity_ids):
        """
        Sets the alert_severity_ids of this AlertFilterRequest.

        :param alert_severity_ids: The alert_severity_ids of this AlertFilterRequest.
        :type: list[str]
        """
        allowed_values = ["CRITICAL", "SIGNIFICANT", "NORMAL", "LOW"]
        if not set(alert_severity_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `alert_severity_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(alert_severity_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._alert_severity_ids = alert_severity_ids

    @property
    def alert_rules(self):
        """
        Gets the alert_rules of this AlertFilterRequest.

        :return: The alert_rules of this AlertFilterRequest.
        :rtype: list[RecordIdentity]
        """
        return self._alert_rules

    @alert_rules.setter
    def alert_rules(self, alert_rules):
        """
        Sets the alert_rules of this AlertFilterRequest.

        :param alert_rules: The alert_rules of this AlertFilterRequest.
        :type: list[RecordIdentity]
        """

        self._alert_rules = alert_rules

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
        if not isinstance(other, AlertFilterRequest):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

