package com.google.sps.data;

/**
 * Class representing an auth response.
 *
 * Note: The private variables in this class are converted into JSON.
 */
public class AuthResponse {
    
  private final boolean isLoggedIn;
  private final String url;

  /** Constructor for an AuthResponse Object */
  public AuthResponse(boolean isLoggedIn, String url){
    this.isLoggedIn = isLoggedIn;
    this.url = url;
  }
}