
/** 
 * Validates the user's inputted number of comments to be displayed, called on input.
 * @return {boolean} Whether user input was a valid, postive integer.
 */
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
 * Creates an html comment element.
 * @param {Comment} The comment to be displayed.
 */
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
 * Gets comments for the page based on user inputted number of comments, fills map with comment pins.
 */
function getComments() {
  clearComments();
  let buttonDiv = document.getElementById("delete-button");
  let commentsContainer = document.getElementById("comments-container");
  const numberCommentsInput = document.getElementById("number-of-comments").value;
  fetch("/data?number=" + numberCommentsInput).then(response => response.json()).then((comments) => {
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

/** 
 * Clears all comments off the display.
 */
function clearComments() {
  const commentsContainer = document.getElementById("comments-container");
  commentsContainer.innerText = "";
  let buttonDiv = document.getElementById("delete-button");
  buttonDiv.innerText = "";  
}

/** 
 * Deletes all comments from the backend, reloads the page.
 */
function deleteComments() {
  fetch("/delete-data", {method: "POST"});
  window.location.reload();
}

/** 
 * Creates a timestamp string for a given comment
 * @param {comment} The comment to be displayed.
 * @return {string} The timestamp string for the comment.
 */
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

/** 
 * Renders the comment form based on the user's login status.
 */
function renderForm() {
  let commentForm = document.getElementById("comment-form");
  let authDiv = document.getElementById("auth-message");
  fetch("/auth").then(response => response.json())
  .then(res => {
    if (res.isLoggedIn) {
      commentForm.style.display = "block";
      authDiv.appendChild(createLoggedInAuthMessage(res.url));
    }
    else {
      commentForm.style.display = "none";
      authDiv.appendChild(createLoggedOutAuthMessage(res.url));
    }
  });
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(function(position) {
      commentForm.lat.value = position.coords.latitude;
      commentForm.lng.value = position.coords.longitude;
    });
  }
}

/**
 * Creates and returns the auth message if user is logged in.
 * @return {Document Element} Mesasge for logged in users.
 */
function createLoggedInAuthMessage(loginUrl) {
  let authMessage = document.createElement("p");
  let authLink = document.createElement("a");
  authLink.innerText = "here";
  authLink.href = loginUrl;
  authMessage.appendChild(document.createTextNode("Logout "));
  authMessage.appendChild(authLink);
  authMessage.appendChild(document.createTextNode("."));
  return authMessage;
}

/**
 * Creates and returns the auth message if user is logged out.
 * @return {Document Element} Mesasge for logged out users.
 */
function createLoggedOutAuthMessage(logoutUrl) {
  let authMessage = document.createElement("p");
  let authLink = document.createElement("a");
  authLink.innerText = "here";
  authLink.href = logoutUrl;
  authMessage.appendChild(document.createTextNode("Login "));
  authMessage.appendChild(authLink);
  authMessage.appendChild(document.createTextNode(" to leave comments."));
  return authMessage;
}

let map; 

/** 
 * Creates the default map, with a pin for my hometown.
 */
function makeMap() {
  map = new google.maps.Map(document.getElementById("map-container"), {
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

/** 
 * Creates and drops a pin and info window for a given comment
 * @param {comment} The comment to be displayed.
 * @param {comment} The time delay for animating the pin drop.
 */
function addPin(comment, time) {
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
  }, time); 
}

/** 
 * Returns the map to its default state.
 */
function emptyMap() {
  for (let i = 0; i < markers.length; i++) {
    markers[i].setMap(null);
  }
  markers = [];   
}

/** 
 * Clears map and renders pins for requested comments.
 */
function fillMap() {  
  emptyMap();
  const numberCommentsInput = document.getElementById("number-of-comments").value;
    fetch("/data?number=" + numberCommentsInput).then(response => response.json()).then((comments) => {
      comments.forEach((c, i) => {
        addPin(c, 500 * i);
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

