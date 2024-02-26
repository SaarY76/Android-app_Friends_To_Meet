package com.example.newproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UserChatsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserChatsFragment extends Fragment
{

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private TextView noChatsTextView;

    private LinearLayout thisLinearLayout;

    private ListView usersChatsListView;

    private ProgressBar progressBar;

    // a local variable that represent the users that came from the database and each run
    // in the runnable we check if there was a difference so we will update the ListView with the new ordered list
    private ArrayList<UserClass> usersArrayList;
    // an array adapter that is connecting between an array list of users and a ListView
    private ArrayAdapter<UserClass> adapter;

    // this variable represent the current number of images that are needed to load from the
    // database to each user and it count down until zero and then all the users are set in the ListView
    private int imagesToLoadCount;

    // handler and runnable to run every 5 seconds to update this Fragment elements by new data from the database
    private Handler handler;
    private Runnable chatUpdater;

    // hash map to store the ids and the messages count with each to know if there was a change
    // from the database and then update this hash map
    private static HashMap<String, Integer> idsAndMessagesCount = new HashMap<>();

    public static HashMap<String, Integer> getIdsAndMessagesCount() {
        return idsAndMessagesCount;
    }

    public static void setIdsAndMessagesCount(HashMap<String, Integer> idsAndMessagesCount)
    {
        UserChatsFragment.idsAndMessagesCount = idsAndMessagesCount;
    }

    public UserChatsFragment()
    {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UserChatsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static UserChatsFragment newInstance(String param1, String param2) {
        UserChatsFragment fragment = new UserChatsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        thisLinearLayout = getView().findViewById(R.id.chatsFragmentLinearLayout);
        noChatsTextView = getView().findViewById(R.id.noChatsTextView);
        progressBar = getView().findViewById(R.id.progress_bar_Chats);
        progressBar.setVisibility(View.VISIBLE);
        if (usersChatsListView == null) {
            usersChatsListView = new ListView(this.getContext());
        } else if (usersChatsListView.getParent() != null) {
            ((ViewGroup)usersChatsListView.getParent()).removeView(usersChatsListView); // <- safely remove the view before adding again
        }
        this.thisLinearLayout.addView(usersChatsListView);
        startOrderListViewUsers();
    }

    /**
     *  this function destroys the onDestroyView function and it stops the handler and runnable
     */
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        if (handler != null && chatUpdater != null)
        {
            handler.removeCallbacks(chatUpdater);
            handler = null;
            chatUpdater = null;
        }
    }

    /**
     * a function that updates the hashmap of ids and message counts from the Chat activity
     * so the ListView in this Fragment will be updated all the time
     * @param id - unique id of a user
     * @param count - the count of unread messages to the user with that unique id
     */
    public static void putIdAndMessagesCountIntoHashMap(String id, int count)
    {
        if (!idsAndMessagesCount.containsKey(id)) {
            idsAndMessagesCount.put(id, count);
        } else {
            int currentCount = idsAndMessagesCount.get(id);
            idsAndMessagesCount.put(id, currentCount + count);
        }
    }

    /**
     * this function starts the handler and runnable and it calls a function that handles the
     * new information from the database and change the elements in this Fragment by that
     */
    private void startOrderListViewUsers()
    {
        handler = new Handler();
        chatUpdater = new Runnable() {
            @Override
            public void run() {
                orderUsersAndRefreshListView();
                handler.postDelayed(chatUpdater, 5000);
            }
        };
        handler.post(chatUpdater);
    }

    /**
     * this function is getting all of the users that the current user has a chat with them
     * and checking by calling another function if there is need to change the order of the users
     * or if there are no users with chats with the current user, this function will set an element
     * that will show the user this information
     */
    private void orderUsersAndRefreshListView()
    {
        DatabaseActivitiesUser.getUsersWithChats().thenAccept(users ->
        {
            if (users == null || users.isEmpty()) {
                if (usersChatsListView != null)
                    usersChatsListView.setVisibility(View.GONE);
                noChatsTextView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                return;
            }

            CompletableFuture<List<UserClass>> orderedUsersFuture = orderUsers(users);

            orderedUsersFuture.thenAccept(orderedUsers -> {
                if (shouldRefreshListView(orderedUsers)) {
                    setUsersInListView(orderedUsers);
                }
            });
        });
    }

    /**
     * this function gets a list of UserClass that representing the other users that the current user
     * has a chat with them and by checking the last message with each one it orders the users by that
     * and then returning async a new list of users after ordering them
     * @param users - a list of UserClass
     */
    private CompletableFuture<List<UserClass>> orderUsers(List<UserClass> users)
    {
        List<CompletableFuture<Object[]>> lastMessageFutures = new ArrayList<>();

        for (UserClass user : users)
        {
            CompletableFuture<Object[]> lastMessageFuture = DatabaseActivitiesUser.getLastMessageWithOtherUser(user.getUniqueID())
                    .thenApply(lastMessage -> new Object[]{user, lastMessage != null ? lastMessage.getTime() : 0});
            lastMessageFutures.add(lastMessageFuture);
        }

        return CompletableFuture.allOf(lastMessageFutures.toArray(new CompletableFuture[0]))
                .thenApply(v ->
                {
                    List<Object[]> userTimePairs = lastMessageFutures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());

                    // Sort the user-time pairs based on the last message time in descending order
                    userTimePairs.sort((pair1, pair2) -> Long.compare((long) pair2[1], (long) pair1[1]));

                    return userTimePairs.stream()
                            .map(pair -> (UserClass) pair[0])
                            .collect(Collectors.toList());
                });
    }

    /**
     * this function is checking if there is a need for refresh by checking differences between the
     * new ordered users and the local users list and if so it will return true and else false
     * @param orderedUsers - a list of UserClass objects that representing the new ordered users
     */
    private boolean shouldRefreshListView(List<UserClass> orderedUsers)
    {
        if (usersArrayList == null || usersArrayList.size() != orderedUsers.size())
        {
            return true;
        }

        for (int i = 0; i < orderedUsers.size(); i++) {
            if (!usersArrayList.get(i).equals(orderedUsers.get(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     *  the function set a list of UserClass that are ordered in a list view with an array adapter
     * @param orderedUsers - a list of UserClass objects that represent users after they are
     * ordered by that the first user is the user that the current user has talked to him/her last
     */
    public void setUsersInListView(List<UserClass> orderedUsers)
    {

        imagesToLoadCount = orderedUsers.size();
        if (orderedUsers.isEmpty())
        {
            progressBar.setVisibility(View.GONE);
            if (usersChatsListView != null)
                usersChatsListView.setVisibility(View.GONE);
            return;
        }
        noChatsTextView.setVisibility(View.GONE);
        if (usersChatsListView != null && usersChatsListView.getVisibility() == View.GONE)
            usersChatsListView.setVisibility(View.VISIBLE);

        usersArrayList = new ArrayList<>(orderedUsers);

        if (adapter == null)
        {
            // Creating an instance of ArrayAdapter with the context, layout file, and list of items.
            adapter = new ArrayAdapter<UserClass>(getContext(), R.layout.line_image_username_chat, orderedUsers) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    // Getting the User object at the current position
                    UserClass user = getItem(position);
                    // Inflating the layout if it is not already inflated
                    if (convertView == null) {
                        convertView = LayoutInflater.from(getContext()).inflate(R.layout.line_image_username_chat, parent, false);
                    }

                    // Finding the TextView and setting the name of the user
                    TextView textView = convertView.findViewById(R.id.textViewLineChat);
                    textView.setText(user.getName());

                    // Finding the ImageView and setting the image of the user
                    ImageView imageView = convertView.findViewById(R.id.imageViewLineChat);
                    // This code will run once the photo is loaded and set in the ImageView
                    setPhotoFromDataBase(imageView, user, this::notifyDataSetChanged);

                    TextView textViewLastMessage = convertView.findViewById(R.id.lastMessageChat);
                    // timeTextViewChat
                    TextView textViewTimeMessage = convertView.findViewById(R.id.timeTextViewChat);

                    DatabaseActivitiesUser.isOtherUserBlockedYou(user.getUniqueID()).thenAccept(result ->
                    {
                        if (result)
                        {
                            textViewLastMessage.setText("This User Blocked you");
                            textViewTimeMessage.setText("");
                        }
                        else
                        {
                            DatabaseActivitiesUser.isOtherUserBlockedByYou(user.getUniqueID()).thenAccept(result1 ->
                            {
                                if (result1)
                                {
                                    textViewLastMessage.setText("You Blocked this User");
                                    textViewTimeMessage.setText("");
                                }
                                else
                                {
                                    DatabaseActivitiesUser.getLastMessageWithOtherUser(user.getUniqueID()).thenAccept(lastMessage -> {
                                        if (lastMessage != null && !lastMessage.getMessage().equals(textViewLastMessage.getText().toString())) {
                                            String checkmark = "\u2714";
                                            String message = lastMessage.getMessage();;

                                            if (lastMessage.getUserReceivedId().equals(user.getUniqueID()))
                                            {
                                                textViewTimeMessage.setText(PublicFunctions.getTimeDifference(lastMessage.getTime()) + " " + checkmark);
                                            }
                                            else
                                            {
                                                textViewTimeMessage.setText(PublicFunctions.getTimeDifference(lastMessage.getTime()));
                                            }
                                            textViewLastMessage.setText(message);

                                        }
                                    });
                                }
                            });
                        }
                    });

                    FrameLayout circle = convertView.findViewById(R.id.newMessagesCircle);
                    TextView numberOfNewMessages = convertView.findViewById(R.id.messageCount);
                    if (idsAndMessagesCount.containsKey(user.getUniqueID()))
                    {
                        circle.setVisibility(View.VISIBLE);
                        int messageCount = idsAndMessagesCount.get(user.getUniqueID());
                        if (messageCount > 0)
                        {
                            String messageCountString = String.valueOf(messageCount);
                            if (PublicFunctions.isNumber(messageCountString))
                                numberOfNewMessages.setText(messageCountString);
                            else
                                circle.setVisibility(View.GONE);
                        }
                    }
                    else
                    {
                        circle.setVisibility(View.GONE);
                        numberOfNewMessages.setText("");
                    }

                    usersChatsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            UserClass user = orderedUsers.get(position);
                            idsAndMessagesCount.remove(user.getUniqueID());
                            // Get the Drawable from the ImageView
                            ImageView imageView = view.findViewById(R.id.imageViewLineChat);
                            Drawable drawable = imageView.getDrawable();
                            FrameLayout circle = view.findViewById(R.id.newMessagesCircle);
                            circle.setVisibility(View.GONE);
                            TextView numberOfNewMessages = view.findViewById(R.id.messageCount);
                            numberOfNewMessages.setText("");
                            // Convert the Drawable to Bitmap
                            Bitmap bitmap = null;
                            if (drawable instanceof BitmapDrawable) {
                                bitmap = ((BitmapDrawable) drawable).getBitmap();
                                ChatActivity.bitmapForImage = bitmap;
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
                    imageView.setOnClickListener(new View.OnClickListener()
                    {
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
            usersChatsListView.setAdapter(adapter);
        } else {
            adapter.clear();
            adapter.addAll(orderedUsers);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * the function call a function that returns a Bitmap object with the image
     * of user from firebase and this function set the image in an ImageView
     * @param userImage - an ImageView to set the image of the user
     * @param user - a UserClass object that represent a user
     */
    public void setPhotoFromDataBase(ImageView userImage, UserClass user, Runnable callback)
    { // a function to gets Photo from the dataBase
        try
        {
            DatabaseActivitiesUser.getBitmapFromUser(user.getUniqueID()).thenAccept(photo ->
            {
                if (photo != null)
                {
                    // set the photo bitmap to the ImageView
                    userImage.setImageBitmap(photo);

                    // Run the callback once the photo is loaded and set in the ImageView
                    if (callback != null)
                    {
                        callback.run();
                    }
                    // Decrement the images to load count and hide the ProgressBar if necessary
                    imagesToLoadCount--;
                    if (imagesToLoadCount == 0)
                    {
                        progressBar.setVisibility(View.GONE);
                    }

                }
            }).exceptionally(throwable -> {
                // handle any errors that might occur while retrieving the photo
                throwable.printStackTrace();
                return null;
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * this function destroys the onResume of this Fragment, and it just refreshing the ListView
     * that shows the users that have a chat with the current user
     */
    public void onResume()
    {
        super.onResume();
        // refresh the list of users
        if (usersChatsListView.getCount() > 0)
            progressBar.setVisibility(View.GONE);
        if (handler == null && chatUpdater == null)
            startOrderListViewUsers();
    }
}