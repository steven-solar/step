package com.google.sps;

import java.util.Comparator;

public final class TimeRangeAndUnavailable {
  private TimeRange range;
  private int unavailable;

  public TimeRangeAndUnavailable(TimeRange range, int unavailable) {
    this.range = range;
    this.unavailable = unavailable;
  }

  public TimeRange getTimeRange() {
    return range;
  }

  public int getUnavailable() {
    return unavailable;
  }

  public static final Comparator<TimeRangeAndUnavailable> ORDER_BY_UNAVAILABLE = new Comparator<TimeRangeAndUnavailable>() {
    @Override
    public int compare(TimeRangeAndUnavailable a, TimeRangeAndUnavailable b) {
      return Integer.compare(a.unavailable, b.unavailable);
    }
  };

  public String toString() {
    return range.start() + " - " + range.end() + ": " + unavailable;
  }
}