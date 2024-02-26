package com.example.newproject;

// this class represent a pattern for representing a message that will be sent between two users in a chat
public class MessageClass
{
    private String message;// the message content
    private String UserSenderId;// the id of the user who sent the message
    private String UserReceivedId;// the id of the user who received the message

    private long time;// the representation of the time that the message was sent

    public MessageClass(String message, String sender, String received, long time)
    {// constructor that fills the object with the necessary data
        this.message = message;
        this.UserSenderId =sender;
        this.UserReceivedId = received;
        this.time = time;
    }

    // necessary empty constructor
    public MessageClass()
    {

    }

    // getters and setters
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUserSenderId(String userSenderId) {
        UserSenderId = userSenderId;
    }

    public void setUserReceivedId(String userReceivedId) {
        UserReceivedId = userReceivedId;
    }

    public String getMessage()
    {
        return message;
    }

    public String getUserSenderId()
    {
        return UserSenderId;
    }

    public String getUserReceivedId()
    {
        return UserReceivedId;
    }
}
