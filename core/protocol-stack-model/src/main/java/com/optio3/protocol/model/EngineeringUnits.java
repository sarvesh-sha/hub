/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.optio3.cloud.model.IEnumDescription;

public enum EngineeringUnits implements IEnumDescription
{
    // @formatter:off
    enumerated                              ("enumerated"     , null, EngineeringUnitsFamily.Enumerated, null, false),
    onOff                                   ("On/Off"         , null, EngineeringUnitsFamily.Binary    , null, false),
    activeInactive                          ("Active/Inactive", null, EngineeringUnitsFamily.Binary    , null, false),
    constant                                ("Pure Number"    , null, EngineeringUnitsFamily.Constant  , null, false),
    log                                     ("Raw Text"       , null, EngineeringUnitsFamily.Enumerated, null, false),

    // -- Length
    meters                                  ("m" , "Meters"     , EngineeringUnitsFamily.Length, null, false),
    nanometers                              ("nm", "Nanometers" , meters.scaled(1.0 / 1_000_000_000)),
    micrometers                             ("μm", "Micrometers", meters.scaled(1.0 / 1_000_000)),
    millimeters                             ("mm", "Millimeters", meters.scaled(1.0 / 1_000)),
    centimeters                             ("cm", "Centimeters", meters.scaled(1.0 / 100)),
    kilometers                              ("km", "Kilometers" , meters.scaled(1_000)),
    inches                                  ("in", "Inches"     , meters.scaled(0.0254)),
    feet                                    ("ft", "Feet"       , meters.scaled(0.3048)),
    miles                                   ("mi", "Miles"      , meters.scaled(1609.344)),

    // -- Time
    seconds                                 ("sec"        , "Seconds"          , EngineeringUnitsFamily.Time, null, false),
    hundredths_seconds                      ("1/100th sec", "Hundredths second", seconds.scaled(1.0 / 100)),
    milliseconds                            ("msec"       , "Milliseconds"     , seconds.scaled(1.0 / 1_000)),
    minutes                                 ("min"        , "Minutes"          , seconds.scaled(60)),
    hours                                   ("hr"         , "Hours"            , minutes.scaled(60)),
    days                                    ("day"        , "Days"             , hours.scaled(24)),
    weeks                                   ("wk"         , "Weeks"            , days.scaled(7)),
    months                                  ("mon"        , "Months"           , days.scaled(30)),
    years                                   ("yr"         , "Years"            , days.scaled(365)),

    // -- Mass
    grams                                   ("g" , "Grams"     , EngineeringUnitsFamily.Mass, null, false),
    nanograms                               ("ng", "Nanograms" , grams.scaled(1.0 / 1_000_000_000)),
    micrograms                              ("μg", "Micrograms", grams.scaled(1.0 / 1_000_000)),
    milligrams                              ("mg", "Milligrams", grams.scaled(1.0 / 1_000)),
    kilograms                               ("kg", "Kilograms" , grams.scaled(1_000)),
    pounds_mass                             ("lb", "Pounds"    , grams.scaled(453.59237)),
    tons                                    ("t" , "Tons"      , grams.scaled(1_000_000)),

    // -- Velocity
    meters_per_second                       ("m/s"  , "Meters per second", ratio(composedOf(meters), composedOf(seconds), EngineeringUnitsFamily.Velocity)),
    meters_per_minute                       ("m/min", "Meters per minute", meters_per_second.scaled(1.0 / 60)),
    meters_per_hour                         ("m/h"  , "Meters per hour"  , meters_per_second.scaled(1.0 / 3_600)),

    millimeters_per_second                  ("mm/s"  , "Millimeters per second", meters_per_second.scaled(1.0 / 1_000)),
    millimeters_per_minute                  ("mm/min", "Millimeters per minute", meters_per_minute.scaled(1.0 / 1_000)),
    millimeters_per_hour                    ("mm/h"  , "Millimeters per hour"  , meters_per_hour  .scaled(1.0 / 1_000)),

    kilometers_per_hour                     ("km/h"  , "Kilometers per hour", meters_per_hour.scaled(1_000)),
    feet_per_second                         ("ft/s"  , "Feet per second"    , meters_per_second.scaled(0.3048)),
    feet_per_minute                         ("ft/min", "Feet per minute"    , meters_per_minute.scaled(0.3048)),
    miles_per_hour                          ("mph"   , "Miles per hour"     , meters_per_hour.scaled(1609.344)),
    knots                                   ("kts"   , "Knots"              , meters_per_hour.scaled(1852)),

    // -- Position
    longitude                               ("lon"   , "Longitude", EngineeringUnitsFamily.Other, null, false),
    latitude                                ("lat"   , "Latitude" , EngineeringUnitsFamily.Other, null, false),

    // -- Acceleration
    meters_per_second_per_second            ("m/s^2", "Meters per second squared", ratio(composedOf(meters), composedOf(seconds, seconds), EngineeringUnitsFamily.Acceleration)),
    millig                                  ("mg"   , "Milli g", meters_per_second_per_second.scaled(9.81 / 1_000)),

    // -- Volume
    cubic_meters                            ("m^3"   , "Cubic meters", ratio(composedOf(meters, meters, meters), composedOf(), EngineeringUnitsFamily.Volume)),
    cubic_feet                              ("ft^3"  , "Cubic feet"  , ratio(composedOf(feet, feet, feet), composedOf(), EngineeringUnitsFamily.Volume)),
    liters                                  ("l"     , "Liters"      , cubic_meters.scaled(1.0 / 1_000)),
    milliliters                             ("ml"    , "Milliliters" , liters.scaled(1.0 / 1_000)),
    imperial_gallons                        ("UK gal", "UK gallons"  , liters.scaled(4.546092)),
    us_gallons                              ("US gal", "US gallons"  , liters.scaled(3.78541178)),

    // -- Volumetric Flow
    cubic_feet_per_minute                   ("cfm"  , "Cubic feet per minute"      , ratio(composedOf(cubic_feet), composedOf(minutes), EngineeringUnitsFamily.VolumetricFlow)),
    cubic_feet_per_second                   ("cfps" , "Cubic feet per second"      , cubic_feet_per_minute.scaled(60)),
    cubic_feet_per_hour                     ("cfph" , "Cubic feet per hour"        , cubic_feet_per_minute.scaled(1.0 / 60)),
    cubic_feet_per_day                      ("cfpd" , "Cubic feet per day"         , cubic_feet_per_hour.scaled( 1.0 / 24)),
    thousand_cubic_feet_per_day             ("mcfpd", "Thousand Cubic feet per day", cubic_feet_per_day.scaled(1_000)),

    standard_cubic_feet_per_day             ("CFD"  , "Standard Cubic feet per day"           , cubic_feet_per_day.scaled(1.0)),
    million_standard_cubic_feet_per_minute  ("MMCFM", "Million Standard Cubic feet per minute", cubic_feet_per_minute.scaled(1_000_000)),
    million_standard_cubic_feet_per_day     ("MMCFD", "Million Standard Cubic feet per day"   , standard_cubic_feet_per_day.scaled(1_000_000)),
    thousand_standard_cubic_feet_per_day    ("MCFD" , "Thousand Standard Cubic feet per day"  , standard_cubic_feet_per_day.scaled(1_000)),

    cubic_meters_per_minute                 ("m^3/min", "Cubic meters per minute", ratio(composedOf(cubic_meters), composedOf(minutes), EngineeringUnitsFamily.VolumetricFlow)),
    cubic_meters_per_second                 ("m^3/s"  , "Cubic meters per second", cubic_meters_per_minute.scaled(60)),
    cubic_meters_per_hour                   ("m^3/h"  , "Cubic meters per hour"  , cubic_meters_per_minute.scaled(1.0 / 60)),
    cubic_meters_per_day                    ("m^3/day", "Cubic meters per day"   , cubic_meters_per_hour.scaled( 1.0 / 24)),

    liters_per_second                       ("l/s"  , "Liters per second"     , cubic_meters_per_second.scaled(1.0 / 1_000)),
    liters_per_minute                       ("l/min", "Liters per minute"     , cubic_meters_per_minute.scaled(1.0 / 1_000)),
    liters_per_hour                         ("l/h"  , "Liters per hour"       , cubic_meters_per_hour.scaled(1.0 / 1_000)),
    milliliters_per_second                  ("ml/s" , "Milliliters per second", liters_per_second.scaled(1.0 / 1_000)),

    pounds_mass_per_day                     ("lb/day"    , "Pounds per day"       , ratio(composedOf(pounds_mass), composedOf(days), EngineeringUnitsFamily.VolumetricFlow)),
    imperial_gallons_per_minute             ("UK gal/min", "UK gallons per minute", ratio(composedOf(imperial_gallons), composedOf(minutes), EngineeringUnitsFamily.VolumetricFlow)),
    us_gallons_per_minute                   ("US gal/min", "US gallons per minute", ratio(composedOf(us_gallons), composedOf(minutes), EngineeringUnitsFamily.VolumetricFlow)),
    us_gallons_per_hour                     ("US gal/h"  , "US gallons per hour"  , ratio(composedOf(us_gallons), composedOf(hours), EngineeringUnitsFamily.VolumetricFlow)),

    // -- Area
    square_meters                           ("m^2" , "Squared meters"     , ratio(composedOf(meters, meters), composedOf(), EngineeringUnitsFamily.Area)),
    square_centimeters                      ("mm^2", "Squared millimeters", square_meters.scaled(Math.sqrt(0.01))),
    square_feet                             ("ft^2", "Squared feet"       , square_meters.scaled(Math.sqrt(0.3048))),
    square_inches                           ("in^2", "Squared inches"     , square_meters.scaled(Math.sqrt(0.0254))),

    // -- Currency
    currency_dollar_US                      ("$ (US)"  , null, EngineeringUnitsFamily.Currency, null, false),
    currency_dollar_Canadian                ("$ (CA)"  , null, EngineeringUnitsFamily.Currency, null, false),
    currency_euro                           ("€"       , null, EngineeringUnitsFamily.Currency, null, false),
    currency_generic                        ("currency", null, EngineeringUnitsFamily.Currency, null, false),

    // -- Electrical
    amperes                                 ("A" , "Amperes"     , EngineeringUnitsFamily.Electrical, new EngineeringUnitsValidator.Ampere(), false),
    milliamperes                            ("mA", "Milliamperes", amperes.scaled(1.0 / 1_000)),
    kiloamperes                             ("kA", "Kiloamperes" , amperes.scaled(      1_000)),
    megaamperes                             ("MA", "Megaamperes" , amperes.scaled(  1_000_000)),

    volts                                   ("V" , "Volts"     , EngineeringUnitsFamily.Electrical, new EngineeringUnitsValidator.Volt(), false),
    millivolts                              ("mV", "Millivolts", volts.scaled(1.0 / 1_000)),
    kilovolts                               ("kV", "Kilovolts" , volts.scaled(1_000)),
    megavolts                               ("MV", "Megavolts" , volts.scaled(1_000_000)),

    watts                                   ("W" , "Watts"     , ratio(composedOf(volts, amperes), composedOf(), EngineeringUnitsFamily.Power)),
    milliwatts                              ("mW", "Milliwatts", watts.scaled(1.0 / 1_000)),
    kilowatts                               ("kW", "Kilowatts" , watts.scaled(1_000)),
    megawatts                               ("MW", "Megawatts" , watts.scaled(1_000_000)),

    amperes_per_meter                       ("A/m"  , "Amperes per meter"       , ratio(composedOf(amperes)                , composedOf(meters)        , EngineeringUnitsFamily.Electrical)),
    amperes_per_square_meter                ("A/m^2", "Amperes per square meter", ratio(composedOf(amperes)                , composedOf(meters, meters), EngineeringUnitsFamily.Electrical)),
    ampere_square_meters                    ("A m^2", "Amperes squared meters"  , ratio(composedOf(amperes, meters, meters), composedOf()              , EngineeringUnitsFamily.Electrical)),

    ohms                                    ("Ω" , "Ohms"      , ratio(composedOf(volts), composedOf(amperes), EngineeringUnitsFamily.Electrical)),
    milliohms                               ("mΩ", "Milli Ohms", ohms.scaled(1.0 / 1_000)),
    kilohms                                 ("kΩ", "Kilo Ohms" , ohms.scaled(1_000)),
    megohms                                 ("MΩ", "Mega Ohms" , ohms.scaled(1_000_000)),

    ohm_meters                              ("Ω-m"    , "Ohm Meters"                 , ratio(composedOf(ohms, meters), composedOf(), EngineeringUnitsFamily.Electrical)),
    ohm_meter_squared_per_meter             ("Ω-m^2/m", "Ohm Meter Squared per meter", ratio(composedOf(ohms, meters), composedOf(), EngineeringUnitsFamily.Electrical)),

    siemens                                 ("S"    , "Siemens"                     , ratio(composedOf(), composedOf(ohms), EngineeringUnitsFamily.Electrical)),
    millisiemens                            ("mS"   , "Milli Siemens"               , siemens.scaled(1.0 / 1_000)),
    microsiemens                            ("μS"   , "Micro Siemens"               , siemens.scaled(1.0 / 1_000_000)),
    siemens_per_meter                       ("S/m"  , "Siemens per meter"           , ratio(composedOf(siemens), composedOf(meters), EngineeringUnitsFamily.Electrical)),
    microsiemens_per_millimeter             ("μS/mm", "Micro Siemens per millimeter", ratio(composedOf(microsiemens), composedOf(millimeters), EngineeringUnitsFamily.Electrical)),

    decibels                                ("dB"  , "Decibels"           , EngineeringUnitsFamily.Electrical, null, false),
    decibels_milliwatts                     ("dBm" , "Decibels milliwatts", EngineeringUnitsFamily.Electrical, null, false),
    decibels_millivolt                      ("dBmV", "Decibels millivolt" , EngineeringUnitsFamily.Electrical, null, false),
    decibels_volt                           ("dBV" , "Decibels volt"      , EngineeringUnitsFamily.Electrical, null, false),

    farads                                  ("F" , "Farads"      , ratio(composedOf(amperes, seconds), composedOf(volts         ), EngineeringUnitsFamily.Electrical)),
    henrys                                  ("H" , "Henrys"      , ratio(composedOf(volts  , seconds), composedOf(amperes       ), EngineeringUnitsFamily.Electrical)),
    teslas                                  ("T" , "Teslas"      , ratio(composedOf(volts  , seconds), composedOf(meters, meters), EngineeringUnitsFamily.Electrical)),
    webers                                  ("Wb", "Webers"      , ratio(composedOf(volts  , seconds), composedOf(              ), EngineeringUnitsFamily.Electrical)),
    power_factor                            ("PF", "Power factor",                                                                 EngineeringUnitsFamily.Electrical, null, false),

    volt_amperes                            ("VA" , "Volt-Ampere"     , ratio(composedOf(volts,amperes), composedOf(), EngineeringUnitsFamily.Electrical)),
    kilovolt_amperes                        ("kVA", "Kilo Volt-Ampere", volt_amperes.scaled(1_000)),
    megavolt_amperes                        ("MVA", "Mega Volt-Ampere", volt_amperes.scaled(1_000_000)),

    volt_amperes_reactive                   ("var" , "Volt-Ampere Reactive"     , volt_amperes.scaled(1.0)),
    kilovolt_amperes_reactive               ("kvar", "Kilo Volt-Ampere Reactive", kilovolt_amperes.scaled(1.0)),
    megavolt_amperes_reactive               ("Mvar", "Mega Volt-Ampere Reactive", megavolt_amperes.scaled(1.0)),

    volts_per_meter                         ("V/m", "Volt per meter", ratio(composedOf(volts), composedOf(meters), EngineeringUnitsFamily.Electrical)),

    volts_per_degree_kelvin                 ("volts_per_degree_kelvin", "volts_per_degree_kelvin", EngineeringUnitsFamily.Electrical, null, false),
    degrees_phase                           ("degrees_phase"          , "degrees_phase"          , EngineeringUnitsFamily.Electrical, null, false),

    // -- Energy
    ampere_seconds                          ("As", "Ampere-seconds", ratio(composedOf(amperes, seconds), composedOf(), EngineeringUnitsFamily.Energy)),
    ampere_hours                            ("Ah", "Ampere-hours"  , ratio(composedOf(amperes, hours  ), composedOf(), EngineeringUnitsFamily.Energy)),

    volt_ampere_hours                       ("VAh" , "Volt-Ampere hours"     , ratio(composedOf(volts, amperes, hours), composedOf(), EngineeringUnitsFamily.Energy)),
    kilovolt_ampere_hours                   ("kVAh", "Kilo Volt-Ampere hours", volt_ampere_hours.scaled(1_000)),
    megavolt_ampere_hours                   ("MVAh", "Mega Volt-Ampere hours", volt_ampere_hours.scaled(1_000_000)),

    volt_ampere_hours_reactive              ("varh" , "Volt-Ampere Reactive hours"     , volt_ampere_hours.scaled(1.0)),
    kilovolt_ampere_hours_reactive          ("kvarh", "Kilo Volt-Ampere Reactive hours", kilovolt_ampere_hours.scaled(1.0)),
    megavolt_ampere_hours_reactive          ("Mvarh", "Mega Volt-Ampere Reactive hours", megavolt_ampere_hours.scaled(1.0)),

    volt_square_hours                       ("V^2 hr", "Volt square hours"  , ratio(composedOf(volts, volts, hours), composedOf(), EngineeringUnitsFamily.Energy)),
    ampere_square_hours                     ("A^2 hr", "Ampere square hours", ratio(composedOf(amperes, amperes, hours), composedOf(), EngineeringUnitsFamily.Energy)),

    joules                                  ("J" , "Joules"    , ratio(composedOf(watts, seconds), composedOf(), EngineeringUnitsFamily.Energy)),
    kilojoules                              ("kJ", "Kilojoules", joules.scaled(1_000)),
    megajoules                              ("MJ", "Megajoules", joules.scaled(1_000_000)),

    kilojoules_per_kilogram                 ("kJ/kg", "Kilojoules per kilogram", ratio(composedOf(kilojoules), composedOf(kilograms), EngineeringUnitsFamily.Energy)),

    watt_hours                              ("Wh" , "Watt-hours"    , ratio(composedOf(watts, hours), composedOf(), EngineeringUnitsFamily.Energy)),
    kilowatt_hours                          ("kWh", "Kilowatt-hours", watt_hours.scaled(1_000)),
    megawatt_hours                          ("MWh", "Megawatt-hours", watt_hours.scaled(1_000_000)),

    watt_hours_reactive                     ("Whr" , "Watt-hours Reactive"    , watt_hours.scaled(1.0)),
    kilowatt_hours_reactive                 ("kWhr", "Kilowatt-hours Reactive", kilowatt_hours.scaled(1.0)),
    megawatt_hours_reactive                 ("MWhr", "Megawatt-hours Reactive", megawatt_hours.scaled(1.0)),

    btus                                    ("BTU"      , "British thermal unit"     , joules.scaled(1054.3503)),
    kilo_btus                               ("kBTU"     , "Kilo British thermal unit", btus.scaled(1_000)),
    mega_btus                               ("MBTU"     , "Mega British thermal unit", btus.scaled(1_000_000)),
    therms                                  ("thm"      , "Therms"                   , btus.scaled(100_000)),
    ton_hours                               ("Ton-Hours", "Ton-Hours"                , btus.scaled(12000)),

    // -- Enthalpy
    joules_per_kilogram_dry_air             ("J/Kg"      , "Joules per Kilogram dry air"     , ratio(composedOf(joules), composedOf(kilograms), EngineeringUnitsFamily.Enthalpy)),
    kilojoules_per_kilogram_dry_air         ("kJ/Kg"     , "Kilo Joules per Kilogram dry air", joules_per_kilogram_dry_air.scaled(1_000)),
    megajoules_per_kilogram_dry_air         ("MJ/Kg"     , "Mega Joules per Kilogram dry air", joules_per_kilogram_dry_air.scaled(1_000_000)),
    btus_per_pound_dry_air                  ("BTU/lb dry", "BTUs per pound dry air"          , ratio(composedOf(btus), composedOf(pounds_mass), EngineeringUnitsFamily.Enthalpy)),
    btus_per_pound                          ("BTU/lb"    , "BTUs per pound"                  , ratio(composedOf(btus), composedOf(pounds_mass), EngineeringUnitsFamily.Enthalpy)),

    // -- Entropy
    joules_per_degree_kelvin                ("J/K"   , "Joules per Kelvin"         , EngineeringUnitsFamily.Entropy, null, false),
    kilojoules_per_degree_kelvin            ("kJ/K"  , "Kilo Joules per Kelvin"    , joules_per_degree_kelvin.scaled(1_000)),
    megajoules_per_degree_kelvin            ("MJ/K"  , "Mega Joules per Kelvin"    , joules_per_degree_kelvin.scaled(1_000_000)),
    joules_per_kilogram_degree_kelvin       ("J/kg K", "Joules per kilogram Kelvin", EngineeringUnitsFamily.Entropy, null, false),

    // -- Force
    newton                                  ("N", "Newton", ratio(composedOf(kilograms, meters), composedOf(seconds, seconds), EngineeringUnitsFamily.Force)),

    // -- Torque
    newton_meters                           ("N·m", "Newton meters", ratio(composedOf(newton, meters), composedOf(), EngineeringUnitsFamily.Torque)),

    // -- Frequency
    hertz                                   ("Hz" , "Hertz"    , ratio(composedOf(), composedOf(seconds), EngineeringUnitsFamily.Frequency)),
    kilohertz                               ("kHz", "Kilohertz", hertz.scaled(1_000)),
    megahertz                               ("MHz", "Megahertz", hertz.scaled(1_000_000)),

    cycles_per_minute                       ("cycles_per_minute", "Cycles per minute", hertz.scaled(1.0 / 60)),
    cycles_per_hour                         ("cycles_per_hour"  , "Cycles per hour"  , hertz.scaled(1.0 / 3_600)),
    per_hour                                ("per_hour"         , "Per hour"         , cycles_per_hour.scaled(1.0)),
    per_minute                              ("per_minute"       , "Per minute"       , cycles_per_minute.scaled(1.0)),
    per_second                              ("per_second"       , "Per second"       , hertz.scaled(1.0)),

    // -- Humidity
    grams_of_water_per_kilogram_dry_air     ("g H2O / Kg dry air", "Grams of water per kilogram dry air", EngineeringUnitsFamily.Humidity, null, false),
    percent_relative_humidity               ("% humidity"        , "Percent relative humidity"          , EngineeringUnitsFamily.Humidity, new EngineeringUnitsValidator.RelativeHumidity(), false),

    // -- Light
    watts_per_square_foot                   ("W/ft^2" , "Watts per square foot"           , ratio(composedOf(watts)     , composedOf(feet, feet)              , EngineeringUnitsFamily.Light)),
    watts_per_square_meter                  ("W/m^2"  , "Watts per square meter"          , ratio(composedOf(watts)     , composedOf(meters, meters)          , EngineeringUnitsFamily.Light)),
    milliwatts_per_square_centimeter        ("mW/cm^2", "MilliWatts per square centimeter", ratio(composedOf(milliwatts), composedOf(centimeters, centimeters), EngineeringUnitsFamily.Light)),
    candelas                                ("Iv"     , "Candelas"                        ,                                                                     EngineeringUnitsFamily.Light, null, false),
    candelas_per_square_meter               ("Iv/m^2" , "Candelas per square meter"       , ratio(composedOf(candelas)  , composedOf(meters, meters)          , EngineeringUnitsFamily.Light)),
    lumens                                  ("Φv"     , "Lumens"                          ,                                                                     EngineeringUnitsFamily.Light, null, false),
    luxes                                   ("Mv"     , "Luxes"                           ,                                                                     EngineeringUnitsFamily.Light, null, false),
    foot_candles                            ("Iv/ft^2", "Candelas per square foot"        , ratio(composedOf(candelas)  , composedOf(feet, feet)              , EngineeringUnitsFamily.Light)),

    // -- Mass Flow
    grams_per_second                        ("g/s"  , "Grams per second"    , ratio(composedOf(grams)      , composedOf(seconds), EngineeringUnitsFamily.MassFlow)),
    grams_per_minute                        ("g/m"  , "Grams per minute"    , ratio(composedOf(grams)      , composedOf(minutes), EngineeringUnitsFamily.MassFlow)),
    kilograms_per_second                    ("kg/s" , "Kilograms per second", ratio(composedOf(kilograms)  , composedOf(seconds), EngineeringUnitsFamily.MassFlow)),
    kilograms_per_minute                    ("kg/m" , "Kilograms per minute", ratio(composedOf(kilograms)  , composedOf(minutes), EngineeringUnitsFamily.MassFlow)),
    kilograms_per_hour                      ("kg/h" , "Kilograms per hour"  , ratio(composedOf(kilograms)  , composedOf(hours)  , EngineeringUnitsFamily.MassFlow)),
    pounds_mass_per_second                  ("lb/s" , "Pounds per second"   , ratio(composedOf(pounds_mass), composedOf(seconds), EngineeringUnitsFamily.MassFlow)),
    pounds_mass_per_minute                  ("lb/m" , "Pounds per minute"   , ratio(composedOf(pounds_mass), composedOf(minutes), EngineeringUnitsFamily.MassFlow)),
    pounds_mass_per_hour                    ("lb/h" , "Pounds per hour"     , ratio(composedOf(pounds_mass), composedOf(hours)  , EngineeringUnitsFamily.MassFlow)),
    tons_per_hour                           ("ton/h", "Tons per hour"       , ratio(composedOf(tons)       , composedOf(hours)  , EngineeringUnitsFamily.MassFlow)),

    // -- Power
    btus_per_hour                           ("BTU/h" , "BTUs per hour"     , ratio(composedOf(btus)  , composedOf(hours), EngineeringUnitsFamily.Power)),
    kilo_btus_per_hour                      ("kBTU/h", "Kilo BTUs per hour", btus.scaled(1_000)),
    joule_per_hours                         ("J/h"   , "Joules per hour"   , ratio(composedOf(joules), composedOf(hours), EngineeringUnitsFamily.Power)),
    horsepower                              ("hp"    , "Horsepower"        , watts.scaled(735.5)),
    tons_refrigeration                      ("TR"    , "Tons refrigeration", btus_per_hour.scaled(12000)),

    // -- Pressure
    pascals                                 ("Pa"   , "Pascals"                    , ratio(composedOf(newton), composedOf(meters, meters), EngineeringUnitsFamily.Pressure)),
    hectopascals                            ("hPa"  , "Hectopascals"               , pascals.scaled(100)),
    kilopascals                             ("kPa"  , "Kilopascals"                , pascals.scaled(1_000)),
    megapascals                             ("MPa"  , "Megapascals"                , pascals.scaled(1_000_000)),
    millibars                               ("mbar" , "Millibars"                  , pascals.scaled(100)),
    bars                                    ("bar"  , "Bars"                       , pascals.scaled(100_000)),
    pounds_force_per_square_inch            ("psi"  , "pound-force per square inch", pascals.scaled(6_894.757)),
    millimeters_of_water                    ("mmH2O", "Millimeters of water"       , pascals.scaled(9.80665)),
    centimeters_of_water                    ("cmH2O", "Centimeters of water"       , pascals.scaled(98.0665)),
    inches_of_water                         ("inH2O", "Inches of water"            , pascals.scaled(248.84)),
    millimeters_of_mercury                  ("mmHg" , "Millimeters of mercury"     , pascals.scaled(133.3224)),
    centimeters_of_mercury                  ("mmHg" , "Millimeters of mercury"     , pascals.scaled(1333.224)),
    inches_of_mercury                       ("inHg" , "Inches of mercury"          , pascals.scaled(3386.389)),

    // -- Temperature
    degrees_celsius                         ("°C", "Celsius"   , EngineeringUnitsFamily.Temperature, new EngineeringUnitsValidator.Temperature(), false),
    degrees_kelvin                          ("K" , "Kelvin"    , degrees_celsius.scaled(1.0, -272.15, 0.0)),
    degrees_fahrenheit                      ("°F", "Fahrenheit", degrees_celsius.scaled(5.0 / 9.0, -32, 0.0)),

    degrees_kelvin_per_hour                 ("K/h" , "Kelvin per hour"      , ratio(composedOf(degrees_kelvin    ), composedOf(hours         ), EngineeringUnitsFamily.Temperature)),
    degrees_kelvin_per_minute               ("K/m" , "Kelvin per minute"    , ratio(composedOf(degrees_kelvin    ), composedOf(minutes       ), EngineeringUnitsFamily.Temperature)),
    degrees_celsius_per_hour                ("°C/h", "Celsius per hour"     , ratio(composedOf(degrees_celsius   ), composedOf(hours         ), EngineeringUnitsFamily.Temperature)),
    degrees_celsius_per_minute              ("°C/m", "Celsius per minute"   , ratio(composedOf(degrees_celsius   ), composedOf(minutes       ), EngineeringUnitsFamily.Temperature)),
    degrees_fahrenheit_per_hour             ("°F/h", "Fahrenheit per hour"  , ratio(composedOf(degrees_fahrenheit), composedOf(hours         ), EngineeringUnitsFamily.Temperature)),
    degrees_fahrenheit_per_minute           ("°F/m", "Fahrenheit per minute", ratio(composedOf(degrees_fahrenheit), composedOf(minutes       ), EngineeringUnitsFamily.Temperature)),

    degree_days_celsius                     ("degree_days_celsius"   , null, EngineeringUnitsFamily.Temperature, null, false),
    degree_days_fahrenheit                  ("degree_days_fahrenheit", null, degree_days_celsius.scaled(5.0 / 9.0)),

    delta_degrees_kelvin                    ("Δ K" , "Delta Kelvin"    , EngineeringUnitsFamily.Temperature, null, false),
    delta_degrees_fahrenheit                ("Δ °F", "Delta Fahrenheit", delta_degrees_kelvin.scaled(5.0 / 9.0)),

    // -- Other

    no_units                                ("<no unit>", "<no unit>"                        , EngineeringUnitsFamily.Other, new EngineeringUnitsValidator.LimitRange(-1E30, 1E30), true),
    ticks                                   ("<ticks>"  , "Setting"                          , EngineeringUnitsFamily.Other, new EngineeringUnitsValidator.LimitRange(-1E30, 1E30), true),
    counts                                  ("<counts>" , "Counts"                           , EngineeringUnitsFamily.Other, new EngineeringUnitsValidator.LimitRange(-1E30, 1E30), true),
    bytes                                   ("bytes"    , "Bytes"                            , EngineeringUnitsFamily.Other, new EngineeringUnitsValidator.LimitRange(    0, 1E30), true),
    kilo_bytes                              ("kilobytes", "Kilobytes"                        , bytes     .scaled(1024)                                                  ),
    mega_bytes                              ("megabytes", "Megabytes"                        , kilo_bytes.scaled(1024)                                                  ),
    giga_bytes                              ("gigabytes", "Gigabytes"                        , mega_bytes.scaled(1024)                                                  ),
    tera_bytes                              ("terabytes", "Terabytes"                        , giga_bytes.scaled(1024)                                                  ),

    degrees_angular                         ("°"        , "Degree angular"                   ,                                                         EngineeringUnitsFamily.Other , null, false),
    radians                                 ("rad"      , "Radians"                          , degrees_angular.scaled(180 / Math.PI)                                    ),
    degrees_angular_per_second              ("dps"      , "Degree per second"                , ratio(composedOf(degrees_angular), composedOf(seconds), EngineeringUnitsFamily.Other)),
    radians_per_second                      ("rad/s"    , "Radians per second"               , ratio(composedOf(radians), composedOf(seconds),         EngineeringUnitsFamily.Other)),
    mole_percent                            ("xi"       , "Mole percent"                     ,                                                         EngineeringUnitsFamily.Other , null, false),
    percent                                 ("%"        , "Percent"                          ,                                                         EngineeringUnitsFamily.Other , null, false),
    per_mille                               ("‰"        , "Per mille"                        , percent.scaled(1.0 / 10)                                                 ),
    percent_per_second                      ("%/s"      , "Percent per second"               , ratio(composedOf(percent), composedOf(seconds),         EngineeringUnitsFamily.Other)),
    percent_obscuration_per_meter           ("% obs/m"  , "Percent obscuration per meter"    , ratio(composedOf(percent), composedOf(meters ),         EngineeringUnitsFamily.Other)),
    percent_obscuration_per_foot            ("% obs/ft" , "Percent obscuration per foot"     , ratio(composedOf(percent), composedOf(feet   ),         EngineeringUnitsFamily.Other)),
    parts_per_million                       ("ppm"      , "Parts per million"                ,                                                         EngineeringUnitsFamily.Other , null, false),
    parts_per_billion                       ("ppb"      , "Parts per billion"                , parts_per_million.scaled(1.0 / 1_000)                                    ),
    revolutions_per_minute                  ("RPM"      , "Revolutions per minute"           , hertz.scaled(1.0 / 60)                                                   ),

    joule_seconds                           ("J·s"      , "Joule-seconds"                    , ratio(composedOf(joules, seconds ), composedOf(                                     ), EngineeringUnitsFamily.Other)),
    newton_seconds                          ("N·s"      , "Newton-seconds"                   , ratio(composedOf(newton, seconds ), composedOf(                                     ), EngineeringUnitsFamily.Other)),
    newtons_per_meter                       ("N/m"      , "Newton per meter"                 , ratio(composedOf(newton          ), composedOf(meters                               ), EngineeringUnitsFamily.Other)),
    pascal_seconds                          ("Pa·s"     , "Pascal-seconds"                   , ratio(composedOf(pascals, seconds), composedOf(                                     ), EngineeringUnitsFamily.Other)),
    psi_per_degree_fahrenheit               ("psi/°F"   , "PSI per Degree Fahrenheit"        ,                                                                                        EngineeringUnitsFamily.Other , null, false),
    kilograms_per_cubic_meter               ("Kg/m^3"   , "Kilograms per cubic meter"        , ratio(composedOf(kilograms       ), composedOf(meters, meters, meters               ), EngineeringUnitsFamily.Other)),
    kilometers_per_liter                    ("km/L"     , "Kilometers per liter"             , ratio(composedOf(kilometers      ), composedOf(liters                               ), EngineeringUnitsFamily.Other)),
    miles_per_us_gallon                     ("mpg"      , "Miles per gallon"                 , ratio(composedOf(miles           ), composedOf(us_gallons                           ), EngineeringUnitsFamily.Other)),
    kilowatts_per_ton                       ("kW/ton"   , "Kilowatts per ton"                , ratio(composedOf(kilowatts       ), composedOf(tons                                 ), EngineeringUnitsFamily.Other)),
    kilowatt_hours_per_square_meter         ("KWh/m^2"  , "Kilowatt-hours per square meter"  , ratio(composedOf(kilowatt_hours  ), composedOf(meters, meters                       ), EngineeringUnitsFamily.Other)),
    kilowatt_hours_per_square_foot          ("KWh/ft^2" , "Kilowatt-hours per square foot"   , ratio(composedOf(kilowatt_hours  ), composedOf(feet, feet                           ), EngineeringUnitsFamily.Other)),
    watt_hours_per_cubic_meter              ("Wh/m^3"   , "Watt-hours per cubic meter"       , ratio(composedOf(watt_hours      ), composedOf(meters, meters, meters               ), EngineeringUnitsFamily.Other)),
    joules_per_cubic_meter                  ("J/m^3"    , "Joules per cubic meter"           , ratio(composedOf(joules          ), composedOf(meters, meters, meters               ), EngineeringUnitsFamily.Other)),
    megajoules_per_square_meter             ("MJ/m^2"   , "Megajoules per square meter"      , ratio(composedOf(megajoules      ), composedOf(meters, meters                       ), EngineeringUnitsFamily.Other)),
    megajoules_per_square_foot              ("MJ/m^2"   , "Megajoules per square foot"       , ratio(composedOf(megajoules      ), composedOf(feet, feet                           ), EngineeringUnitsFamily.Other)),
    square_meters_per_newton                ("m^2/N"    , "Square meters per newton"         , ratio(composedOf(meters, meters  ), composedOf(newton                               ), EngineeringUnitsFamily.Other)),
    watts_per_meter_per_degree_kelvin       ("W/m·K"    , "Watts per meter per Kelvin"       ,                                                                                        EngineeringUnitsFamily.Other , null, false),
    watts_per_square_meter_degree_kelvin    ("W/m^2·K"  , "Watts per square meter per Kelvin",                                                                                        EngineeringUnitsFamily.Other , null, false),
    grams_per_gram                          ("gr/gr"    , "Grams per gram"                   , ratio(composedOf(grams           ), composedOf(grams                                ), EngineeringUnitsFamily.Other)),
    kilograms_per_kilogram                  ("kg/kg"    , "Kilograms per kilogram"           , ratio(composedOf(kilograms       ), composedOf(kilograms                            ), EngineeringUnitsFamily.Other)),
    grams_per_kilogram                      ("gr/kg"    , "Grams per kilogram"               , ratio(composedOf(grams           ), composedOf(kilograms                            ), EngineeringUnitsFamily.Other)),
    milligrams_per_gram                     ("mg/gr"    , "Milligrams per gram"              , ratio(composedOf(milligrams      ), composedOf(grams                                ), EngineeringUnitsFamily.Other)),
    milligrams_per_kilogram                 ("mg/kg"    , "Milligrams per kilogram"          , ratio(composedOf(milligrams      ), composedOf(kilograms                            ), EngineeringUnitsFamily.Other)),
    grams_per_milliliter                    ("gr/ml"    , "Grams per milliliter"             , ratio(composedOf(grams           ), composedOf(milliliters                          ), EngineeringUnitsFamily.Other)),
    grams_per_liter                         ("gr/l"     , "Grams per liter"                  , ratio(composedOf(grams           ), composedOf(liters                               ), EngineeringUnitsFamily.Other)),
    grams_per_square_meter                  ("gr/m^2"   , "Grams per square meter"           , ratio(composedOf(grams           ), composedOf(meters, meters                       ), EngineeringUnitsFamily.Other)),
    milligrams_per_liter                    ("mg/l"     , "Milligrams per liter"             , ratio(composedOf(milligrams      ), composedOf(liters                               ), EngineeringUnitsFamily.Other)),
    micrograms_per_liter                    ("μg/l"     , "Micrograms per liter"             , ratio(composedOf(micrograms      ), composedOf(liters                               ), EngineeringUnitsFamily.Other)),
    grams_per_cubic_meter                   ("gr/m^3"   , "Grams per cubic meter"            , ratio(composedOf(grams           ), composedOf(meters, meters, meters               ), EngineeringUnitsFamily.Other)),
    milligrams_per_cubic_meter              ("mg/m^3"   , "Milligrams per cubic meter"       , ratio(composedOf(milligrams      ), composedOf(meters, meters, meters               ), EngineeringUnitsFamily.Other)),
    micrograms_per_cubic_meter              ("μg/m^3"   , "Micrograms per cubic meter"       , ratio(composedOf(micrograms      ), composedOf(meters, meters, meters               ), EngineeringUnitsFamily.Other)),
    nanograms_per_cubic_meter               ("ng/m^3"   , "Nanograms per cubic meter"        , ratio(composedOf(nanograms       ), composedOf(meters, meters, meters               ), EngineeringUnitsFamily.Other)),
    grams_per_cubic_centimeter              ("gr/cm^3"  , "Grams per cubic centimeter"       , ratio(composedOf(grams           ), composedOf(centimeters, centimeters, centimeters), EngineeringUnitsFamily.Other)),
    becquerels                              ("Bq"       , "Becquerels"                       ,                                                                                        EngineeringUnitsFamily.Other , null, false),
    kilobecquerels                          ("kBq"      , "Kilo Becquerels"                  , becquerels.scaled(1_000)                                                                                            ),
    megabecquerels                          ("MBq"      , "Mega Becquerels"                  , becquerels.scaled(1_000_000)                                                                                        ),
    gray                                    ("Gy"       , "Gray"                             , ratio(composedOf(joules          ), composedOf(kilograms                            ), EngineeringUnitsFamily.Other)),
    milligray                               ("mGy"      , "Milligray"                        , gray.scaled(1.0 / 1_000)                                                                                            ),
    microgray                               ("μGy"      , "Microgray"                        , gray.scaled(1.0 / 1_000_000)                                                                                        ),
    sieverts                                ("Sv"       , "Sieverts"                         ,                                                                                        EngineeringUnitsFamily.Other , null, false),
    millisieverts                           ("mSv"      , "Millisieverts"                    , sieverts.scaled(1.0 / 1_000)                                                                                        ),
    microsieverts                           ("μSv"      , "Microsieverts"                    , sieverts.scaled(1.0 / 1_000_000)                                                                                    ),
    microsieverts_per_hour                  ("μSv/h"    , "Microsieverts per hour"           , ratio(composedOf(microsieverts   ), composedOf(hours                                ), EngineeringUnitsFamily.Other)),
    millirems                               ("mrem"     , "Millirems"                        ,                                                                                        EngineeringUnitsFamily.Other , null, false),
    millirems_per_hour                      ("mrem/h"   , "Millirems per hour"               , ratio(composedOf(millirems       ), composedOf(hours                                ), EngineeringUnitsFamily.Other)),
    decibels_a                              ("dB(A)"    , "Decibel A"                        ,                                                                                        EngineeringUnitsFamily.Other , null, false),
    nephelometric_turbidity_unit            ("FNU"      , "Nephelometric Turbidity Unit"     ,                                                                                        EngineeringUnitsFamily.Other , null, false),
    pH                                      ("pH"       , "pH"                               ,                                                                                        EngineeringUnitsFamily.Other , null, false),
    minutes_per_degree_kelvin               ("min/K"    , "Minutes per Kelvin"               ,                                                                                        EngineeringUnitsFamily.Other , null, false);
    // @formatter:on

    private final String                            m_displayName;
    private final String                            m_description;
    private final EngineeringUnitsFamily            m_family;
    private final EngineeringUnitsValidator.Factory m_validator;
    private final EngineeringUnitsFactors           m_conversion;

    EngineeringUnits(String displayName,
                     String description,
                     EngineeringUnitsFamily family,
                     EngineeringUnitsValidator.Factory validator,
                     boolean dimensionLess)
    {
        m_displayName = displayName;
        m_description = description;
        m_family      = family;
        m_validator   = validator;
        m_conversion  = new EngineeringUnitsFactors(this, dimensionLess);
    }

    EngineeringUnits(String displayName,
                     String description,
                     Ratio ratio)
    {
        m_displayName = displayName;
        m_description = description;
        m_family      = ratio.family;
        m_validator   = null;
        m_conversion  = ratio.simplify(this);
    }

    EngineeringUnits(String displayName,
                     String description,
                     Scaled scaled)
    {
        m_displayName = displayName;
        m_description = description;
        m_family      = scaled.unit.getFamily();
        m_validator   = scaled.unit.getValidator();
        m_conversion  = scaled.simplify(this);
    }

    @Override
    public String getDisplayName()
    {
        return m_displayName;
    }

    @Override
    public String getDescription()
    {
        return m_description;
    }

    //--//

    public static EngineeringUnits parse(String value)
    {
        for (EngineeringUnits t : values())
        {
            if (t.name()
                 .equalsIgnoreCase(value))
            {
                return t;
            }
        }

        return null;
    }

    public EngineeringUnitsFamily getFamily()
    {
        return m_family;
    }

    public EngineeringUnitsFactors getConversionFactors()
    {
        return m_conversion;
    }

    @JsonIgnore
    public EngineeringUnitsValidator.Factory getValidator()
    {
        return m_validator;
    }

    //--//

    public boolean isEquivalent(EngineeringUnits units)
    {
        return Objects.equals(getConversionFactors(), units.getConversionFactors());
    }

    public static EngineeringUnitsConverter buildConverter(EngineeringUnits fromUnit,
                                                           EngineeringUnits toUnit)
    {
        if (fromUnit.isEquivalent(toUnit))
        {
            return buildConverter(fromUnit.getConversionFactors(), toUnit.getConversionFactors());
        }

        return EngineeringUnitsConverter.NoOp;
    }

    public static EngineeringUnitsConverter buildConverter(EngineeringUnitsFactors fromUnit,
                                                           EngineeringUnitsFactors toUnit)
    {
        if (fromUnit == null || toUnit == null)
        {
            return EngineeringUnitsConverter.NoOp;
        }

        fromUnit = fromUnit.simplify();
        toUnit   = toUnit.simplify();

        EngineeringUnitsFactors.Scaling fromScaling = fromUnit.scaling;
        EngineeringUnitsFactors.Scaling toScaling   = toUnit.scaling;

        if (fromScaling.equals(toScaling))
        {
            return EngineeringUnitsConverter.NoOp;
        }

        return new EngineeringUnitsConverter()
        {
            @Override
            public boolean isIdentityTransformation()
            {
                return false;
            }

            @Override
            public double convert(double value)
            {
                if (!Double.isNaN(value))
                {
                    double normalizedValue = fromScaling.convertTo(value);

                    return toScaling.convertFrom(normalizedValue);
                }

                return value;
            }
        };
    }

    public static double convert(double value,
                                 EngineeringUnits fromUnit,
                                 EngineeringUnits toUnit)
    {
        return buildConverter(fromUnit, toUnit).convert(value);
    }

    public static double convert(double value,
                                 EngineeringUnitsFactors fromUnit,
                                 EngineeringUnitsFactors toUnit)
    {
        return buildConverter(fromUnit, toUnit).convert(value);
    }

    //--//

    private Scaled scaled(double scalingFactor)
    {
        return scaled(scalingFactor, 0, 0);
    }

    private Scaled scaled(double scalingFactor,
                          double preScalingOffset,
                          double postScalingOffset)
    {
        return new Scaled(this, scalingFactor, preScalingOffset, postScalingOffset);
    }

    public static class Scaled
    {
        final EngineeringUnits unit;
        final double           scalingFactor;
        final double           preScalingOffset;
        final double           postScalingOffset;

        public Scaled(EngineeringUnits unit,
                      double scalingFactor,
                      double preScalingOffset,
                      double postScalingOffset)
        {
            this.unit              = unit;
            this.scalingFactor     = scalingFactor;
            this.preScalingOffset  = preScalingOffset;
            this.postScalingOffset = postScalingOffset;
        }

        public EngineeringUnitsFactors simplify(EngineeringUnits primaryHints)
        {
            EngineeringUnitsFactors sub = unit.getConversionFactors();

            double multiplier = scalingFactor;
            double offset     = (preScalingOffset * scalingFactor + postScalingOffset);

            EngineeringUnitsFactors.Scaling scaling = EngineeringUnitsFactors.Scaling.build(multiplier, offset);

            return new EngineeringUnitsFactors(scaling.multiplyBy(sub.scaling), sub.numeratorUnits, sub.denominatorUnits, primaryHints, true);
        }
    }

    //--//

    private static EngineeringUnits[] composedOf(EngineeringUnits... units)
    {
        return units != null && units.length > 0 ? units : new EngineeringUnits[0];
    }

    private static Ratio ratio(EngineeringUnits[] numeratorUnits,
                               EngineeringUnits[] denominatorUnits,
                               EngineeringUnitsFamily family)
    {
        return ratio(numeratorUnits, denominatorUnits, family, 1.0);
    }

    private static Ratio ratio(EngineeringUnits[] numeratorUnits,
                               EngineeringUnits[] denominatorUnits,
                               EngineeringUnitsFamily family,
                               double scalingFactor)
    {
        return new Ratio(numeratorUnits, denominatorUnits, family, scalingFactor);
    }

    public static class Ratio
    {
        final EngineeringUnits[]     numeratorUnits;
        final EngineeringUnits[]     denominatorUnits;
        final EngineeringUnitsFamily family;
        final double                 scalingFactor;

        public Ratio(EngineeringUnits[] numeratorUnits,
                     EngineeringUnits[] denominatorUnits,
                     EngineeringUnitsFamily family,
                     double scalingFactor)
        {
            this.numeratorUnits   = numeratorUnits;
            this.denominatorUnits = denominatorUnits;
            this.family           = family;
            this.scalingFactor    = scalingFactor;
        }

        public EngineeringUnitsFactors simplify(EngineeringUnits primaryHints)
        {
            EngineeringUnitsFactors.Scaling scalingNumerator   = EngineeringUnitsFactors.Scaling.Identity;
            EngineeringUnitsFactors.Scaling scalingDenominator = EngineeringUnitsFactors.Scaling.Identity;
            List<EngineeringUnits>          numeratorUnits     = Lists.newArrayList();
            List<EngineeringUnits>          denominatorUnits   = Lists.newArrayList();

            for (EngineeringUnits numeratorUnit : this.numeratorUnits)
            {
                EngineeringUnitsFactors sub = numeratorUnit.getConversionFactors();

                scalingNumerator = scalingNumerator.multiplyBy(sub.scaling);

                numeratorUnits.addAll(sub.numeratorUnits);
                denominatorUnits.addAll(sub.denominatorUnits);
            }

            for (EngineeringUnits denominatorUnit : this.denominatorUnits)
            {
                EngineeringUnitsFactors sub = denominatorUnit.getConversionFactors();

                scalingDenominator = scalingDenominator.multiplyBy(sub.scaling);

                // Flip the units, since this was at the denominator.
                numeratorUnits.addAll(sub.denominatorUnits);
                denominatorUnits.addAll(sub.numeratorUnits);
            }

            //
            // Simplify ratios
            //
            for (Iterator<EngineeringUnits> it = numeratorUnits.iterator(); it.hasNext(); )
            {
                EngineeringUnits unit = it.next();
                int              pos  = denominatorUnits.indexOf(unit);
                if (pos >= 0)
                {
                    denominatorUnits.remove(pos);
                    it.remove();
                }
            }

            return new EngineeringUnitsFactors(scalingNumerator.divideBy(scalingDenominator), numeratorUnits, denominatorUnits, primaryHints, true);
        }
    }
}
