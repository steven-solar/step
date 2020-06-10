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

  /**
   * This method checks if an event attendees who are optional in the request, and no attendees who are 
   * mandatory in the request.
   * @param event This is the Event being checked.
   * @param request This is the MeetingRequest being checked.
   * @return boolean This returns true if there are optional attendees at the meeting, but no mandatory ones.
   */
  public boolean hasNoMandatoryOnlyOptionalAttendees(Event event, MeetingRequest request) {
    return Collections.disjoint(event.getAttendees(), request.getAttendees()) 
        && !Collections.disjoint(event.getAttendees(), request.getOptionalAttendees());
  }

  /**
   * This method get the intersection of two sets
   * @param a This is the first set of strings
   * @param b This is the second set of strings
   * @return Collection<String> This returns the intersection of the two sets (a and b).
   */
  public Collection<String> getIntersection(Collection<String> a, Collection<String> b) {
    Collection<String> intersection = new ArrayList<>();
    intersection.addAll(a);
    intersection.retainAll(b);
    return intersection;
  }

  /**
   * This returns only the events that overlap with the given TimeRange and have no mandatory (and >= 1 optional) 
   * attendees, with their attendee list updated to contain only optional attendees 
   * (all non-optional are thrown out, and there are no mandatory as these are filtered out).
   * @param events This is a list of all the Events throughout the day.
   * @param range This is the TimeRange all events must overlap with, or they're filtered out.
   * @param request This is the original MeetingRequest we are trying to eventually satisfy.
   * @return Collection<Event> This returns the updated Event list, with "optional" events in the TimeRange, with updated attendees.
   */
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

  /**
   * This returns only the events with no mandatory (and >= 1 optional) attendees, with their attendee list updated
   * to contain only optional attendees (all non-optional are thrown out, and there are no mandatory as these are filtered out).
   * @param events This is a list of all the Events throughout the day.
   * @param request This is the original MeetingRequest we are trying to eventually satisfy.
   * @return Collection<Event> This returns the updated Event list, with only "optional" events, and their optional attendees.
   */
  public Collection<Event> getOptionalOnlyEvents(Collection<Event> events, MeetingRequest request) {
    return getOptionalOnlyEventsInRange(TimeRange.WHOLE_DAY, events, request);
  }

  /**
   * This method updates the given slotsPQ (ordered by min unavailable) with the optimal subslots of the give TimeRange, 
   * and the number of unavailable optional attendees in those ranges.
   * @param events This is a list of all the Events throughout the day.
   * @param request This is the original MeetingRequest we are trying to eventually satisfy.
   */
  public void timeSlotSubranges(PriorityQueue<TimeRangeAndUnavailable> slotsPQ, TimeRange range, Collection<Event> events, MeetingRequest request) {
    Collection<Event> optionalOnlyEvents = getOptionalOnlyEventsInRange(range, events, request);

    //No conflicts in this range, the whole range has 0 optional attendees unavailable
    if (optionalOnlyEvents.size() == 0) {
      slotsPQ.add(new TimeRangeAndUnavailable(range, 0));
      return;
    }

    //Add all event start and end times (object contains Event, and whether we should look at start or end) to PQ.
    PriorityQueue<EventAndTime> eventTimesPQ = new PriorityQueue<>(5, EventAndTime.ORDER);
    for (Event e : optionalOnlyEvents) {
      eventTimesPQ.add(new EventAndTime(e, true));
      eventTimesPQ.add(new EventAndTime(e, false));
    }

    int numUnavailable = 0;
    EventAndTime one = eventTimesPQ.poll();
    numUnavailable += one.getEvent().getAttendees().size();

    while (!eventTimesPQ.isEmpty()) {
      EventAndTime two = eventTimesPQ.poll();

      //Is the range long enough to hold the requested meeting?
      if (two.getTime() - one.getTime() >= request.getDuration()) {
        slotsPQ.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(one.getTime(), two.getTime(), false), numUnavailable));
      } 

      //If the second time starts a new event, add its attendees to numUnavailable.
      if (two.isStart()) {
        numUnavailable += two.getEvent().getAttendees().size();
      }

      //If the second time ends an event, remove its attendees from numUnavailable.
      else {
        numUnavailable -= two.getEvent().getAttendees().size();
      }

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
    
    //If no optional events, just return open slots. Nothing to optimize for.
    if (optionalOnlyEvents.size() == 0) {
      return availableTimes;
    }
    
    //Find optimal subranges for all available ranges.
    for (TimeRange t : availableTimes) {
      timeSlotSubranges(slotsPQ, t, optionalOnlyEvents, request);
    }
    
    int numOptional = request.getOptionalAttendees().size();

    //If no optimal subranges are found, just return open slots. No way to optimize.
    if (slotsPQ.isEmpty()) {
      return availableTimes;
    }
    else {
      int bestUnavailable = slotsPQ.peek().getUnavailable();

      //If the best we can do is that all optional attendees are unavailable, no way to optimize.
      if (bestUnavailable == numOptional) {
        return availableTimes;
      }

      //Edge case: If no mandatory and 2 optional attendees and best we can do is 1 free at a time, no meeting.
      if (request.getAttendees().size() == 0 && numOptional == 2 && bestUnavailable >= 1) {
        return new ArrayList<TimeRange>();
      }

      //Add all equivalently "good" subranges to our answer.
      while (!slotsPQ.isEmpty() && slotsPQ.peek().getUnavailable() == bestUnavailable) {
        optimalTimes.add(slotsPQ.poll().getTimeRange());
      }
    }
    return optimalTimes;
  }
}
