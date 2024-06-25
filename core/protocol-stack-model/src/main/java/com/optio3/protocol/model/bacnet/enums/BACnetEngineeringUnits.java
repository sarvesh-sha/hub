/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetEngineeringUnits implements TypedBitSet.ValueGetter
{
    // @formatter:off
    // -- Acceleration
    meters_per_second_per_second            (166  , EngineeringUnits.meters_per_second_per_second),
    // -- Area
    square_meters                           (0    , EngineeringUnits.square_meters),
    square_centimeters                      (116  , EngineeringUnits.square_centimeters),
    square_feet                             (1    , EngineeringUnits.square_feet),
    square_inches                           (115  , EngineeringUnits.square_inches),
    // -- Currency								  
    currency1                               (105  , EngineeringUnits.currency_generic),
    currency2                               (106  , EngineeringUnits.currency_generic),
    currency3                               (107  , EngineeringUnits.currency_generic),
    currency4                               (108  , EngineeringUnits.currency_generic),
    currency5                               (109  , EngineeringUnits.currency_generic),
    currency6                               (110  , EngineeringUnits.currency_generic),
    currency7                               (111  , EngineeringUnits.currency_generic),
    currency8                               (112  , EngineeringUnits.currency_generic),
    currency9                               (113  , EngineeringUnits.currency_generic),
    currency10                              (114  , EngineeringUnits.currency_generic),
    // -- Electrical							  
    milliamperes                            (2    , EngineeringUnits.milliamperes),
    amperes                                 (3    , EngineeringUnits.amperes),
    amperes_per_meter                       (167  , EngineeringUnits.amperes_per_meter),
    amperes_per_square_meter                (168  , EngineeringUnits.amperes_per_square_meter),
    ampere_square_meters                    (169  , EngineeringUnits.ampere_square_meters),
    decibels                                (199  , EngineeringUnits.decibels),
    decibels_millivolt                      (200  , EngineeringUnits.decibels_millivolt),
    decibels_volt                           (201  , EngineeringUnits.decibels_volt),
    farads                                  (170  , EngineeringUnits.farads),
    henrys                                  (171  , EngineeringUnits.henrys),
    ohms                                    (4    , EngineeringUnits.ohms),
    ohm_meter_squared_per_meter             (237  , EngineeringUnits.ohm_meter_squared_per_meter),
    ohm_meters                              (172  , EngineeringUnits.ohm_meters),
    milliohms                               (145  , EngineeringUnits.milliohms),
    kilohms                                 (122  , EngineeringUnits.kilohms),
    megohms                                 (123  , EngineeringUnits.megohms),
    microsiemens                            (190  , EngineeringUnits.microsiemens),
    millisiemens                            (202  , EngineeringUnits.millisiemens),
    siemens                                 (173  , EngineeringUnits.siemens),
    siemens_per_meter                       (174  , EngineeringUnits.siemens_per_meter),
    teslas                                  (175  , EngineeringUnits.teslas),
    volts                                   (5    , EngineeringUnits.volts),
    millivolts                              (124  , EngineeringUnits.millivolts),
    kilovolts                               (6    , EngineeringUnits.kilovolts),
    megavolts                               (7    , EngineeringUnits.megavolts),
    volt_amperes                            (8    , EngineeringUnits.volt_amperes),
    kilovolt_amperes                        (9    , EngineeringUnits.kilovolt_amperes),
    megavolt_amperes                        (10   , EngineeringUnits.megavolt_amperes),
    volt_amperes_reactive                   (11   , EngineeringUnits.volt_amperes_reactive),
    kilovolt_amperes_reactive               (12   , EngineeringUnits.kilovolt_amperes_reactive),
    megavolt_amperes_reactive               (13   , EngineeringUnits.megavolt_amperes_reactive),
    volts_per_degree_kelvin                 (176  , EngineeringUnits.volts_per_degree_kelvin),
    volts_per_meter                         (177  , EngineeringUnits.volts_per_meter),
    degrees_phase                           (14   , EngineeringUnits.degrees_phase),
    power_factor                            (15   , EngineeringUnits.power_factor),
    webers                                  (178  , EngineeringUnits.webers),
    // -- Energy								  
    ampere_seconds                          (238  , EngineeringUnits.ampere_seconds),
    volt_ampere_hours                       (239  , EngineeringUnits.volt_ampere_hours), // -- i.e. VAh
    kilovolt_ampere_hours                   (240  , EngineeringUnits.kilovolt_ampere_hours),
    megavolt_ampere_hours                   (241  , EngineeringUnits.megavolt_ampere_hours),
    volt_ampere_hours_reactive              (242  , EngineeringUnits.volt_ampere_hours_reactive), // -- i.e. varh
    kilovolt_ampere_hours_reactive          (243  , EngineeringUnits.kilovolt_ampere_hours_reactive),
    megavolt_ampere_hours_reactive          (244  , EngineeringUnits.megavolt_ampere_hours_reactive),
    volt_square_hours                       (245  , EngineeringUnits.volt_square_hours),
    ampere_square_hours                     (246  , EngineeringUnits.ampere_square_hours),
    joules                                  (16   , EngineeringUnits.joules),
    kilojoules                              (17   , EngineeringUnits.kilojoules),
    kilojoules_per_kilogram                 (125  , EngineeringUnits.kilojoules_per_kilogram),
    megajoules                              (126  , EngineeringUnits.megajoules),
    watt_hours                              (18   , EngineeringUnits.watt_hours),
    kilowatt_hours                          (19   , EngineeringUnits.kilowatt_hours),
    megawatt_hours                          (146  , EngineeringUnits.megawatt_hours),
    watt_hours_reactive                     (203  , EngineeringUnits.watt_hours_reactive),
    kilowatt_hours_reactive                 (204  , EngineeringUnits.kilowatt_hours_reactive),
    megawatt_hours_reactive                 (205  , EngineeringUnits.megawatt_hours_reactive),
    btus                                    (20   , EngineeringUnits.btus),
    kilo_btus                               (147  , EngineeringUnits.kilo_btus),
    mega_btus                               (148  , EngineeringUnits.mega_btus),
    therms                                  (21   , EngineeringUnits.therms),
    ton_hours                               (22   , EngineeringUnits.ton_hours),
    // -- Enthalpy								  
    joules_per_kilogram_dry_air             (23   , EngineeringUnits.joules_per_kilogram_dry_air),
    kilojoules_per_kilogram_dry_air         (149  , EngineeringUnits.kilojoules_per_kilogram_dry_air),
    megajoules_per_kilogram_dry_air         (150  , EngineeringUnits.megajoules_per_kilogram_dry_air),
    btus_per_pound_dry_air                  (24   , EngineeringUnits.btus_per_pound_dry_air),
    btus_per_pound                          (117  , EngineeringUnits.btus_per_pound),
												  
    // -- Entropy								  
    joules_per_degree_kelvin                (127  , EngineeringUnits.joules_per_degree_kelvin),
    kilojoules_per_degree_kelvin            (151  , EngineeringUnits.kilojoules_per_degree_kelvin),
    megajoules_per_degree_kelvin            (152  , EngineeringUnits.megajoules_per_degree_kelvin),
    joules_per_kilogram_degree_kelvin       (128  , EngineeringUnits.joules_per_kilogram_degree_kelvin),
    // -- Force									  
    newton                                  (153  , EngineeringUnits.newton),
    // -- Frequency								  
    cycles_per_hour                         (25   , EngineeringUnits.cycles_per_hour),
    cycles_per_minute                       (26   , EngineeringUnits.cycles_per_minute),
    hertz                                   (27   , EngineeringUnits.hertz),
    kilohertz                               (129  , EngineeringUnits.kilohertz),
    megahertz                               (130  , EngineeringUnits.megahertz),
    per_hour                                (131  , EngineeringUnits.per_hour),
    // -- Humidity								  
    grams_of_water_per_kilogram_dry_air     (28   , EngineeringUnits.grams_of_water_per_kilogram_dry_air),
    percent_relative_humidity               (29   , EngineeringUnits.percent_relative_humidity),
    // -- Length								  
    micrometers                             (194  , EngineeringUnits.micrometers),
    millimeters                             (30   , EngineeringUnits.millimeters),
    centimeters                             (118  , EngineeringUnits.centimeters),
    kilometers                              (193  , EngineeringUnits.kilometers),
    meters                                  (31   , EngineeringUnits.meters),
    inches                                  (32   , EngineeringUnits.inches),
    feet                                    (33   , EngineeringUnits.feet),
    // -- Light									  
    candelas                                (179  , EngineeringUnits.candelas),
    candelas_per_square_meter               (180  , EngineeringUnits.candelas_per_square_meter),
    watts_per_square_foot                   (34   , EngineeringUnits.watts_per_square_foot),
    watts_per_square_meter                  (35   , EngineeringUnits.watts_per_square_meter),
    lumens                                  (36   , EngineeringUnits.lumens),
    luxes                                   (37   , EngineeringUnits.luxes),
    foot_candles                            (38   , EngineeringUnits.foot_candles),
    // -- Mass									  
    milligrams                              (196  , EngineeringUnits.milligrams),
    grams                                   (195  , EngineeringUnits.grams),
    kilograms                               (39   , EngineeringUnits.kilograms),
    pounds_mass                             (40   , EngineeringUnits.pounds_mass),
    tons                                    (41   , EngineeringUnits.tons),
    // -- Mass Flow								  
    grams_per_second                        (154  , EngineeringUnits.grams_per_second),
    grams_per_minute                        (155  , EngineeringUnits.grams_per_minute),
    kilograms_per_second                    (42   , EngineeringUnits.kilograms_per_second),
    kilograms_per_minute                    (43   , EngineeringUnits.kilograms_per_minute),
    kilograms_per_hour                      (44   , EngineeringUnits.kilograms_per_hour),
    pounds_mass_per_second                  (119  , EngineeringUnits.pounds_mass_per_second),
    pounds_mass_per_minute                  (45   , EngineeringUnits.pounds_mass_per_minute),
    pounds_mass_per_hour                    (46   , EngineeringUnits.pounds_mass_per_hour),
    tons_per_hour                           (156  , EngineeringUnits.tons_per_hour),
    // -- Power									  
    milliwatts                              (132  , EngineeringUnits.milliwatts),
    watts                                   (47   , EngineeringUnits.watts),
    kilowatts                               (48   , EngineeringUnits.kilowatts),
    megawatts                               (49   , EngineeringUnits.megawatts),
    btus_per_hour                           (50   , EngineeringUnits.btus_per_hour),
    kilo_btus_per_hour                      (157  , EngineeringUnits.kilo_btus_per_hour),
    joule_per_hours                         (247  , EngineeringUnits.joule_per_hours),
    horsepower                              (51   , EngineeringUnits.horsepower),
    tons_refrigeration                      (52   , EngineeringUnits.tons_refrigeration),
    // -- Pressure								  
    pascals                                 (53   , EngineeringUnits.pascals),
    hectopascals                            (133  , EngineeringUnits.hectopascals),
    kilopascals                             (54   , EngineeringUnits.kilopascals),
    millibars                               (134  , EngineeringUnits.millibars),
    bars                                    (55   , EngineeringUnits.bars),
    pounds_force_per_square_inch            (56   , EngineeringUnits.pounds_force_per_square_inch),
    millimeters_of_water                    (206  , EngineeringUnits.millimeters_of_water),
    centimeters_of_water                    (57   , EngineeringUnits.centimeters_of_water),
    inches_of_water                         (58   , EngineeringUnits.inches_of_water),
    millimeters_of_mercury                  (59   , EngineeringUnits.millimeters_of_mercury),
    centimeters_of_mercury                  (60   , EngineeringUnits.centimeters_of_mercury),
    inches_of_mercury                       (61   , EngineeringUnits.inches_of_mercury),
    // -- Temperature							  
    degrees_celsius                         (62   , EngineeringUnits.degrees_celsius),
    degrees_kelvin                          (63   , EngineeringUnits.degrees_kelvin),
    degrees_kelvin_per_hour                 (181  , EngineeringUnits.degrees_kelvin_per_hour),
    degrees_kelvin_per_minute               (182  , EngineeringUnits.degrees_kelvin_per_minute),
    degrees_fahrenheit                      (64   , EngineeringUnits.degrees_fahrenheit),
    degree_days_celsius                     (65   , EngineeringUnits.degree_days_celsius),
    degree_days_fahrenheit                  (66   , EngineeringUnits.degree_days_fahrenheit),
    delta_degrees_fahrenheit                (120  , EngineeringUnits.delta_degrees_fahrenheit),
    delta_degrees_kelvin                    (121  , EngineeringUnits.delta_degrees_kelvin),
    // -- Time									  
    years                                   (67   , EngineeringUnits.years),
    months                                  (68   , EngineeringUnits.months),
    weeks                                   (69   , EngineeringUnits.weeks),
    days                                    (70   , EngineeringUnits.days),
    hours                                   (71   , EngineeringUnits.hours),
    minutes                                 (72   , EngineeringUnits.minutes),
    seconds                                 (73   , EngineeringUnits.seconds),
    hundredths_seconds                      (158  , EngineeringUnits.hundredths_seconds),
    milliseconds                            (159  , EngineeringUnits.milliseconds),
    // -- Torque								  
    newton_meters                           (160  , EngineeringUnits.newton_meters),
    // -- Velocity								  
    millimeters_per_second                  (161  , EngineeringUnits.millimeters_per_second),
    millimeters_per_minute                  (162  , EngineeringUnits.millimeters_per_minute),
    meters_per_second                       (74   , EngineeringUnits.meters_per_second),
    meters_per_minute                       (163  , EngineeringUnits.meters_per_minute),
    meters_per_hour                         (164  , EngineeringUnits.meters_per_hour),
    kilometers_per_hour                     (75   , EngineeringUnits.kilometers_per_hour),
    feet_per_second                         (76   , EngineeringUnits.feet_per_second),
    feet_per_minute                         (77   , EngineeringUnits.feet_per_minute),
    miles_per_hour                          (78   , EngineeringUnits.miles_per_hour),
    // -- Volume								  
    cubic_feet                              (79   , EngineeringUnits.cubic_feet),
    cubic_meters                            (80   , EngineeringUnits.cubic_meters),
    imperial_gallons                        (81   , EngineeringUnits.imperial_gallons),
    milliliters                             (197  , EngineeringUnits.milliliters),
    liters                                  (82   , EngineeringUnits.liters),
    us_gallons                              (83   , EngineeringUnits.us_gallons),
    // -- Volumetric Flow
    cubic_feet_per_second                   (142  , EngineeringUnits.cubic_feet_per_second),
    cubic_feet_per_minute                   (84   , EngineeringUnits.cubic_feet_per_minute),
    million_standard_cubic_feet_per_minute  (254  , EngineeringUnits.million_standard_cubic_feet_per_minute),
    cubic_feet_per_hour                     (191  , EngineeringUnits.cubic_feet_per_hour),
    cubic_feet_per_day                      (248  , EngineeringUnits.cubic_feet_per_day),
    standard_cubic_feet_per_day             (47808, EngineeringUnits.standard_cubic_feet_per_day),
    million_standard_cubic_feet_per_day     (47809, EngineeringUnits.million_standard_cubic_feet_per_day),
    thousand_cubic_feet_per_day             (47810, EngineeringUnits.thousand_cubic_feet_per_day),
    thousand_standard_cubic_feet_per_day    (47811, EngineeringUnits.thousand_standard_cubic_feet_per_day),
    pounds_mass_per_day                     (47812, EngineeringUnits.pounds_mass_per_day),
    cubic_meters_per_second                 (85   , EngineeringUnits.cubic_meters_per_second),
    cubic_meters_per_minute                 (165  , EngineeringUnits.cubic_meters_per_minute),
    cubic_meters_per_hour                   (135  , EngineeringUnits.cubic_meters_per_hour),
    cubic_meters_per_day                    (249  , EngineeringUnits.cubic_meters_per_day),
    imperial_gallons_per_minute             (86   , EngineeringUnits.imperial_gallons_per_minute),
    milliliters_per_second                  (198  , EngineeringUnits.milliliters_per_second),
    liters_per_second                       (87   , EngineeringUnits.liters_per_second),
    liters_per_minute                       (88   , EngineeringUnits.liters_per_minute),
    liters_per_hour                         (136  , EngineeringUnits.liters_per_hour),
    us_gallons_per_minute                   (89   , EngineeringUnits.us_gallons_per_minute),
    us_gallons_per_hour                     (192  , EngineeringUnits.us_gallons_per_hour),
    // -- Other
    degrees_angular                         (90   , EngineeringUnits.degrees_angular),
    degrees_celsius_per_hour                (91   , EngineeringUnits.degrees_celsius_per_hour),
    degrees_celsius_per_minute              (92   , EngineeringUnits.degrees_celsius_per_minute),
    degrees_fahrenheit_per_hour             (93   , EngineeringUnits.degrees_fahrenheit_per_hour),
    degrees_fahrenheit_per_minute           (94   , EngineeringUnits.degrees_fahrenheit_per_minute),
    joule_seconds                           (183  , EngineeringUnits.joule_seconds),
    kilograms_per_cubic_meter               (186  , EngineeringUnits.kilograms_per_cubic_meter),
    kilowatt_hours_per_square_meter         (137  , EngineeringUnits.kilowatt_hours_per_square_meter),
    kilowatt_hours_per_square_foot          (138  , EngineeringUnits.kilowatt_hours_per_square_foot),
    watt_hours_per_cubic_meter              (250  , EngineeringUnits.watt_hours_per_cubic_meter),
    joules_per_cubic_meter                  (251  , EngineeringUnits.joules_per_cubic_meter),
    megajoules_per_square_meter             (139  , EngineeringUnits.megajoules_per_square_meter),
    megajoules_per_square_foot              (140  , EngineeringUnits.megajoules_per_square_foot),
    mole_percent                            (252  , EngineeringUnits.mole_percent),
    no_units                                (95   , EngineeringUnits.no_units),
    newton_seconds                          (187  , EngineeringUnits.newton_seconds),
    newtons_per_meter                       (188  , EngineeringUnits.newtons_per_meter),
    parts_per_million                       (96   , EngineeringUnits.parts_per_million),
    parts_per_billion                       (97   , EngineeringUnits.parts_per_billion),
    pascal_seconds                          (253  , EngineeringUnits.pascal_seconds),
    percent                                 (98   , EngineeringUnits.percent),
    percent_obscuration_per_foot            (143  , EngineeringUnits.percent_obscuration_per_foot),
    percent_obscuration_per_meter           (144  , EngineeringUnits.percent_obscuration_per_meter),
    percent_per_second                      (99   , EngineeringUnits.percent_per_second),
    per_minute                              (100  , EngineeringUnits.per_minute),
    per_second                              (101  , EngineeringUnits.per_second),
    psi_per_degree_fahrenheit               (102  , EngineeringUnits.psi_per_degree_fahrenheit),
    radians                                 (103  , EngineeringUnits.radians),
    radians_per_second                      (184  , EngineeringUnits.radians_per_second),
    revolutions_per_minute                  (104  , EngineeringUnits.revolutions_per_minute),
    square_meters_per_newton                (185  , EngineeringUnits.square_meters_per_newton),
    watts_per_meter_per_degree_kelvin       (189  , EngineeringUnits.watts_per_meter_per_degree_kelvin),
    watts_per_square_meter_degree_kelvin    (141  , EngineeringUnits.watts_per_square_meter_degree_kelvin),
    per_mille                               (207  , EngineeringUnits.per_mille),
    grams_per_gram                          (208  , EngineeringUnits.grams_per_gram),
    kilograms_per_kilogram                  (209  , EngineeringUnits.kilograms_per_kilogram),
    grams_per_kilogram                      (210  , EngineeringUnits.grams_per_kilogram),
    milligrams_per_gram                     (211  , EngineeringUnits.milligrams_per_gram),
    milligrams_per_kilogram                 (212  , EngineeringUnits.milligrams_per_kilogram),
    grams_per_milliliter                    (213  , EngineeringUnits.grams_per_milliliter),
    grams_per_liter                         (214  , EngineeringUnits.grams_per_liter),
    milligrams_per_liter                    (215  , EngineeringUnits.milligrams_per_liter),
    micrograms_per_liter                    (216  , EngineeringUnits.micrograms_per_liter),
    grams_per_cubic_meter                   (217  , EngineeringUnits.grams_per_cubic_meter),
    milligrams_per_cubic_meter              (218  , EngineeringUnits.milligrams_per_cubic_meter),
    micrograms_per_cubic_meter              (219  , EngineeringUnits.micrograms_per_cubic_meter),
    nanograms_per_cubic_meter               (220  , EngineeringUnits.nanograms_per_cubic_meter),
    grams_per_cubic_centimeter              (221  , EngineeringUnits.grams_per_cubic_centimeter),
    becquerels                              (222  , EngineeringUnits.becquerels),
    kilobecquerels                          (223  , EngineeringUnits.kilobecquerels),
    megabecquerels                          (224  , EngineeringUnits.megabecquerels),
    gray                                    (225  , EngineeringUnits.gray),
    milligray                               (226  , EngineeringUnits.milligray),
    microgray                               (227  , EngineeringUnits.microgray),
    sieverts                                (228  , EngineeringUnits.sieverts),
    millisieverts                           (229  , EngineeringUnits.millisieverts),
    microsieverts                           (230  , EngineeringUnits.microsieverts),
    microsieverts_per_hour                  (231  , EngineeringUnits.microsieverts_per_hour),
    millirems                               (47814, EngineeringUnits.millirems),
    millirems_per_hour                      (47815, EngineeringUnits.millirems_per_hour),
    decibels_a                              (232  , EngineeringUnits.decibels_a),
    nephelometric_turbidity_unit            (233  , EngineeringUnits.nephelometric_turbidity_unit),
    pH                                      (234  , EngineeringUnits.pH),
    grams_per_square_meter                  (235  , EngineeringUnits.grams_per_square_meter),
    minutes_per_degree_kelvin               (236  , EngineeringUnits.minutes_per_degree_kelvin);
    // @formatter:on

    private final int              m_encoding;
    private final EngineeringUnits m_normalizedUnits;

    BACnetEngineeringUnits(int encoding,
                           EngineeringUnits normalizedUnits)
    {
        m_encoding = encoding;
        m_normalizedUnits = normalizedUnits;
    }

    public static BACnetEngineeringUnits parse(String value)
    {
        for (BACnetEngineeringUnits t : values())
        {
            if (t.name()
                 .equalsIgnoreCase(value))
            {
                return t;
            }
        }

        return null;
    }

    @HandlerForDecoding
    public static BACnetEngineeringUnits parse(int value)
    {
        for (BACnetEngineeringUnits t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    @HandlerForEncoding
    public int encoding()
    {
        return m_encoding;
    }

    public EngineeringUnits getNormalizedUnits()
    {
        return m_normalizedUnits;
    }

    @Override
    public int getEncodingValue()
    {
        return m_encoding;
    }
}
