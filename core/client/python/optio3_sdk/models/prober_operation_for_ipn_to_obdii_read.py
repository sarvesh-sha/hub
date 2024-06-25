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

class ProberOperationForIpnToObdiiRead(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, accelerometer_frequency=None, accelerometer_range=None, accelerometer_threshold=None, can_port=None, can_frequency=None, can_no_termination=None, can_invert=None, epsolar_port=None, epsolar_invert=None, gps_port=None, holykell_port=None, holykell_invert=None, ipn_port=None, ipn_baudrate=None, ipn_invert=None, obdii_port=None, obdii_frequency=None, obdii_invert=None, argohytos_port=None, stealthpower_port=None, tristar_port=None, victron_port=None, montage_bluetooth_gateway_port=None, sampling_seconds=None):
        """
        ProberOperationForIpnToObdiiRead - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'accelerometer_frequency': 'float',
            'accelerometer_range': 'float',
            'accelerometer_threshold': 'float',
            'can_port': 'str',
            'can_frequency': 'int',
            'can_no_termination': 'bool',
            'can_invert': 'bool',
            'epsolar_port': 'str',
            'epsolar_invert': 'bool',
            'gps_port': 'str',
            'holykell_port': 'str',
            'holykell_invert': 'bool',
            'ipn_port': 'str',
            'ipn_baudrate': 'int',
            'ipn_invert': 'bool',
            'obdii_port': 'str',
            'obdii_frequency': 'int',
            'obdii_invert': 'bool',
            'argohytos_port': 'str',
            'stealthpower_port': 'str',
            'tristar_port': 'str',
            'victron_port': 'str',
            'montage_bluetooth_gateway_port': 'str',
            'sampling_seconds': 'int',
            'discriminator___type': 'str'
        }

        self.attribute_map = {
            'accelerometer_frequency': 'accelerometerFrequency',
            'accelerometer_range': 'accelerometerRange',
            'accelerometer_threshold': 'accelerometerThreshold',
            'can_port': 'canPort',
            'can_frequency': 'canFrequency',
            'can_no_termination': 'canNoTermination',
            'can_invert': 'canInvert',
            'epsolar_port': 'epsolarPort',
            'epsolar_invert': 'epsolarInvert',
            'gps_port': 'gpsPort',
            'holykell_port': 'holykellPort',
            'holykell_invert': 'holykellInvert',
            'ipn_port': 'ipnPort',
            'ipn_baudrate': 'ipnBaudrate',
            'ipn_invert': 'ipnInvert',
            'obdii_port': 'obdiiPort',
            'obdii_frequency': 'obdiiFrequency',
            'obdii_invert': 'obdiiInvert',
            'argohytos_port': 'argohytosPort',
            'stealthpower_port': 'stealthpowerPort',
            'tristar_port': 'tristarPort',
            'victron_port': 'victronPort',
            'montage_bluetooth_gateway_port': 'montageBluetoothGatewayPort',
            'sampling_seconds': 'samplingSeconds',
            'discriminator___type': '__type'
        }

        self._accelerometer_frequency = accelerometer_frequency
        self._accelerometer_range = accelerometer_range
        self._accelerometer_threshold = accelerometer_threshold
        self._can_port = can_port
        self._can_frequency = can_frequency
        self._can_no_termination = can_no_termination
        self._can_invert = can_invert
        self._epsolar_port = epsolar_port
        self._epsolar_invert = epsolar_invert
        self._gps_port = gps_port
        self._holykell_port = holykell_port
        self._holykell_invert = holykell_invert
        self._ipn_port = ipn_port
        self._ipn_baudrate = ipn_baudrate
        self._ipn_invert = ipn_invert
        self._obdii_port = obdii_port
        self._obdii_frequency = obdii_frequency
        self._obdii_invert = obdii_invert
        self._argohytos_port = argohytos_port
        self._stealthpower_port = stealthpower_port
        self._tristar_port = tristar_port
        self._victron_port = victron_port
        self._montage_bluetooth_gateway_port = montage_bluetooth_gateway_port
        self._sampling_seconds = sampling_seconds

    @property
    def discriminator___type(self):
        return "ProberOperationForIpnToObdiiRead"

    @discriminator___type.setter
    def discriminator___type(self, discriminator):
        # Don't need to do anything
        return

    @property
    def accelerometer_frequency(self):
        """
        Gets the accelerometer_frequency of this ProberOperationForIpnToObdiiRead.

        :return: The accelerometer_frequency of this ProberOperationForIpnToObdiiRead.
        :rtype: float
        """
        return self._accelerometer_frequency

    @accelerometer_frequency.setter
    def accelerometer_frequency(self, accelerometer_frequency):
        """
        Sets the accelerometer_frequency of this ProberOperationForIpnToObdiiRead.

        :param accelerometer_frequency: The accelerometer_frequency of this ProberOperationForIpnToObdiiRead.
        :type: float
        """

        self._accelerometer_frequency = accelerometer_frequency

    @property
    def accelerometer_range(self):
        """
        Gets the accelerometer_range of this ProberOperationForIpnToObdiiRead.

        :return: The accelerometer_range of this ProberOperationForIpnToObdiiRead.
        :rtype: float
        """
        return self._accelerometer_range

    @accelerometer_range.setter
    def accelerometer_range(self, accelerometer_range):
        """
        Sets the accelerometer_range of this ProberOperationForIpnToObdiiRead.

        :param accelerometer_range: The accelerometer_range of this ProberOperationForIpnToObdiiRead.
        :type: float
        """

        self._accelerometer_range = accelerometer_range

    @property
    def accelerometer_threshold(self):
        """
        Gets the accelerometer_threshold of this ProberOperationForIpnToObdiiRead.

        :return: The accelerometer_threshold of this ProberOperationForIpnToObdiiRead.
        :rtype: float
        """
        return self._accelerometer_threshold

    @accelerometer_threshold.setter
    def accelerometer_threshold(self, accelerometer_threshold):
        """
        Sets the accelerometer_threshold of this ProberOperationForIpnToObdiiRead.

        :param accelerometer_threshold: The accelerometer_threshold of this ProberOperationForIpnToObdiiRead.
        :type: float
        """

        self._accelerometer_threshold = accelerometer_threshold

    @property
    def can_port(self):
        """
        Gets the can_port of this ProberOperationForIpnToObdiiRead.

        :return: The can_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._can_port

    @can_port.setter
    def can_port(self, can_port):
        """
        Sets the can_port of this ProberOperationForIpnToObdiiRead.

        :param can_port: The can_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._can_port = can_port

    @property
    def can_frequency(self):
        """
        Gets the can_frequency of this ProberOperationForIpnToObdiiRead.

        :return: The can_frequency of this ProberOperationForIpnToObdiiRead.
        :rtype: int
        """
        return self._can_frequency

    @can_frequency.setter
    def can_frequency(self, can_frequency):
        """
        Sets the can_frequency of this ProberOperationForIpnToObdiiRead.

        :param can_frequency: The can_frequency of this ProberOperationForIpnToObdiiRead.
        :type: int
        """

        self._can_frequency = can_frequency

    @property
    def can_no_termination(self):
        """
        Gets the can_no_termination of this ProberOperationForIpnToObdiiRead.

        :return: The can_no_termination of this ProberOperationForIpnToObdiiRead.
        :rtype: bool
        """
        return self._can_no_termination

    @can_no_termination.setter
    def can_no_termination(self, can_no_termination):
        """
        Sets the can_no_termination of this ProberOperationForIpnToObdiiRead.

        :param can_no_termination: The can_no_termination of this ProberOperationForIpnToObdiiRead.
        :type: bool
        """

        self._can_no_termination = can_no_termination

    @property
    def can_invert(self):
        """
        Gets the can_invert of this ProberOperationForIpnToObdiiRead.

        :return: The can_invert of this ProberOperationForIpnToObdiiRead.
        :rtype: bool
        """
        return self._can_invert

    @can_invert.setter
    def can_invert(self, can_invert):
        """
        Sets the can_invert of this ProberOperationForIpnToObdiiRead.

        :param can_invert: The can_invert of this ProberOperationForIpnToObdiiRead.
        :type: bool
        """

        self._can_invert = can_invert

    @property
    def epsolar_port(self):
        """
        Gets the epsolar_port of this ProberOperationForIpnToObdiiRead.

        :return: The epsolar_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._epsolar_port

    @epsolar_port.setter
    def epsolar_port(self, epsolar_port):
        """
        Sets the epsolar_port of this ProberOperationForIpnToObdiiRead.

        :param epsolar_port: The epsolar_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._epsolar_port = epsolar_port

    @property
    def epsolar_invert(self):
        """
        Gets the epsolar_invert of this ProberOperationForIpnToObdiiRead.

        :return: The epsolar_invert of this ProberOperationForIpnToObdiiRead.
        :rtype: bool
        """
        return self._epsolar_invert

    @epsolar_invert.setter
    def epsolar_invert(self, epsolar_invert):
        """
        Sets the epsolar_invert of this ProberOperationForIpnToObdiiRead.

        :param epsolar_invert: The epsolar_invert of this ProberOperationForIpnToObdiiRead.
        :type: bool
        """

        self._epsolar_invert = epsolar_invert

    @property
    def gps_port(self):
        """
        Gets the gps_port of this ProberOperationForIpnToObdiiRead.

        :return: The gps_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._gps_port

    @gps_port.setter
    def gps_port(self, gps_port):
        """
        Sets the gps_port of this ProberOperationForIpnToObdiiRead.

        :param gps_port: The gps_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._gps_port = gps_port

    @property
    def holykell_port(self):
        """
        Gets the holykell_port of this ProberOperationForIpnToObdiiRead.

        :return: The holykell_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._holykell_port

    @holykell_port.setter
    def holykell_port(self, holykell_port):
        """
        Sets the holykell_port of this ProberOperationForIpnToObdiiRead.

        :param holykell_port: The holykell_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._holykell_port = holykell_port

    @property
    def holykell_invert(self):
        """
        Gets the holykell_invert of this ProberOperationForIpnToObdiiRead.

        :return: The holykell_invert of this ProberOperationForIpnToObdiiRead.
        :rtype: bool
        """
        return self._holykell_invert

    @holykell_invert.setter
    def holykell_invert(self, holykell_invert):
        """
        Sets the holykell_invert of this ProberOperationForIpnToObdiiRead.

        :param holykell_invert: The holykell_invert of this ProberOperationForIpnToObdiiRead.
        :type: bool
        """

        self._holykell_invert = holykell_invert

    @property
    def ipn_port(self):
        """
        Gets the ipn_port of this ProberOperationForIpnToObdiiRead.

        :return: The ipn_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._ipn_port

    @ipn_port.setter
    def ipn_port(self, ipn_port):
        """
        Sets the ipn_port of this ProberOperationForIpnToObdiiRead.

        :param ipn_port: The ipn_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._ipn_port = ipn_port

    @property
    def ipn_baudrate(self):
        """
        Gets the ipn_baudrate of this ProberOperationForIpnToObdiiRead.

        :return: The ipn_baudrate of this ProberOperationForIpnToObdiiRead.
        :rtype: int
        """
        return self._ipn_baudrate

    @ipn_baudrate.setter
    def ipn_baudrate(self, ipn_baudrate):
        """
        Sets the ipn_baudrate of this ProberOperationForIpnToObdiiRead.

        :param ipn_baudrate: The ipn_baudrate of this ProberOperationForIpnToObdiiRead.
        :type: int
        """

        self._ipn_baudrate = ipn_baudrate

    @property
    def ipn_invert(self):
        """
        Gets the ipn_invert of this ProberOperationForIpnToObdiiRead.

        :return: The ipn_invert of this ProberOperationForIpnToObdiiRead.
        :rtype: bool
        """
        return self._ipn_invert

    @ipn_invert.setter
    def ipn_invert(self, ipn_invert):
        """
        Sets the ipn_invert of this ProberOperationForIpnToObdiiRead.

        :param ipn_invert: The ipn_invert of this ProberOperationForIpnToObdiiRead.
        :type: bool
        """

        self._ipn_invert = ipn_invert

    @property
    def obdii_port(self):
        """
        Gets the obdii_port of this ProberOperationForIpnToObdiiRead.

        :return: The obdii_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._obdii_port

    @obdii_port.setter
    def obdii_port(self, obdii_port):
        """
        Sets the obdii_port of this ProberOperationForIpnToObdiiRead.

        :param obdii_port: The obdii_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._obdii_port = obdii_port

    @property
    def obdii_frequency(self):
        """
        Gets the obdii_frequency of this ProberOperationForIpnToObdiiRead.

        :return: The obdii_frequency of this ProberOperationForIpnToObdiiRead.
        :rtype: int
        """
        return self._obdii_frequency

    @obdii_frequency.setter
    def obdii_frequency(self, obdii_frequency):
        """
        Sets the obdii_frequency of this ProberOperationForIpnToObdiiRead.

        :param obdii_frequency: The obdii_frequency of this ProberOperationForIpnToObdiiRead.
        :type: int
        """

        self._obdii_frequency = obdii_frequency

    @property
    def obdii_invert(self):
        """
        Gets the obdii_invert of this ProberOperationForIpnToObdiiRead.

        :return: The obdii_invert of this ProberOperationForIpnToObdiiRead.
        :rtype: bool
        """
        return self._obdii_invert

    @obdii_invert.setter
    def obdii_invert(self, obdii_invert):
        """
        Sets the obdii_invert of this ProberOperationForIpnToObdiiRead.

        :param obdii_invert: The obdii_invert of this ProberOperationForIpnToObdiiRead.
        :type: bool
        """

        self._obdii_invert = obdii_invert

    @property
    def argohytos_port(self):
        """
        Gets the argohytos_port of this ProberOperationForIpnToObdiiRead.

        :return: The argohytos_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._argohytos_port

    @argohytos_port.setter
    def argohytos_port(self, argohytos_port):
        """
        Sets the argohytos_port of this ProberOperationForIpnToObdiiRead.

        :param argohytos_port: The argohytos_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._argohytos_port = argohytos_port

    @property
    def stealthpower_port(self):
        """
        Gets the stealthpower_port of this ProberOperationForIpnToObdiiRead.

        :return: The stealthpower_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._stealthpower_port

    @stealthpower_port.setter
    def stealthpower_port(self, stealthpower_port):
        """
        Sets the stealthpower_port of this ProberOperationForIpnToObdiiRead.

        :param stealthpower_port: The stealthpower_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._stealthpower_port = stealthpower_port

    @property
    def tristar_port(self):
        """
        Gets the tristar_port of this ProberOperationForIpnToObdiiRead.

        :return: The tristar_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._tristar_port

    @tristar_port.setter
    def tristar_port(self, tristar_port):
        """
        Sets the tristar_port of this ProberOperationForIpnToObdiiRead.

        :param tristar_port: The tristar_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._tristar_port = tristar_port

    @property
    def victron_port(self):
        """
        Gets the victron_port of this ProberOperationForIpnToObdiiRead.

        :return: The victron_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._victron_port

    @victron_port.setter
    def victron_port(self, victron_port):
        """
        Sets the victron_port of this ProberOperationForIpnToObdiiRead.

        :param victron_port: The victron_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._victron_port = victron_port

    @property
    def montage_bluetooth_gateway_port(self):
        """
        Gets the montage_bluetooth_gateway_port of this ProberOperationForIpnToObdiiRead.

        :return: The montage_bluetooth_gateway_port of this ProberOperationForIpnToObdiiRead.
        :rtype: str
        """
        return self._montage_bluetooth_gateway_port

    @montage_bluetooth_gateway_port.setter
    def montage_bluetooth_gateway_port(self, montage_bluetooth_gateway_port):
        """
        Sets the montage_bluetooth_gateway_port of this ProberOperationForIpnToObdiiRead.

        :param montage_bluetooth_gateway_port: The montage_bluetooth_gateway_port of this ProberOperationForIpnToObdiiRead.
        :type: str
        """

        self._montage_bluetooth_gateway_port = montage_bluetooth_gateway_port

    @property
    def sampling_seconds(self):
        """
        Gets the sampling_seconds of this ProberOperationForIpnToObdiiRead.

        :return: The sampling_seconds of this ProberOperationForIpnToObdiiRead.
        :rtype: int
        """
        return self._sampling_seconds

    @sampling_seconds.setter
    def sampling_seconds(self, sampling_seconds):
        """
        Sets the sampling_seconds of this ProberOperationForIpnToObdiiRead.

        :param sampling_seconds: The sampling_seconds of this ProberOperationForIpnToObdiiRead.
        :type: int
        """

        self._sampling_seconds = sampling_seconds

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
        if not isinstance(other, ProberOperationForIpnToObdiiRead):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
