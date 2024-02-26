package com.example.newproject;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;


import android.app.AlertDialog;

import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends NotTurnAroundClass
{
    private EditText editTextEmail;
    private EditText editTextPassword;
    private TextView forgotPassword;
    private TextView errorMessageTextView;
    private CheckBox checkBoxSaveLogin;
    private TextView contactUs;

    // a variable that indicates if the user want to save his login details for next time log in
    private boolean isUserWantToSaveLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginactivity);

        // disable the dark mode on the user's phone in this app and rotation to screen
        PublicFunctions.disableDarkModeAndRotationToScreen(this);

        // Check if the notifications are not enabled
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled())
        {
            // Create an AlertDialog to tell the user about the problem
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Notifications are Disabled")
                    .setMessage("This app will be better using notifications, go to Settings to enable, \n" +
                            "if you don't want to click on cancel")
                    .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Go to app settings
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);

                            // for Android 5-7
                            intent.putExtra("app_package", getPackageName());
                            intent.putExtra("app_uid", getApplicationInfo().uid);

                            // for Android O and beyond
                            intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());

                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
            builder.show();
        }

        // sign out the user if the user hasn't signed out when coming back from the main activity
        DatabaseActivitiesUser.signOutUser();

        // Retrieve login details
        String[] loginDetails = PublicFunctions.retrieveLoginDetails(this);
        String savedEmail = loginDetails[0];
        String savedPassword = loginDetails[1];

        Intent intent = getIntent();
        boolean isUserLoggedOut = false;
        if (intent != null && intent.hasExtra("isUserLoggedOut"))
        {
            isUserLoggedOut = intent.getBooleanExtra("isUserLoggedOut", false);
        }
        if (!isUserLoggedOut && (savedEmail != null  && !savedEmail.equals("")) && (savedPassword != null  && !savedPassword.equals("")))
        {
            loginWithSavedData(savedEmail, savedPassword);
        }

        editTextEmail = findViewById(R.id.emailEditText);
        editTextEmail.setText("");
        editTextPassword = findViewById(R.id.passwordEditText);
        editTextPassword.setText("");
        editTextPasswordVisibility(editTextPassword);

        // handling the forgot password us button
        forgotPassword = findViewById(R.id.forgot_password);
        forgotPassword.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showForgotPasswordDialog();
            }
        });
        // handling the contact us button
        contactUs = findViewById(R.id.contactUs);
        contactUs.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                contactUs();
            }
        });
        errorMessageTextView = findViewById(R.id.errorMessageTextView);
        checkBoxSaveLogin = findViewById(R.id.checkBoxSaveLogin);
        isUserWantToSaveLogin = false;
    }

    /**
     * we override this method for - if the user is in the login activity (this)
     * he/she will go only to their previous screen on their phone and not in the app
     */
    @Override
    public void onBackPressed()
    {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    /**
     * changing the check box of the save login details
     * @param view - a Check Box
     */
    public void changeUserWantToSaveLogin (View view)
    {
        if (checkBoxSaveLogin.isChecked())
            isUserWantToSaveLogin = true;
        else
            isUserWantToSaveLogin = false;
    }

    /**
     * the function handles the contact us button and it called from the on click listener of
     * the button, and it intent an email chooser for the user, to choose an email app to
     * send the email to contact us
     */
    private void contactUs()
    {
        String email = "friendstomeet76@gmail.com";
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[] {email});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Your Subject Here");
        startActivity(Intent.createChooser(intent, "Choose an email client"));
    }

    /**
     * the function handles the eye icon in the password edit text that it will show and will not
     * show, and also handles the time it will be clicked to show the password and will not show it
     * @param editTextPassword - an EditText of the password the user enters for the log in
     */
    public void editTextPasswordVisibility(EditText editTextPassword)
    {

        Drawable startDrawable = getResources().getDrawable(R.drawable.passwordforlogin);
        Drawable visibilityDrawable = getResources().getDrawable(R.drawable.baseline_visibility_24);

        editTextPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(startDrawable, null, null, null);

        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No implementation needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No implementation needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    // Show the visibility toggle drawable
                    editTextPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(startDrawable, null, visibilityDrawable, null);
                } else {
                    // Hide the visibility toggle drawable
                    editTextPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(startDrawable, null, null, null);
                }
            }
        });

        editTextPassword.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Drawable[] compoundDrawables = editTextPassword.getCompoundDrawables();
                Drawable visibilityDrawable = compoundDrawables[2];

                if (compoundDrawables.length >= 3 && visibilityDrawable != null) {
                    int drawableWidth = visibilityDrawable.getIntrinsicWidth();
                    int touchAreaRight = editTextPassword.getRight() - editTextPassword.getPaddingRight();
                    int touchAreaLeft = touchAreaRight - drawableWidth - 32; // Adjust the value as per your desired clickable area size

                    if (event.getAction() == MotionEvent.ACTION_UP && event.getRawX() >= touchAreaLeft && event.getRawX() <= touchAreaRight) {
                        // Toggle password visibility
                        if (editTextPassword.getInputType() == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                            editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            editTextPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_24, 0);
                        } else {
                            editTextPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                            editTextPassword.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.baseline_visibility_off_24, 0);
                        }

                        // Move the cursor to the end of the text
                        editTextPassword.setSelection(editTextPassword.getText().length());

                        return true; // Consume the touch event
                    }
                }

                return false;
            }
        });
    }

    /**
     * the function is the on click for the registration button that will intent to the
     * registration activity
     * @param view = a Button
     */
    public void registration(View view)
    {// when the user click on this button it will move him to Registration Activity
        Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
        startActivity(intent);

    }

    /**
     * the function calls the sign in with email and password function with the saved
     * email and password from the shared preferences and if it's a valid information
     * it will sign in the user
     * @param email - the saved email from the shared preferences
     * @param password - the saved password from the shared preferences
     */
    private void loginWithSavedData (String email, String password)
    {
        DatabaseActivitiesUser.signInWithEmailAndPassword(email,password,
                this.getApplicationContext()).thenAccept(result ->
        {
            if ((Boolean) result[0])
            {
                Intent intent = new Intent(LoginActivity.this, MainUserActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * the function handles the login button click and it called from the on click of the button,
     * it check if the email and password that was taken from the edit text elements in this
     * activity and if they are valid information in the database for a user it will
     * move the user to the main activity, and if it's not, the user will see the reason
     * in a TextView element that will show it
     * @param email - the email that the user entered in the edit text element
     * @param password the password that the user entered in the edit text element
     */
    private void loginFunction (String email, String password)
    {
        if (PublicFunctions.isEmailValid(email) &&
                PublicFunctions.isPasswordValid(password))
        {
            DatabaseActivitiesUser.signInWithEmailAndPassword(email,password,
                    this.getApplicationContext()).thenAccept(result ->
            {
                if ((Boolean) result[0])
                {
                    editTextEmail.setText("");
                    editTextPassword.setText("");
                    errorMessageTextView.setText((String)result[1]);
                    if (isUserWantToSaveLogin)
                        PublicFunctions.saveLoginDetails(this, email, password);
                    Intent intent = new Intent(LoginActivity.this, MainUserActivity.class);
                    startActivity(intent);
                }
                else
                {
                    errorMessageTextView.setText((String)result[1]);
                }
            });
        }
        else
        {
            String emailInput = editTextEmail.getText().toString();
            String passwordInput = editTextPassword.getText().toString();
            boolean emailError = false;
            boolean passwordError = false;
            if (emailInput.equals(""))
            {
                errorMessageTextView.setText("Your email is empty\n");
                emailError = true;
            }
            if (passwordInput.equals(""))
            {
                if (!emailError)
                    errorMessageTextView.setText("Your Password is empty");
                else
                    errorMessageTextView.setText(errorMessageTextView.getText().toString() + "Your Password is empty");
                passwordError = true;
            }
            if (emailError && passwordError)
                return;

            if (!PublicFunctions.isEmailValid(emailInput) && !emailError)
            {
                if (passwordError)
                    errorMessageTextView.setText("Your email syntax isn't valid\n" + errorMessageTextView.getText().toString());
                else
                    errorMessageTextView.setText("Your email syntax isn't valid\n");
                emailError = true;
            }
            if (PublicFunctions.isPasswordValid(passwordInput) && !passwordError)
            {
                if (!emailError)
                    errorMessageTextView.setText("Your password length isn't valid");
                else
                    errorMessageTextView.setText(errorMessageTextView.getText().toString() +"Your password length isn't valid");
            }
        }
    }

    /**
     * the function handles the on click on the login button with calling to a helper function
     * @param view - a Button
     */
    public void login(View view)
    {
        loginFunction(editTextEmail.getText().toString(),editTextPassword.getText().toString());
    }

    /**
     * the function shows to the user an alert dialog when the user clicks on the
     * forgot password in this activity
     */
    private void showForgotPasswordDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        builder.setMessage("Please enter your email address:");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = input.getText().toString();
                if (PublicFunctions.isEmailValid(email))
                {
                    // Call a method to handle password reset with the user's email
                    DatabaseActivitiesUser.sendResetPassword(getApplicationContext(), email);
                } else {
                    Toast.makeText(getApplicationContext(), "Invalid email address. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
