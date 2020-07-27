package com.ververica.troubleshooting;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class NthLastElement extends RichMapFunction<SimpleMeasurement, Tuple2<Integer, Double>> {
  private final int size;

  private ValueState<Integer> countState;
  private ValueState<Integer> writePositionState;
  private MapState<Integer, SimpleMeasurement> lastN;

  private final Map<Integer, Integer> countCachePerKey = new HashMap<>();
  private final Map<Integer, Integer> writePositionCachePerKey = new HashMap<>();

  NthLastElement(int size) {
    this.size = size;
  }

  @Override
  public Tuple2<Integer, Double> map(SimpleMeasurement value) throws Exception {
    int key = value.getSensorId();
    int writePos = writePositionCachePerKey.getOrDefault(key, -1);
    int count = countCachePerKey.getOrDefault(key, -1);

    final SimpleMeasurement nthLast;
    if (count < 0 || writePos < 0) {
      // first time use; fill up caches:
      writePos = Optional.ofNullable(writePositionState.value()).orElse(0);
      count = Optional.ofNullable(countState.value()).orElse(0);

      if (count == size) {
        nthLast = lastN.get(writePos);
        countCachePerKey.put(key, count);
      } else {
        if (count == 0) {
          nthLast = value;
        } else {
          nthLast = lastN.get(0);
        }
        ++count;
        countCachePerKey.put(key, count);
        countState.update(count);
      }
    } else if (count != size) {
      ++count;
      countCachePerKey.put(key, count);
      countState.update(count);
      nthLast = lastN.get(0);
    } else {
      nthLast = lastN.get(writePos);
    }

    lastN.put(writePos, value);
    int nextWritePos = ((writePos + 1) % size);
    writePositionState.update(nextWritePos);
    writePositionCachePerKey.put(key, nextWritePos);

    return Tuple2.of(key, nthLast.getValue());
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
