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

class NormalizationEngineOperatorBinaryScoreDocuments(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, id=None, x=None, y=None, a=None, b=None, min_ngram=None, max_ngram=None, min_doc_frequency=None, min_score=None):
        """
        NormalizationEngineOperatorBinaryScoreDocuments - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'id': 'str',
            'x': 'int',
            'y': 'int',
            'a': 'EngineExpression',
            'b': 'EngineExpression',
            'min_ngram': 'int',
            'max_ngram': 'int',
            'min_doc_frequency': 'int',
            'min_score': 'float',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'id': 'id',
            'x': 'x',
            'y': 'y',
            'a': 'a',
            'b': 'b',
            'min_ngram': 'minNgram',
            'max_ngram': 'maxNgram',
            'min_doc_frequency': 'minDocFrequency',
            'min_score': 'minScore',
            'discriminator___type': '__type'
        }

        self._id = id
        self._x = x
        self._y = y
        self._a = a
        self._b = b
        self._min_ngram = min_ngram
        self._max_ngram = max_ngram
        self._min_doc_frequency = min_doc_frequency
        self._min_score = min_score

    @property
    def discriminator___type(self):
        return "NormalizationEngineOperatorBinaryScoreDocuments"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def id(self):
        """
        Gets the id of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The id of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param id: The id of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: str
        """

        self._id = id

    @property
    def x(self):
        """
        Gets the x of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The x of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: int
        """
        return self._x

    @x.setter
    def x(self, x):
        """
        Sets the x of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param x: The x of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: int
        """

        self._x = x

    @property
    def y(self):
        """
        Gets the y of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The y of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: int
        """
        return self._y

    @y.setter
    def y(self, y):
        """
        Sets the y of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param y: The y of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: int
        """

        self._y = y

    @property
    def a(self):
        """
        Gets the a of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The a of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: EngineExpression
        """
        return self._a

    @a.setter
    def a(self, a):
        """
        Sets the a of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param a: The a of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: EngineExpression
        """

        self._a = a

    @property
    def b(self):
        """
        Gets the b of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The b of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: EngineExpression
        """
        return self._b

    @b.setter
    def b(self, b):
        """
        Sets the b of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param b: The b of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: EngineExpression
        """

        self._b = b

    @property
    def min_ngram(self):
        """
        Gets the min_ngram of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The min_ngram of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: int
        """
        return self._min_ngram

    @min_ngram.setter
    def min_ngram(self, min_ngram):
        """
        Sets the min_ngram of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param min_ngram: The min_ngram of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: int
        """

        self._min_ngram = min_ngram

    @property
    def max_ngram(self):
        """
        Gets the max_ngram of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The max_ngram of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: int
        """
        return self._max_ngram

    @max_ngram.setter
    def max_ngram(self, max_ngram):
        """
        Sets the max_ngram of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param max_ngram: The max_ngram of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: int
        """

        self._max_ngram = max_ngram

    @property
    def min_doc_frequency(self):
        """
        Gets the min_doc_frequency of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The min_doc_frequency of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: int
        """
        return self._min_doc_frequency

    @min_doc_frequency.setter
    def min_doc_frequency(self, min_doc_frequency):
        """
        Sets the min_doc_frequency of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param min_doc_frequency: The min_doc_frequency of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: int
        """

        self._min_doc_frequency = min_doc_frequency

    @property
    def min_score(self):
        """
        Gets the min_score of this NormalizationEngineOperatorBinaryScoreDocuments.

        :return: The min_score of this NormalizationEngineOperatorBinaryScoreDocuments.
        :rtype: float
        """
        return self._min_score

    @min_score.setter
    def min_score(self, min_score):
        """
        Sets the min_score of this NormalizationEngineOperatorBinaryScoreDocuments.

        :param min_score: The min_score of this NormalizationEngineOperatorBinaryScoreDocuments.
        :type: float
        """

        self._min_score = min_score

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
        if not isinstance(other, NormalizationEngineOperatorBinaryScoreDocuments):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
