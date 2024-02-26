package com.example.newproject;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Calendar;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserProfileFragment extends Fragment
{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private ImageView profileImage;
    private TextView nameTextView;
    private TextView ageTextView;
    private TextView genderTextView;
    private TextView areaTextView;
    private TextView summeryTextView;
    private TextView interestsTextView;

    public UserProfileFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserProfileFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserProfileFragment newInstance(String param1, String param2)
    {
        UserProfileFragment fragment = new UserProfileFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // in this onViewCreated function all of the current user information will be loaded
        // to this Fragment elements to be shown to the user

        nameTextView = getView().findViewById(R.id.nameProfile);
        ageTextView = getView().findViewById(R.id.ageProfile);
        areaTextView = getView().findViewById(R.id.areaProfile);
        genderTextView = getView().findViewById(R.id.genderProfile);
        summeryTextView = getView().findViewById(R.id.summaryProfileTextView);
        interestsTextView = getView().findViewById(R.id.interestsProfileTextView);
        UserClass user = MainUserActivity.getCurrentUser();
        if (user != null)
        {
            long birthdate = user.getBirthDate();
            String name = user.getName();
            String area = user.getArea();
            String gender = user.getGender();
            String summery = user.getSummery();

            nameTextView.setText(name);
            areaTextView.setText(area);
            genderTextView.setText(gender);
            ageTextView.setText(""+ PublicFunctions.calculateAge(birthdate));
            summeryTextView.setText(summery);

            String str = "";
            for (int i=0; i<user.getInterests().size(); i++)
            {
                if (i != user.getInterests().size()-1)
                    str += user.getInterests().get(i) + ", ";
                else
                    str += user.getInterests().get(i);
            }
            interestsTextView.setText(str);
        }

        // Retrieve the ImageView using the getView() method
        profileImage = getView().findViewById(R.id.imageViewProfileUser);
        profileImage.setImageBitmap(user.getImage());
    }

}