/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(FIELD)
public @interface FieldModelDescription
{
    String description() default "";

    String notes() default "";

    EngineeringUnits units();

    /**
     * The max frequency for these samples.
     *
     * @return resolution flag
     */
    FieldTemporalResolution temporalResolution() default FieldTemporalResolution.Max1Hz;

    /**
     * Max digits of error when storing value in a time series.
     *
     * @return number of digits
     */
    int digitsOfPrecision() default 0;

    /**
     * Whether the field is event-based or time-based.
     *
     * @return true if samples are generated at a fixed rate
     */
    boolean fixedRate() default false;

    /**
     * For event-based fields, whether to flush samples to the hub on change.
     *
     * @return true if samples should be flushed on change.
     */
    boolean flushOnChange() default false;

    /**
     * Minimum change in the value that causes an event.
     *
     * @return delta
     */
    double minimumDelta() default 0.0;

    /**
     * Number of seconds to debounce a state change.
     *
     * @return debounce delay
     */
    int debounceSeconds() default 0;

    /**
     * When debouncing, which state to prioritize
     *
     * @return FieldChangeMode
     */
    FieldChangeMode stickyMode() default FieldChangeMode.None;

    /**
     * If we want to present a sample as a different type
     *
     * @return Type of the desired value representation
     */
    Class<?> desiredTypeForSamples() default Object.class;

    /**
     * If we want to encode a value that means there's no input to a field
     *
     * @return The value that acts as a marker for "no input"
     */
    double noValueMarker() default Double.NaN;

    //--//

    /**
     * If present, the classification for this field
     *
     * @return WellKnownPointClass
     */
    WellKnownPointClass pointClass() default WellKnownPointClass.None;

    /**
     * When multiple the points have the same point class, they are sorted by priority.
     *
     * @return field priority
     */
    int pointClassPriority() default 0;

    /**
     * Extra tags to associate with the point.
     *
     * @return comma-separated list of tags
     */
    String pointTags() default "";

    /**
     * Set to true to have the value included in search engine.
     *
     * @return true if the value should be included in the search results
     */
    boolean indexed() default false;
}

