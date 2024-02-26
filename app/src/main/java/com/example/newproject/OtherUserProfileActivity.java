package com.example.newproject;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.gson.Gson;
import java.util.ArrayList;

public class OtherUserProfileActivity extends AppCompatActivity
{
    private ImageView profileOtherImage;
    private TextView nameOtherTextView;
    private TextView ageOtherTextView;
    private TextView genderOtherTextView;
    private TextView areaOtherTextView;
    private TextView summeryOtherTextView;
    private TextView interestsOtherTextView;

    // a pattern to store the other user's information from the Intent
    private UserClass user;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_user_profile);

        // disable the dark mode on the user's phone in this app and rotation to screen
        PublicFunctions.disableDarkModeAndRotationToScreen(this);

        // getting the user by the Intent and if there are no problems,
        // all of the elements in this activity will be filled with the other user's information
        Intent intent = getIntent();
        if (intent.hasExtra("user"))
        {
            // The intent contains the "user" extra
            String userJson = intent.getStringExtra("user");
            Gson gson = new Gson();
            user = gson.fromJson(userJson, UserClass.class);

            nameOtherTextView = findViewById(R.id.nameOtherProfile);
            ageOtherTextView = findViewById(R.id.ageOtherProfile);
            areaOtherTextView = findViewById(R.id.areaOtherProfile);
            genderOtherTextView = findViewById(R.id.genderOtherProfile);
            summeryOtherTextView = findViewById(R.id.summaryOtherProfileTextView);
            interestsOtherTextView = findViewById(R.id.interestsOtherProfileTextView);

            long birthdate = user.getBirthDate();
            String name = user.getName();
            String area = user.getArea();
            String gender = user.getGender();
            String summery = user.getSummery();

            nameOtherTextView.setText(name);
            areaOtherTextView.setText(area);
            genderOtherTextView.setText(gender);
            ageOtherTextView.setText(""+ PublicFunctions.calculateAge(birthdate));
            summeryOtherTextView.setText(summery);

            String str = "";
            for (int i=0; i<user.getInterests().size(); i++)
            {
                if (i != user.getInterests().size()-1)
                    str += user.getInterests().get(i) + ", ";
                else
                    str += user.getInterests().get(i);
            }
            interestsOtherTextView.setText(str);
        }

            colorSharedInterests();

            profileOtherImage = findViewById(R.id.imageViewProfileOtherUser);
            profileOtherImage.setImageBitmap(user.getImage());
    }

    /**
     * this function is calling the user variable from the main activity and get his/her
     * interests list, and then colors the shared interests between the current user and the
     * other user, so the current user will be able to see what are the shared interests between the two
     */
    public void colorSharedInterests ()
    {
        UserClass user = MainUserActivity.getCurrentUser();
        if (user != null)
        {
            ArrayList<String> thisUserInterests = user.getInterests();
            String otherUserInterestsText = interestsOtherTextView.getText().toString();
            int colorSharedInterests = Color.BLUE;
            SpannableString spannableString = new SpannableString(otherUserInterestsText);
            for (String interest : thisUserInterests)
            {
                int startIndex = otherUserInterestsText.indexOf(interest);
                if (startIndex != -1)
                {
                    int endIndex = startIndex + interest.length();
                    spannableString.setSpan(new ForegroundColorSpan(colorSharedInterests),
                            startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            interestsOtherTextView.setText(spannableString);
        }
    }
}