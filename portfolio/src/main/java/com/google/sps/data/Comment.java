package com.google.sps.data;

/**
 * Class representing a comment.
 *
 * Note: The private variables in this class are converted into JSON.
 */
public class Comment {
    
  private final long id;
  private final String email;
  private final String name;
  private final String text;
  private final long timestamp;
  private final double lat;
  private final double lng;

  /** Constructor for a Comment Object */
  public Comment(long id, String email, String name, String text, long timestamp, double lat, double lng){
    this.id = id;
    this.email = email;
    this.name = name;
    this.text = text;
    this.timestamp = timestamp;
    this.lat = lat;
    this.lng = lng;
  }
}