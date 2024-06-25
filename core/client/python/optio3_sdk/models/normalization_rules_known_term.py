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

class NormalizationRulesKnownTerm(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, acronym=None, positive_weight=None, negative_weight=None, weight_reason=None, synonyms=None):
        """
        NormalizationRulesKnownTerm - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'acronym': 'str',
            'positive_weight': 'float',
            'negative_weight': 'float',
            'weight_reason': 'str',
            'synonyms': 'list[str]'
        }

        self.attribute_map = {
            'acronym': 'acronym',
            'positive_weight': 'positiveWeight',
            'negative_weight': 'negativeWeight',
            'weight_reason': 'weightReason',
            'synonyms': 'synonyms'
        }

        self._acronym = acronym
        self._positive_weight = positive_weight
        self._negative_weight = negative_weight
        self._weight_reason = weight_reason
        self._synonyms = synonyms


    @property
    def acronym(self):
        """
        Gets the acronym of this NormalizationRulesKnownTerm.

        :return: The acronym of this NormalizationRulesKnownTerm.
        :rtype: str
        """
        return self._acronym

    @acronym.setter
    def acronym(self, acronym):
        """
        Sets the acronym of this NormalizationRulesKnownTerm.

        :param acronym: The acronym of this NormalizationRulesKnownTerm.
        :type: str
        """

        self._acronym = acronym

    @property
    def positive_weight(self):
        """
        Gets the positive_weight of this NormalizationRulesKnownTerm.

        :return: The positive_weight of this NormalizationRulesKnownTerm.
        :rtype: float
        """
        return self._positive_weight

    @positive_weight.setter
    def positive_weight(self, positive_weight):
        """
        Sets the positive_weight of this NormalizationRulesKnownTerm.

        :param positive_weight: The positive_weight of this NormalizationRulesKnownTerm.
        :type: float
        """

        self._positive_weight = positive_weight

    @property
    def negative_weight(self):
        """
        Gets the negative_weight of this NormalizationRulesKnownTerm.

        :return: The negative_weight of this NormalizationRulesKnownTerm.
        :rtype: float
        """
        return self._negative_weight

    @negative_weight.setter
    def negative_weight(self, negative_weight):
        """
        Sets the negative_weight of this NormalizationRulesKnownTerm.

        :param negative_weight: The negative_weight of this NormalizationRulesKnownTerm.
        :type: float
        """

        self._negative_weight = negative_weight

    @property
    def weight_reason(self):
        """
        Gets the weight_reason of this NormalizationRulesKnownTerm.

        :return: The weight_reason of this NormalizationRulesKnownTerm.
        :rtype: str
        """
        return self._weight_reason

    @weight_reason.setter
    def weight_reason(self, weight_reason):
        """
        Sets the weight_reason of this NormalizationRulesKnownTerm.

        :param weight_reason: The weight_reason of this NormalizationRulesKnownTerm.
        :type: str
        """

        self._weight_reason = weight_reason

    @property
    def synonyms(self):
        """
        Gets the synonyms of this NormalizationRulesKnownTerm.

        :return: The synonyms of this NormalizationRulesKnownTerm.
        :rtype: list[str]
        """
        return self._synonyms

    @synonyms.setter
    def synonyms(self, synonyms):
        """
        Sets the synonyms of this NormalizationRulesKnownTerm.

        :param synonyms: The synonyms of this NormalizationRulesKnownTerm.
        :type: list[str]
        """

        self._synonyms = synonyms

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
        if not isinstance(other, NormalizationRulesKnownTerm):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other