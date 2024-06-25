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

class NormalizationEquipmentLocation(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """
    def __init__(self, name=None, type=None):
        """
        NormalizationEquipmentLocation - a model defined in Swagger

        :param dict swaggerTypes: The key is attribute name
                                  and the value is attribute type.
        :param dict attributeMap: The key is attribute name
                                  and the value is json key in definition.
        """
        self.swagger_types = {
            'name': 'str',
            'type': 'str'
        }

        self.attribute_map = {
            'name': 'name',
            'type': 'type'
        }

        self._name = name
        self._type = type


    @property
    def name(self):
        """
        Gets the name of this NormalizationEquipmentLocation.

        :return: The name of this NormalizationEquipmentLocation.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this NormalizationEquipmentLocation.

        :param name: The name of this NormalizationEquipmentLocation.
        :type: str
        """

        self._name = name

    @property
    def type(self):
        """
        Gets the type of this NormalizationEquipmentLocation.

        :return: The type of this NormalizationEquipmentLocation.
        :rtype: str
        """
        return self._type

    @type.setter
    def type(self, type):
        """
        Sets the type of this NormalizationEquipmentLocation.

        :param type: The type of this NormalizationEquipmentLocation.
        :type: str
        """
        allowed_values = ["ADMITTING", "APARTMENT", "ATRIUM", "AUDITORIUM", "BACKOFFICE", "BALCONY", "BAR_ROOM", "BATHROOM", "BEDROOM", "BREAK_ROOM", "BUILDING", "CABLE_ROOM", "CAFETERIA_ROOM", "CAMPUS", "CINEMA", "CLASSROOM", "CLEANING_ROOM", "CLIMATE_CONTROL_ROOM", "CLOAK_ROOM", "CONFERENCE_ROOM", "CONVERSATION_ROOM", "COOKING_ROOM", "COPYING_ROOM", "COPY_ROOM", "DATAS_ERVER_ROOM", "DELIVERY_ROOM", "DINING_ROOM", "DISTRIBUTION_CENTER", "DRESSING_ROOM", "EDUCATIONAL_ROOM", "ELECTRICAL_ROOM", "ELEVATOR", "ELEVATOR_ROOM", "ELEVATOR_SHAFT", "ENTRANCE", "EXERCISE_ROOM", "EXHIBITION_ROOM", "FACADE", "FACTORY", "FITTING_ROOM", "FLOOR", "FOOD_HANDLING_ROOM", "FRONT_DESK", "GARAGE", "GROUP_ROOM", "HALLWAY", "HOME", "HOSPITAL", "ICU", "INPATIENT_SERVICES", "KITCHEN", "LABORATORY", "LAB_SERVICES", "LAND", "LAUNDRY_ROOM", "LEVEL", "LIBRARY", "LIVING_ROOM", "LOADING_RECEIVING_ROOM", "LOBBY", "LOCKER_ROOM", "LOUNGE", "MEDITATION_ROOM", "MORGUE", "MOTHERS_ROOM", "MULTI_PURPOSE_ROOM", "NURSERY", "NURSING_FACILITY", "OFFICE", "OFFICE_ROOM", "OPERATING_ROOM", "OUTPATIENT_SERVICES", "PANTRY", "PARKING", "PERSONAL_HYGIENE", "PHARMACY", "RADIOLOGY", "RECEPTION", "RECORDING_ROOM", "RECOVERY_ROOM", "RECREATIONAL_ROOM", "REGION", "REGIONAL_CENTER", "RESTROOM", "RESTING_ROOM", "RETAIL_ROOM", "ROOF_INNER", "ROOF_OUTER", "ROOF_TOP", "ROOM", "SCHOOL", "SECTION", "SECURITY_ROOM", "SERVER_ROOM", "SERVICE_SHAFT", "SHELTER", "SHIP", "SHOPPING_MALL", "SLAB", "SMALL_STUDY_ROOM", "SPRINKLER_ROOM", "STADIUM", "STAFF_ROOM", "STAIRWELL", "STAIRS", "STORAGE", "STORAGE_ROOM", "SUB_BUILDING", "SUPPLY_ROOM", "TELECOMMUNICATION_ROOM", "TENANT_UNIT", "TERRACE", "THEATER", "THERAPY", "TRAILER", "TREATMENT_ROOM", "TREATMENT_WAITING_ROOM", "TRUCK", "UTILITIES_ROOM", "WARD", "WASTE_MANAGEMENT_ROOM", "WORKSHOP", "ZONE", "OTHER"]
        if type is not None and type not in allowed_values:
            raise ValueError(
                "Invalid value for `type` ({0}), must be one of {1}"
                .format(type, allowed_values)
            )

        self._type = type

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
        if not isinstance(other, NormalizationEquipmentLocation):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other

