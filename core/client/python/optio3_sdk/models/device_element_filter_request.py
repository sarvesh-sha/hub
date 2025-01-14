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

class DeviceElementFilterRequest(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, version=None, start_offset=None, max_results=None, location_ids=None, location_inclusive=None, location_missing=None, sys_ids=None, parent_ids=None, parent_relations=None, parent_tags_query=None, like_filter=None, children_ids=None, children_relations=None, state_ids=None, sort_by=None, discovery_range_start=None, discovery_range_end=None, has_no_metadata=None, has_metadata=None, tags_query=None, is_hidden=None, is_not_hidden=None, has_no_sampling=None, has_any_sampling=None):
        """
        DeviceElementFilterRequest - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'version': 'int',
            'start_offset': 'int',
            'max_results': 'int',
            'location_ids': 'list[str]',
            'location_inclusive': 'bool',
            'location_missing': 'bool',
            'sys_ids': 'list[str]',
            'parent_ids': 'list[str]',
            'parent_relations': 'list[str]',
            'parent_tags_query': 'TagsCondition',
            'like_filter': 'str',
            'children_ids': 'list[str]',
            'children_relations': 'list[str]',
            'state_ids': 'list[str]',
            'sort_by': 'list[SortCriteria]',
            'discovery_range_start': 'datetime',
            'discovery_range_end': 'datetime',
            'has_no_metadata': 'bool',
            'has_metadata': 'bool',
            'tags_query': 'TagsCondition',
            'is_hidden': 'bool',
            'is_not_hidden': 'bool',
            'has_no_sampling': 'bool',
            'has_any_sampling': 'bool',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'version': 'version',
            'start_offset': 'startOffset',
            'max_results': 'maxResults',
            'location_ids': 'locationIDs',
            'location_inclusive': 'locationInclusive',
            'location_missing': 'locationMissing',
            'sys_ids': 'sysIds',
            'parent_ids': 'parentIDs',
            'parent_relations': 'parentRelations',
            'parent_tags_query': 'parentTagsQuery',
            'like_filter': 'likeFilter',
            'children_ids': 'childrenIDs',
            'children_relations': 'childrenRelations',
            'state_ids': 'stateIDs',
            'sort_by': 'sortBy',
            'discovery_range_start': 'discoveryRangeStart',
            'discovery_range_end': 'discoveryRangeEnd',
            'has_no_metadata': 'hasNoMetadata',
            'has_metadata': 'hasMetadata',
            'tags_query': 'tagsQuery',
            'is_hidden': 'isHidden',
            'is_not_hidden': 'isNotHidden',
            'has_no_sampling': 'hasNoSampling',
            'has_any_sampling': 'hasAnySampling',
            'discriminator___type': '__type'
        }

        self._version = version
        self._start_offset = start_offset
        self._max_results = max_results
        self._location_ids = location_ids
        self._location_inclusive = location_inclusive
        self._location_missing = location_missing
        self._sys_ids = sys_ids
        self._parent_ids = parent_ids
        self._parent_relations = parent_relations
        self._parent_tags_query = parent_tags_query
        self._like_filter = like_filter
        self._children_ids = children_ids
        self._children_relations = children_relations
        self._state_ids = state_ids
        self._sort_by = sort_by
        self._discovery_range_start = discovery_range_start
        self._discovery_range_end = discovery_range_end
        self._has_no_metadata = has_no_metadata
        self._has_metadata = has_metadata
        self._tags_query = tags_query
        self._is_hidden = is_hidden
        self._is_not_hidden = is_not_hidden
        self._has_no_sampling = has_no_sampling
        self._has_any_sampling = has_any_sampling

    @property
    def discriminator___type(self):
        return "DeviceElementFilterRequest"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def version(self):
        """
        Gets the version of this DeviceElementFilterRequest.

        :return: The version of this DeviceElementFilterRequest.
        :rtype: int
        """
        return self._version

    @version.setter
    def version(self, version):
        """
        Sets the version of this DeviceElementFilterRequest.

        :param version: The version of this DeviceElementFilterRequest.
        :type: int
        """

        self._version = version

    @property
    def start_offset(self):
        """
        Gets the start_offset of this DeviceElementFilterRequest.

        :return: The start_offset of this DeviceElementFilterRequest.
        :rtype: int
        """
        return self._start_offset

    @start_offset.setter
    def start_offset(self, start_offset):
        """
        Sets the start_offset of this DeviceElementFilterRequest.

        :param start_offset: The start_offset of this DeviceElementFilterRequest.
        :type: int
        """

        self._start_offset = start_offset

    @property
    def max_results(self):
        """
        Gets the max_results of this DeviceElementFilterRequest.

        :return: The max_results of this DeviceElementFilterRequest.
        :rtype: int
        """
        return self._max_results

    @max_results.setter
    def max_results(self, max_results):
        """
        Sets the max_results of this DeviceElementFilterRequest.

        :param max_results: The max_results of this DeviceElementFilterRequest.
        :type: int
        """

        self._max_results = max_results

    @property
    def location_ids(self):
        """
        Gets the location_ids of this DeviceElementFilterRequest.

        :return: The location_ids of this DeviceElementFilterRequest.
        :rtype: list[str]
        """
        return self._location_ids

    @location_ids.setter
    def location_ids(self, location_ids):
        """
        Sets the location_ids of this DeviceElementFilterRequest.

        :param location_ids: The location_ids of this DeviceElementFilterRequest.
        :type: list[str]
        """

        self._location_ids = location_ids

    @property
    def location_inclusive(self):
        """
        Gets the location_inclusive of this DeviceElementFilterRequest.

        :return: The location_inclusive of this DeviceElementFilterRequest.
        :rtype: bool
        """
        return self._location_inclusive

    @location_inclusive.setter
    def location_inclusive(self, location_inclusive):
        """
        Sets the location_inclusive of this DeviceElementFilterRequest.

        :param location_inclusive: The location_inclusive of this DeviceElementFilterRequest.
        :type: bool
        """

        self._location_inclusive = location_inclusive

    @property
    def location_missing(self):
        """
        Gets the location_missing of this DeviceElementFilterRequest.

        :return: The location_missing of this DeviceElementFilterRequest.
        :rtype: bool
        """
        return self._location_missing

    @location_missing.setter
    def location_missing(self, location_missing):
        """
        Sets the location_missing of this DeviceElementFilterRequest.

        :param location_missing: The location_missing of this DeviceElementFilterRequest.
        :type: bool
        """

        self._location_missing = location_missing

    @property
    def sys_ids(self):
        """
        Gets the sys_ids of this DeviceElementFilterRequest.

        :return: The sys_ids of this DeviceElementFilterRequest.
        :rtype: list[str]
        """
        return self._sys_ids

    @sys_ids.setter
    def sys_ids(self, sys_ids):
        """
        Sets the sys_ids of this DeviceElementFilterRequest.

        :param sys_ids: The sys_ids of this DeviceElementFilterRequest.
        :type: list[str]
        """

        self._sys_ids = sys_ids

    @property
    def parent_ids(self):
        """
        Gets the parent_ids of this DeviceElementFilterRequest.

        :return: The parent_ids of this DeviceElementFilterRequest.
        :rtype: list[str]
        """
        return self._parent_ids

    @parent_ids.setter
    def parent_ids(self, parent_ids):
        """
        Sets the parent_ids of this DeviceElementFilterRequest.

        :param parent_ids: The parent_ids of this DeviceElementFilterRequest.
        :type: list[str]
        """

        self._parent_ids = parent_ids

    @property
    def parent_relations(self):
        """
        Gets the parent_relations of this DeviceElementFilterRequest.

        :return: The parent_relations of this DeviceElementFilterRequest.
        :rtype: list[str]
        """
        return self._parent_relations

    @parent_relations.setter
    def parent_relations(self, parent_relations):
        """
        Sets the parent_relations of this DeviceElementFilterRequest.

        :param parent_relations: The parent_relations of this DeviceElementFilterRequest.
        :type: list[str]
        """
        allowed_values = ["structural", "controls"]
        if not set(parent_relations).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `parent_relations` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(parent_relations)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._parent_relations = parent_relations

    @property
    def parent_tags_query(self):
        """
        Gets the parent_tags_query of this DeviceElementFilterRequest.

        :return: The parent_tags_query of this DeviceElementFilterRequest.
        :rtype: TagsCondition
        """
        return self._parent_tags_query

    @parent_tags_query.setter
    def parent_tags_query(self, parent_tags_query):
        """
        Sets the parent_tags_query of this DeviceElementFilterRequest.

        :param parent_tags_query: The parent_tags_query of this DeviceElementFilterRequest.
        :type: TagsCondition
        """

        self._parent_tags_query = parent_tags_query

    @property
    def like_filter(self):
        """
        Gets the like_filter of this DeviceElementFilterRequest.

        :return: The like_filter of this DeviceElementFilterRequest.
        :rtype: str
        """
        return self._like_filter

    @like_filter.setter
    def like_filter(self, like_filter):
        """
        Sets the like_filter of this DeviceElementFilterRequest.

        :param like_filter: The like_filter of this DeviceElementFilterRequest.
        :type: str
        """

        self._like_filter = like_filter

    @property
    def children_ids(self):
        """
        Gets the children_ids of this DeviceElementFilterRequest.

        :return: The children_ids of this DeviceElementFilterRequest.
        :rtype: list[str]
        """
        return self._children_ids

    @children_ids.setter
    def children_ids(self, children_ids):
        """
        Sets the children_ids of this DeviceElementFilterRequest.

        :param children_ids: The children_ids of this DeviceElementFilterRequest.
        :type: list[str]
        """

        self._children_ids = children_ids

    @property
    def children_relations(self):
        """
        Gets the children_relations of this DeviceElementFilterRequest.

        :return: The children_relations of this DeviceElementFilterRequest.
        :rtype: list[str]
        """
        return self._children_relations

    @children_relations.setter
    def children_relations(self, children_relations):
        """
        Sets the children_relations of this DeviceElementFilterRequest.

        :param children_relations: The children_relations of this DeviceElementFilterRequest.
        :type: list[str]
        """
        allowed_values = ["structural", "controls"]
        if not set(children_relations).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `children_relations` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(children_relations)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._children_relations = children_relations

    @property
    def state_ids(self):
        """
        Gets the state_ids of this DeviceElementFilterRequest.

        :return: The state_ids of this DeviceElementFilterRequest.
        :rtype: list[str]
        """
        return self._state_ids

    @state_ids.setter
    def state_ids(self, state_ids):
        """
        Sets the state_ids of this DeviceElementFilterRequest.

        :param state_ids: The state_ids of this DeviceElementFilterRequest.
        :type: list[str]
        """
        allowed_values = ["provisioned", "offline", "passive", "operational", "maintenance", "retired"]
        if not set(state_ids).issubset(set(allowed_values)):
            raise ValueError(
                "Invalid values for `state_ids` [{0}], must be a subset of [{1}]"
                .format(", ".join(map(str, set(state_ids)-set(allowed_values))),
                        ", ".join(map(str, allowed_values)))
            )

        self._state_ids = state_ids

    @property
    def sort_by(self):
        """
        Gets the sort_by of this DeviceElementFilterRequest.

        :return: The sort_by of this DeviceElementFilterRequest.
        :rtype: list[SortCriteria]
        """
        return self._sort_by

    @sort_by.setter
    def sort_by(self, sort_by):
        """
        Sets the sort_by of this DeviceElementFilterRequest.

        :param sort_by: The sort_by of this DeviceElementFilterRequest.
        :type: list[SortCriteria]
        """

        self._sort_by = sort_by

    @property
    def discovery_range_start(self):
        """
        Gets the discovery_range_start of this DeviceElementFilterRequest.

        :return: The discovery_range_start of this DeviceElementFilterRequest.
        :rtype: datetime
        """
        return self._discovery_range_start

    @discovery_range_start.setter
    def discovery_range_start(self, discovery_range_start):
        """
        Sets the discovery_range_start of this DeviceElementFilterRequest.

        :param discovery_range_start: The discovery_range_start of this DeviceElementFilterRequest.
        :type: datetime
        """

        self._discovery_range_start = discovery_range_start

    @property
    def discovery_range_end(self):
        """
        Gets the discovery_range_end of this DeviceElementFilterRequest.

        :return: The discovery_range_end of this DeviceElementFilterRequest.
        :rtype: datetime
        """
        return self._discovery_range_end

    @discovery_range_end.setter
    def discovery_range_end(self, discovery_range_end):
        """
        Sets the discovery_range_end of this DeviceElementFilterRequest.

        :param discovery_range_end: The discovery_range_end of this DeviceElementFilterRequest.
        :type: datetime
        """

        self._discovery_range_end = discovery_range_end

    @property
    def has_no_metadata(self):
        """
        Gets the has_no_metadata of this DeviceElementFilterRequest.

        :return: The has_no_metadata of this DeviceElementFilterRequest.
        :rtype: bool
        """
        return self._has_no_metadata

    @has_no_metadata.setter
    def has_no_metadata(self, has_no_metadata):
        """
        Sets the has_no_metadata of this DeviceElementFilterRequest.

        :param has_no_metadata: The has_no_metadata of this DeviceElementFilterRequest.
        :type: bool
        """

        self._has_no_metadata = has_no_metadata

    @property
    def has_metadata(self):
        """
        Gets the has_metadata of this DeviceElementFilterRequest.

        :return: The has_metadata of this DeviceElementFilterRequest.
        :rtype: bool
        """
        return self._has_metadata

    @has_metadata.setter
    def has_metadata(self, has_metadata):
        """
        Sets the has_metadata of this DeviceElementFilterRequest.

        :param has_metadata: The has_metadata of this DeviceElementFilterRequest.
        :type: bool
        """

        self._has_metadata = has_metadata

    @property
    def tags_query(self):
        """
        Gets the tags_query of this DeviceElementFilterRequest.

        :return: The tags_query of this DeviceElementFilterRequest.
        :rtype: TagsCondition
        """
        return self._tags_query

    @tags_query.setter
    def tags_query(self, tags_query):
        """
        Sets the tags_query of this DeviceElementFilterRequest.

        :param tags_query: The tags_query of this DeviceElementFilterRequest.
        :type: TagsCondition
        """

        self._tags_query = tags_query

    @property
    def is_hidden(self):
        """
        Gets the is_hidden of this DeviceElementFilterRequest.

        :return: The is_hidden of this DeviceElementFilterRequest.
        :rtype: bool
        """
        return self._is_hidden

    @is_hidden.setter
    def is_hidden(self, is_hidden):
        """
        Sets the is_hidden of this DeviceElementFilterRequest.

        :param is_hidden: The is_hidden of this DeviceElementFilterRequest.
        :type: bool
        """

        self._is_hidden = is_hidden

    @property
    def is_not_hidden(self):
        """
        Gets the is_not_hidden of this DeviceElementFilterRequest.

        :return: The is_not_hidden of this DeviceElementFilterRequest.
        :rtype: bool
        """
        return self._is_not_hidden

    @is_not_hidden.setter
    def is_not_hidden(self, is_not_hidden):
        """
        Sets the is_not_hidden of this DeviceElementFilterRequest.

        :param is_not_hidden: The is_not_hidden of this DeviceElementFilterRequest.
        :type: bool
        """

        self._is_not_hidden = is_not_hidden

    @property
    def has_no_sampling(self):
        """
        Gets the has_no_sampling of this DeviceElementFilterRequest.

        :return: The has_no_sampling of this DeviceElementFilterRequest.
        :rtype: bool
        """
        return self._has_no_sampling

    @has_no_sampling.setter
    def has_no_sampling(self, has_no_sampling):
        """
        Sets the has_no_sampling of this DeviceElementFilterRequest.

        :param has_no_sampling: The has_no_sampling of this DeviceElementFilterRequest.
        :type: bool
        """

        self._has_no_sampling = has_no_sampling

    @property
    def has_any_sampling(self):
        """
        Gets the has_any_sampling of this DeviceElementFilterRequest.

        :return: The has_any_sampling of this DeviceElementFilterRequest.
        :rtype: bool
        """
        return self._has_any_sampling

    @has_any_sampling.setter
    def has_any_sampling(self, has_any_sampling):
        """
        Sets the has_any_sampling of this DeviceElementFilterRequest.

        :param has_any_sampling: The has_any_sampling of this DeviceElementFilterRequest.
        :type: bool
        """

        self._has_any_sampling = has_any_sampling

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
        if not isinstance(other, DeviceElementFilterRequest):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

