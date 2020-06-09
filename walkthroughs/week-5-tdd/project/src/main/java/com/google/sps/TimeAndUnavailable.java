package com.google.sps;

public final class TimeAndUnavailable {
  private TimeRange range;
  private int unavailable;

  public TimeAndUnavailable(TimeRange range, int unavailable) {
    this.range = range;
    this.unavailable = unavailable;
  }

  public TimeRange getRange() {
    return range;
  }

  public int getUnavailable() {
    return unavailable;
  }
}