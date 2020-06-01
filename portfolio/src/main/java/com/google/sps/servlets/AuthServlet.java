package com.google.sps.servlets;

import com.google.gson.Gson;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json;");
    UserService userService = UserServiceFactory.getUserService();
    Gson gson = new Gson();
    if (userService.isUserLoggedIn()) {
      String logoutUrl = userService.createLogoutURL("/");
      response.getWriter().println("["+gson.toJson(true)+","+gson.toJson(logoutUrl)+"]");
      return;
    }
    String loginUrl = userService.createLoginURL("/");
    response.getWriter().println("["+gson.toJson(false)+","+gson.toJson(loginUrl)+"]");
    return;
  }
}