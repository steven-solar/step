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
   * This method gets all the unavailability slots throughout a day.
   * @param optionalOnlyEvents represents all the optional only events throughout the dat
   * @param request is the original MeetingRequest object
   * @return Collection<TimeRangeAndUnavailable> is the entire day broken into slots in which unavailability is constant.
   */
  public Collection<TimeRangeAndUnavailable> getAllUnavailability(Collection<Event> optionalOnlyEvents, MeetingRequest request) {
    List<TimeRangeAndUnavailable> unavailability = new ArrayList<>();
    PriorityQueue<EventAndTime> eventTimesPQ = new PriorityQueue<>(5, EventAndTime.ORDER);
    
    for (Event e : optionalOnlyEvents) {
      eventTimesPQ.add(new EventAndTime(e, true));
      eventTimesPQ.add(new EventAndTime(e, false));
    }

    int numUnavailable = 0;
    if (eventTimesPQ.isEmpty()) return unavailability;
    EventAndTime one = eventTimesPQ.poll();
    if (one.getTime() != TimeRange.START_OF_DAY) {
      unavailability.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, one.getTime(), false), 0));
    }
    numUnavailable += one.getEvent().getAttendees().size();
    while (!eventTimesPQ.isEmpty()) {
      EventAndTime two = eventTimesPQ.poll();

      //Is the range long enough to hold the requested meeting?
      if (two.getTime() - one.getTime() >= request.getDuration()) {
        unavailability.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(one.getTime(), two.getTime(), false), numUnavailable));
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
    if (one.getTime() != TimeRange.END_OF_DAY) {
      unavailability.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(one.getTime(), TimeRange.END_OF_DAY, true), 0));
    }

    return unavailability;
  }

  /**
   * This method retrieves a list of the optimal free times
   * @param openRanges is the list of free times, based on mandatory attendees
   * @param events is all the events throughout the day
   * @param request is the original MeetingRequest object.
   * @return Collection<TimeRangeAndUnavailable> is a list of the optimal timeranges, and their associated unavailability of optional attendees.
   */
  public Collection<TimeRangeAndUnavailable> optimalTimeAndUnavailables(Collection<TimeRange> openRanges, Collection<Event> events, MeetingRequest request) {
    List<TimeRange> openRangeList = new ArrayList(openRanges);
    Collection<Event> optionalOnlyEvents = getOptionalOnlyEvents(events, request);
    List<TimeRangeAndUnavailable> unavailability = new ArrayList(getAllUnavailability(optionalOnlyEvents, request));
    Collection<TimeRangeAndUnavailable> optimalTimes = new ArrayList<>();

    int uIdx = 0;
    int minUnavailability = Integer.MAX_VALUE;
    for (int i = 0; i < openRangeList.size(); i++) {
      
      //Skip all intervals that are entirely before, they're irrelevant
      while (uIdx < unavailability.size() && !unavailability.get(uIdx).getTimeRange().overlaps(openRangeList.get(i))) {
        uIdx++;
      }
      if (uIdx >= unavailability.size()) break;

      //If unavail starts on or before, ends on or after. TimeRange is just the open one dictated by mandatory attendees.
      //If end together, move to next unavailability range. 
      if (unavailability.get(uIdx).getTimeRange().startsOnOrBeforeEndsOnOrAfter(openRangeList.get(i)) 
        && unavailability.get(uIdx).getTimeRange().end() - openRangeList.get(i).start() >= request.getDuration()) {
        if (unavailability.get(uIdx).getUnavailable() == minUnavailability) {
          optimalTimes.add(new TimeRangeAndUnavailable(openRangeList.get(i), minUnavailability));
        }
        if (unavailability.get(uIdx).getUnavailable() < minUnavailability) {
          optimalTimes.clear();
          minUnavailability = unavailability.get(uIdx).getUnavailable();
          optimalTimes.add(new TimeRangeAndUnavailable(openRangeList.get(i), minUnavailability));
        }

        if (unavailability.get(uIdx).getTimeRange().end() == openRangeList.get(i).end()) {
          uIdx++;
        }
        continue;
      }

      //If unavail start on or before, but ends during, TimeRange goes from start of open and ends with the unavailability range.
      //We move to the next unavailability range
      if (unavailability.get(uIdx).getTimeRange().overlapsBefore(openRangeList.get(i)) 
      && unavailability.get(uIdx).getTimeRange().end() - openRangeList.get(i).start() >= request.getDuration()) {
        System.out.println(unavailability.get(uIdx).getTimeRange().end() - openRangeList.get(i).start());
        if (unavailability.get(uIdx).getUnavailable() == minUnavailability) {
          optimalTimes.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(
              openRangeList.get(i).start(), unavailability.get(uIdx).getTimeRange().end(), false), minUnavailability));
        }
        if (unavailability.get(uIdx).getUnavailable() < minUnavailability) {
          optimalTimes.clear();
          minUnavailability = unavailability.get(uIdx).getUnavailable();
          optimalTimes.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(
              openRangeList.get(i).start(), unavailability.get(uIdx).getTimeRange().end(), false), minUnavailability));

        }
        uIdx++;
      }
      if (uIdx >= unavailability.size()) break;

      //If unavailability slot starts during and ends during, it defines the range of the slot and we move to the next slot
      while (uIdx < unavailability.size() 
      && openRangeList.get(i).contains(unavailability.get(uIdx).getTimeRange())) {
        if (unavailability.get(uIdx).getTimeRange().duration() >= request.getDuration()) {
          if (unavailability.get(uIdx).getUnavailable() == minUnavailability) {
            optimalTimes.add(new TimeRangeAndUnavailable(unavailability.get(uIdx).getTimeRange(), minUnavailability));
          }
          if (unavailability.get(uIdx).getUnavailable() < minUnavailability) {
            optimalTimes.clear();
            minUnavailability = unavailability.get(uIdx).getUnavailable();
            optimalTimes.add(new TimeRangeAndUnavailable(unavailability.get(uIdx).getTimeRange(), minUnavailability));
          }
        }
        uIdx++;
      }
      if (uIdx >= unavailability.size()) break;

      //If unavail starts during and ends on or after, time range starts at beginning of unavailability slot, and ends at the end of the open slot
      //We move to the next unavailability slot only if they end at same time
      if (unavailability.get(uIdx).getTimeRange().startsDuringEndsOnOrAfter(openRangeList.get(i)) 
      && unavailability.get(uIdx).getTimeRange().end() - openRangeList.get(i).start() >= request.getDuration()) {
        if (unavailability.get(uIdx).getUnavailable() == minUnavailability) {
          optimalTimes.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(
              unavailability.get(uIdx).getTimeRange().start(), openRangeList.get(i).end(), false), minUnavailability));
        }
        if (unavailability.get(uIdx).getUnavailable() < minUnavailability) {
          optimalTimes.clear();
          minUnavailability = unavailability.get(uIdx).getUnavailable();
          optimalTimes.add(new TimeRangeAndUnavailable(TimeRange.fromStartEnd(
              unavailability.get(uIdx).getTimeRange().start(),openRangeList.get(i).end(), false), minUnavailability));

        }
        if (unavailability.get(uIdx).getTimeRange().end() == openRangeList.get(i).end()) {
          uIdx++;
        }
      }
    }
    return optimalTimes;
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

    List<TimeRangeAndUnavailable> optimalTimeAndUnavailables = new ArrayList(optimalTimeAndUnavailables(availableTimes, events, request));
    int numOptional = request.getOptionalAttendees().size();

    //If no optimal subranges are found, just return open slots. No way to optimize.
    if (optimalTimeAndUnavailables.isEmpty()) {
      return availableTimes;
    }
    
    else {
      int minUnavailability = optimalTimeAndUnavailables.get(0).getUnavailable();
      //If the best we can do is that all optional attendees are unavailable, no way to optimize.
      if (minUnavailability == numOptional) {
        return availableTimes;
      }

      //Edge case: If no mandatory and 2 optional attendees and best we can do is 1 free at a time, no meeting.
      if (request.getAttendees().size() == 0 && numOptional == 2 && minUnavailability >= 1) {
        return new ArrayList<TimeRange>();
      }

    }
    List<TimeRange> optimalTimes = new ArrayList<>();
    for (TimeRangeAndUnavailable t : optimalTimeAndUnavailables) {
      if (t.getTimeRange().duration() >= request.getDuration()) {
        optimalTimes.add(t.getTimeRange());
      }
    } 

    if (request.getAttendees().size() > 0 && optimalTimes.isEmpty()) {
      return availableTimes;
    }

    return optimalTimes;
  }
}
