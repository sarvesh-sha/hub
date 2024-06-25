/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn.explorer;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.lang.Unsigned8;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.obdii.pgn.BasePgnObjectModel;
import com.optio3.protocol.model.obdii.pgn.enums.PgnActivationMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnAdaptiveCruiseControlMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnAntiTheftCommand;
import com.optio3.protocol.model.obdii.pgn.enums.PgnColdStartConfiguration;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCommandOverride;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCommandSignal;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCountdownTimer;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCruiseControlEnable;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCruiseControlMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCruiseControlRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCruiseControlStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnDieselExhaustFluid;
import com.optio3.protocol.model.obdii.pgn.enums.PgnDirectionSelector;
import com.optio3.protocol.model.obdii.pgn.enums.PgnDoserValueProtectionRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnEngineStarterMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnEngineState;
import com.optio3.protocol.model.obdii.pgn.enums.PgnExternalBraking;
import com.optio3.protocol.model.obdii.pgn.enums.PgnExternalBrakingStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnFanState;
import com.optio3.protocol.model.obdii.pgn.enums.PgnHaltBrake;
import com.optio3.protocol.model.obdii.pgn.enums.PgnHeaterRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnHeaterStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnHillHolder;
import com.optio3.protocol.model.obdii.pgn.enums.PgnInducementAnomaly;
import com.optio3.protocol.model.obdii.pgn.enums.PgnLaunchGear;
import com.optio3.protocol.model.obdii.pgn.enums.PgnMainLightSwitch;
import com.optio3.protocol.model.obdii.pgn.enums.PgnNOxSensor;
import com.optio3.protocol.model.obdii.pgn.enums.PgnOilLevelIndicator;
import com.optio3.protocol.model.obdii.pgn.enums.PgnParticulateFilterForcedExecution;
import com.optio3.protocol.model.obdii.pgn.enums.PgnParticulateFilterRegeneration;
import com.optio3.protocol.model.obdii.pgn.enums.PgnParticulateFilterRegenerationState;
import com.optio3.protocol.model.obdii.pgn.enums.PgnPowerTakeoffGovernor;
import com.optio3.protocol.model.obdii.pgn.enums.PgnPurging;
import com.optio3.protocol.model.obdii.pgn.enums.PgnResetRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnScrFeedbackControlStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnScrState;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStarterConsent;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStarterRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStarterStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTemperatureRange;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTorqueMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTransmissionOverheat;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTransmissionService;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTransmissionWarning;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTurnSignal;
import com.optio3.protocol.model.obdii.pgn.enums.PgnValveState;
import com.optio3.protocol.model.obdii.pgn.enums.PgnWarningStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnWasherSwitch;
import com.optio3.protocol.model.obdii.pgn.enums.PgnWiperState;
import com.optio3.protocol.model.obdii.pgn.enums.PgnWorkLightSwitch;
import com.optio3.protocol.model.obdii.pgn.sys.BaseSysPgnObjectModel;
import com.optio3.protocol.obdii.J1939Manager;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializablePiece;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationSlotToFields;
import com.optio3.serialization.SerializationValueProcessor;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public final class J1939CodeGen
{
    public static class LogTransport
    {
    }

    public static class LogTypes
    {
    }

    public static final Logger LoggerInstance = new Logger(J1939CodeGen.class);

    //--//

    private final J1939Decoder m_pgnDecoder;

    //--//

    public J1939CodeGen()
    {
        m_pgnDecoder = new J1939Decoder();
    }

    //--//

    public void analyzeTypes()
    {
        for (J1939Manager.PgnMessageClass messageClass : J1939Manager.getPgns()
                                                                     .values())
        {
            if (Reflection.isSubclassOf(BaseSysPgnObjectModel.class, messageClass.clz))
            {
                continue;
            }

            List<SerializationSlotToFields> slots            = SerializationHelper.collectTags(messageClass.clz);
            J1939Decoder.Pgn                desc             = m_pgnDecoder.get(messageClass.pgn);
            int                             offsetCorrection = 0;

            Iterator<J1939Decoder.Spn> it = desc.values.iterator();
            for (SerializationSlotToFields slot : slots)
            {
                SerializablePiece normalPiece = slot.normalPiece;
                if (normalPiece != null)
                {
                    if (!it.hasNext())
                    {
                        throw Exceptions.newRuntimeException("Tag '%s' doesn't correspond to any value in PGN %d", slot, messageClass.pgn);
                    }

                    offsetCorrection = checkTag(offsetCorrection, messageClass, normalPiece, it.next());
                }
                else
                {
                    for (SerializablePiece bitfieldPiece : slot.bitfieldPieces)
                    {
                        if (!it.hasNext())
                        {
                            throw Exceptions.newRuntimeException("Tag '%s' doesn't correspond to any value in PGN %d", slot, messageClass.pgn);
                        }

                        offsetCorrection = checkTag(offsetCorrection, messageClass, bitfieldPiece, it.next());
                    }
                }
            }
        }
    }

    private static int checkTag(int offsetCorrection,
                                J1939Manager.PgnMessageClass messageClass,
                                SerializablePiece bitfieldPiece,
                                J1939Decoder.Spn value)
    {
        J1939Decoder.SpnPosition pos               = value.decodePosition();
        J1939Decoder.SpnScaling  scaling           = value.decodeResolution();
        J1939Decoder.SpnScaling  postScalingOffset = value.decodeValueOffset();

        int bitOffset = pos.bitOffset + offsetCorrection * 8;
        int index     = pos.index - offsetCorrection;

        if (index != bitfieldPiece.tagNumber)
        {
            throw Exceptions.newRuntimeException("Tag '%s' doesn't match index of PGN %d/%s", bitfieldPiece, messageClass.pgn, value.name);
        }

        if (bitfieldPiece.bitOffset == -1)
        {
            if (bitOffset != 0)
            {
                throw Exceptions.newRuntimeException("Tag '%s' doesn't match bit offset of PGN %d/%s", bitfieldPiece, messageClass.pgn, value.name);
            }
        }
        else
        {
            if (bitOffset != bitfieldPiece.bitOffset)
            {
                throw Exceptions.newRuntimeException("Tag '%s' doesn't match bit offset of PGN %d/%s", bitfieldPiece, messageClass.pgn, value.name);
            }
        }

        if (pos.lengthInBits != bitfieldPiece.bitWidth)
        {
            if (bitfieldPiece.isArray)
            {
                // That's okay...
            }
            else
            {
                throw Exceptions.newRuntimeException("Tag '%s' doesn't match width of PGN %d/%s", bitfieldPiece, messageClass.pgn, value.name);
            }
        }

        SerializationScaling annoScaling = bitfieldPiece.scaling;
        if (annoScaling != null)
        {
            if (!isAlmostEqual(annoScaling.scalingFactor(), scaling.value))
            {
                throw Exceptions.newRuntimeException("Tag '%s' doesn't match scaling of PGN %d/%s: %f <-> %f", bitfieldPiece, messageClass.pgn, value.name, annoScaling.scalingFactor(), scaling.value);
            }

            if (!isAlmostEqual(annoScaling.postScalingOffset(), postScalingOffset.value))
            {
                throw Exceptions.newRuntimeException("Tag '%s' doesn't match postScalingOffset of PGN %d/%s: %f <-> %f",
                                                     bitfieldPiece,
                                                     messageClass.pgn,
                                                     value.name,
                                                     annoScaling.postScalingOffset(),
                                                     postScalingOffset.value);
            }
        }

        // If the bitfield spills to the next byte, we need to pull in the next field, so it gets processed in the same slot.
        if (pos.lengthInBits < 8 && pos.bitOffset + pos.lengthInBits > 8)
        {
            offsetCorrection++;
        }

        return offsetCorrection;
    }

    private static boolean isDecimal(double a)
    {
        return !isAlmostEqual(a - Math.round(a), 0);
    }

    private static boolean isAlmostEqual(double a,
                                         double b)
    {
        double diff = Math.abs(a - b);
        if (diff == 0.0)
        {
            return true;
        }

        double scaledDiff = diff / Math.max(Math.abs(a), Math.abs(b));
        return scaledDiff < 1E-6;
    }

    //--//

    @FunctionalInterface
    public interface TypeSourceCodeCallback
    {
        void accept(String className,
                    int pgn,
                    boolean shouldIgnore,
                    String body) throws
                                 Exception;
    }

    private static class FieldType
    {
        final Class<?>       type;
        final boolean        isOptional;
        final TypeDescriptor td;

        Class<? extends SerializationValueProcessor> preProcessorClass;
        double                                       lowerRange;
        double                                       upperRange;

        private FieldType(Class<?> type,
                          boolean isOptional,
                          Class<? extends SerializationValueProcessor> preProcessorClass,
                          double lowerRange,
                          double upperRange)
        {
            this.type = type;
            this.isOptional = isOptional;

            this.preProcessorClass = preProcessorClass;
            this.lowerRange = lowerRange;
            this.upperRange = upperRange;

            this.td = Reflection.getDescriptor(type);
        }
    }

    public void emitTypeSourceCode(TypeSourceCodeCallback callback) throws
                                                                    Exception
    {
        Map<Integer, Class<? extends SerializationValueProcessor>> lookupOutOfRange = Maps.newHashMap();
        Map<Integer, Class<? extends SerializationValueProcessor>> lookupNaN        = Maps.newHashMap();
        Map<Integer, Class<? extends SerializationValueProcessor>> lookupMissing    = Maps.newHashMap();

        lookupOutOfRange.put(4, null);
        lookupOutOfRange.put(8, BasePgnObjectModel.DetectOutOfRange.class);
        lookupOutOfRange.put(16, BasePgnObjectModel.DetectOutOfRange.class);
        lookupOutOfRange.put(32, BasePgnObjectModel.DetectOutOfRange.class);

        lookupNaN.put(4, null);
        lookupNaN.put(8, BasePgnObjectModel.DetectNAN.class);
        lookupNaN.put(16, BasePgnObjectModel.DetectNAN.class);
        lookupNaN.put(32, BasePgnObjectModel.DetectNAN.class);

        lookupMissing.put(4, null);
        lookupMissing.put(8, BasePgnObjectModel.DetectMissing.class);
        lookupMissing.put(16, BasePgnObjectModel.DetectMissing.class);
        lookupMissing.put(32, BasePgnObjectModel.DetectMissing.class);

        //--//

        for (J1939Manager.PgnMessageClass messageClass : J1939Manager.getPgns()
                                                                     .values())
        {
            if (Reflection.isSubclassOf(BaseSysPgnObjectModel.class, messageClass.clz))
            {
                continue;
            }

            J1939Decoder.Pgn desc = m_pgnDecoder.get(messageClass.pgn);
            if (desc == null)
            {
                throw Exceptions.newRuntimeException("Class '%s' with PGN %d doesn't correspond to any definition!", messageClass.clz.getSimpleName(), messageClass.pgn);
            }

            StringBuilder body             = new StringBuilder();
            int           offsetCorrection = 0;

            for (J1939Decoder.Spn value : desc.values)
            {
                final J1939Decoder.SpnPosition pos               = value.decodePosition();
                final J1939Decoder.SpnScaling  scaling           = value.decodeResolution();
                final J1939Decoder.SpnScaling  postScalingOffset = value.decodeValueOffset();
                final String                   fieldName         = extractName(value.name);
                final FieldType                fieldType         = extractType(value, pos, scaling, postScalingOffset, fieldName);

                if (body.length() > 0)
                {
                    body.append("\n\n");
                }

                //--//

                EngineeringUnits unit = scaling.unit;
                if (unit == EngineeringUnits.enumerated && !fieldType.type.isEnum())
                {
                    unit = EngineeringUnits.no_units;
                }

                int debounceSeconds;

                if (unit == EngineeringUnits.enumerated)
                {
                    debounceSeconds = 5;
                }
                else
                {
                    debounceSeconds = 15;
                }

                boolean isWholeWordOrFloat = pos.lengthInBits % 8 == 0 || isDecimal(scaling.value);
                boolean isFloatingType     = fieldType.td != null && fieldType.td.isFloatingType();
                boolean notSupported       = false;

                if (isWholeWordOrFloat && isFloatingType && fieldType.preProcessorClass == null)
                {
                    Map<Integer, Class<? extends SerializationValueProcessor>> lookup;

                    boolean mustHaveValue = (fieldType.type == float.class || fieldType.type == double.class);
                    if (mustHaveValue)
                    {
                        lookup = lookupOutOfRange;
                    }
                    else
                    {
                        lookup = lookupMissing;
                    }

                    if (lookup.containsKey(pos.lengthInBits))
                    {
                        fieldType.preProcessorClass = lookup.get(pos.lengthInBits);

                        if (fieldType.preProcessorClass != null)
                        {
                            fieldType.upperRange = (1L << pos.lengthInBits) - 1;

                            if (unit == EngineeringUnits.kilometers_per_hour)
                            {
                                // Reduce the top of the range a bit.
                                fieldType.upperRange = fieldType.upperRange * 0.975;
                            }
                        }
                    }
                    else
                    {
                        notSupported = true;
                    }
                }

                append(body, "    @FieldModelDescription(description = \"%s\", units = EngineeringUnits.%s, debounceSeconds = %d", escapeQuotedString(value.name), unit.name(), debounceSeconds);

                if (!fieldType.type.isEnum())
                {
                    if (!Double.isNaN(fieldType.upperRange) || !Double.isNaN(fieldType.lowerRange))
                    {
                        append(body, ", noValueMarker = %f", Math.floor(postScalingOffset.value) - 100);
                    }
                }

                append(body, ")\n");

                if (value.slotName != null)
                {
                    switch (value.slotName)
                    {
                        case "SAEatf0007":
                            append(body, "    @SerializationTag(number = %d)\n", pos.index - offsetCorrection);
                            append(body, "    public byte[] %s;\n", fieldName);
                            continue;
                    }
                }

                if (notSupported)
                {
                    throw Exceptions.newRuntimeException("Unsupported length for SPN %d: %d", value.spn, pos.lengthInBits);
                }

                //--//

                append(body, "    @SerializationTag(number = %d, width = %d", pos.index - offsetCorrection, pos.lengthInBits);

                if (isWholeWordOrFloat)
                {
                    if (pos.bitOffset > 0)
                    {
                        append(body, ", bitOffset = %d", pos.bitOffset);
                    }

                    if (isFloatingType)
                    {
                        append(body, ", scaling = { @SerializationScaling(");

                        if (!isAlmostEqual(scaling.value, 1.0))
                        {
                            append(body, "scalingFactor = %s, ", scaling.value);
                        }

                        if (!isAlmostEqual(postScalingOffset.value, 0))
                        {
                            append(body, "postScalingOffset = %s, ", postScalingOffset.value);
                        }

                        append(body, "assumeUnsigned = true) }");
                    }
                }
                else
                {
                    append(body, ", bitOffset = %d", pos.bitOffset + offsetCorrection * 8);

                    if (!fieldType.type.isEnum())
                    {
                        append(body, ", scaling = { @SerializationScaling(");

                        if (!isAlmostEqual(scaling.value, 1.0))
                        {
                            append(body, "scalingFactor = %s, ", scaling.value);
                        }

                        if (!isAlmostEqual(postScalingOffset.value, 0))
                        {
                            append(body, "postScalingOffset = %s, ", postScalingOffset.value);
                        }

                        append(body, "assumeUnsigned = true) }");
                    }

                    // If the bitfield spills to the next byte, we need to pull in the next field, so it gets processed in the same slot.
                    if (pos.lengthInBits < 8 && pos.bitOffset + pos.lengthInBits > 8)
                    {
                        offsetCorrection++;
                    }
                }

                if (fieldType.preProcessorClass != null)
                {
                    append(body, ", preProcessor = %s.class", getNestedName(fieldType.preProcessorClass));

                    if (!Double.isNaN(fieldType.lowerRange))
                    {
                        append(body, ", preProcessorLowerRange = %d", (int) fieldType.lowerRange);
                    }

                    if (!Double.isNaN(fieldType.upperRange))
                    {
                        append(body, ", preProcessorUpperRange = %d", (int) fieldType.upperRange);
                    }
                }

                append(body, ")\n");

                String simpleName = fieldType.type.getSimpleName();

                if (fieldType.isOptional)
                {
                    append(body, "    public Optional<%s> %s;", simpleName, fieldName);
                }
                else
                {
                    append(body, "    public %s %s;", simpleName, fieldName);
                }
            }

            callback.accept(messageClass.clz.getSimpleName(), messageClass.pgn, messageClass.shouldIgnore, body.toString());
        }
    }

    private static String getNestedName(Class<?> clz)
    {
        String simpleName = clz.getSimpleName();

        Class<?> clzEnclosing = clz.getEnclosingClass();
        return clzEnclosing != null ? getNestedName(clzEnclosing) + "." + simpleName : simpleName;
    }

    private String escapeQuotedString(String name)
    {
        return name.replace("\\", "\\\\")
                   .replace("\"", "\\\"");
    }

    private String extractName(String name)
    {
        name = name.replace("'s ", " ")
                   .replace("and/or", "and");

        StringBuilder sb          = new StringBuilder();
        boolean       inSeparator = false;

        for (char c : name.toCharArray())
        {
            if (c < 128)
            {
                switch (c)
                {
                    case ' ':
                    case '-':
                    case '(':
                    case ')':
                    case '/':
                    case '\\':
                    case '"':
                    case '#':
                    case ';':
                    case ':':
                    case ',':
                    case '.':
                        inSeparator = true;
                        break;

                    default:
                        if (inSeparator && !Character.isDigit(c)) // Don't add space before a number.
                        {
                            sb.append("_");
                        }

                        sb.append(c);
                        inSeparator = false;
                        break;
                }
            }
        }

        return sb.toString();
    }

    private FieldType extractType(J1939Decoder.Spn value,
                                  J1939Decoder.SpnPosition pos,
                                  J1939Decoder.SpnScaling scaling,
                                  J1939Decoder.SpnScaling postScalingOffset,
                                  String fieldName)
    {
        switch (fieldName)
        {
            case "Engine_Torque_Mode":
            case "Retarder_Torque_Mode":
                return new FieldType(PgnTorqueMode.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);

            case "Engine_Starter_Mode":
                return new FieldType(PgnEngineStarterMode.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
        }

        if (pos.lengthInBits % 8 == 0 || isDecimal(scaling.value))
        {
            if (fieldName.startsWith("Source_Address_of_"))
            {
                return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 0xFF);
            }

            if (scaling.value != 0 && Math.abs(scaling.value) < 1E-6)
            {
                return new FieldType(double.class, false, null, Double.NaN, Double.NaN);
            }

            if (isAlmostEqual(scaling.value, 0.1) && isAlmostEqual(postScalingOffset.value, -12.5))
            {
                return new FieldType(float.class, false, BasePgnObjectModel.DetectOutOfRange.class, Double.NaN, 250);
            }

            if (isAlmostEqual(scaling.value, 1.0 / 512) && scaling.unit == EngineeringUnits.kilometers_per_liter)
            {
                return new FieldType(float.class, false, BasePgnObjectModel.DetectOutOfRange.class, Double.NaN, 0xFB00);
            }

            return new FieldType(float.class, false, null, Double.NaN, Double.NaN);
        }

        switch (pos.lengthInBits)
        {
            case 2:
            {
                switch (value.spn)
                {
                    case 681:
                    case 682:
                    case 683:
                        return new FieldType(PgnCommandOverride.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);

                    case 1851:
                        //    00b = Inactive; shift is not inhibited
                        //    01b = Active (on continuously); shift is inhibited
                        //    10b = Active (flashing)
                        //    11b = Take no action
                        return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);

                    case 4176:
                        //    00b = Not Blanked
                        //    01b = Blanked
                        //    10b = Error
                        //    11b = Not Supported
                        return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);

                    case 4178:
                        return new FieldType(PgnTransmissionService.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);

                    case 5344:
                        return new FieldType(PgnTransmissionWarning.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);

                    case 5345:
                        return new FieldType(PgnTransmissionOverheat.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);

                    case 5675:
                        //    00b = indicator(s) inactive
                        //    01b = upshift indicator active
                        //    10b = downshift indicator active
                        //    11b = don't care/take no action
                        return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);

                    case 9373:
                        //    00b = Inactive; automatic start is not inhibited
                        //    01b = Active (on continuously); automatic start is inhibited
                        //    10b = Active (flashing)
                        //    11b = Take no action
                        return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00b = Deactivate") && StringUtils.containsIgnoreCase(value.description, "01b = Activate"))
                {
                    return new FieldType(PgnActivationMode.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00b = Take no action"))
                {
                    //    00b = Take no action
                    //    01b = Reset
                    //    10b = Reserved
                    //    11b = Not applicable
                    return new FieldType(PgnCommandSignal.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00b = Do not reset"))
                {
                    //    00b = Do not reset
                    //    01b = Reset
                    //    10b = Reserved
                    //    11b = Take no action
                    return new FieldType(PgnResetRequest.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00b = Fueling not desired"))
                {
                    //    00b = Fueling not desired (shut off engine fueling)
                    //    01b = Fueling desired (keep engine running)
                    //    10b = Parameter supported, but no request
                    //    11b = Don't care / Take no action
                    return new FieldType(PgnActivationMode.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "11b = Heater off"))
                {
                    //    00b = Automatic
                    //    01b = Preheat 2
                    //    10b = Preheat 1
                    //    11b = Heater off
                    return new FieldType(PgnHeaterStatus.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "10b = regeneration needed"))
                {
                    //    00b = not active
                    //    01b = active
                    //    10b = regeneration needed - automatically initiated active regeneration imminent
                    //    11b = not available
                    return new FieldType(PgnParticulateFilterRegenerationState.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "10b = Start requested, automatic type"))
                {
                    //    00b = Start not requested
                    //    01b = Start requested, operator type
                    //    10b = Start requested, automatic type
                    //    11b = Not available
                    return new FieldType(PgnStarterRequest.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00b = Purging not enabled"))
                {
                    //    00b = Purging not enabled
                    //    01b = Purging enabled - less urgent
                    //    10b = Purging enabled - urgent
                    //    11b = Not available
                    return new FieldType(PgnPurging.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00b = Cruise Control is allowed") || fieldName.equals("Cruise_Control_Set_Command"))
                {
                    //    00b = Cruise Control is allowed
                    //    01b = Cruise Control is not allowed
                    //    10b = Reserved
                    //    11b = Don't care/take no action
                    return new FieldType(PgnCruiseControlEnable.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00b = Cruise Control Resume not Requested"))
                {
                    //    00b = Cruise Control Resume not Requested
                    //    01b = Cruise Control Resume Requested
                    //    10b = Reserved
                    //    11b = Don't Care/take no action
                    return new FieldType(PgnCruiseControlRequest.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00b = Any external brake demand"))
                {
                    //    00b = Any external brake demand will be accepted (brake system fully operational)
                    //    01b = Only external brake demand of highest XBR Priority (00b) will be accepted  (e.g. because the temperature limit of the brake system is exceeded)
                    //    10b = No external brake demand will be accepted (e.g. because of fault in brake system)
                    //    11b = not available
                    return new FieldType(PgnExternalBrakingStatus.class, false, null, Double.NaN, Double.NaN);
                }

                boolean hasOn           = false;
                boolean hasOff          = false;
                boolean hasError        = false;
                boolean hasReserved     = false;
                boolean hasNotAvailable = false;
                boolean hasNoAction     = false;

                hasOff |= StringUtils.containsIgnoreCase(value.description, "00b = Off");

                hasOn |= StringUtils.containsIgnoreCase(value.description, "01b = On");

                hasError |= StringUtils.containsIgnoreCase(value.description, "10b = Error");
                hasError |= StringUtils.containsIgnoreCase(value.description, "10b = Fault");

                hasNotAvailable |= StringUtils.containsIgnoreCase(value.description, "11b = Not available");
                hasNotAvailable |= StringUtils.containsIgnoreCase(value.description, "11b = Not availble");
                hasNotAvailable |= StringUtils.containsIgnoreCase(value.description, "11b = Not_available");
                hasNotAvailable |= StringUtils.containsIgnoreCase(value.description, "11b = Unavailable");
                hasNotAvailable |= StringUtils.containsIgnoreCase(value.description, "11b = Trailer ABS Status Information Not Available");

                hasReserved |= StringUtils.containsIgnoreCase(value.description, "10b = Reserved");
                hasReserved |= StringUtils.containsIgnoreCase(value.description, "10b = SAE Reserved");
                hasReserved |= StringUtils.containsIgnoreCase(value.description, "10b = undefined");
                hasReserved |= StringUtils.containsIgnoreCase(value.description, "10b = not defined");

                hasNoAction |= StringUtils.containsIgnoreCase(value.description, "11b = Don't care");
                hasNoAction |= StringUtils.containsIgnoreCase(value.description, "11b = Take No Action");

                if (hasNotAvailable)
                {
                    if (hasOn && hasOff)
                    {
                        return new FieldType(PgnStatusMode.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);
                    }

                    if (hasReserved | hasError)
                    {
                        return new FieldType(PgnStatusMode.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 3);
                    }
                }

                if (hasNoAction)
                {
                    if (hasReserved | hasError)
                    {
                        return new FieldType(PgnActivationMode.class, false, null, Double.NaN, Double.NaN);
                    }
                }

                break;
            }

            case 3:
                switch (value.spn)
                {
                    case 1590:
                        return new FieldType(PgnAdaptiveCruiseControlMode.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);

                    case 1589:
                        //    000b = ACC Distance mode #1 (largest distance)
                        //    001b = ACC Distance mode #2
                        //    010b = ACC Distance mode #3
                        //    011b = ACC Distance mode #4
                        //    100b = ACC Distance mode #5 (shortest distance)
                        //    101b = Conventional cruise control mode
                        //    110b = Error condition
                        //    111b = Not available/not valid
                        return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);

                    case 6566:
                        //    000b = Normal Pressure Range - The transmission supply air pressure is within the normal operational range.
                        //    001b = Low pressure - The transmission supply air pressure is within a range that may reduce the transmission's  performance.
                        //    010b = Very low pressure - The transmission supply air pressure is below a range that will reduce the transmission's performance.
                        //    011b = No pressure - The transmission supply air pressure is below a range that will stop the transmission from functioning.
                        //    100b = Over pressure - The transmission supply air pressure is above a range that may reduce the transmission's performance.
                        //    101b = Not defined
                        //    110b = Error indicator
                        //    111b = Not available
                        return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);

                    case 7696:
                        //    000b = No request received; conditions are such that an Auto-Neutral request would be honored.
                        //    001b = No request received; conditions are such that an Auto-Neutral request would NOT be honored.
                        //    010b = Auto-Neutral request received; shift to Auto-Neutral state is pending.
                        //    011b = Auto-Neutral request received; shift to Auto-Neutral state has been achieved.
                        //    100b = Auto-Neutral request received; shift to Auto-Neutral state is inhibited.
                        //    101b = Reserved
                        //    110b = Error
                        //    111b = Not Available
                        return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "010b = NOx Sensor"))
                {
                    //    000b = Diagnosis not active
                    //    001b = NOx Sensor EEB1h Self Diagnosis active flag
                    //    010b = NOx Sensor EEB1h Self Diagnosis Result Complete
                    //    011b = NOx Sensor EEB1h Self Diagnosis aborted
                    //    100b = NOx Sensor EEB1h Self Diagnosis not possible
                    //    101b = Reserved
                    //    110b = Reserved
                    //    111b = Not Supported
                    return new FieldType(PgnNOxSensor.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "001b = reduction request stage 1"))
                {
                    //    000b = no request
                    //    001b = reduction request stage 1
                    //    010b = reduction request stage 2
                    //    011b = reserved for future assignment by SAE
                    //    100b = reserved for future assignment by SAE
                    //    101b = reserved for future assignment by SAE
                    //    110b = error
                    //    111b = not available
                    return new FieldType(PgnDoserValueProtectionRequest.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "000b = open loop control active"))
                {
                    //    000b = open loop control active
                    //    001b = closed loop control active
                    //    010b = reserved for future assignment by SAE
                    //    011b = reserved for future assignment by SAE
                    //    100b = reserved for future assignment by SAE
                    //    101b = reserved for future assignment by SAE
                    //    110b = error
                    //    111b = not available
                    return new FieldType(PgnScrFeedbackControlStatus.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "001b = On"))
                {
                    //    000b = Off -  indicates adequate DEF level
                    //    001b = On solid -  indicates low DEF level
                    //    010b = reserved for SAE assignment
                    //    011b = reserved for SAE assignment
                    //    100b = On fast blink (1 Hz) indicates the DEF level is lower than the level indicated by the solid illumination (state 001b)
                    //    101b = reserved for SAE assignment
                    //    110b = reserved for SAE assignment
                    //    111b = not available
                    return new FieldType(PgnWarningStatus.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "001b = Inducement Level 1"))
                {
                    //    000b = Driver Warning, Low-Level Inducement, and Severe Inducement Non-Active
                    //    001b = Inducement Level 1 É EPA defined SCR Inducement - DEF Warning  ...................... FMI 15
                    //    010b = Inducement Level 2 É DEF Warning, second level (optional) ...................................... FMI 15
                    //    011b = Inducement Level 3 É EPA defined SCR Inducement - Engine Derate ..................... FMI 16
                    //    100b = Inducement Level 4  ... Severe Inducement Pre-Trigger (optional) É.ÉÉ.................. FMI 16
                    //    101b = Inducement Level 5  ... EPA defined SCR Inducement - Severe Inducement É........  FMI 0
                    //    110b = Temporary Override of Inducement  -  The SCR inducement has been temporarily interrupted.
                    //    111b = Not Available / Not Supported
                    return new FieldType(PgnInducementAnomaly.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "000b = Regeneration not needed"))
                {
                    //    000b = Regeneration not needed
                    //    001b = Regeneration needed - lowest level
                    //    010b = Regeneration needed - moderate level
                    //    011b = Regeneration needed - highest level
                    //    100b = reserved for SAE assignment
                    //    101b = reserved for SAE assignment
                    //    110b = reserved for SAE assignment
                    //    111b = not available
                    return new FieldType(PgnParticulateFilterRegeneration.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "Forced by Service Tool"))
                {
                    //    000b = Not Active
                    //    001b = Active Ð Forced by Switch (See SPN 3696)
                    //    010b = Active Ð Forced by Service Tool
                    //    011b = Reserved for SAE Assignment
                    //    100b = Reserved for SAE Assignment
                    //    101b = Reserved for SAE Assignment
                    //    110b = Reserved for SAE Assignment
                    //    111b = not available
                    return new FieldType(PgnParticulateFilterForcedExecution.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "000b = High Most severe"))
                {
                    //    000b = High Most severe
                    //    001b = High Least severe
                    //    010b = In Range
                    //    011b = Low Least severe
                    //    100b = Low Most severe
                    //    101b = Not Defined
                    //    110b = Error
                    //    111b = Not available
                    return new FieldType(PgnTemperatureRange.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "110b = Halt brake not functional"))
                {
                    //    000b = Inactive
                    //    001b = Active
                    //    010b = Active, but not functioning properly.  (This mode may be used to warn the driver)
                    //    011b to 101b = Not defined
                    //    110b = Halt brake not functional
                    //    111b = Not available
                    return new FieldType(PgnHaltBrake.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "110b = Hill holder not functional"))
                {
                    //    000b = Inactive
                    //    001b = Active
                    //    010b = Active, but will change to inactive in a short time.  (This mode may be used to warn the driver)
                    //    011b = Active, but may activate parking brake if needed.
                    //    100b = Reserved
                    //    101b = Reserved
                    //    110b = Hill holder not functional
                    //    111b = Not available
                    return new FieldType(PgnHillHolder.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "Front Operator Washer Switch"))
                {
                    //    000b = Off
                    //    001b = Low
                    //    010b = Medium
                    //    011b = High
                    //    100b to 110b = Reserved
                    //    111b = Not available (do not change)
                    return new FieldType(PgnWasherSwitch.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "000b = No Cruise Control System Command"))
                {
                    //    000b = No Cruise Control System Command
                    //    001b = Cruise control has been disabled by Cruise Control Disable Command (SPN 5603)
                    //    010b = Cruise control  has been disabled by Cruise Control Pause Command (SPN 5605)
                    //    011b = Cruise control has been re-activated by Cruise Control Resume Command (SPN 5604) and Cruise Control States (SPN 527) is equal to Resume (100b).  Cruise Control System Command State will be equal to 011b as long as Cruise Control States is equal to Resume.  Cruise Control System Command State will change to the appropriate value when Cruise Control States is no longer equal to Resume.
                    //    100b = Cruise control device has received a Cruise Control Resume Command (SPN 5604), but there is no previous set speed.
                    //    101b = Cruise control device has received a Cruise Control Set Command (SPN 9843), cruise control is active, and the cruise control set speed has been set to the current vehicle speed.
                    //    110b = SAE reserved
                    //    111b = Not Supported
                    return new FieldType(PgnCruiseControlStatus.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "010b = Accelerate"))
                {
                    //    000b = Off/Disabled
                    //    001b = Hold
                    //    010b = Accelerate
                    //    011b = Decelerate
                    //    100b = Resume
                    //    101b = Set
                    //    110b = Accelerator Override
                    //    111b = Not available
                    return new FieldType(PgnCruiseControlMode.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "010b = Forward direction selected"))
                {
                    //    000b = No direction selected Ð Park
                    //    001b = No direction selected Ð Neutral
                    //    010b = Forward direction selected
                    //    011b = Reverse direction selected
                    //    100b to 101b = 4 SAE Reserved
                    //    110b = Error
                    //    111b = Not supported
                    return new FieldType(PgnDirectionSelector.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "000b = Starter not commanded"))
                {
                    //    000b = Starter not commanded
                    //    001b = Starter command latched, starter active
                    //    010b = Starter command unlatched, start completed (returns to 0 Ð Starter not commanded after 1000 ms)
                    //    011b = Starter command unlatched, start abort command received from arbitrator (returns to 0 Ð Starter not commanded after 1000 ms)
                    //    100b = Starter command unlatched, start aborted by starter controller (returns to 0 Ð Starter not commanded after 1000 ms).
                    //    101b to 110b = Reserved
                    //    111b = Not available
                    return new FieldType(PgnStarterStatus.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "010b = Consent to automatic start only"))
                {
                    //    000b = No consent
                    //    001b = Consent to operator-requested start only
                    //    010b = Consent to automatic start only
                    //    011b = Consent to both, operator-requested and automatic start
                    //    100b to 110b = Reserved
                    //    111b = Not available
                    return new FieldType(PgnStarterConsent.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                if (StringUtils.containsIgnoreCase(value.description, "000b = Add_Password"))
                {
                    //    000b = Add_Password
                    //    001b = Delete_Password
                    //    010b = Change_Password
                    //    011b = Lock_or_Unlock
                    //    100b = Check_Status
                    //    101b = Login
                    //    110b to 111b = Not defined
                    return new FieldType(PgnAntiTheftCommand.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 7);
                }

                break;

            case 4:
                switch (value.spn)
                {
                    case 2898:
                    case 2899:
                        return new FieldType(PgnColdStartConfiguration.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);

                    case 4255:
                        return new FieldType(PgnLaunchGear.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);

                    case 5445:
                    case 5446:
                    case 5447:
                    case 5448:
                        return new FieldType(PgnValveState.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0001b = Preparing dosing readiness"))
                {
                    //    0000b = Dormant (sleep mode)
                    //    0001b = Preparing dosing readiness (wake up; prepare to operate; wait for start)
                    //    0010b = Normal dosing operation
                    //    0011b = System error pending
                    //    0100b = Purging (SCR dosing system is removing residual DEF from system prior to shutdown)
                    //    0101b = Protect mode against heat  (pressure buildup)
                    //    0110b = Protect mode against cold  (defreeze)
                    //    0111b = Shutoff  (wait for after-run)
                    //    1000b = Diagnosis (after-run)
                    //    1001b = Service test mode, dosing allowed
                    //    1010b = Service test mode, dosing not allowed
                    //    1011b = Ok to Power Down (SCR dosing system has complete housekeeping and power may safely be removed.)
                    //    1100b = Priming (Pressure Buildup)
                    //    1101b = Reserved for future assignment by SAE
                    //    1110b = Error
                    //    1111b = Not available
                    return new FieldType(PgnScrState.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0000b = De-activate auxiliary heater"))
                {
                    //    0000b = De-activate auxiliary heater
                    //    0001b = Off due to ADR per European Regulations for Transport of hazardous materials
                    //    0010b = Economy mode
                    //    0011b = Normal mode
                    //    0100b = Heater pump up-keep
                    //    0101b to 1000b = Reserved
                    //    1001b to 1101b = Not defined
                    //    1110b = Reserved
                    //    1111b = Don't care/take no action
                    return new FieldType(PgnHeaterRequest.class, false, null, Double.NaN, Double.NaN);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0000b = Engine Stopped"))
                {
                    //    0000b = Engine Stopped
                    //    0001b = Pre-Start
                    //    0010b = Starting
                    //    0011b = Warm-Up
                    //    0100b = Running
                    //    0101b = Cool-down
                    //    0110b = Engine Stopping
                    //    0111b = Post-Run
                    //    1000b to 1101b = available for SAE assignment
                    //    1110b = <reserved>
                    //    1111b = not available
                    return new FieldType(PgnEngineState.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0000b = Urea concentration too high"))
                {
                    //    0000b = Urea concentration too high
                    //    0001b = Urea concentration too low
                    //    0010b = Fluid is diesel
                    //    0011b = Diesel exhaust fluid is proper mixture
                    //    0100b to 1011b = Reserved for SAE assignment
                    //    1100b = Diesel exhaust fluid, but concentration cannot be determined
                    //    1101b = Not able to determine fluid property (fluid type unknown)
                    //    1110b = Error with diesel exhaust fluid property detection
                    //    1111b = Not available
                    return new FieldType(PgnDieselExhaustFluid.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0010b = Excessive engine air temperature"))
                {
                    //    0000b = Fan off
                    //    0001b = Engine systemÐGeneral
                    //    0010b = Excessive engine air temperature
                    //    0011b = Excessive engine oil temperature
                    //    0100b = Excessive engine coolant temperature
                    //    0101b = Excessive transmission oil temperature
                    //    0110b = Excessive hydraulic oil temperature
                    //    0111b = Default Operation
                    //    1000b = Reverse Operation
                    //    1001b = Manual control
                    //    1010b = Transmission retarder
                    //    1011b = A/C system
                    //    1100b = Timer
                    //    1101b = Engine brake
                    //    1110b = Other
                    //    1111b = Not available
                    return new FieldType(PgnFanState.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0110b = Mist"))
                {
                    //    0000b = Off
                    //    0001b = Low
                    //    0010b = Medium
                    //    0011b = High
                    //    0100b = Delayed 1 (used for the first delay choice when the wiper switch position controls the delay)
                    //    0101b = Delayed 2 (used for the second delay choice when the wiper switch position controls the delay)
                    //    0110b = Mist (position where external sensor  controls wiper rate)
                    //    0111b to 1110b = Reserved
                    //    1111b = Not available (do not change)
                    return new FieldType(PgnWiperState.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0010b = Right turn to be Flashing"))
                {
                    //    0000b = No Turn being signaled
                    //    0001b = Left Turn to be Flashing
                    //    0010b = Right turn to be Flashing
                    //    0011b to 1101b = Reserved
                    //    1110b = Error (to include both left and right selected simultaneously)
                    //    1111b = Not available (do not change)
                    return new FieldType(PgnTurnSignal.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0000b = less than 1 minute"))
                {
                    //    0000b = less than 1 minute
                    //    0001b = One minute
                    //    0010b = Two minutes
                    //    0011b = Three minutes
                    //    0100b = Four minutes
                    //    0101b = Five minutes
                    //    0110b = Six minutes
                    //    0111b = Seven minutes
                    //    1000b = Eight minutes
                    //    1001b = Nine minutes
                    //    1010b = Ten minutes
                    //    1011b = Eleven minutes
                    //    1100b = Twelve minutes
                    //    1101b = Thirteen minutes
                    //    1110b = Error
                    //    1111b = Not Available
                    return new FieldType(PgnCountdownTimer.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0000b = Conditions valid for transmission oil level measurement"))
                {
                    //    0000b = Conditions valid for transmission oil level measurement
                    //    0001b = Conditions not valid Ð Settling timer still counting down
                    //    0010b = Conditions not valid Ð Transmission in gear
                    //    0011b = Conditions not valid Ð Transmission fluid temperature too low
                    //    0100b = Conditions not valid Ð Transmission fluid temperature too high
                    //    0101b = Conditions not valid Ð Vehicle moving; output shaft speed too high
                    //    0110b = Conditions not valid Ð Vehicle not level
                    //    0111b = Conditions not valid Ð Engine speed too low
                    //    1000b = Conditions not valid Ð Engine speed too high
                    //    1001b = Conditions not valid Ð No request for reading
                    //    1010b = Not defined
                    //    1011b = Not defined
                    //    1100b = Not defined
                    //    1101b = Conditions not valid - Other
                    //    1110b = Error
                    //    1111b = Not available
                    return new FieldType(PgnOilLevelIndicator.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0000b = No brake demand being executed"))
                {
                    //    0000b = No brake demand being executed (default mode)
                    //    0001b = Driver's brake demand being executed, no external brake demand
                    //    0010b = Addition mode of XBR acceleration control being executed
                    //    0011b = Maximum mode of XBR acceleration control being executed
                    //    0100b to 1110b = Reserved for SAE assignment
                    //    1111b = Not available
                    return new FieldType(PgnExternalBraking.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0001b = Work Light Combination"))
                {
                    //    0000b = Off - The position by which the operator selects that none of the work lamps are to be on.
                    //    0001b = Work Light Combination #1 On - The position by which the operator selects that the lamps in the combination defined as Work Light Combination #1 are to be on.
                    //    0010b = Work Light Combination #2 On - The position by which the operator selects that the lamps in the combination defined as Work Light Combination #2 are to be on.
                    //    0011b = Work Light Combination #3 On - The position by which the operator selects that the lamps in the combination defined as Work Light Combination #3 are to be on.
                    //    0100b = Work Light Combination #4 On - The position by which the operator selects that the lamps in the combination defined as Work Light Combination #4 are to be on.
                    //    0101b to 1101b = Reserved
                    //    1110b = Error
                    //    1111b = Not available (do not change)
                    return new FieldType(PgnWorkLightSwitch.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(value.description, "0010b = Headlight On"))
                {
                    //    0000b = Off - The position by which the operator selects that none of the lamps are to be on.
                    //    0001b = Park On - The position by which the operator selects that the park lamps are to be on.
                    //    0010b = Headlight On - The position by which the operator selects that the headlamps are to be on.
                    //    0011b = Headlight and Park On - The position by which the operator selects that Both the Headlamps and the Park lamps are to be on.
                    //    0100b = Automatic Lights - The position by which the operator selects that the Headlamps and Park Lamps turn on and off due to outside ambient light conditions
                    //    0101b = Reserved
                    //    0110b = Reserved
                    //    0111b = Reserved
                    //    1000b = Delayed Off - The position by which the operator selects that a certain set of lamps are to come On and then are to be turned Off following a delay time (Operators Desired - Delayed Lamp Off Time).
                    //    1001b = Reserved
                    //    1010b = Reserved
                    //    1011b = Reserved
                    //    1100b = Reserved
                    //    1101b = Reserved
                    //    1110b = Error
                    //    1111b = Not available (do not change)
                    return new FieldType(PgnMainLightSwitch.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(fieldName, "_Message_Counter"))
                {
                    return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 15);
                }

                if (StringUtils.containsIgnoreCase(fieldName, "_Counter"))
                {
                    return new FieldType(int.class, false, null, Double.NaN, Double.NaN);
                }

                break;

            case 5:
                if (StringUtils.containsIgnoreCase(fieldName, "Preliminary_FMI"))
                {
                    return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 31);
                }

                if (StringUtils.containsIgnoreCase(value.description, "FMI detected"))
                {
                    return new FieldType(Unsigned8.class, true, BasePgnObjectModel.DetectMissing.class, Double.NaN, 31);
                }

                if (StringUtils.containsIgnoreCase(value.description, "00110b = Decelerate/Coast"))
                {
                    //    00000b = Off/Disabled
                    //    00001b = Hold
                    //    00010b = Remote Hold
                    //    00011b = Standby
                    //    00100b = Remote Standby
                    //    00101b = Set
                    //    00110b = Decelerate/Coast
                    //    00111b = Resume
                    //    01000b = Accelerate
                    //    01001b = Accelerator Override
                    //    01010b = Preprogrammed set speed 1
                    //    01011b = Preprogrammed set speed 2
                    //    01100b = Preprogrammed set speed 3
                    //    01101b = Preprogrammed set speed 4
                    //    01110b = Preprogrammed set speed 5
                    //    01111b = Preprogrammed set speed 6
                    //    10000b = Preprogrammed set speed 7
                    //    10001b = Preprogrammed set speed 8
                    //    10010b = PTO set speed memory 1
                    //    10011b = PTO set speed memory 2
                    //    10100b = PTO set speed memory 3
                    //    10101b to 11110b = Not defined
                    //    11111b = Not available
                    return new FieldType(PgnPowerTakeoffGovernor.class, false, BasePgnObjectModel.DetectMissing.class, Double.NaN, 31);
                }

                break;

            case 6:
                if (StringUtils.containsIgnoreCase(fieldName, "_Counter"))
                {
                    return new FieldType(int.class, false, BasePgnObjectModel.DetectOutOfRange.class, Double.NaN, 63);
                }

                break;

            case 8:
            case 16:
            case 32:
                if (!isDecimal(scaling.value) && !isDecimal(postScalingOffset.value))
                {
                    return new FieldType(int.class, false, null, Double.NaN, Double.NaN);
                }
                else
                {
                    return new FieldType(float.class, false, null, Double.NaN, Double.NaN);
                }
        }

        throw Exceptions.newRuntimeException("Unsupported type for SPN %d: %s", value.spn, value.description);
    }

    private static void append(StringBuilder body,
                               String fmt,
                               Object... args)
    {
        body.append(String.format(fmt, args));
    }
}