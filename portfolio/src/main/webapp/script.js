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

function createCommentElement(comment) {
  let commentElement = document.createElement("div");

  let textDiv = document.createElement("div");
  let textSpan = document.createElement("div");
  textSpan.innerText = comment.text;
  textSpan.classList.add("comment-text");
  textDiv.appendChild(textSpan);
  textDiv.classList.add("box");
  commentElement.appendChild(textDiv);

  let infoDiv = document.createElement("div");
  let nameSpan = document.createElement("span");
  nameSpan.innerText = `${comment.name} (${comment.email}) `;
  nameSpan.classList.add("comment-name");
  infoDiv.appendChild(nameSpan);
  let timeSpan = document.createElement("span");
  timeSpan.innerText = getTimeStamp(comment);
  timeSpan.classList.add("comment-time");
  infoDiv.appendChild(timeSpan);
  infoDiv.classList.add("comment-info");
  commentElement.appendChild(infoDiv);

  return commentElement;
}
/**
 * Gets comments for the page.
 */
function getComments() {
  const commentsContainer = document.getElementById("comments-container");
  commentsContainer.innerText = "";
  let buttonDiv = document.getElementById("delete");
  buttonDiv.innerText = "";
  const numberCommentsInput = document.getElementById("number-of-comments").value;
  fetch("/data?number=" + numberCommentsInput).then(response => response.json()).then((comments) => {
    const commentsContainer = document.getElementById("comments-container");
     comments.forEach(c => {
        let comment = createCommentElement(c);
        commentsContainer.appendChild(comment);
    });
    if (comments.length > 0) {
      let button = document.createElement("button");
      button.onclick = function() { deleteComments(); }; 
      if (comments.length === 1) {
        button.innerText = "Delete Only Comment";
      }
      else {
        button.innerText = `Delete All ${comments.length} Comments`;
      }
      buttonDiv.appendChild(button);
    }
  }); 
  fillMap();
}

function clearComments() {
  const commentsContainer = document.getElementById("comments-container");
  commentsContainer.innerText = "";
  let buttonDiv = document.getElementById("delete");
  buttonDiv.innerText = "";  
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
    return seconds === 1 ? `${seconds} second ago` : `${seconds} seconds ago`;
  }
  else if (minutes < 60) {
    return minutes === 1 ? `${minutes} minute ago` : `${minutes} minutes ago`;
  }
  else if (hours < 24) {
    return hours === 1 ? `${hours} hour ago` : `${hours} hours ago`;
  }
  else if (days < 365) {
    return days === 1 ? `${days} day ago` : `${days} days ago`;
  }
  else {
    return years === 1 ? `${years} year ago` : `${years} years ago`;
  }
}

function renderForm() {
  let commentForm = document.getElementById("comment-form");
  let authDiv = document.getElementById("auth-message");
  fetch("/auth").then(response => response.json())
  .then(res => {
    if (res.isLoggedIn) {
      commentForm.style.display = "block";
      let authMessage = document.createElement("p");
      let authLink = document.createElement("a");
      authLink.innerText = "here";
      authLink.href = res.url;
      authMessage.appendChild(document.createTextNode("Logout "));
      authMessage.appendChild(authLink);
      authMessage.appendChild(document.createTextNode("."));
      authDiv.appendChild(authMessage);
    }
    else {
      commentForm.style.display = "none";
      let authMessage = document.createElement("p");
      let authLink = document.createElement("a");
      authLink.innerText = "here";
      authLink.href = res.url;
      authMessage.appendChild(document.createTextNode("Login "));
      authMessage.appendChild(authLink);
      authMessage.appendChild(document.createTextNode(" to leave comments."));
      authDiv.appendChild(authMessage);
    }
  });
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function(position) {
      commentForm.lat.value = position.coords.latitude;
      commentForm.lng.value = position.coords.longitude;
    });
  }
}

let map; 

function makeMap() {
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
    infowindow.open(map, marker);
    marker.setAnimation(google.maps.Animation.BOUNCE);
      setTimeout(function() { marker.setAnimation(null); }, 600);
  });
}

let markers = [];
let marker;

function addPin(comment, time, i) {
  setTimeout(function() {
    marker = new google.maps.Marker({
      position: {
        lat: comment.lat, 
        lng: comment.lng
      }, 
      animation: google.maps.Animation.DROP,
      map: map
    });
    
    markers.push(marker);

    const contentString = `<div> ${comment.name} </div> <div> ${comment.text} </div>`;
    const infowindow = new google.maps.InfoWindow({content: contentString});

    marker.addListener("click", function() {
      infowindow.open(map, marker);
      marker.setAnimation(google.maps.Animation.BOUNCE);
        setTimeout(function() { marker.setAnimation(null); }, 600);
      });
  }, time * i); 
}

function emptyMap() {
  for (let i = 0; i < markers.length; i++) {
    markers[i].setMap(null);
  }
  markers = [];   
}

function fillMap() {  
  emptyMap();
  const numberCommentsInput = document.getElementById("number-of-comments").value;
    fetch("/data?number=" + numberCommentsInput).then(response => response.json()).then((comments) => {
      comments.forEach((c, i) => {
        addPin(c, 1000, i);
      });
    });
}

document.addEventListener("DOMContentLoaded", function() {
  renderForm();
  makeMap();
  document.getElementById("number-of-comments").addEventListener("input", function() {
    if (validateForm()) {
      getComments();
    }
    else {
      clearComments();
    }
  });
});

