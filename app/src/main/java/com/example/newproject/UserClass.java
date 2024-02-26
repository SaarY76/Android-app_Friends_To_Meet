package com.example.newproject;
import android.graphics.Bitmap;
import java.util.ArrayList;

// this class represent a pattern for representing a user from the database and when creating one
public class UserClass
{
    private String name;// the name of the user
    private String email;// the email of the user
    private String area;// the area where the user is living
    private String summery;// the summery profile of the user
    private String password;// the password of the user
    private ArrayList<String> interests;// a list of the interests of the user

    private String gender;// the gender of the user
    private long birthDate;// the representation in a long type of the user's birthdate

    private String uniqueID;// the unique id of the user from the database

    private Bitmap image;// the image of the user from the database

    public UserClass(String email ,String name,
                     String area, ArrayList<String> interests, String gender, long birthDate, String summery)
    {// constructor for getting User Profile from firebase to show
        this.email = email;
        this.name = name;
        this.area = area;
        this.interests = interests;
        this.gender = gender;
        this.birthDate = birthDate;
        this.summery = summery;
    }

    // necessary empty constructor
    public UserClass ()
    {

    }

    // getters and setters
    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public String getPassword (){return this.password;};

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setUniqueID (String uniqueID)
    {
        this.uniqueID = uniqueID;
    }

    public String getUniqueID ()
    {
        return this.uniqueID;
    }

    public String getEmail() {
        return email;
    }

    public String getArea()
    {
        return area;
    }

    public String getSummery()
    {
        return summery;
    }

    public ArrayList<String> getInterests()
    {
        return interests;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public void setSummery(String summery) {
        this.summery = summery;
    }

    public void setInterests(ArrayList<String> interests) {
        this.interests = interests;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public long getBirthDate()
    {
        return birthDate;
    }

    public void setBirthDate(long birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * function that returns true if the interests and living area are the same between two users
     * and else false
     * @param other - other UserClass object
     */
    public boolean equals (UserClass other)
    {
        if (this.interests.size() != other.getInterests().size())
            return false;
        for (int i=0; i<this.interests.size(); i++)
        {
            if (!other.getInterests().contains(this.interests.get(i)))
                return false;
        }
        return this.area.equals(other.getArea());
    }

    @Override
    public String toString()
    {
        return "UserClass{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", summery='" + summery + '\'' +
                ", interests=" + interests +
                ", gender='" + gender + '\'' +
                ", birthDate=" + birthDate +
                '}';
    }
}
