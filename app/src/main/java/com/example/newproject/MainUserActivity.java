package com.example.newproject;

import static com.example.newproject.UserSettingsFragment.editTextSummaryS;
import static com.example.newproject.UserSettingsFragment.editTextUserNameS;
import static com.example.newproject.UserSettingsFragment.errorMessageInterestsSetting;
import static com.example.newproject.UserSettingsFragment.linearLayoutS;
import static com.example.newproject.UserSettingsFragment.spinnerCityS;
import static com.example.newproject.UserSettingsFragment.uploadImageS2;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.BidiFormatter;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;


import java.io.IOException;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainUserActivity extends NotTurnAroundClass
{
    // a variable that represent the current user with the details of him/her from the database
    private static UserClass currentUser;

    // setters and getters for the currentUser variable
    public static UserClass getCurrentUser()
    {
        return currentUser;
    }

    public static void setCurrentUser(UserClass currentUser)
    {
        MainUserActivity.currentUser = currentUser;
    }

    private BottomNavigationView bottomNav;
    private MenuItem selectedNavItem;
    private Fragment currentFragment;
    private Fragment previousFragment = null;

    // variables that will store the data that the user changed in the settings fragment
    private static List<String> selectedInterests;

    public static String selectedArea;

    public static String newPassword;

    public static String newEmail;

    private Bitmap userNewImage;
    public static boolean isImageChanged = false;

    // two variables that helps to run a code in each couple of time
    private Handler handler;
    private Runnable chatUpdater;

    // Hashmap that stores the ids of the users the current user has a chat with them and the
    // number of messages with each one
    private static HashMap<String, Integer> chatsCount;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_user);

        // disable the dark mode on the user's phone in this app and rotation to screen
        PublicFunctions.disableDarkModeAndRotationToScreen(this);

        // first when this activity loads, we load the current user's details from the database
        // and store the details in a UserClass variable
        DatabaseActivitiesUser.getUserProfileDetailsFromDatabase(DatabaseActivitiesUser.mAuth.getUid()).thenAccept(user ->
        {
            this.setCurrentUser(user);

            // sets a default first Fragment as the home fragment
            Fragment defaultFragment = new UserHomeConnectionsFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.linear_layout_empty, defaultFragment).commit();
            currentFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);

            bottomNav = findViewById(R.id.layout_navigation_bar);

            // Set an OnNavigationItemSelectedListener to handle clicks on the items in the nav bar
            bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
            {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item)
                {
                    Fragment selectedFragment = null;
                    // Determine which item was clicked and create a fragment accordingly
                    switch (item.getItemId())
                    {
                        case R.id.settingsButton:
                            previousFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);
                            selectedFragment = new UserSettingsFragment();
                            break;
                        case R.id.profileButton:
                            if (previousFragment instanceof UserSettingsFragment) {
                                ((UserSettingsFragment) previousFragment).resetListenersAndVariables();
                            }
                            previousFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);
                            selectedFragment = new UserProfileFragment();
                            break;
                        case R.id.homeButton:
                            if (previousFragment instanceof UserSettingsFragment) {
                                ((UserSettingsFragment) previousFragment).resetListenersAndVariables();
                            }
                            previousFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);
                            selectedFragment = new UserHomeConnectionsFragment();
                            break;
                        case R.id.chatsButton:
                            if (previousFragment instanceof UserSettingsFragment) {
                                ((UserSettingsFragment) previousFragment).resetListenersAndVariables();
                            }
                            previousFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);
                            selectedFragment = new UserChatsFragment();
                            break;
                    }
                    // Check if selected fragment is same as current fragment
                    if (currentFragment != null && currentFragment.getClass() == selectedFragment.getClass())
                    {
                        return true; // if same, do nothing
                    }

                    // Replace the current fragment with the selected one
                    if (previousFragment != null)
                        getSupportFragmentManager().beginTransaction().replace(R.id.linear_layout_empty,
                                selectedFragment).addToBackStack(null).commit();
                    else
                        getSupportFragmentManager().beginTransaction().replace(R.id.linear_layout_empty,
                                selectedFragment).commit();
                    selectedNavItem = item; // Store the selected navigation item
                    currentFragment = selectedFragment;
                    return true;
                }
            });
            // Set the home button as the default selected button
            bottomNav.setSelectedItemId(R.id.homeButton);

            // starting the runner and handler to start listening to messages from other users
            startChatUpdates();
        });
    }

    // the function that runs when the activity closes or destroyed
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // Code to be executed when the app is closed or removed from the open apps
        stopChatUpdates();
        DatabaseActivitiesUser.signOutUser();
    }

    // the function that runs when the user clicks on the back button on the phone
    @Override
    public void onBackPressed()
    {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.getBackStackEntryCount() > 0)
        {
            // If there is at least one fragment in the back stack, pop it
            fragmentManager.popBackStackImmediate();

            currentFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);

            // Determine the ID of the current fragment
            int currentFragmentId = 0;
            if (currentFragment instanceof UserHomeConnectionsFragment) {
                currentFragmentId = R.id.homeButton;
            } else if (currentFragment instanceof UserSettingsFragment) {
                UserSettingsFragment.resetListenersAndVariables();
                currentFragmentId = R.id.settingsButton;
            } else if (currentFragment instanceof UserProfileFragment) {
                currentFragmentId = R.id.profileButton;
            } else if (currentFragment instanceof UserChatsFragment) {
                currentFragmentId = R.id.chatsButton;
            }

            // Find the corresponding navigation item for the current fragment
            MenuItem currentNavItem = bottomNav.getMenu().findItem(currentFragmentId);

            // Uncheck all navigation items and then check the current navigation item
            for (int i = 0; i < bottomNav.getMenu().size(); i++)
            {
                bottomNav.getMenu().getItem(i).setChecked(false);
            }
            currentNavItem.setChecked(true);
            // Replace the current fragment with the new instance

        }
        else
        {
            // If there are no fragments in the back stack, show the exit confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Exit the app")
                    .setMessage("Are you sure you want to exit and logout?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            //DatabaseActivitiesUser.signOutUser();
                            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                            homeIntent.addCategory(Intent.CATEGORY_HOME);
                            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(homeIntent);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    /**
     * the function runs when clicking on the button of delete user in the : settings Fragment
     * and if the user's password is correct the user will delete his account
     * @param view - the button the user clicked on
     */
    public void deleteUser(View view)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordInput.setHint("Enter your current password");
        layout.addView(currentPasswordInput);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        final TextView showPasswordTextView = new TextView(this);
        showPasswordTextView.setText("Show Password");

        final CheckBox currentPasswordCheckBox = new CheckBox(this);
        currentPasswordCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublicFunctions.showPassword(currentPasswordCheckBox, currentPasswordInput);
            }
        });

        showPasswordTextView.setLayoutParams(textParams);
        currentPasswordCheckBox.setLayoutParams(checkBoxParams);

        linearLayout.addView(showPasswordTextView);
        linearLayout.addView(currentPasswordCheckBox);

        layout.addView(linearLayout);

        final TextView newErrorMessage = new TextView(this);
        newErrorMessage.setText("");
        layout.addView(newErrorMessage);

        builder.setView(layout);

        builder.setTitle("Delete User")
                .setMessage("Are You sure that you want to delete your User ?")
                .setPositiveButton("Delete", null) // set the positive button to null for now
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the onClickListener of the dialog's positive button
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (currentPasswordInput.getText().toString().length() > 6)
                {
                    DatabaseActivitiesUser.isCurrentPasswordValid(currentPasswordInput.getText().toString())
                            .thenAccept(result ->
                            {
                                if (result)
                                {
                                    DatabaseActivitiesUser.deleteUserFromDatabaseAndAuthentication
                                            ().thenAccept(res ->
                                    {
                                        if (res)
                                        {
                                            PublicFunctions.saveLoginDetails(MainUserActivity.this,"","");
                                            Toast.makeText(MainUserActivity.this,"Your User was successfully deleted",Toast.LENGTH_LONG).show();
                                            Intent intent = new Intent(MainUserActivity.this, LoginActivity.class);
                                            startActivity(intent);
                                        }
                                        else
                                        {
                                            Toast.makeText(MainUserActivity.this,"Your User wasn't delete",Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                                else
                                {
                                    newErrorMessage.setText("The password is not valid!");
                                }
                            });
                }
                else
                {
                    newErrorMessage.setText("The password syntax is not valid!");
                }
            }
        });
    }

    /**
     * the function that runs when the button logout in the : settings Fragment
     * is clicked and calls a function that sign out the user from the Firebase authentication
     * and then moves the user to the Login Activity
     * @param view - the logout button in the settings Fragment
     */
    public void logoutUser(View view)
    {
        stopChatUpdates();
        DatabaseActivitiesUser.signOutUser();
        Intent intent = new Intent(MainUserActivity.this, LoginActivity.class);
        intent.putExtra("isUserLoggedOut", true);
        PublicFunctions.saveLoginDetails(this,"","");
        startActivity(intent);
    }

    /**
     * the function runs when the user click on the info button in the : settings Fragment
     * and moves the user into the Info Activity
     * @param view - the info button in the settings Fragment
     */
    public void info (View view)
    {
        Intent intent = new Intent(MainUserActivity.this, InfoActivity.class);
        startActivity(intent);
    }

    /**
     * the function runs when the user click on the change settings button
     * in the : HomeConnections Activity, and it happens when the user has no connections with others
     * @param view - the change settings button in the HomeConnections Activity
     */
    public void changeSettings (View view)
    {// for the home connections fragment
        Fragment selectedFragment = new UserSettingsFragment();
        previousFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);
        // Replace the current fragment with the selected one
        if (previousFragment != null)
            getSupportFragmentManager().beginTransaction().replace(R.id.linear_layout_empty,
                    selectedFragment).addToBackStack(null).commit();
        else
            getSupportFragmentManager().beginTransaction().replace(R.id.linear_layout_empty,
                    selectedFragment).commit();
        currentFragment = selectedFragment;
        selectedNavItem = bottomNav.getMenu().findItem(R.id.settingsButton);
        selectedNavItem.setChecked(true);
    }

    /**
     * the function runs when the user click on the upload image button in the : settings Fragment
     * to upload an image from the user's gallery on the phone
     * @param view - the upload image button in the : settings Fragment
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
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {// the function handle the result of uploading an image to the app from the user's gallery on the phone
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PublicFunctions.PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
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
                uploadImageS2.setImageBitmap(resizedBitmap);
                userNewImage = resizedBitmap;
                //set a boolean to check if img was change
                isImageChanged = true;
                UserSettingsFragment.checkIfSaveButtonShouldBeEnabled();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * the function is for the save settings button in the settings fragment.
     * it saves the changes the user made in a user object and then call a function
     * that store the changes of the user in the database
     * @param view - the save settings button in the : settings Fragment
     */
    public void saveSettings (View view)
    {
        if (PublicFunctions.isConnectedToTheInternet(MainUserActivity.this))
        {
            UserClass user = new UserClass();
            user.setGender(MainUserActivity.getCurrentUser().getGender());
            selectedArea = spinnerCityS.getSelectedItem().toString();
            if (selectedArea != null)
                user.setArea(selectedArea);

            if (newEmail != null)
            {
                user.setEmail(newEmail);
                PublicFunctions.saveLoginDetails(this, null, null);
            }

            if (PublicFunctions.isUserNameValid(editTextUserNameS.getText().toString()))
            {
                user.setName(editTextUserNameS.getText().toString());
            }
            else
            {
                editTextUserNameS.setError("Invalid username");

            }
            selectedInterests = new ArrayList<>();
            if (fillSelectedInterests())
            {
                user.setInterests((ArrayList<String>) selectedInterests);
                RegistrationActivity.cancelError(errorMessageInterestsSetting);
            }

            else
            {
                RegistrationActivity.setErrorTextView(errorMessageInterestsSetting);
                Toast.makeText(MainUserActivity.this,"You need to fill at least one interest",Toast.LENGTH_LONG).show();
            }

            if (newPassword != null)
            {
                user.setPassword(newPassword);
                PublicFunctions.saveLoginDetails(this, null, null);
            }

            user.setSummery(editTextSummaryS.getText().toString());

            if (isImageChanged)
                user.setImage(userNewImage);
            else
                user.setImage(MainUserActivity.getCurrentUser().getImage());

            if(PublicFunctions.isUserNameValid(editTextUserNameS.getText().toString())
                    && fillSelectedInterests())
            {
                DatabaseActivitiesUser.setUserSettingsInDatabase(user, userNewImage).thenAccept(result ->
                {
                    if (result)
                    {
                        user.setBirthDate(MainUserActivity.getCurrentUser().getBirthDate());
                        MainUserActivity.setCurrentUser(user);
                        Fragment selectedFragment = new UserHomeConnectionsFragment(); // Create an instance of FragmentB
                        previousFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);
                        // Replace the current fragment with the selected one
                        getSupportFragmentManager().beginTransaction().replace(R.id.linear_layout_empty,
                                selectedFragment).commit();
                        currentFragment = selectedFragment;
                        selectedNavItem = bottomNav.getMenu().findItem(R.id.homeButton);
                        selectedNavItem.setChecked(true);
                    }
                });
            }
        }
        else
        {
            PublicFunctions.alertDialogNoInternet(MainUserActivity.this);
        }
    }

    /**
     * function that adds the selected interests from the : settings Fragment
     * into an array list we will put to the User's details
     * @return - true if the list is not empty and else false
     */
    public static boolean fillSelectedInterests()
    {
        selectedInterests.clear();
        for (int i = 0; i < linearLayoutS.getChildCount(); i++) {
            View view = linearLayoutS.getChildAt(i);
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
     * the function handles the user's click on the change password button in the :
     * setting Fragment, and checks if the user can change the password and if so, sets it in a variable
     * @param view - the change password button in the : setting Fragment
     */
    public void changePassword (View view)
    {// a function that create ChangePassword Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        currentPasswordInput.setHint("Enter your current password");
        layout.addView(currentPasswordInput);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        final TextView showPasswordTextView = new TextView(this);
        showPasswordTextView.setText("Show Password");

        final CheckBox currentPasswordCheckBox = new CheckBox(this);
        currentPasswordCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublicFunctions.showPassword(currentPasswordCheckBox, currentPasswordInput);
            }
        });

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        showPasswordTextView.setLayoutParams(textParams);
        currentPasswordCheckBox.setLayoutParams(checkBoxParams);

        linearLayout.addView(showPasswordTextView);
        linearLayout.addView(currentPasswordCheckBox);

        layout.addView(linearLayout);

        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPasswordInput.setHint("Enter your new password");
        layout.addView(newPasswordInput);

        LinearLayout linearLayoutNew = new LinearLayout(this);
        linearLayoutNew.setOrientation(LinearLayout.HORIZONTAL);

        final TextView showPasswordNewTextView = new TextView(this);
        showPasswordNewTextView.setText("Show Password");

        final CheckBox newPasswordCheckBox = new CheckBox(this);
        newPasswordCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PublicFunctions.showPassword(newPasswordCheckBox, newPasswordInput);
            }
        });

        showPasswordNewTextView.setLayoutParams(textParams);
        newPasswordCheckBox.setLayoutParams(checkBoxParams);

        linearLayoutNew.addView(showPasswordNewTextView);
        linearLayoutNew.addView(newPasswordCheckBox);

        layout.addView(linearLayoutNew);

        final TextView newErrorMessage = new TextView(this);
        newErrorMessage.setText("");
        layout.addView(newErrorMessage);

        builder.setView(layout);

        builder.setTitle(BidiFormatter.getInstance().unicodeWrap("Changing Password"))
                .setMessage("Insert your current password to change your password.")
                .setPositiveButton("Change", null) // set the positive button to null for now
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });


        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the onClickListener of the dialog's positive button
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) 
            {
                if (PublicFunctions.isPasswordValid(currentPasswordInput.getText().toString()) &&
                        PublicFunctions.isPasswordValid(newPasswordInput.getText().toString())) {
                    DatabaseActivitiesUser.isCurrentPasswordValid(currentPasswordInput.getText().toString())
                            .thenAccept(result -> {
                                if (result) {
                                    newPassword = newPasswordInput.getText().toString();
                                    UserSettingsFragment.checkIfSaveButtonShouldBeEnabled();
                                    dialog.dismiss(); // dismiss the dialog if the password is valid
                                    Toast.makeText(MainUserActivity.this,"New Password is valid",Toast.LENGTH_LONG).show();
                                } else {
                                    newErrorMessage.setText("The current password is not valid!");
                                    newErrorMessage.setTextColor(getResources().getColor(R.color.red)); // Set the color resource
                                }
                            });
                } else {
                    newErrorMessage.setText("The New password syntax is not valid!");
                    newErrorMessage.setTextColor(getResources().getColor(R.color.red));
                }
            }
        });
    }

    /**
     * the function handles the user's click on the change email button in the :
     * setting Fragment, and checks if the user can change the email and if so, sets it in a variable
     * @param view- the change email button in the : setting Fragment
     */
    public void changeEmail (View view)
    { // a function that create Alert Dialog changeEmail
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final TextView currentEmail = new TextView(this);
        currentEmail.setText("Your current Email : " + UserSettingsFragment.currentEmail);
        layout.addView(currentEmail);

        final EditText newEmailInput = new EditText(this);
        newEmailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        newEmailInput.setHint("Enter your new Email");
        layout.addView(newEmailInput);

        final TextView emailErrorMessage = new TextView(this);
        emailErrorMessage.setText("");

        layout.addView(emailErrorMessage);

        builder.setView(layout);

        builder.setView(layout);
        builder.setTitle("Changing Email")
                .setMessage("Insert your new Email to change it")
                .setPositiveButton("Change", null) // set the positive button to null for now
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

        // Override the onClickListener of the dialog's positive button
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (PublicFunctions.isEmailValid(newEmailInput.getText().toString()))
                {
                    DatabaseActivitiesUser.isNewEmailIsNotInUse(newEmailInput.getText().toString()).thenAccept(res ->{
                        if (res)
                        {
                            newEmail = newEmailInput.getText().toString();
                            UserSettingsFragment.checkIfSaveButtonShouldBeEnabled();
                            dialog.dismiss(); // dismiss the dialog if the email is valid
                            Toast.makeText(MainUserActivity.this,"New Email is valid",Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            emailErrorMessage.setText("Your Email is already in use !");
                            emailErrorMessage.setTextColor(getResources().getColor(R.color.red));
                        }
                    });
                }
                else
                {
                    emailErrorMessage.setText("Your new Email Syntax is not valid !");
                    emailErrorMessage.setTextColor(getResources().getColor(R.color.red));
                }
            }
        });
    }

    /**
     * the function runs when the user click on the cancel save button in the : settings Fragment
     * and it cancel the changes the user made before saving them and it returns him/her to the
     * home Fragment
     * @param view - the save button in the : settings Fragment
     */
    public void cancelSave (View view)
    {
        // Reset the newEmail and newPassword and the is image changed variable
        newEmail = null;
        newPassword = null;
        isImageChanged = false;

        Fragment selectedFragment = new UserHomeConnectionsFragment(); // Create an instance of FragmentB
        previousFragment = getSupportFragmentManager().findFragmentById(R.id.linear_layout_empty);
        // Replace the current fragment with the selected one
        getSupportFragmentManager().beginTransaction().replace(R.id.linear_layout_empty,
                    selectedFragment).addToBackStack(null).commit();
        currentFragment = selectedFragment;
        selectedNavItem = bottomNav.getMenu().findItem(R.id.homeButton);
        selectedNavItem.setChecked(true);
    }

    /**
     * the function creates a Handler and a Runnable that will be called every 5 seconds to check
     * if the current user is need to get a new message
     */
    private void startChatUpdates()
    {
        handler = new Handler();
        chatUpdater = new Runnable()
        {
            @Override
            public void run() {
                if (!PublicFunctions.isConnectedToTheInternet(MainUserActivity.this))
                {
                    PublicFunctions.alertDialogNoInternet(MainUserActivity.this);
                }
                else
                {
                    updateNewMessage();
                    handler.postDelayed(this, 5000); // Update every 5 seconds
                }
            }
        };
        handler.post(chatUpdater);
    }

    /**
     * the function stops the chat updates handler and runnable
     */
    private void stopChatUpdates()
    {
        if (handler != null && chatUpdater != null)
        {
            handler.removeCallbacks(chatUpdater);
        }
    }

    /**
     * the function retrieves the chat counts of the chats of the current user and compare
     * it with the older chat counts that was loaded before and if there is a difference
     * it will sets the chat counts in the user's node in the database and will update
     * the current user  with the new messages he/she needs to get
     */
    private void updateNewMessage()
    {
        chatsCount = new HashMap<>();
        DatabaseActivitiesUser.getChatCountsFromDatabase().thenAccept(result ->
        {
            if (result != null) {
                DatabaseActivitiesUser.retrieveChatsCounts().thenAccept(oldHashMap ->
                {
                    for (Map.Entry<String, Integer> current : result.entrySet())
                    {
                        String otherUserID = current.getKey();
                        int otherUserChatCount = current.getValue();
                        int oldUserChatCount = oldHashMap.getOrDefault(otherUserID, 0);

                        if (otherUserChatCount > oldUserChatCount)
                        {
                            DatabaseActivitiesUser.getOtherUserNameFromDatabase(otherUserID).thenAccept(profileOtherName ->
                            {
                                if (profileOtherName != null)
                                {
                                    DatabaseActivitiesUser.getLastMessages(otherUserID, otherUserChatCount - oldUserChatCount).thenAccept(messages ->
                                    {
                                        if (messages != null)
                                        {
                                            for (int i=0; i<messages.size(); i++)
                                            {
                                                sendNotification(profileOtherName, messages.get(i).getMessage(),
                                                        messages.get(i).getTime());
                                            }
                                            chatsCount.put(otherUserID,messages.size());
                                            UserChatsFragment.putIdAndMessagesCountIntoHashMap(otherUserID,messages.size());
                                        }
                                    });
                                }
                            });
                            oldHashMap.put(otherUserID, otherUserChatCount); // update the oldHashMap with the new count
                            // Save the old (now updated) hashmap after processing the messages
                            DatabaseActivitiesUser.saveChatsCounts(oldHashMap);
                        }

                    }
                });
            }
        });
    }

    /**
     * the function sends a push notification to the currents user's phone with a new message
     * with the details of the parameters provided
     * @param otherUserName - the name of the other user that sent the message
     * @param message - the message content
     * @param time - the time that the message sent
     */
    private void sendNotification(String otherUserName, String message, long time)
    {
        NotificationManager notificationManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel = new NotificationChannel("channelId1", "Home"
                    , NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(true);

            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            int notificationId = (int) System.currentTimeMillis();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channelId1")
                    .setContentTitle("New Message from : "+ otherUserName + "\n"+ PublicFunctions.getTimeDifference(time))
                    .setContentText(message)
                    .setSmallIcon(R.drawable.logo);

            notificationManager.notify(notificationId, builder.build());
        }
    }
}