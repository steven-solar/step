// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

function validateForm() {
  const numberCommentsInput = document.getElementById("number-of-comments").value;
  const errorSpan = document.getElementById("error-message");
  const regex = /^\d+$/;
  errorSpan.innerText = "";
  if (numberCommentsInput.length > 0) {
    if (regex.test(numberCommentsInput)) {
      errorSpan.innerText = "";
      return true;
    }
    else {
      errorSpan.innerText = "Please enter a valid, positive integer."
      return false;
    }
  }
  else {
    return false;
  }
}

/**
 * Gets comments for the page.
 */
function getComments() {
  const commElem = document.getElementById("comments-container");
  commElem.innerHTML = "";
  var buttonDiv = document.getElementById("delete");
  buttonDiv.innerHTML = "";
  const number = document.getElementById("number-of-comments").value;
  fetch("/data?number=" + number).then(response => response.json()).then((comments) => {
    const commElem = document.getElementById("comments-container");
     comments.forEach(c => {
        var textDiv = document.createElement("div");
        textDiv.innerHTML = "<div class='comment-text'>" + c.text + "</div>";
        textDiv.classList.add("box");
        commElem.appendChild(textDiv);
        var infoDiv = document.createElement("div");
        infoDiv.innerHTML = "<span class='comment-name'>" + c.name + " " + "</span>" + 
                      "<span class='comment-name'>" + " ("+ c.email + ") " + "</span>" +
                      "<span class='comment-time'>" + getTimeStamp(c) + "</span>";
        infoDiv.classList.add("comment-info");
        commElem.appendChild(infoDiv);
    });
    if (comments.length > 0) {
      var button = document.createElement("button");
      button.onclick = function() { deleteComments(); }; 
      if (comments.length === 1) {
        button.innerText = "Delete Only Comment";
      }
      else {
        button.innerText = "Delete All " + comments.length + " Comments";
      }
      buttonDiv.appendChild(button);
    }
  }); 
  fillMap();
}

function deleteComments() {
  fetch("/delete-data", {method: "POST"});
  window.location.reload();
}

function getTimeStamp(comment) {
  const timeSinceLastUpdated = ((new Date()).getTime() - comment.timestamp)/1000; 
  const seconds = (timeSinceLastUpdated).toFixed(0);
  const minutes = (timeSinceLastUpdated/60).toFixed(0);
  const hours = (timeSinceLastUpdated/(60*60)).toFixed(0);
  const days = (timeSinceLastUpdated/(60*60*24)).toFixed(0);
  const years = (timeSinceLastUpdated/(60*60*24*365)).toFixed(0);
  if (seconds < 1) {
    return "just now";
  }
  if (seconds < 60) {
    return seconds === 1 ? seconds + " second ago" : seconds + " seconds ago";
  }
  else if (minutes < 60) {
    return minutes === 1 ? minutes + " minute ago" : minutes + " minutes ago";
  }
  else if (hours < 24) {
    return hours === 1 ? hours + " hour ago" : hours + " hours ago";
  }
  else if (days < 365) {
    return days === 1 ? days + " day ago" : days + " days ago";
  }
  else {
    return years === 1 ? years + " year ago" : years + " years ago";
  }
}

function renderForm() {
  var commentForm = document.getElementById("comment-form");
  const authDiv = document.getElementById("auth-message");
  fetch("/auth").then(response => response.json())
  .then(res => {
    if (res.isLoggedIn) {
      commentForm.style.display = "block";
      authDiv.innerHTML = "<p>Logout <a href=\"" + res.url + "\">here</a>.</p>"
    }
    else {
      commentForm.style.display = "none";
      authDiv.innerHTML = "<p>Login <a href=\"" + res.url + "\">here</a> to leave comments.</p>"
    }
  });
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function(position) {
      commentForm.lat.value = position.coords.latitude;
      commentForm.lng.value = position.coords.longitude;
    });
  }
}

var map; 

function initMap() {
  map = new google.maps.Map(document.getElementById("map"), {
    center: {lat: 40.7128, lng: -98.006},
    zoom: 4
  });

  const marker = new google.maps.Marker({
    position: {
      lat: 40.9115, 
      lng: -73.7824
    }, 
    title: "My Hometown!",
    animation: google.maps.Animation.DROP,
    map: map
  });
  
  const contentString = "<div> I live in New Rochelle, NY </div>";
  const infowindow = new google.maps.InfoWindow({
    content: contentString
  });

  marker.addListener("click", function() {
    if (marker.getAnimation() === google.maps.Animation.BOUNCE)  
      marker.setAnimation(null);
    else {
      infowindow.open(map, marker);
      marker.setAnimation(google.maps.Animation.BOUNCE);
      setTimeout(function() { marker.setAnimation(null); }, 600);
    }
  });
}

var markers = [];
var marker;

function fillMap() {
  for (var i = 0; i < markers.length; i++) {
    markers[i].setMap(null);
  }
  markers = [];
  const number = document.getElementById("number").value;
    fetch("/data?number=" + number).then(response => response.json()).then((comments) => {
      for (const c of comments) {
        window.setTimeout(function() {
          marker = new google.maps.Marker({
            position: {
              lat: c.lat, 
              lng: c.lng
            }, 
            animation: google.maps.Animation.DROP,
            map: map
          });
          markers.push(marker);
          const contentString = "<div>" + c.name + "</div>" + "<div>" + c.text + "</div>";
          const infowindow = new google.maps.InfoWindow({content: contentString});
          marker.addListener("click", function() {
            infowindow.open(map, marker);
            marker.setAnimation(google.maps.Animation.BOUNCE);
            setTimeout(function() { marker.setAnimation(null); }, 600);
          });
        }, 100);
      }
    });
}

document.addEventListener("DOMContentLoaded", function() {
  renderForm();
  makeMap();
  document.getElementById("number-of-comments").addEventListener("input", function() {
    if (validateForm()) {
      getComments();
    }
  });
});

