package com.ververica.troubleshooting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

public class FakeSource extends RichParallelSourceFunction<SimpleMeasurement>
    implements CheckpointedFunction {

  public static final int NUM_OF_LOCATIONS = 100;
  public static final int NUM_OF_MEASUREMENTS = 100_000;
  public static final int RANDOM_SEED = 1;

  private static final long serialVersionUID = 1L;

  private final Random rand;

  private transient volatile boolean cancelled;

  private final List<SimpleMeasurement> measurements;

  FakeSource(final int seed, final List<SimpleMeasurement> measurements) {
    this.measurements = measurements;
    this.rand = new Random(seed);
  }

  @Override
  public void run(final SourceContext<SimpleMeasurement> sourceContext) throws Exception {
    while (!cancelled) {
      SimpleMeasurement measurement = measurements.get(rand.nextInt(measurements.size()));

      synchronized (sourceContext.getCheckpointLock()) {
        sourceContext.collect(measurement);
      }
    }
  }

  @Override
  public void cancel() {
    cancelled = true;

    // there will be an interrupt() call to the main thread anyways
  }

  @Override
  public void snapshotState(final FunctionSnapshotContext context) {}

  @Override
  public void initializeState(final FunctionInitializationContext context) {}

  public static FakeSource createSource() {
    List<SimpleMeasurement> measurements = createMeasurements();
    return new FakeSource(RANDOM_SEED, measurements);
  }

  private static List<SimpleMeasurement> createMeasurements() {
    Random rand = new Random(RANDOM_SEED);

    final List<String> locations = readLocationsFromFile();

    List<SimpleMeasurement> measurements = new ArrayList<>(NUM_OF_MEASUREMENTS);
    for (int i = 0; i < NUM_OF_MEASUREMENTS; i++) {
      SimpleMeasurement nextMeasurement =
          new SimpleMeasurement(
              rand.nextInt(100),
              rand.nextDouble() * 100,
              locations.get(rand.nextInt(locations.size())));
      measurements.add(nextMeasurement);
    }
    return measurements;
  }

  private static List<String> readLocationsFromFile() {
    List<String> locations = new ArrayList<>(NUM_OF_LOCATIONS);
    for (int i = 0; i < NUM_OF_LOCATIONS; i++) {
      String city = RandomStringUtils.randomAlphabetic(30);
      locations.add(city);
    }
    return locations;
  }
}
