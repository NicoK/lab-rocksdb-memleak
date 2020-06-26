package com.ververica.troubleshooting;

import java.util.HashMap;
import java.util.Map;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

class MovingCountAverage extends RichMapFunction<SimpleMeasurement, Tuple2<Integer, Double>> {
  private final int size;

  private ValueState<Integer> countState;
  private ValueState<Integer> writePositionState;
  private MapState<Integer, SimpleMeasurement> lastN;

  private Map<Integer, Integer> countPerKey = new HashMap<>();
  private Map<Integer, Double> sumPerKey = new HashMap<>();

  MovingCountAverage(int size) {
    this.size = size;
  }

  @Override
  public Tuple2<Integer, Double> map(SimpleMeasurement value) throws Exception {
    int key = value.getSensorId();
    double sum = sumPerKey.getOrDefault(key, 0.0d);
    int count = countPerKey.getOrDefault(key, 0);

    Integer writePos = writePositionState.value();
    if (writePos == null) {
      writePos = 0;
    }
    int nextWritePos = ((writePos + 1) % size);
    if (count < 0) {
      // first time use; fill up caches:
      count = countState.value() != null ? countState.value() : 0;

      final int excludePos;
      if (count == size) {
        excludePos = writePos;
      } else {
        excludePos = -1;
      }
      for (int i = 0; i < count; ++i) {
        if (i != excludePos) {
          sum += lastN.get(i).getValue();
        }
      }
      countPerKey.put(key, count);
    } else if (count != size) {
      ++count;
      countPerKey.put(key, count);
    } else {
      SimpleMeasurement measurement = lastN.get(writePos);
      sum -= measurement.getValue();
    }

    sum += value.getValue();
    lastN.put(writePos, value);
    writePositionState.update(nextWritePos);
    sumPerKey.put(key, sum);

    return Tuple2.of(key, sum / count);
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);

    ValueStateDescriptor<Integer> readPosDesc = new ValueStateDescriptor<>("readPos", Types.INT);
    countState = getRuntimeContext().getState(readPosDesc);

    ValueStateDescriptor<Integer> writePosDesc = new ValueStateDescriptor<>("writePos", Types.INT);
    writePositionState = getRuntimeContext().getState(writePosDesc);

    MapStateDescriptor<Integer, SimpleMeasurement> lastNDescr =
        new MapStateDescriptor<>("lastN", Types.INT, Types.POJO(SimpleMeasurement.class));
    lastN = getRuntimeContext().getMapState(lastNDescr);
  }
}
