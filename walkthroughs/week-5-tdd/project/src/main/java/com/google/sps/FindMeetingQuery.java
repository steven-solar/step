// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;


public final class FindMeetingQuery {

  /**
   * This method merges together the Event's TimeRanges as a helper to our query function.
   * @param events This is a list of all the Events throughout the day.
   * @param request This is the original MeetingRequest we are trying to eventually satisfy.
   * @return Collection<TimeRange> This returns the TimeRange list, with overlapping intervals merged together
   */
  private Collection<TimeRange> mergeTimes(Collection<Event> events, MeetingRequest request) {
    List<String> requestAttendees = new ArrayList(request.getAttendees());
    List<TimeRange> mergedTimes = new ArrayList<TimeRange>();
    List<Event> eventList = new ArrayList(events);

    if (eventList.size() == 0) {
      return mergedTimes;
    }

    Collections.sort(eventList, Event.ORDER_BY_START);

    TimeRange last = TimeRange.fromStartDuration(TimeRange.START_OF_DAY, 0);
    for (Event e : events) {
      if (!Collections.disjoint(requestAttendees, e.getAttendees())) {
          if (mergedTimes.size() == 0) {
            last = e.getWhen();
            mergedTimes.add(last);
          }
          
        if (last.overlaps(e.getWhen())) {
          last = TimeRange.fromStartEnd(last.start(), Math.max(last.end(), e.getWhen().end()), false);
          mergedTimes.set(mergedTimes.size() - 1, last);
        }
        else {
          last = e.getWhen();
          mergedTimes.add(last);
        }
      }
    }
    return mergedTimes;
  }

  public boolean hasNoMandatoryOnlyOptionalAttendees(Event event, MeetingRequest request) {
    return Collections.disjoint(event.getAttendees(), request.getAttendees()) 
        && !Collections.disjoint(event.getAttendees(), request.getOptionalAttendees());
  }

  public Collection<String> getIntersection(Collection<String> a, Collection<String> b) {
    Collection<String> intersection = new ArrayList<>();
    intersection.addAll(a);
    intersection.retainAll(b);
    return intersection;
  }

  public Collection<Event> getOptionalOnlyEvents(Collection<Event> events, MeetingRequest request) {
    Collection<Event> optionalOnlyEvents = new ArrayList<>();
    for (Event e : events) {
      Collection<String> optionalAttendees = new ArrayList<>();
      if (hasNoMandatoryOnlyOptionalAttendees(e, request)) {
            optionalAttendees = getIntersection(e.getAttendees(), request.getOptionalAttendees());
            Event optionalEvent = new Event(e.getTitle(), e.getWhen(), optionalAttendees);
            optionalOnlyEvents.add(optionalEvent);
          }
    }
    return optionalOnlyEvents;
  }

  public Collection<Event> getOptionalOnlyEventsInRange(TimeRange range, Collection<Event> events, MeetingRequest request) {
    Collection<Event> optionalOnlyEvents = new ArrayList<>();
    for (Event e : events) {
      Collection<String> optionalAttendees = new ArrayList<>();
      if (hasNoMandatoryOnlyOptionalAttendees(e, request) && range.overlaps(e.getWhen())) {
            optionalAttendees = getIntersection(e.getAttendees(), request.getOptionalAttendees());
            Event optionalEvent = new Event(e.getTitle(), e.getWhen(), optionalAttendees);
            optionalOnlyEvents.add(optionalEvent);
          }
    }
    return optionalOnlyEvents;
  }

  public void timeSlotSubranges(PriorityQueue<TimeRangeAndUnavailable> slotsPQ, TimeRange range, Collection<Event> events, MeetingRequest request) {
    Collection<Event> optionalOnlyEvents = getOptionalOnlyEventsInRange(range, events, request);
    if (optionalOnlyEvents.size() == 0) {
      slotsPQ.add(new TimeRangeAndUnavailable(range, 0));
      return;
    }

    PriorityQueue<EventAndTime> eventTimesPQ = new PriorityQueue<>(5, EventAndTime.ORDER);
    for (Event e : optionalOnlyEvents) {
      eventTimesPQ.add(new EventAndTime(e, true));
      eventTimesPQ.add(new EventAndTime(e, false));
    }
    Set<EventAndTime> runningEvents = new HashSet<>();
    int numUnavailable = 0;
    EventAndTime one = eventTimesPQ.poll();
    runningEvents.add(one);
    numUnavailable += one.getEvent().getAttendees().size();
    while (!eventTimesPQ.isEmpty()) {
      EventAndTime two = eventTimesPQ.poll();
      if (two.getTime() - one.getTime() >= request.getDuration()) {
        slotsPQ.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(one.getTime(), two.getTime(), false), numUnavailable));
      } 
      if (two.isStart()) numUnavailable += two.getEvent().getAttendees().size();
      else numUnavailable -= two.getEvent().getAttendees().size();
      one = two;
      }
    }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Collection<TimeRange> mergedTimes = mergeTimes(events, request);
    Collection<TimeRange> availableTimes = new ArrayList<>();
    int start = TimeRange.START_OF_DAY;
    int end = TimeRange.START_OF_DAY;
    for (TimeRange t : mergedTimes) {
      end = t.start();
      if (end - start >= request.getDuration()) {
        availableTimes.add(TimeRange.fromStartEnd(start, end, false));
      }
      start = t.end();
    }
    end = TimeRange.END_OF_DAY;
    if (end - start >= request.getDuration()) {
      availableTimes.add(TimeRange.fromStartEnd(start, end, true));
    }

    Collection<TimeRange> optimalTimes = new ArrayList<>();
    PriorityQueue<TimeRangeAndUnavailable> slotsPQ = new PriorityQueue<>(5, TimeRangeAndUnavailable.ORDER_BY_UNAVAILABLE);
    Collection<Event> optionalOnlyEvents = getOptionalOnlyEvents(events, request);
    
    if (optionalOnlyEvents.size() == 0) {
      return availableTimes;
    }

    for (TimeRange t : availableTimes) {
      timeSlotSubranges(slotsPQ, t, optionalOnlyEvents, request);
    }
    
    int numOptional = request.getOptionalAttendees().size();
    if (slotsPQ.isEmpty()) {
      return availableTimes;
    }
    else {
      int bestUnavailable = slotsPQ.peek().getUnavailable();
      if (bestUnavailable == numOptional) {
        return availableTimes;
      }
      if (request.getAttendees().size() == 0 && numOptional == 2 && bestUnavailable >= 1) {
        return new ArrayList<TimeRange>();
      }
      while (!slotsPQ.isEmpty() && slotsPQ.peek().getUnavailable() == bestUnavailable) {
        optimalTimes.add(slotsPQ.poll().getTimeRange());
      }
    }
    return optimalTimes;
  }
}
