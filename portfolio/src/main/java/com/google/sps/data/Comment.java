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

  /** Constructor for a Comment Object */
  public Comment(long i, String email, String n, String txt, long time){
    id = i;
    this.email = email;
    name = n;
    text = txt;
    timestamp = time;
  }
}