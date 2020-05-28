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
          console.log(c);
          var li = document.createElement("li");
          li.innerText = c;
          commElem.appendChild(li);
      });
    });  
}
