package com.example.newproject;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;



/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserSettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserSettingsFragment extends Fragment
{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // two TextWatchers that are static because they used in the reset listeners function that
    // is being called not only from this Fragment
    private static TextWatcher userNameTextWatcher;
    private static TextWatcher summaryTextWatcher;

    // this variable gets the user's information from the user variable in the MainActivity
    // when this Fragment is loaded, to show the user's information on the settings screen
    private static UserClass user;

    // these variables are public static, because they are changing also from the MainActivity
    public static EditText editTextUserNameS;

    public static LinearLayout linearLayoutS;

    public static ArrayAdapter<String> areaAdapter;

    public static Button change_settings_button;


    public static String currentEmail;

    public static Spinner spinnerCityS;

    public static EditText editTextSummaryS;

    public static ImageView uploadImageS2;

    public static TextView errorMessageInterestsSetting;

    public UserSettingsFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserSettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserSettingsFragment newInstance(String param1, String param2) {
        UserSettingsFragment fragment = new UserSettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        user = MainUserActivity.getCurrentUser();
        editTextUserNameS = getView().findViewById(R.id.editTextUserNameS);
        editTextSummaryS = getView().findViewById(R.id.editTextSummaryS);
        linearLayoutS = getView().findViewById(R.id.linearLayoutS);

        spinnerCityS = getView().findViewById(R.id.spinnerCityS);
        areaAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item);
        // Set the adapter for the Spinner
        spinnerCityS.setAdapter(areaAdapter);

        // TextWatchers for two EditText elements

        userNameTextWatcher = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkIfSaveButtonShouldBeEnabled();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        };

        summaryTextWatcher = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkIfSaveButtonShouldBeEnabled();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        // OnItemSelectedListener for the Spinner
        Spinner.OnItemSelectedListener spinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                checkIfSaveButtonShouldBeEnabled();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        };

        // Add listeners
        editTextUserNameS.addTextChangedListener(userNameTextWatcher);
        editTextSummaryS.addTextChangedListener(summaryTextWatcher);
        spinnerCityS.setOnItemSelectedListener(spinnerItemSelectedListener);

        errorMessageInterestsSetting = getView().findViewById(R.id.errorMessageInterestsSetting);
        change_settings_button = getView().findViewById(R.id.settings_fragment_button);
        change_settings_button.setEnabled(false);

        setSettings();
    }

    /**
     * this function is checking if there is a need to enable or disable the save settings button
     * it enables the button only if there was a change in the settings from what was before
     */
    public static void checkIfSaveButtonShouldBeEnabled()
    {
        if (user != null)
        {
            String name = user.getName();
            String summary = user.getSummery();
            String userArea = user.getArea();

            boolean shouldEnable = false;

            if (spinnerCityS != null && spinnerCityS.getSelectedItem() != null && !spinnerCityS.getSelectedItem().toString().equals(userArea))
                shouldEnable = true;
            if (editTextUserNameS != null && !editTextUserNameS.getText().toString().equals(name))
                shouldEnable = true;
            if (editTextSummaryS != null && !editTextSummaryS.getText().toString().equals(summary))
                shouldEnable = true;
            if (MainUserActivity.newEmail != null || MainUserActivity.newPassword != null || MainUserActivity.isImageChanged)
                shouldEnable = true;


            change_settings_button.setEnabled(shouldEnable); // replace saveSettingsButton with your actual button instance
        }
    }

    /**
     * this function sets the user's information on this Fragment screen, so the user can see
     * what setting he/her can change
     */
    public void setSettings()
    { // set Setting to the User
        if (user != null)
        {
            String name = user.getName();
            String email = user.getEmail();
            String summary = user.getSummery();
            String userArea = user.getArea();
            List<String> userInterests = user.getInterests();

            editTextSummaryS.setText(summary);

            // Retrieve the areas list from Firebase
            DatabaseActivitiesUser.getListForDisplay(DatabaseActivitiesUser.areas).thenAccept(areaList ->
            {
                // Add the areas to the adapter
                if (areaAdapter == null || areaAdapter.isEmpty())
                    areaAdapter.addAll(areaList);

                // Set the default selection for the Spinner based on the user's previous selection
                if (userArea != null && !userArea.isEmpty())
                {
                    int defaultAreaIndex = areaAdapter.getPosition(userArea);
                    spinnerCityS.setSelection(defaultAreaIndex);
                }
                }).exceptionally(e -> {
                    // Handle the exception if getListForDisplay fails
                    return null;
                });
                DatabaseActivitiesUser.getListForDisplay(DatabaseActivitiesUser.interests)
                .thenAccept(interests -> {
                    // Clear the linearLayoutS
                    linearLayoutS.removeAllViews();
                    HashSet<String> originalInterests = new HashSet<>(userInterests);
                    HashSet<String> currentInterests = new HashSet<>(userInterests);

                    // Do something with the list of interests
                    for (int i = 0; i < interests.size(); i++)
                    {
                        CheckBox checkbox = new CheckBox(this.getContext());
                        checkbox.setText(interests.get(i));
                        checkbox.setId(i);
                        if (userHasThisInterest(userInterests,checkbox))
                        {
                            checkbox.setChecked(true);
                        }

                        // Add an OnCheckedChangeListener to the checkbox
                        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            String interest = checkbox.getText().toString();
                            if (isChecked) {
                                currentInterests.add(interest);
                            } else {
                                currentInterests.remove(interest);
                            }

                            // Enable or disable the save button based on whether the interests have changed
                            change_settings_button.setEnabled(!originalInterests.equals(currentInterests));
                        });
                        linearLayoutS.addView(checkbox);
                    }
                });

                editTextUserNameS.setText(name);
                currentEmail=email;
                editTextSummaryS.setText(summary);

                // Retrieve the ImageView using the getView() method
                uploadImageS2 = getView().findViewById(R.id.uploadImageS2);
                uploadImageS2.setImageBitmap(user.getImage());
            }
    }

    /**
     *  the function gets a list of interests of the user and a checkbox ,
     *  and returns true if the checkbox text is equals to one of the interests and else false
     * @param interests - an array list of String that each one representing an interests
     * @param checkbox - a CheckBox element
     */
    private boolean userHasThisInterest (List<String> interests, CheckBox checkbox)
    {
        for (String interest : interests)
        {
            if (interest.equals(checkbox.getText()))
                return true;
        }
        return false;
    }

    /**
     * this function target is to reset listeners and variables that maybe has changed by the user
     */
    public static void resetListenersAndVariables ()
    {
        // Remove listeners
        if (editTextUserNameS != null && editTextSummaryS != null && spinnerCityS != null)
        {
            editTextUserNameS.removeTextChangedListener(userNameTextWatcher);
            editTextSummaryS.removeTextChangedListener(summaryTextWatcher);
            spinnerCityS.setOnItemSelectedListener(null);  // Android SDK does not provide a way to remove a specific OnItemSelectedListener
        }
        if (MainUserActivity.newEmail != null)
            MainUserActivity.newEmail = null;
        if (MainUserActivity.newPassword != null)
            MainUserActivity.newPassword = null;
        MainUserActivity.isImageChanged = false;
    }

    /**
     * this function destroys the onDestroy function of this Fragment and it calls in it to the
     * function that reset listeners and variables
     */
    @Override
    public void onDestroyView()
    {
        resetListenersAndVariables();
        super.onDestroyView();
    }

}