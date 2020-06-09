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


public final class FindMeetingQuery {

  public Collection<TimeRange> mergeTimes(Collection<Event> events, MeetingRequest request) {
    List<String> requestAttendees = new ArrayList(request.getAttendees());
    List<TimeRange> mergedTimes = new ArrayList<TimeRange>();
    ArrayList<Event> eventList = new ArrayList(events);

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

  public boolean hasOptionalAttendees(Event event, MeetingRequest request) {
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
      if (hasOptionalAttendees(e, request)) {
            optionalAttendees = getIntersection(e.getAttendees(), request.getOptionalAttendees());
            Event optionalEvent = new Event(e.getTitle(), e.getWhen(), optionalAttendees);
            optionalOnlyEvents.add(optionalEvent);
          }
    }
    return optionalOnlyEvents;
  }

  public int getUnavailable(TimeRange range, Collection<Event> events) {
    int numUnavailable = 0;
    for (Event e : events) {
      if (e.getWhen().overlaps(range)) {
        numUnavailable += e.getAttendees().size();
      }
    }
    return numUnavailable;
  }

  public TimeAndUnavailable optimalSubRange(TimeRange range, Collection<Event> events, MeetingRequest request) {
    Collection<Event> optionalOnlyEvents = getOptionalOnlyEvents(events, request);

    TimeRange currSubRange = TimeRange.fromStartDuration(range.start(), (int)request.getDuration());
    int currUnavailable = 0;

    TimeRange optimalSubRange = currSubRange;
    int minUnavailable =  getUnavailable(currSubRange, optionalOnlyEvents);

    for (Event e : optionalOnlyEvents) {
      currSubRange = TimeRange.fromStartDuration(e.getWhen().start(), (int)request.getDuration());
      currUnavailable = getUnavailable(currSubRange, optionalOnlyEvents);
      if (currUnavailable < minUnavailable) {
        minUnavailable = currUnavailable;
        optimalSubRange = currSubRange;
      }

      currSubRange = TimeRange.fromStartDuration(e.getWhen().end(), (int)request.getDuration());
      currUnavailable = getUnavailable(currSubRange, optionalOnlyEvents);
      if (currUnavailable < minUnavailable) {
        minUnavailable = currUnavailable;
        optimalSubRange = currSubRange;
      }
    }
    return new TimeAndUnavailable(optimalSubRange, minUnavailable);
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
    int minUnavailable = Integer.MAX_VALUE;
    for (TimeRange t : availableTimes) {
      TimeAndUnavailable optimal = optimalSubRange(t, events, request);
      if (optimal.getUnavailable() < minUnavailable) {
        minUnavailable = optimal.getUnavailable();
      }
    }

    for (TimeRange t : availableTimes) {
      TimeAndUnavailable optimal = optimalSubRange(t, events, request);
      if (optimal.getUnavailable() == minUnavailable) {
        optimalTimes.add(optimal.getRange());
      }
    }

    return optimalTimes;
  }
}
