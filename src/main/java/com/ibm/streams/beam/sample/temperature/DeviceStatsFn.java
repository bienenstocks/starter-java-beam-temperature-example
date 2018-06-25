/* begin_generated_IBM_copyright_prolog                             */
/*                                                                  */
/* This is an automatically generated copyright prolog.             */
/* After initializing,  DO NOT MODIFY OR MOVE                       */
/* **************************************************************** */
/* THIS SAMPLE CODE IS PROVIDED ON AN "AS IS" BASIS. IBM MAKES NO   */
/* REPRESENTATIONS OR WARRANTIES, EXPRESS OR IMPLIED, CONCERNING    */
/* USE OF THE SAMPLE CODE, OR THE COMPLETENESS OR ACCURACY OF THE   */
/* SAMPLE CODE. IBM DOES NOT WARRANT UNINTERRUPTED OR ERROR-FREE    */
/* OPERATION OF THIS SAMPLE CODE. IBM IS NOT RESPONSIBLE FOR THE    */
/* RESULTS OBTAINED FROM THE USE OF THE SAMPLE CODE OR ANY PORTION  */
/* OF THIS SAMPLE CODE.                                             */
/*                                                                  */
/* LIMITATION OF LIABILITY. IN NO EVENT WILL IBM BE LIABLE TO ANY   */
/* PARTY FOR ANY DIRECT, INDIRECT, SPECIAL OR OTHER CONSEQUENTIAL   */
/* DAMAGES FOR ANY USE OF THIS SAMPLE CODE, THE USE OF CODE FROM    */
/* THIS [ SAMPLE PACKAGE,] INCLUDING, WITHOUT LIMITATION, ANY LOST  */
/* PROFITS, BUSINESS INTERRUPTION, LOSS OF PROGRAMS OR OTHER DATA   */
/* ON YOUR INFORMATION HANDLING SYSTEM OR OTHERWISE.                */
/*                                                                  */
/* (C) Copyright IBM Corp. 2017, 2017  All Rights reserved.         */
/*                                                                  */
/* end_generated_IBM_copyright_prolog                               */
package com.ibm.streams.beam.sample.temperature;

import java.io.Serializable;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.transforms.Combine.CombineFn;

/**
 * This transform is used generate basic per-device stats: minimum and maximum
 * values, count, and an estimated mean and variance.
 */
public class DeviceStatsFn extends CombineFn<Double, DeviceStatsFn.Stats, DeviceStatsFn.Stats> {
    private static final long serialVersionUID = 684213186017165359L;

    /**
     * This class acts an accumulator, holding the raw data necessary for the
     * basic statics we are tracking, and is updated by {@link DeviceStatsFn}.
     * It is also used as the output type for that class.
     */
    @DefaultCoder(SerializableCoder.class)
    public static class Stats implements Serializable {
        private static final long serialVersionUID = 3145361195382654372L;

        int count;
        double min;
        double max;
        double mean;
        double runningM2;   // sum of squares of differences from mean

        public Stats() { }

        public Stats(DeviceStatsFn.Stats other) {
            if (this != other) {
                count = other.count;
                min = other.min;
                max = other.max;
                mean = other.mean;
                runningM2 = other.runningM2;
            }
        }

        public int count() {
            return count;
        }

        public double min() {
            return count > 0 ? min : Double.NaN;
        }

        public double max() {
            return count > 0 ? max : Double.NaN;
        }

        public double mean() {
            return count > 0 ? mean : Double.NaN;
        }

        public double variance() {
            return count > 1 ? runningM2 / (count - 1) : Double.NaN;
        }

        public double stddev() {
            return Math.sqrt(variance());
        }

        @Override
        public String toString() {
            if (0 == count) {
                return "N/A";
            }
            StringBuilder str = new StringBuilder("{ min = ")
                    .append(min)
                    .append(", max = ")
                    .append(max)
                    .append(", count = ")
                    .append(count)
                    .append(", mean = ")
                    .append(mean())
                    .append(", stddev = ")
                    .append(stddev())
                    .append(", var = ")
                    .append(variance())
                    .append(" }");
            return str.toString();
        }
    }

    /**
     * Update accumulator using <a
     * href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Online_algorithm">Welford's
     * online algorithm</a>
     */
    @Override
    public DeviceStatsFn.Stats addInput(DeviceStatsFn.Stats accum, Double value) {
        if (0 == accum.count || value < accum.min) {
            accum.min = value;
        }
        if (value > accum.max) {
            accum.max = value;
        }
        ++accum.count;
        double delta = value.doubleValue() - accum.mean;
        accum.mean += delta / accum.count;
        double updatedDelta = value.doubleValue() - accum.mean;
        accum.runningM2 += delta * updatedDelta;
        return accum;
    }

    @Override
    public DeviceStatsFn.Stats createAccumulator() {
        return new Stats();
    }

    @Override
    public DeviceStatsFn.Stats extractOutput(DeviceStatsFn.Stats accum) {
        return new Stats(accum);
    }

    /**
     * Merge accumulators using <a
     * href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Parallel_algorithm">Chan's
     * parallel algorithm</a>, with the more stable variant.
     */
    @Override
    public DeviceStatsFn.Stats mergeAccumulators(Iterable<DeviceStatsFn.Stats> accums) {
        DeviceStatsFn.Stats merged = null;
        for (DeviceStatsFn.Stats accum : accums)
        {
            if (merged == null && accum.count > 0) {
                // First non-empty accumulator
                merged = new Stats(accum);
            } else if (accum.count > 0) {
                // Subsequent non-empty accumulator
                if (accum.min < merged.min) {
                    merged.min = accum.min;
                }
                if (accum.max > merged.max) {
                    merged.max = accum.max;
                }
                int count = merged.count + accum.count;
                double delta = accum.mean - merged.mean;
                merged.mean = (merged.mean * merged.count + accum.mean * accum.count)
                        / count;
                merged.runningM2 = merged.runningM2 + accum.runningM2
                        + delta * delta * merged.count * accum.count
                        / count;
                merged.count = count;
            }
        }
        return merged == null ? new Stats() : merged;
    }
}
