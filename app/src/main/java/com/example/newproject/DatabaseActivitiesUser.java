package com.example.newproject;

import android.content.Context;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import android.net.Uri;
import android.provider.MediaStore;
import android.util.*;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DatabaseActivitiesUser
{
    // firebase objects and references
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    public static final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final FirebaseStorage storage = FirebaseStorage.getInstance();
    private static final StorageReference storageRef = storage.getReference();

    // Databases references that other activities and fragments will use
    public static final DatabaseReference usersRef = database.getReference("Users").getRef();
    public static final DatabaseReference chats = database.getReference("Chats");
    public static final DatabaseReference publicRef = database.getReference("Public");
    public static final DatabaseReference interests = publicRef.child("Interests").getRef();
    public static final DatabaseReference areas = publicRef.child("Areas").getRef();

    public static final DatabaseReference reports = database.getReference("Reports").getRef();

    // List of Targets that listen to image loading for better image loading from the database
    private static List<Target> picassoTargets = Collections.synchronizedList(new ArrayList<>());

    /**
     * the function is async and by the reference it searches for a list of String
     * in the database and then return it
     * @param ref - reference in the database for a list we want to display in spinner
     * @return - CompletableFuture<List<String>> that will be displayed in a spinner in user's screen
     */
    public static CompletableFuture<List<String>> getListForDisplay(DatabaseReference ref)
    {
        CompletableFuture<List<String>> future = new CompletableFuture<>();

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("getListForDisplay", "Data snapshot: " + dataSnapshot.toString());
                List<String> names = new ArrayList<>();

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren())
                {
                    String name = childSnapshot.getValue(String.class);
                    names.add(name);
                }

                // Complete the future with the list of names
                future.complete(names);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Complete the future exceptionally with the database error
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }

    /**
     * the function creating a new user in the authentication field in firebase,
     * using another function to add him to the real time database with his details and,
     * using another function it's calling - to upload an image to firebase storage and,
     * send to the user's email a verification mail that he needs to click on
     * @params - The user's details from the Registration activity
     */
    public static CompletableFuture<Boolean> createUserWithEmailAndPassword(String email, String password, String name,
                                                                            String area, ArrayList<String> interests,
                                                                            String gender, long birthdate, String summary, Bitmap image) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Send email verification
                            user.sendEmailVerification().addOnCompleteListener(verificationTask -> {
                                if (verificationTask.isSuccessful()) {
                                    // Create a UserClass object with the additional information
                                    UserClass newUser = new UserClass(email, name, area, interests, gender, birthdate, summary);
                                    Task<Void> addUserTask = addUserAsFieldInDatabase(user.getUid(), newUser);

                                    addUserTask.addOnSuccessListener(rVoid -> {
                                                uploadImageToFirebaseStorage(user.getUid(), image).thenAccept(url -> {
                                                    if (!url.contains("Error")) {
                                                        future.complete(true);
                                                    } else {
                                                        future.complete(false);
                                                    }
                                                });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("createUserWithEmailAndPassword", "Failed to add user to database: " + e.getMessage());
                                                future.complete(false);
                                            });
                                } else {
                                    Log.e("createUserWithEmailAndPassword", "Failed to send verification email: " + verificationTask.getException().getMessage());
                                    future.complete(false);
                                }
                            });
                        } else {
                            future.complete(false);
                        }
                    } else {
                        Log.e("createUserWithEmailAndPassword", "Failed to create user: " + task.getException().getMessage());
                        future.complete(false);
                    }
                });
        return future;
    }




    /**
     * the function creates a node in the real time database in firebase
     * with the current user's id as a key, and the newUser details as keys and
     * values under it
     * @param uid - a string that represent the unique id of the new user
     * @param newUser - an Object of UserClass filled with the user's details
     */
    public static Task<Void> addUserAsFieldInDatabase(String uid, UserClass newUser)
    {
        // Add the UserClass object to the specified database reference
        DatabaseReference newUserRef = usersRef.child(uid);
        return newUserRef.setValue(newUser);
    }

    /**
     * the function uploading an image to the firebase storage with name of the user's
     * unique id and puts as a node and value the url of the image in the user's node
     * in the real time database
     * @param userId - a string that represent the unique id of the new user
     * @param image - a Bitmap Object with the data of the image the user want to upload
     */
    public static CompletableFuture<String> uploadImageToFirebaseStorage(String userId, Bitmap image) {
        CompletableFuture<String> future = new CompletableFuture<>();
        // Create a reference to the Firebase Storage location where the image will be saved
        StorageReference imageRef = storageRef.child("images/" + userId + ".jpg");

        // Compress the image and convert it to a byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] imageData = baos.toByteArray();

        // Upload the image to Firebase Storage
        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL for the uploaded image
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                // Save the download URL to the Realtime Database
                                usersRef.child(userId).child("photoUrl").setValue(uri.toString());
                                future.complete(uri.toString());
                            })
                            .addOnFailureListener(e -> {
                                future.complete("Error getting URL");
                                Log.e("uploadImageToFirebaseStorage", "Failed to get download URL: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    future.complete("Error uploading");
                    // Handle the error
                    Log.e("uploadImageToFirebaseStorage", "Failed to upload image: " + e.getMessage());
                });

        return future;
    }


    /**
     * the function returns async the image Bitmap of the user from the database
     * @param uid - a string that represent the unique id of the user
     * @return - a Bitmap with the data of the image of the user from the database
     */
    public static CompletableFuture<Bitmap> getBitmapFromUser(String uid) throws IOException
    {
        CompletableFuture<Bitmap> future = new CompletableFuture<>();

        usersRef.orderByKey().equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (!dataSnapshot.exists())
                {
                    future.complete(null);
                    return;
                }
                DataSnapshot userSnapshot = dataSnapshot.getChildren().iterator().next();
                String photoUrl = userSnapshot.child("photoUrl").getValue(String.class);
                // Append cache-busting query parameter to the photo URL
                String cacheBustingUrl = photoUrl + "?time=" + System.currentTimeMillis();

                Target target = new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        // Resize the bitmap to a smaller size
                        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 1000, 1000, true);
                        future.complete(resizedBitmap);
                        synchronized(picassoTargets) {
                            picassoTargets.remove(this);
                        }
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        // Handle the error here
                        future.completeExceptionally(e);
                        synchronized(picassoTargets) {
                            picassoTargets.remove(this);
                        }
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        // Handle the placeholder here
                    }
                };

                picassoTargets.add(target);
                Picasso.get().load(cacheBustingUrl).into(target);
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                // Handle the error here
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    /**
     * the function deletes user's image from firebase storage
     * @param uid - the user's unique id
     * @return - boolean async that if the photo was deleted or not
     */
    public static CompletableFuture<Boolean> deleteImageFromFirebaseStorageWithUniqueID(String uid)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        // Create a reference to the Firebase Storage location where the image is saved
        StorageReference imageRef = FirebaseStorage.getInstance().getReference("images/" + uid + ".jpg");

        // Delete the image from Firebase Storage
        imageRef.delete().addOnSuccessListener(aVoid -> {
            // Image deleted successfully
            future.complete(true);
        }).addOnFailureListener(e -> {
            // Handle the error
            future.complete(false);
            Log.e("deleteImageFromFirebaseStorage", "Failed to delete image: " + e.getMessage());
        });
        return future;
    }


    /**
     * the function changes the orientation of the photo if it's not state to the
     * borders of the ImageView in the Activity
     * @param photoUri - the URI Object of the photo
     * @param context - the Context of the Activity that uses the function
     * @return - int of the orientation
     */
    public static int getOrientation(Uri photoUri, Context context)
    {// the function return an orientation to an image if it's rotated
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

        if (cursor == null || cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        int orientation = cursor.getInt(0);
        cursor.close();
        return orientation;
    }

    /**
     * the function deletes the User from the real time database and the authentication
     * @return - CompletableFuture<Boolean> - with True if the user successfully deleted
     * and False if not
     */
    public static CompletableFuture<Boolean> deleteUserFromDatabaseAndAuthentication()
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uniqueId = currentUser.getUid();
            DatabaseReference user = usersRef.child(uniqueId);
            if (user != null) {
                deleteAllUserChats();
                usersRef.child(uniqueId).getRef().removeValue();
                deleteImageFromFirebaseStorageWithUniqueID(currentUser.getUid());
                currentUser.delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // User deleted successfully
                                future.complete(true);
                                return;
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // An error occurred while deleting the user
                                future.complete(false);
                                return;
                            }
                        });
            } else {
                future.complete(false);
            }
        }
        else {
            future.complete(false);
        }
        return future;
    }

    /**
     * the function deletes all of the chats that related to the current user from the database
     */
    private static void deleteAllUserChats ()
    {
        chats.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                for (DataSnapshot chat : snapshot.getChildren())
                {
                    if (chat.getKey().contains(mAuth.getUid()))
                    {
                        chat.getRef().removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                // Handle errors here if necessary
                Log.e("Firebase", "Error deleting chats", error.toException());
            }
        });
    }



    /**
     * the function used for sign in a User to our app and makes him sign in in the authentication
     * in firebase
     * @param email - String with the value of the email in the Login Activity
     * @param password - String with the value of the email in the Login Activity
     * @param context - the context of the activity it called from
     * @return - CompletableFuture<Object[]> , True if the function succeeded to find the user
     * in the authentication field in firebase and makes him sign in , else False ,
     * and the String in index 1 in the Object array is the result of the specific error
     */
    public static CompletableFuture<Object[]> signInWithEmailAndPassword(String email, String password, Context context) {
        CompletableFuture<Object[]> future = new CompletableFuture<>();
        Object[] result = new Object[2];

        boolean isConnected = PublicFunctions.isConnectedToTheInternet(context);

        if (!isConnected) {
            result[0] = false;
            result[1] = "No internet connection.\nPlease check your internet connectivity \nand try again.";
            future.complete(result);
        } else {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                // User's email is verified
                                getIfUserIsOnline(mAuth.getUid()).thenAccept(isOnline -> {
                                    if (isOnline) {
                                        result[0] = false;
                                        result[1] = "This User is currently online";
                                        future.complete(result);
                                    } else {
                                        isUserDeleted(mAuth.getUid()).thenAccept(isDeleted -> {
                                            if (!isDeleted) {
                                                usersRef.child(DatabaseActivitiesUser.mAuth.getUid()).child("last signed in").onDisconnect().setValue(System.currentTimeMillis());
                                                usersRef.child(mAuth.getUid()).child("Online").onDisconnect().setValue(false);
                                            }
                                        });
                                        usersRef.child(DatabaseActivitiesUser.mAuth.getUid()).child("last signed in").setValue(System.currentTimeMillis());
                                        usersRef.child(mAuth.getUid()).child("Online").setValue(true);

                                        result[0] = true;
                                        result[1] = "";
                                        future.complete(result);
                                    }
                                });
                            } else {
                                // User's email is not verified or user is null
                                result[0] = false;
                                result[1] = user == null ? "Authentication failed." : "Your email is not verified,\nPlease verify your email.";
                                future.complete(result);
                            }
                        } else {
                            String errorMessage = task.getException().getMessage();
                            if (errorMessage.contains("disabled")) {
                                result[0] = false;
                                result[1] = "You are Disabled from this app,\nif You have suggestions please contact us";
                                future.complete(result);
                            } else {
                                result[0] = false;
                                result[1] = "The email or password aren't valid.\nPlease enter valid email and password.";
                                future.complete(result);
                            }
                        }
                    });
        }
        return future;
    }


    /**
     * the function using collect the user's data from the real time data base and
     * @return - CompletableFuture<UserClass> Object with the user's details from
     * the real time database
     */
    public static CompletableFuture<UserClass> getUserProfileDetailsFromDatabase(String uid)
    {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        CompletableFuture<UserClass> future = new CompletableFuture<>();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {

                if (dataSnapshot.exists())
                {
                    String email = currentUser.getEmail();
                    long birthDate = dataSnapshot.child("birthDate").getValue(Long.class);
                    String gender = dataSnapshot.child("gender").getValue(String.class);
                    String area = dataSnapshot.child("area").getValue(String.class);
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String summery = dataSnapshot.child("summery").getValue(String.class);
                    ArrayList<String> interests = new ArrayList<>();
                    DataSnapshot interestsSnapshot = dataSnapshot.child("interests");
                    for (DataSnapshot interestSnapshot : interestsSnapshot.getChildren())
                    {
                        String interest = interestSnapshot.getValue(String.class);
                        interests.add(interest);
                    }

                    UserClass user = new UserClass(email, name, area, interests, gender, birthDate,summery);
                    user.setUniqueID(uid);
                    try {
                        getBitmapFromUser(uid).thenAccept(image ->
                        {
                            user.setImage(image);
                            future.complete(user);
                        });
                    } catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else
                {
                    // No child with the current user's UID found
                    future.completeExceptionally(new Exception("User not found in database"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle the error here
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    /**
     * the function returns the name of the other user with the unique id we got
     * @param uid - the unique id of the other user
     * @return - a String with the data of the name of the other user
     */
    public static CompletableFuture<String> getOtherUserNameFromDatabase(String uid)
    {
        CompletableFuture<String> future = new CompletableFuture<>();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    future.complete(name);
                }
                else
                {
                    // No child with the current user's UID found
                    future.completeExceptionally(new Exception("User not found in database"));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                // Handle the error here
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }

    /**
     * the function sign out the user from the authentication in firebase
     */
    public static void signOutUser()
    {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null)
        {
            usersRef.child(mAuth.getUid()).child("Online").setValue(false);
            saveLastTimeSignedIn();
            mAuth.signOut();
        }
    }


    /**
     * the function uses the auth Object of firebase authentication and send
     * reset password to the user's email
     * @param context - Context of the Activity that will use this function
     * @param email - a String with value of the email
     */
    public static void sendResetPassword(Context context, String email)
    {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Password reset email sent. Please check your inbox.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "Failed to send password reset email. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * the function makes a list of other users with at least one shared interest and
     * same area with current user and
     * @return - ArrayList<UserClass> with the users
     */
    public static CompletableFuture<ArrayList<UserClass>> listOfUsersBySharedAreaAndInterests ()
    {
        CompletableFuture<ArrayList<UserClass>> future = new CompletableFuture<>();
        UserClass thisUser = MainUserActivity.getCurrentUser();
        if (thisUser != null)
        {
            String area = thisUser.getArea();
            ArrayList<String> interests = thisUser.getInterests();

            // Query Firebase to retrieve all users
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    // Keep this outside the for loop
                    ArrayList<CompletableFuture<UserClass>> futures = new ArrayList<>();

                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren())
                    {
                        UserClass currentUser = userSnapshot.getValue(UserClass.class);
                        String currentUserArea = userSnapshot.child("area").getValue(String.class);
                        String otherUserID = userSnapshot.getKey();
                        currentUser.setUniqueID(otherUserID);
                        if (currentUser != null && !userSnapshot.getKey().equals(mAuth.getUid()) &&
                                hasOneMatchingInterests(currentUser, interests) && area.equals(currentUserArea))
                        {
                            CompletableFuture<UserClass> userFuture = null;
                            try {
                                userFuture = getBitmapFromUser(otherUserID).thenApply(image -> {
                                    currentUser.setImage(image);
                                    return currentUser;
                                }).thenCompose(user -> {
                                    CompletableFuture<Boolean> future1 = isOtherUserBlockedByYou(otherUserID);
                                    CompletableFuture<Boolean> future2 = isOtherUserBlockedYou(otherUserID);
                                    CompletableFuture<Boolean> future3 = isThere_A_ChatWithOtherUser(otherUserID);
                                    return CompletableFuture.allOf(future1, future2, future3).thenApply(v ->
                                    {
                                        if (!future1.join() && !future2.join() && !future3.join()) {
                                            return user;
                                        } else {
                                            return null;
                                        }
                                    });
                                });
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            futures.add(userFuture);
                        }
                    }

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() ->
                    {
                        ArrayList<UserClass> filteredAndSortedUsers = new ArrayList<>();
                        futures.forEach(future -> {
                            UserClass user = future.join();
                            if(user != null){
                                filteredAndSortedUsers.add(user);
                            }
                        });
                        ArrayList<UserClass> sortedUsers = new ArrayList<>();
                        while (!filteredAndSortedUsers.isEmpty())
                        {
                            UserClass user = theUserWithMostSharedInterests(filteredAndSortedUsers,interests);
                            filteredAndSortedUsers.remove(user);
                            sortedUsers.add(user);
                        }
                        future.complete(sortedUsers);
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle the error
                }
            });
        }
        return future;
    }


    /**
     * the function returns an async boolean that represent if the other user with the unique id
     * we got is blocked by current user or not
     * @param otherUserID - the other user's unique id
     */
    public static CompletableFuture<Boolean> isOtherUserBlockedByYou (String otherUserID) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (mAuth.getCurrentUser() != null)
        {
            DatabaseReference ref = usersRef.child(mAuth.getUid()).child("Users You Blocked");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            if (childSnapshot.getValue().equals(otherUserID)) {
                                future.complete(true);
                            }
                        }
                    }
                    future.complete(false);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // handle error
                    future.complete(false);
                }
            });
        }
        return future;
    }

    /**
     * the function returns an async boolean that represent if the other user with the unique id
     * we got blocked the current user or not
     * @param otherUserID - the other user's unique id
     */
    public static CompletableFuture<Boolean> isOtherUserBlockedYou (String otherUserID)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (mAuth.getCurrentUser() != null)
        {
            DatabaseReference ref = usersRef.child(otherUserID).child("Users You Blocked");

            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                            if (childSnapshot.getValue().equals(mAuth.getUid())) {
                                future.complete(true);
                            }
                        }
                    }
                    future.complete(false);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // handle error
                    future.complete(false);
                }
            });
        }

        return future;
    }

    /**
     * the function returns true if the other user with unique id we got has a chat with the
     * current user or not
     * @param otherUserID - the other user's unique id
     */
    private static CompletableFuture<Boolean> isThere_A_ChatWithOtherUser(String otherUserID)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        chats.addListenerForSingleValueEvent(new ValueEventListener() {
            String uid = mAuth.getUid();
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isFound = false;
                for (DataSnapshot childSnapshot : snapshot.getChildren())
                {
                    String key = childSnapshot.getKey();
                    if (key != null && (key.contains(uid)) && (key.contains(otherUserID)))
                    {
                        isFound = true;
                        break;
                    }
                }
                if (isFound)
                    future.complete(true);
                else
                    future.complete(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                future.complete(false);
            }
        });
        return future;
    }

    /**
     * the function check if another user have at least one matching interest with
     * the current user from his interests
     * @param user - another user
     * @param thisUserInterests - an ArrayList of Interests of the current user
     * @return - true if there is at least one matching interest and else, false
     */
    private static boolean hasOneMatchingInterests(UserClass user, List<String> thisUserInterests)
    {
        List<String> userInterests = user.getInterests();
        for (String interest : thisUserInterests)
        {
            if (userInterests.contains(interest))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * the function check who is the other user with the most shared interests with
     * current user
     * @param otherUsers - an ArrayList<UserClass> with users that have at least one shared interest
     * with the current user and live at the same area with him
     * @param thisUserInterests - an ArrayList<String> with this user's interests
     * @return - the user with the most shared interests with him
     */
    private static UserClass theUserWithMostSharedInterests (List<UserClass> otherUsers,List<String> thisUserInterests)
    {
        UserClass mostSharedInterestsUser = null;
        int maximumSharedInterests = 0;
        int indexOfUserWithMostSharedInterests = -1;
        for (int i=0; i<otherUsers.size(); i++)
        {
            ArrayList<String> otherUserInterests = otherUsers.get(i).getInterests();
            int countInterests = 0;
            for (String interest : otherUserInterests)
            {
                if (thisUserInterests.contains(interest))
                    countInterests++;
            }
            if (countInterests > maximumSharedInterests)
            {
                maximumSharedInterests = countInterests;
                indexOfUserWithMostSharedInterests = i;
            }
        }
        mostSharedInterestsUser = otherUsers.get(indexOfUserWithMostSharedInterests);
        otherUsers.remove(indexOfUserWithMostSharedInterests);
        return mostSharedInterestsUser;
    }

    /**
     * the function sets the current user's details that changed, in the database and also
     * an image if it changed
     * @param currentUser - the new UserClass object with the new details
     * @param image - and image Bitmap with the data of the new image
     * @return - async - true if it succeeded and else false
     */
    public static CompletableFuture<Boolean> setUserSettingsInDatabase(UserClass currentUser, Bitmap image) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        List<CompletableFuture> updateTasks = new ArrayList<>();

        if (currentUser.getEmail() != null) {
            CompletableFuture<Void> emailUpdateFuture = new CompletableFuture<>();
            user.updateEmail(currentUser.getEmail())
                    .addOnCompleteListener(updateEmailTask -> {
                        if (updateEmailTask.isSuccessful()) {
                            emailUpdateFuture.complete(null);
                        } else {
                            future.complete(false);
                            emailUpdateFuture.completeExceptionally(updateEmailTask.getException());
                        }
                    });
            updateTasks.add(emailUpdateFuture);
        }

        if (MainUserActivity.isImageChanged) {
            CompletableFuture<Boolean> imageDeleteFuture = deleteImageFromFirebaseStorageWithUniqueID(user.getUid());
            imageDeleteFuture.thenAccept(result -> {
                if (result) {
                    uploadImageToFirebaseStorage(user.getUid(), image);
                }
            });
            updateTasks.add(imageDeleteFuture);
        }

        // Call a method to save the user object to your database
        saveUserToDatabase(currentUser);

        if (currentUser.getPassword() != null) {
            CompletableFuture<Void> passwordUpdateFuture = new CompletableFuture<>();
            user.updatePassword(currentUser.getPassword())
                    .addOnCompleteListener(updatePasswordTask -> {
                        if (updatePasswordTask.isSuccessful()) {
                            passwordUpdateFuture.complete(null);
                        } else {
                            future.complete(false);
                            passwordUpdateFuture.completeExceptionally(updatePasswordTask.getException());
                        }
                    });
            updateTasks.add(passwordUpdateFuture);
        }

        CompletableFuture.allOf(updateTasks.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    future.complete(true);
                })
                .exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    return null;
                });

        return future;
    }

    /**
     * it's an helper function to the function that save the user's new details in the database
     * and this function is doing this also
     * @param user - an UserClass object
     */
    private static void saveUserToDatabase (UserClass user)
    {
        FirebaseUser thisUser = mAuth.getCurrentUser();
        DatabaseReference currentUserDbReference = usersRef.child(thisUser.getUid());
        if (user.getArea() != null)
            currentUserDbReference.child("area").setValue(user.getArea());
        if (user.getEmail() != null)
            currentUserDbReference.child("email").setValue(user.getEmail());
        if (user.getInterests() != null)
            currentUserDbReference.child("interests").setValue(user.getInterests());
        if (user.getName() != null)
            currentUserDbReference.child("name").setValue(user.getName());
        if (user.getSummery() != null)
            currentUserDbReference.child("summery").setValue(user.getSummery());

    }

    /**
     * the function checks if the new email the user wants to change to is not already in use
     * @param newEmail - the new email the user wants to change to
     * @return - CompletableFuture<Boolean> -
     * true if the email is not in use and else false
     */
    public static CompletableFuture<Boolean> isNewEmailIsNotInUse(String newEmail)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Check if the current user exists
        if (user == null) {
            future.completeExceptionally(new IllegalStateException("No authenticated user found."));
            return future;
        }

        // Use Firebase Auth to check if the new email is already in use
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(newEmail)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Get the sign-in methods for the provided email
                        List<String> signInMethods = task.getResult().getSignInMethods();
                        if (signInMethods != null && signInMethods.isEmpty()) {
                            // The new email is not in use by any existing user
                            future.complete(true);
                        } else {
                            // The new email is already associated with another user
                            future.complete(false);
                        }
                    } else {
                        // An error occurred while checking the new email
                        future.completeExceptionally(task.getException());
                    }
                });

        return future;
    }

    /**
     * the function check if the current user's password is his real password
     * @param currentPassword - the password the user entered in the settings fragment
     * @return - CompletableFuture<Boolean> -
     * true if it's the real password and else false
     */
    public static CompletableFuture<Boolean> isCurrentPasswordValid (String currentPassword)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Create a credential with the user's email and current password
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

        // Re-authenticate the user with the credential
        user.reauthenticate(credential)
                .addOnCompleteListener(task ->
                {
                    if (task.isSuccessful())
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
     * the function returns a list of MessageClass that represent the chat between the current user
     * and the user with the unique id we got
     * @param otherUserID - the other user's unique id
     */
    public static CompletableFuture<List<MessageClass>> getChatWithOtherUser(String otherUserID)
    {
        CompletableFuture<List<MessageClass>> future = new CompletableFuture<>();

        chats.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String uid = user.getUid();
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String key = childSnapshot.getKey();

                        if (key != null && uid!=null && otherUserID!=null && key.contains(uid) && key.contains(otherUserID)) {
                            List<MessageClass> messages = new ArrayList<>();

                            for (DataSnapshot messageSnapshot : childSnapshot.child("Messages").getChildren()) {
                                messages.add(messageSnapshot.getValue(MessageClass.class));
                            }

                            // Sort the messages by ID (time in milliseconds)
                            Collections.sort(messages, new Comparator<MessageClass>() {
                                @Override
                                public int compare(MessageClass message1, MessageClass message2) {
                                    return Long.compare(message1.getTime(), message2.getTime());
                                }
                            });

                            future.complete(messages);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }

    /**
     * the function adds the MessageClass it gets to the database in the chat node with the id of
     * the chat between the current user and the other user
     * @param message - a MessageClass object with the data of the message the current user sent
     * @param otherUid - the other user's unique id
     */
    public static CompletableFuture<Void> addMessage(MessageClass message, String otherUid)
    {
        CompletableFuture<Void> future = new CompletableFuture<>();

        chats.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                FirebaseUser user = mAuth.getCurrentUser();
                String uid = user.getUid();
                boolean chatIsFound = false;
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren())
                {
                    String key = childSnapshot.getKey();

                    if (key != null && (key.contains(uid)) && key.contains(otherUid))
                    {
                        int messageIndex = (int) childSnapshot.child("Messages").getChildrenCount() + 1;
                        chats.child(key).child("Messages")
                                .child("" + message.getTime()).setValue(message);
                        future.complete(null);
                        chatIsFound = true;
                    }
                }
                if (!chatIsFound) {
                    chats.child(uid + " " +otherUid).child("Messages")
                            .child("" + message.getTime()).setValue(message);
                    future.complete(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                future.completeExceptionally(databaseError.toException());
            }
        });
        return future;
    }


    /**
     * the function returns an Hashmap with the keys represent the ids of the other users that
     * the current user has a chat with them and the values represent the number of messages that
     * sent between them
     */
    public static CompletableFuture<HashMap<String, Integer>> getChatCountsFromDatabase()
    {
        CompletableFuture<HashMap<String, Integer>> future = new CompletableFuture<>();
        if (mAuth.getCurrentUser() == null)
        {
            future.complete(null);
            return future;
        }
        chats.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (mAuth.getCurrentUser() != null)
                {
                    String uid = mAuth.getCurrentUser().getUid();

                    List<CompletableFuture<Void>> queryFutures = new ArrayList<>(); // Track individual query futures
                    HashMap<String, Integer> chatCounts = new HashMap<>();

                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String key = childSnapshot.getKey();

                        if (key != null && (key.contains(uid))) {
                            String[] options = key.split(" ");

                            // Get the reference to the 'Messages' child node for the chat
                            DatabaseReference messagesRef = childSnapshot.child("Messages").getRef();

                            CompletableFuture<Void> queryFuture = new CompletableFuture<>(); // Future for the current query
                            queryFutures.add(queryFuture); // Add it to the list of query futures

                            // Count the number of messages using a ValueEventListener
                            messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    int messageCount = (int) dataSnapshot.getChildrenCount();

                                    // Store the count in the chatCounts map
                                    if (options[0].equals(uid))
                                        chatCounts.put(options[1], messageCount);
                                    else
                                        chatCounts.put(options[0], messageCount);

                                    queryFuture.complete(null); // Mark the current query as completed
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    // Handle the error if needed
                                    queryFuture.completeExceptionally(databaseError.toException());
                                }
                            });
                        }
                    }

                    // Wait for all individual query futures to complete
                    CompletableFuture<Void> allQueriesFuture = CompletableFuture.allOf(queryFutures.toArray(new CompletableFuture[0]));

                    allQueriesFuture.thenAccept((Void) -> {
                        future.complete(chatCounts); // Complete the future with the populated chatCounts map
                    }).exceptionally(ex -> {
                        future.completeExceptionally(ex); // Complete the future exceptionally if any query encounters an error
                        return null;
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle the error if needed
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }

    /**
     * the function returns a list of MessageClass with the size of the number that in the parameter
     * @param otherUserID - the other user's unique id
     * @param count - the number of the difference between the number of messages count that stored
     * in the current user's node in the database and the number of messages there are in the chat
     * node between the two users
     * @return
     */
    public static CompletableFuture<List<MessageClass>> getLastMessages(String otherUserID, int count)
    {
        CompletableFuture<List<MessageClass>> future = new CompletableFuture<>();
        if (mAuth.getCurrentUser() == null)
        {
            future.complete(null);
            return future;
        }
        chats.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null)
                    return;
                String uid = user.getUid();

                for (DataSnapshot childSnapshot : dataSnapshot.getChildren())
                {
                    String key = childSnapshot.getKey();
                    if (key != null && (key.contains(uid)) && (key.contains(otherUserID)))
                    {
                        // Get the reference to the 'Messages' child node for the chat
                        DatabaseReference messagesRef = childSnapshot.child("Messages").getRef();

                        messagesRef.orderByKey().limitToLast(count).addListenerForSingleValueEvent(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                List<MessageClass> lastMessages = new ArrayList<>();

                                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                                    MessageClass message = messageSnapshot.getValue(MessageClass.class);

                                    // Check if the message is sent by the other user
                                    if (message != null && message.getUserSenderId().equals(otherUserID)) {
                                        lastMessages.add(message);
                                    }
                                }

                                future.complete(lastMessages);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Handle the error if needed
                                future.completeExceptionally(databaseError.toException());
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle the error if needed
                future.completeExceptionally(databaseError.toException());
            }
        });

        return future;
    }

    /**
     * the function save in the database in the node of the current user, the ids of the other users
     * that the current user has a chat with them and the number of messages with each one
     * with an HashMap
     */
    public static void saveChatsCounts(HashMap<String, Integer> hashMap)
    {
        if (mAuth.getCurrentUser() == null)
        {
            return ;
        }
        String userId = mAuth.getUid();
        DatabaseReference userRef = usersRef.child(userId);

        // Create a 'Chats Count' node under this user and save the HashMap there
        userRef.child("Chats Count").setValue(hashMap);
    }

    /**
     * the function retrieves the chats counts with each user from the database :
     * the number of messages between the current user and each other user the current user
     * has a chat with them as an HashMap
     */
    public static CompletableFuture<HashMap<String, Integer>> retrieveChatsCounts()
    {
        CompletableFuture<HashMap<String, Integer>> future = new CompletableFuture<>();
        if (mAuth.getCurrentUser() == null)
        {
            future.complete(null);
            return future;
        }
        String userId = mAuth.getUid();
        DatabaseReference userRef = usersRef.child(userId);

        // Attach a listener to read the data at the 'Chats Count' node
        userRef.child("Chats Count").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value
                HashMap<String, Integer> hashMap = new HashMap<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren())
                {
                    hashMap.put(snapshot.getKey(), snapshot.getValue(Integer.class));
                }

                future.complete(hashMap); // Complete the future with the retrieved HashMap
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                future.completeExceptionally(error.toException());
            }
        });

        return future;
    }

    /**
     * the function returns a Long with the data of the last time that the user with the unique
     * id we got, has last signed in to the app
     * @param uniqueID - a String with the data of the unique id of an User
     */
    public static CompletableFuture<Long> getLastTimeSignedIn (String uniqueID)
    {
        CompletableFuture<Long> future = new CompletableFuture<>();
        usersRef.child(uniqueID).addListenerForSingleValueEvent(new ValueEventListener()
        {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                Long last_signed_in = snapshot.child("last signed in").getValue(Long.class);
                if (last_signed_in != null) {
                    future.complete(last_signed_in);
                } else {
                    future.complete(0L); // return 0 or any other default value that makes sense in your context
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {

            }
        });
        return future;
    }

    /**
     * the function saves in the database in the current user's node, the current time in milliseconds
     * that represent the last time the current user last signed in
     */
    public static void saveLastTimeSignedIn ()
    {
        long currentTime = System.currentTimeMillis();
        usersRef.child(mAuth.getUid()).child("last signed in").setValue(currentTime);
    }

    /**
     * returns a boolean data with the value of :
     * true if the current user is online
     * else false
     * @param uniqueID - the unique id of the user we want to check if is online
     */
    public static CompletableFuture<Boolean> getIfUserIsOnline (String uniqueID)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        usersRef.child(uniqueID).addListenerForSingleValueEvent(new ValueEventListener()
        {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                Boolean isSignIn = snapshot.child("Online").getValue(Boolean.class);
                if (isSignIn != null) {
                    future.complete(isSignIn);
                } else {
                    future.complete(null); // return 0 or any other default value that makes sense in your context
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {

            }
        });
        return future;
    }

    /**
     * the function returns an ArrayList of UserClass that represent the users that the current user
     * has a chat with them
     */
    public static CompletableFuture<ArrayList<UserClass>> getUsersWithChats()
    {
        CompletableFuture<ArrayList<UserClass>> future = new CompletableFuture<>();
        if (mAuth.getCurrentUser() == null)
        {
            future.complete(null);
            return future;
        }
        chats.addListenerForSingleValueEvent(new ValueEventListener()
        {
            List<CompletableFuture<UserClass>> futures = new ArrayList<>();
            String uid = mAuth.getUid();
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                if (snapshot.hasChildren())
                {
                    for (DataSnapshot childSnapshot : snapshot.getChildren())
                    {
                        if (childSnapshot.getKey() != null && childSnapshot.getKey().contains(uid))
                        {
                            String [] keys = childSnapshot.getKey().split(" ");
                            String otherUserID = keys[0].equals(uid) ? keys[1] : keys[0];
                            futures.add(getUserProfileDetailsFromDatabase(otherUserID));
                        }
                    }
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenRun(() ->
                    {
                        ArrayList<UserClass> chatsUsers = futures.stream().map(future -> future.join()).collect(Collectors.toCollection(ArrayList::new));
                        future.complete(chatsUsers);
                    });
                }
                else
                    future.complete(new ArrayList<UserClass>());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                future.complete(null);
            }
        });
        return future;
    }

    /**
     * the function gets the last message (MessageClass)in the chat of the current user
     * with other user with the unique id we got
     * @param otherUserID - the unique id of the other user
     */
    public static CompletableFuture<MessageClass> getLastMessageWithOtherUser (String otherUserID)
    {
        CompletableFuture<MessageClass> future = new CompletableFuture<>();
        chats.addListenerForSingleValueEvent(new ValueEventListener()
        {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                for (DataSnapshot childSnapshot : snapshot.getChildren())
                {
                    if (childSnapshot.getKey().contains(mAuth.getUid()) &&
                            childSnapshot.getKey().contains(otherUserID))
                    {
                        DataSnapshot messagesSnapshot = childSnapshot.child("Messages");
                        long count = messagesSnapshot.getChildrenCount();
                        if (count > 0) {
                            DataSnapshot lastChildSnapshot = null;
                            Iterator<DataSnapshot> iterator = messagesSnapshot.getChildren().iterator();
                            while (iterator.hasNext()) {
                                lastChildSnapshot = iterator.next();
                            }
                            MessageClass lastMessage = lastChildSnapshot.getValue(MessageClass.class);
                            future.complete(lastMessage);
                        } else {
                            future.complete(null);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                future.complete(null);
            }
        });
        return future;
    }

    /**
     * the function sets the other user's id in a node of the current user that represent
     * the ids of the users the current user blocked
     * @param otherUserID - the unique id of the other user
     */
    public static void blockUser (String otherUserID)
    {
        DatabaseReference blockedRef = usersRef.child(mAuth.getUid()).child("Users You Blocked").push();
        blockedRef.setValue(otherUserID);
    }

    /**
     * the function removes the other user's id in a node of the current user that represent
     * the ids of the users the current user blocked
     * @param otherUserID - the unique id of the other user
     */
    public static void unBlockUser (String otherUserID)
    {
        DatabaseReference ref = usersRef.child(mAuth.getUid()).child("Users You Blocked");

        ref.addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists())
                {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren())
                    {
                        if (childSnapshot.getValue().equals(otherUserID))
                        {
                            childSnapshot.getRef().removeValue();
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // handle error
            }
        });
    }

    /**
     * the function checks if the user with the unique id we got is still in the database or not
     * @param otherUserID - the unique id of the other user
     * @return - true if the user was deleted and else false
     */
    public static CompletableFuture<Boolean> isUserDeleted(String otherUserID)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener()
        {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                for (DataSnapshot childSnapshot : snapshot.getChildren())
                {
                    if (childSnapshot.getKey().equals(otherUserID))
                    {
                        future.complete(false);
                        break;
                    }
                }
                future.complete(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                future.complete(false);
            }
        });
        return future;
    }

    /**
     * the function checks if the current user is already reported on the user with the unique id
     * we got, and if so it returns true and else false
     * @param otherUserID - the unique id of the other user we want to check
     */
    public static CompletableFuture<Boolean> isUserIsAlreadyReportedOnThisUser (String otherUserID)
    {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        reports.addListenerForSingleValueEvent(new ValueEventListener()
        {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                for (DataSnapshot childSnapshot : snapshot.getChildren())
                {
                    if (childSnapshot.getKey().equals(mAuth.getUid() + " " + otherUserID))
                    {
                        future.complete(true);
                        break;
                    }
                }
                future.complete(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * the function sets a report that the current user reported on the user with the id we got
     * @param otherUserID - the other user's id that the current user wants to report on
     * @param report- a string with the value of the report
     */
    public static void reportOnOtherUser (String otherUserID, String report)
    {
        reports.child(mAuth.getUid() + " " + otherUserID).setValue(report);
    }

}



