package com.google.sps;

import java.util.Comparator;

public final class EventAndTime {
  private Event event;
  private boolean start;

  public EventAndTime(Event event, boolean start) {
    this.event = event;
    this.start = start;
  }

  public Event getEvent() {
    return event;
  }

  public boolean isStart() {
    return start;
  }

  public int getTime() {
    if (start) return event.getWhen().start();
    else return event.getWhen().end();
  }

  public static final Comparator<EventAndTime> ORDER = new Comparator<EventAndTime>() {
    @Override
    public int compare(EventAndTime a, EventAndTime b) {
      int aTime;
      int bTime;

      if (a.start) aTime = a.event.getWhen().start();
      else aTime = a.event.getWhen().end();

      if (b.start) bTime = b.event.getWhen().start();
      else bTime = b.event.getWhen().end();

      return Long.compare(aTime, bTime);
    }
  };
}