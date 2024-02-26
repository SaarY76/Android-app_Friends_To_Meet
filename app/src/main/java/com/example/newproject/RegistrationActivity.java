package com.example.newproject;

import android.app.AlertDialog;
import android.app.DatePickerDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RegistrationActivity extends NotTurnAroundClass
{
    private Spinner spinnerAreas;
    private Spinner spinnerGenders;
    private Button birthdateBtn;
    private TextView birthdateText;
    private ImageView imageUser;
    private EditText summeryEditText;
    private LinearLayout interestsLinearLayoutR;
    private EditText emailEditText;
    private EditText passwordEditText;
    private CheckBox showPasswordCheckBox;
    private EditText editTextUserNameR;

    // boolean variables that indicates if specific elements was selected by the user
    private boolean isImageUploaded;
    private boolean isBirthDaySelected = false;
    private boolean isGenderSelected = false;
    private boolean isAreaSelected = false;

    // user's information
    private ArrayList<String> selectedInterests;
    private String area;
    private String gender;
    private long birthDate;
    private ImageView vectorImage;
    private Bitmap image;

    // TextViews error messages
    private TextView errorMessageGender;
    private TextView uploadImageError;
    private TextView errorMessageInterests;
    private TextView errorMessageBirthday;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // disable the dark mode on the user's phone in this app and rotation to screen
        PublicFunctions.disableDarkModeAndRotationToScreen(this);

        // getting the views by their ID from the xml layout
        summeryEditText = findViewById(R.id.editTextSummaryR);
        emailEditText = findViewById(R.id.editTextEmailR);
        passwordEditText = findViewById(R.id.editTextPasswordR);
        editTextUserNameR = findViewById(R.id.editTextUserNameR);
        spinnerAreas = findViewById(R.id.spinnerCityR);
        birthdateText = findViewById(R.id.birthdateText);
        birthdateBtn = findViewById(R.id.birthdateBtn);
        imageUser = findViewById(R.id.uploadImageR);
        interestsLinearLayoutR = findViewById(R.id.interestsLinearLayoutR);
        errorMessageGender = findViewById(R.id.errorMessageGender);
        errorMessageInterests = findViewById(R.id.errorMessageInterests);
        errorMessageBirthday = findViewById(R.id.errorMessageBirthday);
        uploadImageError = findViewById(R.id.errorUploadImage);
        vectorImage = findViewById(R.id.vectorImage);
        showPasswordCheckBox = findViewById(R.id.passwordCheckBox);


        // setting the areas spinner
        ArrayAdapter<String> adapterAreas = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        fillArrayAdapter(DatabaseActivitiesUser.areas, adapterAreas, spinnerAreas);
        // Set the layout for the Spinner items (optional)
        adapterAreas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set a listener for when an item is selected from the Spinner
        setSpinnerListener(spinnerAreas);

        // setting the genders spinner
        spinnerGenders = findViewById(R.id.spinnerGenderR);
        ArrayList<String> genders = new ArrayList<>();
        genders.add("Select");
        genders.add("Man");
        genders.add("Women");
        genders.add("Other");

        // Create an ArrayAdapter to populate the Spinner with the String array
        ArrayAdapter<String> adapterGenders = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, genders);
        spinnerGenders.setAdapter(adapterGenders);
        // Set the layout for the Spinner items (optional)
        adapterGenders.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set a listener for when an item is selected from the Spinner
        setSpinnerListener(spinnerGenders);

        // when the user click on add birthdate
        birthdateBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                // Calculate the maximum date (18 years ago from today)
                Calendar calendarMax = Calendar.getInstance();
                calendarMax.add(Calendar.YEAR, PublicFunctions.MIN_AGE_REGISTRATION);
                long maxDate = calendarMax.getTimeInMillis();
                // Create a new instance of DatePickerDialog and set the maximum date
                DatePickerDialog datePickerDialog = new DatePickerDialog(RegistrationActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar currentYear = Calendar.getInstance();
                        Calendar dateOfBirth = Calendar.getInstance();
                        dateOfBirth.set(year, month, dayOfMonth); // Set the date of birth here
                        birthDate = dateOfBirth.getTimeInMillis();
                        int age = currentYear.get(Calendar.YEAR) - dateOfBirth.get(Calendar.YEAR);

                        // Check if the user hasn't had their birthday yet this year
                        if (calendarMax.get(Calendar.DAY_OF_YEAR) < dateOfBirth.get(Calendar.DAY_OF_YEAR)) {
                            age--;
                        }
                        // Do something with the selected date
                        String birthdate = dayOfMonth + "/" + (month + 1) + "/" + year + ", age : " + age;
                        birthdateText.setText(birthdate);
                        isBirthDaySelected = true;
                        // Here you can collect the selected birthdate and use it in your Java code
                    }
                }, 2000, 0, 1); // Set the initial date to January 1, 2000
                datePickerDialog.getDatePicker().setMaxDate(maxDate);
                datePickerDialog.show();
            }
        });


        // setting the interests checkboxes inside linear layout
        CompletableFuture<List<String>> interests = DatabaseActivitiesUser.getListForDisplay(DatabaseActivitiesUser.interests);
        interests.thenAccept(dbInterests ->
        {
            // Do something with the list of interests
            for (int i = 0; i < dbInterests.size(); i++)
            {
                CheckBox checkbox = new CheckBox(this);
                checkbox.setText(dbInterests.get(i));
                checkbox.setId(i);
                interestsLinearLayoutR.addView(checkbox);
            }
        });
    }

    /**
     * function that adds the selected interests into an array list we will put to the User's details
     * and returns true if at least one interest was selected and else false
     */
    public boolean fillSelectedInterests()
    {
        selectedInterests = new ArrayList<>();
        for (int i = 0; i < interestsLinearLayoutR.getChildCount(); i++) {
            View view = interestsLinearLayoutR.getChildAt(i);
            if (view instanceof CheckBox)
            {
                CheckBox checkBox = (CheckBox) view;
                if (checkBox.isChecked())
                {
                    selectedInterests.add(checkBox.getText().toString());
                }
            }
        }
        return !selectedInterests.isEmpty();
    }

    /**
     * the function fills an array adapter to one of the spinner based
     * on the database reference that is in the parameter
     * @param ref - a Database reference that from it will be displayed a list in the spinner
     * @param adapter - an array adapter that connect between a list and a spinner
     * @param spinner - a spinner that in it will be displayed a list from the database
     */
    public static void fillArrayAdapter(DatabaseReference ref, ArrayAdapter<String> adapter, Spinner spinner)
    {

        DatabaseActivitiesUser.getListForDisplay(ref).thenAccept(names -> {
            adapter.clear();
            adapter.add("Select");
            adapter.addAll(names);
            spinner.setAdapter(adapter);
        });
    }

    /**
     * the function setting a spinner listener so we will be able to collect the data
     * from it and it will function for both two spinners in the xml layout
     * @param spinner - a Spinner that we will be collect the data from
     */
    public void setSpinnerListener(Spinner spinner)
    {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int positionGender = ((ArrayAdapter) spinner.getAdapter()).getPosition("Man");
                // Get the selected item from the Spinner
                if (position != 0) {
                    String selectedItem = parent.getItemAtPosition(position).toString();
                    if (selectedItem.equals("Man") || selectedItem.equals("Women") || selectedItem.equals("Other"))
                    {
                        isGenderSelected = true;
                        gender = selectedItem;
                    }

                    else
                    {
                        isAreaSelected = true;
                        area = selectedItem;

                    }
                } else {
                    if (positionGender != -1) {
                        // the adapter contains the search string
                        isGenderSelected = false;
                    } else {
                        // the adapter does not contain the search string
                        isAreaSelected = false;
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * the function runs when the user click on the upload image button
     * to upload an image from the user's gallery on the phone
     * @param view - the upload image button
     */
    public void uploadImage(View view)
    {
        PublicFunctions.uploadImage(this);
    }

    /**
     * the action we handle after the user clicked on the image in the gallery of the phone
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {// the function handle the result of uploading an image to the app from the user's gallery on the phone
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PublicFunctions.PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                isImageUploaded = true;
                vectorImage.setVisibility(View.GONE);
                // Get the bitmap from the selected image URI
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);

                // Check the orientation of the image and rotate if needed
                int orientation = DatabaseActivitiesUser.getOrientation(selectedImage,this);
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                // Resize the bitmap to a smaller size
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 1000, 1000, true);
                // Set the resized image to the ImageView
                imageUser.setImageBitmap(resizedBitmap);
                image = resizedBitmap;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * the function sets the information and handles the reactions on the alert dialog that shows
     * when the User finished to insert all of his details in the registration page
     * @param view - the submit button after the user sets all his/her registration data
     */
    public void termsR(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Terms and Conditions")
                .setMessage("Please read and agree to the terms and conditions before proceeding:\n"+
                        "- You need to verify your email\n" +
                        "- Don't use a bad language, because other Users can block you")
                .setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isValidInformation();
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Handle negative button click
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * the function that checks all of the details that the User inserted,
     * and if the information is valid it will call functions that will create the User
     * in Firebase and if not, it will tell the User what was wrong
     */
    public void isValidInformation()
    {
        if (PublicFunctions.isEmailValid(emailEditText.getText().toString()) && PublicFunctions.isPasswordValid(passwordEditText.getText().toString()) &&
                fillSelectedInterests() && isBirthDaySelected && PublicFunctions.isUserNameValid(editTextUserNameR.getText().toString())
                && isGenderSelected && isAreaSelected && isImageUploaded)
        {
            // saving the date that the user created his user in our app to check if
            // in his sign in , if he verified his email in less then 24 hours from creating his user

            // Save current time when user creates account
            Calendar currentTime = Calendar.getInstance(); // Get current time
            long accountCreationTime = currentTime.getTimeInMillis(); // Get time in milliseconds

            DatabaseActivitiesUser.createUserWithEmailAndPassword(emailEditText.getText().toString(), passwordEditText.getText().toString()
                    , editTextUserNameR.getText().toString(), area , selectedInterests, gender, birthDate,summeryEditText.getText().toString(),image)
                            .thenAccept(result ->{
                               if (result)
                               {
                                   DatabaseActivitiesUser.signOutUser();
                                   Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                                   startActivity(intent);
                                   Toast.makeText(RegistrationActivity.this, "User Created !", Toast.LENGTH_LONG);
                               }
                               else
                               {
                                   StringBuilder errorMessage = new StringBuilder("Please check your ");
                                   emailEditText.setError("The email address is already in use");
                                   errorMessage.append("email address, ");
                               }
                            });

        }
        else
        {
            checkErrors();
        }
    }

    /**
     * the function checks after errors of the user has done and then, sets error messages so
     * the user will be able to see them and fix his/her information provided
     */
    public void checkErrors()
    { // a function to check the errors
        StringBuilder errorMessage = new StringBuilder("Please check your ");

        if (!PublicFunctions.isEmailValid(emailEditText.getText().toString())) {
            emailEditText.setError("Invalid email address");
            errorMessage.append("email address, ");
        }
        if (!PublicFunctions.isPasswordValid(passwordEditText.getText().toString())) {
            passwordEditText.setError("Invalid password");
            errorMessage.append("password, ");
        }
        if (!fillSelectedInterests())
        {
            errorMessage.append("checkbox, ");
            setErrorTextView(errorMessageInterests);
        }
        else
            cancelError(errorMessageInterests);

        TextView errorMessageArea = findViewById(R.id.errorMessageCity);
        if (!isAreaSelected)
        {
            errorMessage.append("Area, ");

            setErrorTextView(errorMessageArea);
        }
        else
            cancelError(errorMessageArea);
        if (!isGenderSelected)
        {

            errorMessage.append("Gender, ");
            setErrorTextView(errorMessageGender);
        }
        else
            cancelError(errorMessageGender);
        if (!isBirthDaySelected)
        {
            errorMessage.append("birthday, ");
            setErrorTextView(errorMessageBirthday);
        }
        else
            cancelError(errorMessageBirthday);

        if (!PublicFunctions.isUserNameValid(editTextUserNameR.getText().toString())) {
            editTextUserNameR.setError("Invalid username");
            errorMessage.append("username, ");
        }

        if(! isImageUploaded)
        {
            errorMessage.append("image, ");
            setErrorTextView(uploadImageError);
        }
        else
            cancelError(uploadImageError);
        // Remove the trailing comma and space
        errorMessage.setLength(errorMessage.length() - 2);

        Toast.makeText(RegistrationActivity.this, errorMessage.toString() + " and try again.", Toast.LENGTH_SHORT);
    }

    /**
     * the function sets an TextView visible if there is an error in the element next to it in
     * the screen
     * @param error - a TextView
     */
    public static void setErrorTextView(TextView error)
    { // a function to set a error
        error.setVisibility(View.VISIBLE);

    }

    /**
     * the function sets an TextView visibility gone if there is np error in the element next to it
     * in the screen
     * @param errorToCancel - a TextView
     */
    public static void cancelError(TextView errorToCancel)
    { // a function to cancel the error
        errorToCancel.setVisibility(View.GONE);

    }

    /**
     * the function is the on click of the button cancel that the user will click on it when
     * he/her wants to cancel their registration process and then they will be going back to the
     * login screen
     * @param view - a Button
     */
    public void cancelR(View view)
    {
        Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    /**
     * the function is the on click on a CheckBox and it this function there is a call to a function
     * that handles the onclick on the CheckBox and an EditText to show and not show a password
     * in an EditText
     * @param view - a CheckBox
     */
    public void showPassword(View view)
    {
        PublicFunctions.showPassword(showPasswordCheckBox, passwordEditText);
    }
}