package com.example.newproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.view.Gravity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatDelegate;


import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatActivity extends NotTurnAroundClass
{
    private ListView listViewMessages;
    private ImageView otherUserImageChat;
    private EditText chatEditText;
    private TextView otherUserName;
    private TextView otherUserLastSignedIn;
    private ImageButton chatSendButton;
    private Button blockUser;
    private LinearLayout chat_input_layout;
    // this is an array adapter that connects between the messages and the ListView that shows the messages in the chat
    private ArrayAdapter<Object> adapter;
    private String otherUserID;
    // this variable is a pattern to store the information about the other user in the chat
    private UserClass otherUser;
    // handler and runnable to set a runnable that run every 5 seconds to see if there is a change
    // in the chat like : new messages or the state of the other user
    private Handler handler;
    private Runnable chatUpdater;
    // this Bitmap variable is set from two Fragment and because of it, it's public
    public static Bitmap bitmapForImage;
    private int currentMessagesCount;
    // this variable is the indicator if this user is blocked or the other user is block by the current user
    private boolean isUserBlockOrBlocked = false;
    // these two colors are the two colors that are color the block user button
    private int colorEnabled = Color.RED;
    private int colorDisabled = Color.GRAY;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent intent = getIntent();
        // The intent contains the "user" extra
        if (intent.hasExtra("user"))
        {
            String userJson = intent.getStringExtra("user");
            Gson gson = new Gson();
            otherUser = gson.fromJson(userJson, UserClass.class);
            otherUserID = otherUser.getUniqueID();
        }

        otherUserImageChat = findViewById(R.id.otherUserImageView);
        listViewMessages = findViewById(R.id.chat_list_view);
        otherUserLastSignedIn = findViewById(R.id.lastSignIn);
        chat_input_layout = findViewById(R.id.chat_input_layout);
        otherUserName = findViewById(R.id.otherUserNameTextViewChat);
        blockUser = findViewById(R.id.blockUserButton);

        if (otherUser != null)
        {
            otherUserImageChat.setImageBitmap(otherUser.getImage());
            otherUserName.setText(otherUser.getName());

            DatabaseActivitiesUser.getChatWithOtherUser(otherUserID).thenAccept(chat ->
            {
                setAdapter(chat,otherUserID);
                listViewMessages.setAdapter(adapter);
                // Set the selection to the last item in the list
                if (listViewMessages.getCount() > 0)
                    listViewMessages.setSelection(adapter.getCount() - 1);
                currentMessagesCount = chat.size();
            });
        }

        chatSendButton = findViewById(R.id.chat_send_button);

        // checking if the current user blocked the other user or the current user has been blocked
        youBlockedOtherUser().thenAccept(youBlocked ->
        {
            if (youBlocked) {
                enableBlockButton();
                setDisabledChatSettingsAfterBlockUser();
            } else {
                youBlockedByOtherUser().thenAccept(youBlockedBy ->
                {
                    if (youBlockedBy) {
                        setDisabledChatSettingsAfterBlockedByUser();
                    } else {
                        setLastTimeSignInToOtherUser();
                        startChatUpdates();
                    }
                });
            }
        });


        // Set the selection to the last item in the ListView
        if (listViewMessages.getCount() > 0)
        {
            listViewMessages.setSelection(adapter.getCount() - 1);
        }
        else
            disableBlockButton();

        chatSendButton.setEnabled(false);
        chatEditText = findViewById(R.id.chat_input_edit_text);

        // if there is no blocking between the two users we will set a TextChangedListener
        if (!isUserBlockOrBlocked)
        {
            chatEditText.addTextChangedListener(new TextWatcher()
            {

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    Layout layout = chatEditText.getLayout();
                    if (adapter != null && layout != null && layout.getLineCount() > 0) {
                        listViewMessages.setSelection(adapter.getCount() - 1);
                    }
                    if (chatEditText.getText().toString().length() == 0)
                        chatSendButton.setEnabled(false);
                    else {
                        if (!isUserBlockOrBlocked)
                            chatSendButton.setEnabled(true);
                    }
                }

                @Override
                public void afterTextChanged(Editable s)
                {

                }
            });
        }
    }

    /**
     * this function enables the block user button
     */
    private void enableBlockButton ()
    {
        blockUser.setEnabled(true);
        blockUser.setBackgroundColor(colorEnabled);
    }

    /**
     * this function disables the block user button
     */
    private void disableBlockButton ()
    {
        blockUser.setEnabled(false);
        blockUser.setBackgroundColor(colorDisabled);
    }

    /**
     * this function sets the TextView's text that shows the last time that the user has been signed in
     */
    private void setLastTimeSignInToOtherUser ()
    {
        DatabaseActivitiesUser.getLastTimeSignedIn(otherUserID).thenAccept(time ->
        {
            if (time == 0L)
                otherUserLastSignedIn.setText("");
            else
                otherUserLastSignedIn.setText(PublicFunctions.getTimeDifference(time));
        });
    }

    /**
     * this function destroys the onDestroy function of this activity, and it stops the
     * runnable and handler and reset the otherUserID variable
     */
    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        otherUserID = null;
        stopChatUpdates();
    }

    /**
     * this function is checking if the current user blocked the other user and if so
     * it will return an async result and sets the block or blocked variable
     */
    public CompletableFuture<Boolean> youBlockedOtherUser()
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        DatabaseActivitiesUser.isOtherUserBlockedByYou(otherUserID).thenAccept(youBlocked -> {
            if (youBlocked != isUserBlockOrBlocked) {
                isUserBlockOrBlocked = youBlocked;
                future.complete(youBlocked);
            } else {
                future.complete(false);
            }
        });
        return future;
    }

    /**
     * this function is checking if the current blocked by the other user and if so
     * it will return an async result and sets the block or blocked variable
     */
    public CompletableFuture<Boolean> youBlockedByOtherUser ()
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        DatabaseActivitiesUser.isOtherUserBlockedYou(otherUserID).thenAccept(youBlocked ->
        {
            if (youBlocked)
            {
                future.complete(true);
            }
            else
            {
                future.complete(false);
            }
        });
        return future;
    }

    /**
     * this function updates the chat data of all it's elements :
     * new messages and sets the listview by it, if there was a blocking by one of the users,
     * the last time the other user has been signed in and this function is called by the runnable object
     */
    private void updateChatData()
    {
        youBlockedOtherUser().thenAccept(result1 -> {
            if (result1) {
                enableBlockButton();
                setDisabledChatSettingsAfterBlockUser();
            } else {
                youBlockedByOtherUser().thenAccept(result2 -> {
                    if (result2)
                    {
                        setDisabledChatSettingsAfterBlockedByUser();
                    }
                    else
                    {
                        isUserBlockOrBlocked = false;
                        DatabaseActivitiesUser.getChatWithOtherUser(otherUserID)
                                .thenAccept(chat -> {
                                    if (chat.size() > currentMessagesCount) {
                                        if (chat.size() > 0)
                                            enableBlockButton();
                                        setAdapter(chat, otherUserID);
                                        listViewMessages.setAdapter(adapter);
                                        listViewMessages.setSelection(adapter.getCount() - 1);
                                    }
                                    DatabaseActivitiesUser.getIfUserIsOnline(otherUserID).thenAccept(result -> {
                                        if (result == null) {
                                            otherUserLastSignedIn.setText("");
                                        } else {
                                            if (result)
                                                otherUserLastSignedIn.setText("Online");
                                            else {
                                                DatabaseActivitiesUser.getLastTimeSignedIn(otherUserID).thenAccept(time -> {
                                                    if (time != 0L) {
                                                        String status = PublicFunctions.getTimeDifference(time);
                                                        if (!status.equals(otherUserLastSignedIn.getText().toString()))
                                                            otherUserLastSignedIn.setText(status);
                                                    } else
                                                        otherUserLastSignedIn.setText("");
                                                });
                                            }
                                        }
                                    });
                                });
                    }
                });
            }
        });
    }

    /**
     * this function disable elements in the chat after this user blocked the other user
     */
    private void setDisabledChatSettingsAfterBlockUser ()
    {
        if (!otherUserLastSignedIn.getText().toString().equals("You Blocked this User"))
            otherUserLastSignedIn.setText("You Blocked this User");
        if (!blockUser.getText().toString().equals("UnBlock")) {
            blockUser.setText("UnBlock");
        }
        if(chat_input_layout.getVisibility() == View.VISIBLE) {
            chat_input_layout.setVisibility(View.GONE);
        }
        isUserBlockOrBlocked = true;
    }

    /**
     * this function disable elements in the chat after this user blocked by the other user
     */
    private void setDisabledChatSettingsAfterBlockedByUser ()
    {
        if (!otherUserLastSignedIn.getText().toString().equals("This User Blocked You"))
            otherUserLastSignedIn.setText("This User Blocked You");
        blockUser.setVisibility(View.GONE);
        if(chat_input_layout.getVisibility() == View.VISIBLE) {
            chat_input_layout.setVisibility(View.GONE);
        }
        isUserBlockOrBlocked = true;
    }

    /**
     * this function is starting the handler and the runnable that run every 5 seconds to
     * provide updated information to the chat and it calls other functions that provides the information
     */
    private void startChatUpdates()
    {
        handler = new Handler();
        chatUpdater = new Runnable()
        {
            @Override
            public void run()
            {
                if (!PublicFunctions.isConnectedToTheInternet(ChatActivity.this)) {
                    PublicFunctions.alertDialogNoInternet(ChatActivity.this);
                } else {
                    AtomicBoolean userBlocked = new AtomicBoolean(false);  // Define a boolean to detect if blocking occurs

                    DatabaseActivitiesUser.isUserDeleted(otherUserID).thenAccept(result -> {
                        if (result) {
                            onBackPressed();
                            return;
                        }
                        youBlockedOtherUser().thenAccept(result1 -> {
                            if (result1) {
                                setDisabledChatSettingsAfterBlockUser();
                                userBlocked.set(true);
                                return;
                            }
                            else
                            {
                                youBlockedByOtherUser().thenAccept(result2 -> {
                                    if (result2) {
                                        setDisabledChatSettingsAfterBlockedByUser();
                                        userBlocked.set(true);
                                        return;
                                    }
                                    if (!userBlocked.get() && !isUserBlockOrBlocked)
                                    {// Only proceed if no blocking condition has been met
                                        DatabaseActivitiesUser.getChatWithOtherUser(otherUserID).thenAccept(chat ->
                                        {
                                            if (chat == null || chat.isEmpty())
                                                disableBlockButton();
                                            else
                                                enableBlockButton();
                                            chat_input_layout.setVisibility(View.VISIBLE);
                                            isUserBlockOrBlocked = false;
                                            blockUser.setVisibility(View.VISIBLE);
                                            updateChatData();
                                        });
                                    }
                                });
                            }
                        });
                    });
                }
                handler.postDelayed(this, 5000); // Update every 5 seconds
            }
        };
        handler.post(chatUpdater);
    }

    /**
     * this function stops the handler and the runnable and it called by the onDestroy function
     */
    private void stopChatUpdates()
    {
        if (handler != null && chatUpdater != null)
        {
            handler.removeCallbacks(chatUpdater);
        }
    }

    /**
     * this function sets the array list of messages with the ListView that shows the messages
     * on the screen
     * @param messages - an array list of MesssageClass
     * @param uid - the other user's unique id
     */
    public void setAdapter (List<MessageClass> messages, String uid)
    {
        List<Object> dataList = new ArrayList<>();
        for (MessageClass message : messages)
        {
            dataList.add(message);
            dataList.add(PublicFunctions.getTimeDifference(message.getTime()));  // add the time after each message
        }

        adapter = new ArrayAdapter<Object>(this, R.layout.chat_text_message, dataList)
        {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null)
                {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.chat_text_message, parent
                            , false);
                }
                LinearLayout layoutTextMessage = convertView.findViewById(R.id.layout_text_message);
                TextView textView = convertView.findViewById(R.id.textViewMessage);
                textView.setTextIsSelectable(true);

                if (position % 2 == 0)// a message
                {
                    MessageClass message = (MessageClass) dataList.get(position);
                    textView.setText(message.getMessage());

                    if (!message.getUserSenderId().equals(uid))
                    {
                        textView.setBackgroundResource(R.drawable.chat_bubble_left);
                        layoutTextMessage.setGravity(Gravity.START);
                    }
                    else
                    {
                        textView.setBackgroundResource(R.drawable.chat_bubble_right);
                        layoutTextMessage.setGravity(Gravity.END);
                    }
                }
                else if (position > 0)// the time of the message
                {
                    String timeOfMessage = (String) dataList.get(position);
                    textView.setText(timeOfMessage);
                    if (!((MessageClass)dataList.get(position-1)).getUserSenderId().equals(uid))
                    {
                        layoutTextMessage.setGravity(Gravity.START);
                    }
                    else
                    {
                        layoutTextMessage.setGravity(Gravity.END);
                    }
                }

                return convertView;
            }
        };
    }

    /**
     * this function is the on click of the send button in the chat and it updates the database
     * with the new message and then after another update with the runnable that runs in the background
     * the user will show the new message in the ListView and the other user will see it also
     * @param view - a Button
     */
    public void send (View view)
    {
        if (PublicFunctions.isConnectedToTheInternet(ChatActivity.this))
        {
            if (!chatEditText.getText().toString().trim().equals(""))
            {
                MessageClass message = new MessageClass(chatEditText.getText().toString(),
                        DatabaseActivitiesUser.mAuth.getCurrentUser().getUid(),otherUserID,System.currentTimeMillis());

                DatabaseActivitiesUser.addMessage(message,otherUserID).thenAccept(result ->
                {
                    DatabaseActivitiesUser.getChatWithOtherUser(otherUserID).thenAccept(chat ->
                    {
                        setAdapter(chat,otherUserID);
                        listViewMessages.setAdapter(adapter);
                        listViewMessages.setSelection(adapter.getCount() - 1);
                    });
                });
                chatEditText.setText("");
            }
        }
        else
        {
            PublicFunctions.alertDialogNoInternet(ChatActivity.this);
        }
    }

    /**
     * this is the on click on the Image of the other user, and this function moves the current user
     * to the OtherUserProfile activity to see there the other user's profile from the database
     * @param view
     */
    public void photoOtherUser (View view)
    {
        Intent intent = new Intent(ChatActivity.this, OtherUserProfileActivity.class);
        otherUser.setImage(((BitmapDrawable)otherUserImageChat.getDrawable()).getBitmap());
        String userJson = new Gson().toJson(otherUser);
        intent.putExtra("user", userJson);
        startActivity(intent);
    }

    /**
     * this function is the on click of the block unBlock button and it block the other user
     * by the current user or it unblock the other user by the current user and the data from this
     * blocking is changing in the database also
     * @param view - a Button
     */
    public void blockUnBlockUser(View view)
    {
        if (blockUser.getText().toString().equals("Block User"))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);

            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);

            final TextView reportUserTextView = new TextView(this);
            reportUserTextView.setText("Report this User");
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            reportUserTextView.setLayoutParams(textParams);

            CheckBox checkBoxReport = new CheckBox(this);
            LinearLayout.LayoutParams checkBoxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            checkBoxReport.setLayoutParams(checkBoxParams);

            DatabaseActivitiesUser.isUserIsAlreadyReportedOnThisUser(otherUserID).thenAccept(result ->
            {
                if (!result)
                {
                    linearLayout.addView(reportUserTextView);
                    linearLayout.addView(checkBoxReport);
                }

            });

            layout.addView(linearLayout);

            builder.setView(layout);

            builder.setMessage("Are you sure you want to Block this User?")
                    .setTitle("Confirmation Required");

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id) {
                    DatabaseActivitiesUser.blockUser(otherUserID);
                    if (checkBoxReport.isChecked())
                        reportUserAlertDialog();
                    else
                        ChatActivity.this.onBackPressed();
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
        else if (blockUser.getText().toString().equals("UnBlock"))
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setMessage("Are you sure you want to Un-Block this User?")
                    .setTitle("Confirmation Required");

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    DatabaseActivitiesUser.unBlockUser(otherUserID);
                    ChatActivity.this.onBackPressed();
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    dialog.dismiss();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * this function is being called after the current user clicked on the block user button
     * and it shows to the user another alert dialog after the one before it if the user clicked
     * on a checkbox that he/her wants to also report on the other user
     */
    private void reportUserAlertDialog ()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final TextView reportUserTextView = new TextView(this);
        reportUserTextView.setText("Write here your report :");

        final EditText reportUserEditText = new EditText(this);

        layout.addView(reportUserEditText);

        builder.setView(layout);

        builder.setMessage("Report on Other User")
                .setTitle("Confirmation Required");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id) {
                DatabaseActivitiesUser.reportOnOtherUser(otherUserID,reportUserEditText.getText().toString());
                ChatActivity.this.onBackPressed();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * this function destroys the onResume function of this activity and it collect from the
     * intent the user object
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        Intent intent = getIntent();
        // The intent contains the "user" extra
        if (intent.hasExtra("user"))
        {
            String userJson = intent.getStringExtra("user");
            Gson gson = new Gson();
            otherUser = gson.fromJson(userJson, UserClass.class);
            otherUserID = otherUser.getUniqueID();
        }
    }

    /**
     * this function destroys the onBackPressed function and it sets in the chats Fragments
     * the hash map of ids and count messages hashmap to update it so there will be updated data
     * on this Fragment and also this function resets the otherUser's id and stops the
     * handler and runnable by calling a function that is doing it
     */
    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        HashMap<String, Integer> hashMap = UserChatsFragment.getIdsAndMessagesCount();
        if (hashMap.containsKey(otherUserID))
            hashMap.remove(otherUserID);
        UserChatsFragment.setIdsAndMessagesCount(hashMap);
        stopChatUpdates();
    }
}