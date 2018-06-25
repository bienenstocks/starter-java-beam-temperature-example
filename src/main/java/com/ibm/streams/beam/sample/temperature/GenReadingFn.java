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

import java.util.Random;

import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;

/**
 * Generate random temperature readings for a device. The input Long is ignored,
 * it is just used to drive the generation. The output KV<String,Double> has a
 * device name as key, and a temperature in the range [0, maxTemp).
 */
public class GenReadingFn extends SimpleFunction<Long, KV<String,Double>> {
    private static final long serialVersionUID = 8543151546013003534L;

    private final String deviceName;
    private double maxTemp;
    // Make the PRNG transient so it gets regenerated if serialized
    private transient Random prng;

    public GenReadingFn(int deviceId, double maxTemp) {
        deviceName = "device_" + deviceId;
        this.maxTemp = maxTemp;
    }

    public KV<String,Double> apply(Long unused) {
        if (prng == null) {
            prng = new Random();
        }
        double temp = prng.nextDouble() * maxTemp;
        Metrics.distribution(deviceName, "generated").update(Math.round(temp));
        return KV.<String, Double> of(deviceName, temp);
    }
}
