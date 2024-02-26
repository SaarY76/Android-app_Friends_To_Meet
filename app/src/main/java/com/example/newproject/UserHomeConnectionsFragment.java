package com.example.newproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserHomeConnectionsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserHomeConnectionsFragment extends Fragment
{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Button changeSettingsButton;
    private TextView noConnectionsTextView;
    private ListView usersListView;
    private LinearLayout thisLinearLayout;

    private ProgressBar progressBar;

    // an array list that will be filled with UserClass object for comparison with the data
    // that will come from the database to see if there is a need for change in the list view
    // that shows the users that have a connection with the current user
    private ArrayList<UserClass> usersArrayList;

    public UserHomeConnectionsFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserHomeConnectionsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserHomeConnectionsFragment newInstance(String param1, String param2) {
        UserHomeConnectionsFragment fragment = new UserHomeConnectionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_home_connections, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        changeSettingsButton = getView().findViewById(R.id.changeSettingsButton);
        noConnectionsTextView = getView().findViewById(R.id.noConnectionsMessageTextView);
        thisLinearLayout = getView().findViewById(R.id.connectionFragmentLinearLayout);
        progressBar = getView().findViewById(R.id.progress_bar_Connections);
        usersListView = new ListView(getActivity());
        thisLinearLayout.addView(usersListView);
        usersArrayList = new ArrayList<>();
        putUsersInListView();
    }

    /**
     * this function destroys the onResume of this Fragment, and it just refreshing the ListView
     * that shows the users that have a connection with the current user
     */
    public void onResume()
    {
        super.onResume();
        // refresh the list of users
        putUsersInListView();
    }

    /**
     * this function puts the users that have a connection with the current user in a ListView
     * and if there are no users like that, it will show a button that when the user will click
     * on it, he/her will be moved to the settings Fragment to change their settings to maybe
     * find a connection
     */
    private void putUsersInListView()
    {
        DatabaseActivitiesUser.listOfUsersBySharedAreaAndInterests().thenAccept(users -> {
            if (users == null || users.isEmpty())
            {
                usersListView.setVisibility(View.GONE);
                changeSettingsButton.setVisibility(View.VISIBLE);
                noConnectionsTextView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
            else if(thereIsNeedToChangeUsersList(users)) {
                usersArrayList = users;
                usersListView.setVisibility(View.VISIBLE);
                changeSettingsButton.setVisibility(View.GONE);
                noConnectionsTextView.setVisibility(View.GONE);
                setUsersInListView(usersListView, users);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    /**
     * this function returns true if the local array list of users is different then the array list
     * that in the parameter and else false
     * @param users - an array list of UserClass objects
     */
    private boolean thereIsNeedToChangeUsersList (ArrayList<UserClass> users)
    {
        if (users != null && usersArrayList != null)
        {
            if (users.size() != usersArrayList.size())
                return true;
            for (int i = 0; i < users.size(); i++) {
                if (!usersArrayList.get(i).equals(users.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * the function returns the percentage of shared interests between two users
     * @param other - other UserClass object
     */
    public double percentageWithOtherUser(UserClass other)
    {
        UserClass user = MainUserActivity.getCurrentUser();
        if (user != null)
        {
            double numberOfInterests = user.getInterests().size();
            double numberOfSharedInterests = 0;
            for (String interest : other.getInterests())
            {
                if (user.getInterests().contains(interest))
                    numberOfSharedInterests++;
            }
            double result = numberOfSharedInterests / numberOfInterests * 100;
            return result;
        }
        return 0;
    }

    /**
     * the function sets an ArrayList<UserClass> in a ListView with an array adapter
     * @param listView - a ListView object that will fill users
     * @param users - an ArrayList<UserClass> of users
     */
    public void setUsersInListView(ListView listView, ArrayList<UserClass> users)
    {
        progressBar.setVisibility(View.VISIBLE);

        // Creating an instance of ArrayAdapter with the context, layout file and list of items.
        ArrayAdapter<UserClass> adapter = new ArrayAdapter<UserClass>(this.getContext(), R.layout.line_image_username, users)
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                // Getting the User object at the current position
                UserClass user = getItem(position);
                // Inflating the layout if it is not already inflated
                if (convertView == null)
                {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.line_image_username, parent
                            , false);
                }

                // Finding the TextView and setting the name of the user
                TextView textView = convertView.findViewById(R.id.textViewLine);
                textView.setText(user.getName());

                // Finding the ImageView and setting the image of the user
                ImageView imageView = convertView.findViewById(R.id.imageViewLine);
                imageView.setImageBitmap(user.getImage());

                TextView percentage = convertView.findViewById(R.id.presentage);

                String formatted = String.format("%.1f", percentageWithOtherUser(user));
                percentage.setText(formatted + "%");

                usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
                {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        // Get the Drawable from the ImageView
                        ImageView imageView = view.findViewById(R.id.imageViewLine);
                        Drawable drawable = imageView.getDrawable();

                        // Convert the Drawable to Bitmap
                        Bitmap bitmap = null;
                        if (drawable instanceof BitmapDrawable) {
                            bitmap = ((BitmapDrawable) drawable).getBitmap();
                            ChatActivity.bitmapForImage= bitmap;
                        }
                        Intent intent = new Intent(getContext(), ChatActivity.class);
                        UserClass clickedUser = getItem(position);
                        clickedUser.setImage(((BitmapDrawable)imageView.getDrawable()).getBitmap());
                        String userJson = new Gson().toJson(clickedUser);
                        intent.putExtra("user", userJson);
                        getContext().startActivity(intent);
                    }
                });

                // Set OnClickListener for the ImageView
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Handle the click event for the ImageView
                        Intent intent = new Intent(getContext(), OtherUserProfileActivity.class);
                        user.setImage(((BitmapDrawable)imageView.getDrawable()).getBitmap());
                        String userJson = new Gson().toJson(user);
                        intent.putExtra("user", userJson);
                        getContext().startActivity(intent);
                    }
                });

                return convertView;
            }
        };
        // Setting the adapter to the ListView
        listView.setAdapter(adapter);
    }
}