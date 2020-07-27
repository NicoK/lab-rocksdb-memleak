package com.ververica.troubleshooting;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

  private final Map<Integer, Integer> countCachePerKey = new HashMap<>();
  private final Map<Integer, Integer> writePositionCachePerKey = new HashMap<>();
  private final Map<Integer, Double> sumPerKey = new HashMap<>();

  MovingCountAverage(int size) {
    this.size = size;
  }

  @Override
  public Tuple2<Integer, Double> map(SimpleMeasurement value) throws Exception {
    int key = value.getSensorId();
    int writePos = writePositionCachePerKey.getOrDefault(key, -1);
    int count = countCachePerKey.getOrDefault(key, -1);
    double sum = sumPerKey.getOrDefault(key, 0.0d);

    if (count < 0 || writePos < 0) {
      // first time use; fill up caches:
      writePos = Optional.ofNullable(writePositionState.value()).orElse(0);
      count = Optional.ofNullable(countState.value()).orElse(0);

      final int excludePos;
      final int newCount;
      if (count == size) {
        excludePos = writePos;
        newCount = count;
      } else {
        excludePos = -1;
        newCount = count + 1;
        countState.update(newCount);
      }
      for (int i = 0; i < count; ++i) {
        if (i != excludePos) {
          sum += lastN.get(i).getValue();
        }
      }
      count = newCount;
      countCachePerKey.put(key, count);
    } else if (count != size) {
      ++count;
      countCachePerKey.put(key, count);
      countState.update(count);
    } else {
      SimpleMeasurement measurement = lastN.get(writePos);
      sum -= measurement.getValue();
    }

    sum += value.getValue();
    lastN.put(writePos, value);
    int nextWritePos = ((writePos + 1) % size);
    writePositionState.update(nextWritePos);
    writePositionCachePerKey.put(key, nextWritePos);
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
