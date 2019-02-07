package com.MemDerPack.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.util.DiffUtil;
import android.text.Layout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.MemDerPack.ChatsActivity;
import com.MemDerPack.LoadActivity;
import com.MemDerPack.Logic.PictureLogic;
import com.MemDerPack.Logic.UserLogic;
import com.MemDerPack.R;
import com.MemDerPack.StartActivity;
import com.MemDerPack.Swipes.CardStackAdapter;
import com.MemDerPack.Swipes.Methods;
import com.MemDerPack.Swipes.Spot;
import com.MemDerPack.Swipes.SpotDiffCallback;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.yuyakaido.*;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static com.MemDerPack.LoadActivity.memeFolder;
import static com.MemDerPack.LoadActivity.numberOfMemesInBuffer;
import static com.MemDerPack.Logic.JSONConverter.convertToJSON;

public class MemesFragment extends Fragment implements CardStackListener {

    // Arraylist of memes.
    public ArrayList<PictureLogic.Picture> pictureList;
    public Integer counter;

    // Initializing firebase elements.
    FirebaseUser firebaseUser;
    DatabaseReference reference;
    DatabaseReference referenceChangeRight;
    DatabaseReference referenceChangeLeft;

    // Initializing swipes elements. public и методы добавил Федос, аккуратно для теста. OnCreate как в sample ? lazy - аналог этих методов synchronized, которые у него просто идут перед OnCreate
    //Итог: белые листы много реже + почти нет белых вечных листов, которые не подгрузятся вообще => рабоать надо от его сэмпла и продумывать дальше, но lazy(синхронайзд) что-то решило точно
    public DrawerLayout drawerLayout;
    public CardStackView cardStackView;
    public CardStackLayoutManager manager;
    public CardStackAdapter adapter;

    public synchronized DrawerLayout getlayout(View view) {

        drawerLayout = view.findViewById(R.id.drawer_layout);

        return drawerLayout;
    }

    public synchronized CardStackView getCardStackView(View view) {

        cardStackView = view.findViewById(R.id.card_stack_view);

        return cardStackView;
    }

    public synchronized CardStackLayoutManager getManager() {

        manager = new CardStackLayoutManager(getContext(), this);

        return manager;
    }

    public synchronized CardStackAdapter getAdapter() {

        adapter = new CardStackAdapter(LoadActivity.pictureList);

        return adapter;
    }

    // Categories array.
    public static ArrayList<String> categories;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memes, container, false);

        // Attaching swipes elements.
        drawerLayout = getlayout(view);
        cardStackView = getCardStackView(view);
        manager = getManager();
        adapter = getAdapter();

//        drawerLayout = view.findViewById(R.id.card_stack_view);
//        cardStackView = gview.findViewById(R.id.card_stack_view);
//        manager = new CardStackLayoutManager(getContext(), this);
//        adapter = new CardStackAdapter(LoadActivity.pictureList);

        setupCardStackView();

        // Associating firebase variables
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        referenceChangeRight = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());
        referenceChangeLeft = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid());

        // Taking picturelist array from load activity.
        pictureList = LoadActivity.pictureList;
        categories = new ArrayList<>();


        // Setting the counter of meme to display.
        counter = 0;

        // Creating reference for the list of categories..
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(memeFolder);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                // Filling the categories array.
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    // Filling categories array.
                    categories.add(snapshot.getKey().toString());

                    // Filling buffer array with zeros.
                    numberOfMemesInBuffer.put(snapshot.getKey().toString(), 0);
                }

                // Filling the local buffer.
                for (PictureLogic.Picture pic : pictureList) {

                    int category = pic.Category;

                    // Defining the category of current meme.
                    String categoryNext = categories.get(category);

                    // Adding a point to local buffer.
                    numberOfMemesInBuffer.put(categoryNext, numberOfMemesInBuffer.get(categoryNext) + 1);
                }

                //System.out.println(numberOfMemesInBuffer);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        return view;
    }

    @Override
    public void onCardDragging(Direction direction, float ratio) {
       // Log.d("CardStackView", "onCardDragging: d = ${direction.name}, r = $ratio");
    }

    @Override
    public void onCardSwiped(Direction direction) {
       // Log.d("CardStackView", "onCardSwiped: p = ${manager.topPosition}, d = $direction");

        PictureLogic.Picture picture = ChooseMeme(direction);

        paginate(picture);
    }

    @Override
    public void onCardRewound() {
       // Log.d("CardStackView", "onCardRewound: ${manager.topPosition}");
    }

    @Override
    public void onCardCanceled() {
     //   Log.d("CardStackView", "onCardCanceled: ${manager.topPosition}");
    }

    @Override
    public void onCardAppeared(View view, int position) {
//        TextView textView = view.findViewById(R.id.item_name);
       //Log.d("CardStackView", "onCardAppeared: ($position) ${textView.text}");
    }

    @Override
    public void onCardDisappeared(View view, int position) {
//        TextView textView = view.findViewById(R.id.item_name);
//        Log.d("CardStackView", "onCardDisappeared: ($position) ${textView.text}");
    }

    private List<Spot> createSpots() {
        ArrayList<Spot> spots = new ArrayList<>();

        Methods methods = new Methods();
        spots = methods.createSpots();

        return spots;
    }

    private void setupCardStackView() {
        initialize();
    }

    private void initialize() {
        manager.setStackFrom(StackFrom.Top);
        manager.setVisibleCount(3);
        manager.setTranslationInterval(10.0f);
        manager.setScaleInterval(0.95f);
        manager.setSwipeThreshold(0.30f);
        manager.setMaxDegree(30.0f);
        manager.setDirections(Direction.FREEDOM);
        manager.setCanScrollHorizontal(true);
        manager.setCanScrollVertical(true);

        cardStackView.setLayoutManager(manager);
        cardStackView.setAdapter(adapter);

        //ToDo: разобраться, влияет ли это на свайпы.
//        cardStackView.itemAnimator.apply {
//            if (this is DefaultItemAnimator){
//                supportsChangeAnimations = false
//            }
//        }
    }

    private void paginate(PictureLogic.Picture picture) {
        List<PictureLogic.Picture> old = adapter.getSpots();


        List<PictureLogic.Picture> old_new = old;

//        List<PictureLogic.Picture> temp = new ArrayList<>();
//        temp.add(picture);


        old_new.add(picture);


        SpotDiffCallback callback = new SpotDiffCallback(old, old_new);


        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback);
        adapter.setSpots(old_new);
        result.dispatchUpdatesTo(adapter);
    }

    public PictureLogic.Picture ChooseMeme(Direction direction) {

        PictureLogic.Picture picture = new PictureLogic.Picture();

        if (direction == Direction.Left) {
            picture = ChangePreferenceAndLoadImage("dislike");
        } else if (direction == Direction.Right) {
            picture = ChangePreferenceAndLoadImage("like");
        } else if (direction == Direction.Top) {
            picture = ChangePreferenceAndLoadImage("superlike");
        } else if (direction == Direction.Bottom) {
            picture = ChangePreferenceAndLoadImage("superdislike");
        }


        return picture;
    }

    public PictureLogic.Picture pictureToLoad = new PictureLogic.Picture();

    public PictureLogic.Picture ChangePreferenceAndLoadImage(final String forHowMuchLiked) {


        // Database synchronization.
        referenceChangeLeft.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                /*
                 * Changing preferences.
                 */

                // Creating new reference.
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                // Initializing local user.
                final UserLogic.User fireUser = dataSnapshot.getValue(UserLogic.User.class);

                // Changing preferences.
                PictureLogic.PictureMethods.ChangePreference(fireUser, pictureList.get(counter), forHowMuchLiked);

                // Sending them to sever.
                databaseReference.child("Users").child(firebaseUser.getUid()).child("preferences").setValue(fireUser.getPreferencesList().toString());

                /*
                 * Adding a point to number of meme.
                 */

                // String of current category.
                final String category = categories.get(pictureList.get(counter).Category);

                // Number of meme in category.

                // Adding point to a category of current meme.
                final DatabaseReference currentMemeCategory = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid())
                        .child("Categories_seen").child(category);
                currentMemeCategory.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        // Current meme number.
                        final Integer memeNumber = Integer.parseInt(dataSnapshot.getValue().toString());

                        // Creating reference for subfolder (selecting subfolder by choosing the prefered category).
                        final DatabaseReference memeReference = FirebaseDatabase.getInstance().getReference(memeFolder).child(category);
                        memeReference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                // Getting the hasmap of memes.
                                Map<String, Object> td = (HashMap<String, Object>) dataSnapshot.getValue();

                                if (memeNumber >= td.size()) {
                                    System.out.println("Закончились мемесы.");
                                    //numberOfMemesInBuffer.put(category, 0);

                                } else {
                                    currentMemeCategory.setValue(memeNumber + 1);
                                    //Decrease number of meme of this category in buffer.
                                    numberOfMemesInBuffer.put(category, numberOfMemesInBuffer.get(category) - 1);
                                }

                                // Defining the category of next meme.
                                final String categoryNext = categories.get(UserLogic.UserMethods.getCategory(fireUser.getPreferencesList()));

                                // Creating reference for subfolder (selecting subfolder by choosing the prefered category).
                                DatabaseReference memeReference = FirebaseDatabase.getInstance().getReference(memeFolder).child(categoryNext);
                                memeReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        // Getting the list of meme values (links).
                                        final Map<String, Object> td = (HashMap<String, Object>) dataSnapshot.getValue();

                                        // Getting and sorting the keys.
                                        final ArrayList<String> keys = new ArrayList<>(td.keySet());
                                        Collections.sort(keys);

                                        // Taking the number of the meme to pick.
                                        DatabaseReference memeCategory = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid()).child("Categories_seen").child(categoryNext);
                                        memeCategory.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                String memeUrl;
                                                // Current meme number.
                                                Integer memeNumber;
                                                numberOfMemesInBuffer.put(categoryNext, numberOfMemesInBuffer.get(categoryNext) + 1);
                                                memeNumber = Integer.parseInt(dataSnapshot.getValue().toString()) + numberOfMemesInBuffer.get(categoryNext);
                                                if (memeNumber < td.size()) {
                                                    memeUrl = td.get(keys.get(memeNumber)).toString();
                                                } else {
                                                    memeUrl = td.get(keys.get(keys.size() - 1)).toString();
                                                }


                                                // Making a Picture element (attaching category to a picture).
                                                pictureToLoad = new PictureLogic.Picture(memeUrl, categories.indexOf(categoryNext));
                                                counter++;


//                                        // Add it to pic list.
//                                        pictureList.add(picture);
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


                /*
                 * Adding new picture.
                 */

//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//
//                        // Defining the category of next meme.
//                        final String categoryNext = categories.get(UserLogic.UserMethods.getCategory(fireUser.getPreferencesList()));
//
//                        // Creating reference for subfolder (selecting subfolder by choosing the prefered category).
//                        DatabaseReference memeReference = FirebaseDatabase.getInstance().getReference(memeFolder).child(categoryNext);
//                        memeReference.addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                                // Getting the list of meme values (links).
//                                final Map<String, Object> td = (HashMap<String, Object>) dataSnapshot.getValue();
//
//                                // Getting and sorting the keys.
//                                final ArrayList<String> keys = new ArrayList<>(td.keySet());
//                                Collections.sort(keys);
//
//                                // Taking the number of the meme to pick.
//                                DatabaseReference memeCategory = FirebaseDatabase.getInstance().getReference("Users").child(firebaseUser.getUid()).child("Categories_seen").child(categoryNext);
//                                memeCategory.addListenerForSingleValueEvent(new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                                        String memeUrl;
//                                        // Current meme number.
//                                        Integer memeNumber;
//                                        numberOfMemesInBuffer.put(categoryNext, numberOfMemesInBuffer.get(categoryNext) + 1);
//                                        memeNumber = Integer.parseInt(dataSnapshot.getValue().toString()) + numberOfMemesInBuffer.get(categoryNext);
//                                        if (memeNumber < td.size()) {
//                                            memeUrl = td.get(keys.get(memeNumber)).toString();
//                                        } else {
//                                            memeUrl = td.get(keys.get(keys.size() - 1)).toString();
//                                        }
//
//
//                                        // Making a Picture element (attaching category to a picture).
//                                        pictureToLoad = new PictureLogic.Picture(memeUrl, categories.indexOf(categoryNext));
//                                        counter++;
//
//
////                                        // Add it to pic list.
////                                        pictureList.add(picture);
//                                    }
//
//                                    @Override
//                                    public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                                    }
//                                });
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                            }
//                        });
//
////                        // Remove the object we just swiped.
////                        pictureList.remove(0);
//                    }
//                }, 10);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // Vibration.
        Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(30);
        }


        deleteCache(getActivity().getApplicationContext());

        String jsonString = convertToJSON(pictureList);

        // Saving data to shared preferences.
        SharedPreferences.Editor editor = getContext().getSharedPreferences("Buffer", MODE_PRIVATE).edit();
        editor.putString("pictures", jsonString);
        editor.apply();


        return pictureToLoad;
    }


    @Override
    public void onDestroy() {


        super.onDestroy();
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

}