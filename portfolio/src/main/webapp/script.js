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

/**
 * Gets comments for the page.
 */
function getComments() {
    fetch("/data").then(response => response.json()).then((comments) => {
      const commElem = document.getElementById('comments-container');
      commElem.innerHtml = '';
      
      comments.forEach(c => {
          var d = document.createElement("div");
          d.innerHTML = "<div class='comment-name'>" + c.name + "</div>" + 
                        "<div class='comment-text'>" + c.text + "</div>" +
                        "<div class='comment-time'>" + getTimeStamp(c) + "</div>";
          d.classList.add("comment");
          commElem.appendChild(d);
      });
    });  
}

function getTimeStamp(comment) {
  var time = ((new Date()).getTime() - comment.timestamp)/1000; 
  var seconds = (time).toFixed(0);
  var minutes = (time/60).toFixed(0);
  var hours = (time/(60*60)).toFixed(0);
  var days = (time/(60*60*24)).toFixed(0);
  var years = (time/(60*60*24*365)).toFixed(0);
  if (seconds < 1) {
    return "just now";
  }
  if (seconds < 60) {
    return seconds === 1 ? seconds + " second ago" : seconds + " seconds ago";
  }
  else if (minutes < 60) {
    return minutes === 1 ? minutes + " second ago" : minutes + " minutes ago";
  }
  else if (hours < 24) {
    return hours === 1 ? hours + " second ago" : hours + " hours ago";
  }
  else if (days < 365) {
    return days === 1 ? days + " second ago" : days + " days ago";
  }
  else {
    return years === 1 ? years + " second ago" : years + " years ago";
  }
}
