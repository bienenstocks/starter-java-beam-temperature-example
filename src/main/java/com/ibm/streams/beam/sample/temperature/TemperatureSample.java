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

import com.ibm.streams.beam.sample.temperature.DeviceStatsFn.Stats;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.PipelineResult.State;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.metrics.*;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.*;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This is the main class for the temperature example.
 * <p>
 * The example creates a pipeline that has simple sources that generate random
 * temperature readings from a set of devices, splits those into good and bad
 * readings based on a threshold, calculates some basic statistics on the good
 * ones and counts the bad ones, and logs these results.
 * <p>
 * It also uses Beam metrics on several of the transforms to demonstrate how
 * metrics can be collected. This is the supported way to collect metrics
 * programmatically.
 * <p>
 * Metrics may also be visible in the runtime environment, and may
 * include additional metrics beyond what the application explicitly uses.
 * For example, the Streams runtime shows both Beam metrics and internal
 * Streams metrics in its console. Beam counters will be shown with a name
 * starting with <code>BeamCounter::</code> and including the Beam namespace
 * and name, for example, <code>BeamCounter::MyNamepace::metricName</code>.
 * Beam distributions are shown as several distinct Streams metrics with
 * names starting with <code>BeamDistribution::</code> and ending with a
 * suffix of <code>::count</code>, <code>::min</code>, <code>::max</code>,
 * or <code>::sum</code>, for example
 * <code>BeamDistribution::MyNamespace::distribution::max</code>. Note that
 * these formats are subject to change in the Streams runner, and developers
 * should use the Beam APIs to collect metrics as shown at the end of this
 * example. The naming is only described here to allow developers to monitor
 * their Beam applications with Streams tools that are not yet Beam-aware.
 */

public class TemperatureSample {

    /**
     * Metrics in this namespace will be collected and displayed by the runner
     * after the job is started.
     */
    private static final String COLLECTED_METRIC_NAMESPACE = "TemperatureSample";

    /**
     * Custom pipeline options for the {@code TemperatureSample} application.
     */
    public interface TemperatureOptions extends PipelineOptions {
        static final int NUM_DEVICES = 3;
        static final double MAX_TEMP = 120.0;
        static final double BAD_TEMP = 100.0;
        static final long RATE = 107;
        static final long WINDOW_SIZE = 10;
        static final String STEP_NAME = "MergeReadings";

        /**
         * The number of devices to simulate.
         */
        @Description("Number of devices to simulate")
        @Default.Integer(NUM_DEVICES)
        Integer getNumDevices();
        void setNumDevices(Integer value);

        /**
         * The maximum temperature limit a device can generate.
         */
        @Description("Maximum temperature")
        @Default.Double(MAX_TEMP)
        Double getMaxTemp();
        void setMaxTemp(Double value);

        /**
         * The temperature threshold to mark as bad if its greater or equal to this number.
         */
        @Description("Bad temperature threshold")
        @Default.Double(BAD_TEMP)
        Double getBadTempThreshold();
        void setBadTempThreshold(Double value);

        /**
         * The generation rate in readings/second.
         */
        @Description("Rate of generation of each devices, in readings/second")
        @Default.Long(RATE)
        Long getRate();
        void setRate(Long value);

        /**
         * The window size, in seconds.
         */
        @Description("Fixed window size, in seconds")
        @Default.Long(WINDOW_SIZE)
        Long getWindowSize();
        void setWindowSize(Long value);

        /**
         * The step name used to query metrics.
         */
        @Description("Step name to query for metrics")
        @Default.String(STEP_NAME)
        String getStepName();
        void setStepName(String value);
    }

    /**
     * A {@link DoFn} that will log input values and has no output values. It
     * is used in the sample to log windowed per-device counts of bad readings
     * and statistics about good readings.
     */
    public static class LogWindowFn<T> extends DoFn<KV<String, T>, Void> {
        private static final long serialVersionUID = 8543151546013003534L;
        private static final Logger LOG = LoggerFactory.getLogger(LogWindowFn.class);
        private static final String LOG_WRITES_NAMESPACE = "LogWrites";

        private final String prefix;

        /**
         * @param prefix The prefix for the logged values
         */
        public LogWindowFn(String prefix) {
            this.prefix = prefix;
        }

        @ProcessElement
        public void processElement(ProcessContext context) throws Exception {
            long written = 0;
            StringBuilder line = new StringBuilder(prefix)
                    .append(": ")
                    .append(context.element().getKey())
                    .append(": ")
                    .append(context.element().getValue());
            LOG.info(line.toString());
            written += line.length();

            Metrics.gauge(COLLECTED_METRIC_NAMESPACE, "writeSize").set(written);
        }

    }

    public static void main(String[] args) throws IOException {
        TemperatureOptions options = PipelineOptionsFactory.fromArgs(args).
                withValidation().as(TemperatureOptions.class);
        Pipeline pipeline = Pipeline.create(options);

        /*
         * Create the artificial devices that will produce temperature
         * readings. The number of devices is variable, specified by the
         * --numDevices option, so we loop, creating a PCollectionList
         * with the transforms for each device.
         */
        final Double maxTemp = options.getMaxTemp();
        PCollectionList<KV<String,Double>> deviceReadings = PCollectionList.empty(pipeline);
        for (int deviceId = 1; deviceId <= options.getNumDevices(); ++deviceId) {
            /*
             * For each device, we use a counter to drive another transform
             * that just generates random values. The rate at which readings
             * are generated is controlled by the --rate option. The output of
             * the device is a key-value KV<String,Double> where the key is the
             * device name and the value is a random temperature reading from
             * the device.
             */
            PCollection<KV<String,Double>> readings = pipeline
                    .apply("Counter_" + deviceId,
                            GenerateSequence.from(0).withRate(
                                    options.getRate(), Duration.standardSeconds(1)))
                    .apply("Device_" + deviceId,
                            MapElements.<Long, KV<String, Double>>via(
                                    new GenReadingFn(deviceId, maxTemp)));
            deviceReadings = deviceReadings.and(readings);
        }

        /*
         * The readings from all devices are merged into a single stream, and
         * grouped into fixed-duration windows, where the duration is given by
         * the --windowSize option in seconds.
         */
        PCollection<KV<String,Double>> mergedReadings = deviceReadings
                .apply("MergeReadings", Flatten.<KV<String,Double>>pCollections())
                .apply("WindowReadings",
                        Window.<KV<String,Double>>into(
                                FixedWindows.of(Duration.standardSeconds(options.getWindowSize()))));

        /*
         * Readings are partitioned into "good" readings, whose temperature is
         * below the threshold set in the --badTempThreshold option, and bad
         * readings at or above the threshold. The partitioning is done by
         * using output tags, with the good tags being the main output of the
         * transform, and the bad tag the side output.
         * <p>
         * The transform also updates some metrics that represent a count of
         * bad values across all devices, and by device as well as both summary
         * and per-device distribution information about good values. These
         * metrics will be collected later when the pipeline is run.
         */
        final TupleTag<KV<String,Double>> goodTag = new TupleTag<KV<String,Double>>(){
            private static final long serialVersionUID = 1L;
        };
        final TupleTag<KV<String,Double>> badTag = new TupleTag<KV<String,Double>>(){
            private static final long serialVersionUID = 1L;
        };
        final double badTempThreshold = options.getBadTempThreshold();

        PCollectionTuple validatedReadings = mergedReadings.apply("ValidateReadings",
                ParDo.of(new DoFn<KV<String,Double>,KV<String,Double>>() {
                    private static final long serialVersionUID = 1L;

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String device = c.element().getKey();
                        Double temp = c.element().getValue();
                        if (temp < badTempThreshold) {
                            // Good reading, output and update distribution metrics
                            c.output(c.element());
                            long roundedTemp = Math.round(temp);
                            Metrics.distribution(COLLECTED_METRIC_NAMESPACE, "good.summary")
                                    .update(roundedTemp);
                            Metrics.distribution(COLLECTED_METRIC_NAMESPACE, "good." + device)
                                    .update(roundedTemp);
                        } else {
                            // Bad reading, output to side output, update counters
                            c.output(badTag, c.element());
                            Metrics.counter(COLLECTED_METRIC_NAMESPACE, "bad.total").inc();
                            Metrics.counter(COLLECTED_METRIC_NAMESPACE, "bad." + device).inc();
                        }
                    }
                }).withOutputTags(goodTag, TupleTagList.of(badTag)));

        /*
         * For good readings, we combine by key to get per-device statistics
         * over the current window, and log them.
         */
        validatedReadings
                .get(goodTag)
                .apply("GoodStats", Combine.<String, Double, Stats>perKey(new DeviceStatsFn()))
                .apply("GoodLog", ParDo.of(new LogWindowFn<Stats>("Temperature device statistics")));

        /*
         * For bad readings, we get per-device counts and log those.
         */
        validatedReadings
                .get(badTag)
                .apply("BadCount", Count.<String,Double>perKey())
                .apply("BadLog", ParDo.of(new LogWindowFn<Long>("Temperature device error count")));

        /*
         * Execute the pipeline.
         */
        Logger LOG = LoggerFactory.getLogger(TemperatureSample.class);
        PipelineResult result = pipeline.run();

        boolean returnAfterDeploy = true;
        if (returnAfterDeploy) {
            return;
        }

        /*
         * Collect metrics, if they are available with the given runner. We
         * will use two filters, one by COLLECTED_METRIC_NAMESPACE to get the
         * metrics we created above, and a second on a step to see if the
         * runtime provides any of its own metrics.
         *
         * Note that step names are runner-specific, so while this may show
         * metrics under Streams, it might not on other runners, even if they
         * support metrics.
         */
        State state = null;
        try {
            MetricResults metrics = result.metrics();
            MetricsFilter collectedFilter = MetricsFilter.builder()
                    .addNameFilter(MetricNameFilter.inNamespace(COLLECTED_METRIC_NAMESPACE))
                    .build();
            MetricsFilter stepFilter = MetricsFilter.builder()
                    .addStep(options.getStepName())
                    .build();
            /*
             * Give the pipeline some time to start up and run.
             */
            Duration wait = Duration.standardSeconds(options.getWindowSize());
            state = result.waitUntilFinish(wait);
            /*
             * Attempt to query metrics as long as the job is still running.
             * Treat UNKNOWN as terminal so we stop if we can't get job status.
             */
            while (!state.isTerminal() && state != State.UNKNOWN) {
                // First fetch and print the collected metrics
                MetricQueryResults metricResults = metrics.queryMetrics(collectedFilter);
                System.out.println("----- Collected Metrics --------------------------------------------------");
                // First good distribution statistics
                for (MetricResult<DistributionResult> dist : metricResults.distributions()) {
                    System.out.println(dist.name().name() + ": " + dist.attempted());
                }
                // Next bad counts
                for (MetricResult<Long> counter : metricResults.counters()) {
                    System.out.println(counter.name().name() + ": " + counter.attempted());
                }
                // And logging gauges, with step
                for (MetricResult<GaugeResult> gauge : metricResults.gauges()) {
                    System.out.println(gauge.step() + ": " + gauge.name().name()
                            + ": " + gauge.attempted());
                }
                // Now fetch and print step metrics. The format is more general
                // since we don't know in advance what the query will return
                metricResults = metrics.queryMetrics(stepFilter);
                System.out.println("----- System Metrics for step \"" +
                        options.getStepName() + "\" -----");
                boolean anyMetrics = false;
                for (MetricResult<Long> counter : metricResults.counters()) {
                    anyMetrics = true;
                    System.out.println(counter.step() + ": " + counter.name() +
                            ": " + counter.attempted());
                }
                for (MetricResult<DistributionResult> dist : metricResults.distributions()) {
                    anyMetrics = true;
                    System.out.println(dist.step() + ": " + dist.name() +
                            ": " + dist.attempted());
                }
                for (MetricResult<GaugeResult> gauge : metricResults.gauges()) {
                    anyMetrics = true;
                    System.out.println(gauge.step() + ": " + gauge.name() +
                            ": " + gauge.attempted());
                }
                if (!anyMetrics) {
                    System.out.println("(none)");
                }
                state = result.waitUntilFinish(wait);
            }
        } catch (Exception e) {
            /*
             * Treat any excpetion as an indication that metrics are not
             * supported, but allow the job to continue to run.
             */
            LOG.warn("Exception while collecting metrics for job", e);
        }
    }
}
