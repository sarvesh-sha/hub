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

class I2CSensorSHT3x(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, bus=None, sampling_period=None, averaging_samples=None, equipment_class=None, instance_selector=None):
        """
        I2CSensorSHT3x - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'bus': 'int',
            'sampling_period': 'float',
            'averaging_samples': 'int',
            'equipment_class': 'WellKnownEquipmentClassOrCustom',
            'instance_selector': 'str',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'bus': 'bus',
            'sampling_period': 'samplingPeriod',
            'averaging_samples': 'averagingSamples',
            'equipment_class': 'equipmentClass',
            'instance_selector': 'instanceSelector',
            'discriminator___type': '__type'
        }

        self._bus = bus
        self._sampling_period = sampling_period
        self._averaging_samples = averaging_samples
        self._equipment_class = equipment_class
        self._instance_selector = instance_selector

    @property
    def discriminator___type(self):
        return "I2CSensor_SHT3x"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def bus(self):
        """
        Gets the bus of this I2CSensorSHT3x.

        :return: The bus of this I2CSensorSHT3x.
        :rtype: int
        """
        return self._bus

    @bus.setter
    def bus(self, bus):
        """
        Sets the bus of this I2CSensorSHT3x.

        :param bus: The bus of this I2CSensorSHT3x.
        :type: int
        """

        self._bus = bus

    @property
    def sampling_period(self):
        """
        Gets the sampling_period of this I2CSensorSHT3x.

        :return: The sampling_period of this I2CSensorSHT3x.
        :rtype: float
        """
        return self._sampling_period

    @sampling_period.setter
    def sampling_period(self, sampling_period):
        """
        Sets the sampling_period of this I2CSensorSHT3x.

        :param sampling_period: The sampling_period of this I2CSensorSHT3x.
        :type: float
        """

        self._sampling_period = sampling_period

    @property
    def averaging_samples(self):
        """
        Gets the averaging_samples of this I2CSensorSHT3x.

        :return: The averaging_samples of this I2CSensorSHT3x.
        :rtype: int
        """
        return self._averaging_samples

    @averaging_samples.setter
    def averaging_samples(self, averaging_samples):
        """
        Sets the averaging_samples of this I2CSensorSHT3x.

        :param averaging_samples: The averaging_samples of this I2CSensorSHT3x.
        :type: int
        """

        self._averaging_samples = averaging_samples

    @property
    def equipment_class(self):
        """
        Gets the equipment_class of this I2CSensorSHT3x.

        :return: The equipment_class of this I2CSensorSHT3x.
        :rtype: WellKnownEquipmentClassOrCustom
        """
        return self._equipment_class

    @equipment_class.setter
    def equipment_class(self, equipment_class):
        """
        Sets the equipment_class of this I2CSensorSHT3x.

        :param equipment_class: The equipment_class of this I2CSensorSHT3x.
        :type: WellKnownEquipmentClassOrCustom
        """

        self._equipment_class = equipment_class

    @property
    def instance_selector(self):
        """
        Gets the instance_selector of this I2CSensorSHT3x.

        :return: The instance_selector of this I2CSensorSHT3x.
        :rtype: str
        """
        return self._instance_selector

    @instance_selector.setter
    def instance_selector(self, instance_selector):
        """
        Sets the instance_selector of this I2CSensorSHT3x.

        :param instance_selector: The instance_selector of this I2CSensorSHT3x.
        :type: str
        """

        self._instance_selector = instance_selector

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
        if not isinstance(other, I2CSensorSHT3x):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
