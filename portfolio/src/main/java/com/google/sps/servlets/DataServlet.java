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

package com.google.sps.servlets;

import java.util.List;
import java.util.ArrayList;
import com.google.sps.data.Comment;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;

/** Servlet that allows users to post and get comments. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    String sNumber = request.getParameter("number");
    int num = 0;
    if (sNumber.length() > 0) {
      try {
        num = Integer.parseInt(sNumber);
      }
      catch (NumberFormatException e) {
        System.err.println(e);
        response.sendRedirect("/");
        return;
      }
    }
    if (num < 0) {
        System.err.println("Enter a positive number.");
        response.sendRedirect("/");
        return;
    }
    
    List<Entity> entities = results.asList(FetchOptions.Builder.withLimit(num));
    List<Comment> comments = new ArrayList<>();
    for(Entity e : entities) {
      long id = e.getKey().getId();
      String email = (String) e.getProperty("email");
      String name = (String) e.getProperty("name");
      String text = (String) e.getProperty("text");
      long timestamp = (long) e.getProperty("timestamp");
      double lat = (double) e.getProperty("lat");
      double lng = (double) e.getProperty("lng");
      Comment comment = new Comment(id, email, name, text, timestamp, lat, lng);
      comments.add(comment);
    }

    Gson gson = new Gson();
    response.setContentType("application/json;");
    response.getWriter().println(gson.toJson(comments));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();   
    String email = userService.getCurrentUser().getEmail();   
    String name = request.getParameter("name");
    String text = request.getParameter("text");
    long timestamp = System.currentTimeMillis();
    double lat = Double.parseDouble(request.getParameter("lat"));
    double lng = Double.parseDouble(request.getParameter("lng"));

    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("email", email);
    commentEntity.setProperty("name", name);
    commentEntity.setProperty("text", text);
    commentEntity.setProperty("timestamp", timestamp);
    commentEntity.setProperty("lat", lat);
    commentEntity.setProperty("lng", lng);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

    response.sendRedirect("/");
  }
}
