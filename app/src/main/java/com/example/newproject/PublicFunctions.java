package com.example.newproject;

import static android.content.Context.MODE_PRIVATE;

import static com.example.newproject.UserSettingsFragment.uploadImageS2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatDelegate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// a class that is sharing a public variables and functions to all of the app's Activities and Fragments
public class PublicFunctions
{
    // two static variables that will not change and are used in couple of places in the Activities
    public static final int MIN_PASSWORD_LENGTH = 8;// the Minimum length of password the user can create
    public static final int MIN_AGE_REGISTRATION = 18;// the Minimum age that the current user is needs to be to create a user in our app

    // the request code of the request to pick an image from the user's phone gallery
    public static final int PICK_IMAGE = 100;

    /**
     * the function gets a CheckBox and an EditText, and it's perform the user's click on the
     * CheckBox to show and unShow the value in the EditText
     * @param showPasswordCheckBox - a CheckBox
     * @param passwordEditText - an EditText with password data in it
     */
    public static void showPassword(CheckBox showPasswordCheckBox, EditText passwordEditText)
    {
        // Save cursor position
        int cursorPosition = passwordEditText.getSelectionStart();
        if (!showPasswordCheckBox.isChecked())
            passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        else
            passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        // Restore cursor position
        passwordEditText.setSelection(cursorPosition);
    }

    /**
     * the function gets a long variable with a data of a birthdate and returns the current age
     * of the user with that birthdate
     * @param dateOfBirthMillis - a long variable with a data of a birthdate
     * @return
     */
    public static int calculateAge(long dateOfBirthMillis)
    {
        Calendar today = Calendar.getInstance();
        Calendar dob = Calendar.getInstance();
        dob.setTimeInMillis(dateOfBirthMillis);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    /**
     * the function gets a long that represents a time and it returns a String that represent it
     * in words
     * @param timestamp a long data of a specific time
     */
    public static String getTimeDifference(long timestamp)
    {
        String timeDifference = "";

        // get current and last sign in calendar days
        Calendar now = Calendar.getInstance();
        Calendar lastSignIn = Calendar.getInstance();
        lastSignIn.setTimeInMillis(timestamp);

        if (now.get(Calendar.DAY_OF_YEAR) == lastSignIn.get(Calendar.DAY_OF_YEAR) &&
                now.get(Calendar.YEAR) == lastSignIn.get(Calendar.YEAR)) {
            timeDifference = "Today at " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        } else if (now.get(Calendar.DAY_OF_YEAR) - lastSignIn.get(Calendar.DAY_OF_YEAR) == 1 ||
                (now.get(Calendar.DAY_OF_YEAR) == 1 && lastSignIn.get(Calendar.DAY_OF_YEAR) > 1)) {
            timeDifference = "Yesterday at " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        } else {
            long days = (now.getTimeInMillis() - lastSignIn.getTimeInMillis()) / (1000 * 60 * 60 * 24) + 1;
            timeDifference = days + " days ago at " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        }

        return timeDifference;
    }

    /**
     * the function returns true if the current String's value is a number and else false
     * @param s - the String we want to check on
     */
    public static boolean isNumber(String s)
    {
        if (s == null || s.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * the function saves in the shared preferences, the login details of the current user
     * @param context - the context of the activity that the function is called from
     * @param email - the email of the current user
     * @param password - the password of the current user
     */
    public static void saveLoginDetails(Context context, String email, String password)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("login_details", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("password", password);
        editor.apply();
    }

    /**
     * the function returns the current user's email and password from the shared preferences
     * @param context - the context of the activity that the function is called from
     */
    public static String[] retrieveLoginDetails(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences("login_details", MODE_PRIVATE);
        String email = sharedPreferences.getString("email", "");
        String password = sharedPreferences.getString("password", "");
        return new String[]{email, password};
    }

    /**
     * the function creates an alert dialog that shows to the user that there is no Internet and
     * it makes the user quit from the app to the login screen
     * @param context - the context of the MainActivity
     */
    public static void alertDialogNoInternet(Context context)
    {
        // Create an AlertDialog to tell the user about the problem
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setCancelable(false)  // This prevents users from dismissing the dialog with the back button
                .setTitle("No Internet Connection")
                .setMessage("There is no Internet, so you will now go to the Login Screen.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // Go to app settings
                        Intent intent = new Intent(context,LoginActivity.class);
                        DatabaseActivitiesUser.signOutUser();
                        context.startActivity(intent);
                    }
                });
        builder.show();
    }

    /**
     * the function returns a boolean that represent if the user is connected to the internet or not
     * @param context - the context of the activity that it called from
     * @return - a boolean with true if the user is connected to the internet and else false
     */
    public static boolean isConnectedToTheInternet (Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * the function sets the activity that passed as a parameter to disable the dark mode
     * in the user's phone from this app and disable rotation to the screen
     * @param activity - the activity we want to set the changes to
     */
    public static void disableDarkModeAndRotationToScreen (Activity activity)
    {
        // disable the dark mode on the user's phone in this app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        // disable rotation
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * the function returns true if the email is matching an email syntax and else false
     * @param email - a String representing an email
     */
    public static boolean isEmailValid(String email)
    { // check if the email is valid
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * the function returns true if the user name is matching aa syntax we chose and else false
     * @param userName - a String representing a user name
     */
    public static boolean isUserNameValid(String userName)
    {
        // Only letters allowed, case-insensitive
        String pattern = "^[a-zA-Z]+$";
        return userName.matches(pattern);
    }

    /**
     * the function returns true if the password length of the user is bigger or equal to
     * the MINIMUM length of password we chose and else false
     * @param password
     * @return
     */
    public static boolean isPasswordValid(String password)
    {
        return password.length() >= PublicFunctions.MIN_PASSWORD_LENGTH;
    }

    /**
     * the function runs when the user click on the upload image button in the : settings Fragment
     * to upload an image from the user's gallery on the phone
     * @param activity - the activity when this function is called from
     */
    public static void uploadImage(Activity activity)
    { // function that uploads the image from the User's phone gallery
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(pickImageIntent, PICK_IMAGE, null);
    }
}

