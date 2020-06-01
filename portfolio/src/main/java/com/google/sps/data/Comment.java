package com.google.sps.data;

/**
 * Class representing a comment.
 *
 * Note: The private variables in this class are converted into JSON.
 */
public class Comment {
    
  private final long id;
  private final String name;
  private final String text;
  private final long timestamp;

  /** Constructor for a Comment Object */
  public Comment(long id, String name, String text, long timestamp){
    this.id = id;
    this.name = name;
    this.text = text;
    this.timestamp = timestamp;
  }
}