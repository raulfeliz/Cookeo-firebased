package com.rukiasoft.androidapps.cocinaconroll.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rukiasoft.androidapps.cocinaconroll.CocinaConRollApplication;
import com.rukiasoft.androidapps.cocinaconroll.R;
import com.rukiasoft.androidapps.cocinaconroll.ui.model.RecipeComplete;
import com.rukiasoft.androidapps.cocinaconroll.utilities.ReadWriteTools;
import com.rukiasoft.androidapps.cocinaconroll.utilities.RecetasCookeoConstants;
import com.rukiasoft.androidapps.cocinaconroll.utilities.Tools;
import com.squareup.leakcanary.RefWatcher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import icepick.State;


public class EditRecipePhotoFragment extends Fragment {

    private Uri mImageCaptureUri;
    

    private Bitmap photo;
    private Tools mTools;
    private ReadWriteTools rwTools;
    private String nameOfNewImage = "";

    private static final int PICK_FROM_CAMERA = 1;
    private static final int CROP_FROM_CAMERA = 2;
    private static final int PICK_FROM_FILE = 3;
    private static final int CROP_FROM_FILE = 4;

    @BindView(R.id.edit_recipe_photo) ImageView mImageView;
    @BindView(R.id.edit_recipe_minutes) EditText minutes;
    @BindView(R.id.edit_recipe_minutes_layout)
    TextInputLayout minutesLayout;
    @BindView(R.id.edit_recipe_portions_layout) TextInputLayout portionsLayout;
    @BindView(R.id.edit_recipe_portions) EditText portions;
    @BindView(R.id.create_recipe_name_layout) TextInputLayout createRecipeNameLayout;
    @BindView(R.id.create_recipe_name_edittext) EditText createRecipeName;
    @BindView(R.id.spinner_type_dish) Spinner spinner;

    @BindView(R.id.checkbox_vegetarian)CheckBox checkBox;
    @State String newPicName;
    private Unbinder unbinder;

    public EditRecipePhotoFragment() {
    }

    public String getNameOfNewImage() {
        return nameOfNewImage;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mTools = new Tools();
        rwTools = new ReadWriteTools();
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if(getActivity().isFinishing()){
            return null;
        }
        View view;
        RecipeComplete recipe = ((EditRecipeActivity) getActivity()).getRecipe();

        view = inflater.inflate(R.layout.fragment_edit_recipe_foto_create, container, false);

        unbinder = ButterKnife.bind(this, view);

        List<String> list = new ArrayList<>();
        list.add(getResources().getString(R.string.starters));
        list.add(getResources().getString(R.string.main_courses));
        list.add(getResources().getString(R.string.desserts));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>
                (getActivity(), android.R.layout.simple_spinner_item, list);

        dataAdapter.setDropDownViewResource
                (android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);

        int currentApiVersion = Build.VERSION.SDK_INT;
        if (currentApiVersion <= Build.VERSION_CODES.JELLY_BEAN){
            final float scale = this.getResources().getDisplayMetrics().density;
            checkBox.setPadding(checkBox.getPaddingLeft() + (int)(20.0f * scale + 0.5f),
                    checkBox.getPaddingTop(),
                    checkBox.getPaddingRight(),
                    checkBox.getPaddingBottom());
        }

        if (recipe != null && recipe.getName() != null) {
            createRecipeName.setText(recipe.getName());
            rwTools.loadImageFromPath(getActivity().getApplicationContext(), mImageView,
                    recipe.getPicture(),
                    R.drawable.default_dish, recipe.getTimestamp());

            if(recipe.getMinutes()>0)
                minutes.setText(recipe.getMinutes().toString());
            else
                minutes.setText("0");

            if(recipe.getPortions()>0)
                portions.setText(recipe.getPortions().toString());
            else
                portions.setText("0");

            checkBox.setChecked(recipe.getVegetarian());

            String type = "";
            if(recipe.getType().compareTo(RecetasCookeoConstants.TYPE_STARTERS) == 0)
                type = getResources().getString(R.string.starters);
            else if(recipe.getType().compareTo(RecetasCookeoConstants.TYPE_MAIN) == 0)
                type = getResources().getString(R.string.main_courses);
            else if(recipe.getType().compareTo(RecetasCookeoConstants.TYPE_DESSERTS) == 0)
                type = getResources().getString(R.string.desserts);
            spinner.setSelection(dataAdapter.getPosition(type));
        }

        mImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        ContextCompat.checkSelfPermission(getActivity(),
                                Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.CAMERA)) {
                        android.support.v7.app.AlertDialog.Builder builder =
                                new android.support.v7.app.AlertDialog.Builder(getActivity());

                        builder.setMessage(getResources().getString(R.string.camera_explanation))
                                .setTitle(getResources().getString(R.string.permissions_title))
                                .setPositiveButton(getResources().getString(R.string.accept),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                                ActivityCompat.requestPermissions(getActivity(),
                                                        new String[]{Manifest.permission.CAMERA},
                                                        RecetasCookeoConstants.MY_PERMISSIONS_REQUEST_CAMERA);
                                            }
                                        });
                        builder.create().show();
                    } else {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.CAMERA},
                                RecetasCookeoConstants.MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                }else{
                    selectPhoto(true);
                }
            }
        });


        //spinner1.setOnItemSelectedListener(new CustomOnItemSelectedListener());


        return view;
    }

    /*private class CustomOnItemSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View view, int pos,
                                   long id) {
            switch (pos){
                case 0:
                    recipe.setType(RecetasCookeoConstants.TYPE_STARTERS);
                    break;
                case 1:
                    recipe.setType(RecetasCookeoConstants.TYPE_MAIN);
                    break;
                case 2:
                    recipe.setType(RecetasCookeoConstants.TYPE_DESSERTS);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {

        }

    }*/


    public void selectPhoto(Boolean cameraAllowed){

        final String [] items;
        if(cameraAllowed){
            items = new String [] {getResources().getString(R.string.pick_from_gallery),
                    getResources().getString(R.string.pick_from_camera)};
        }else{
            items = new String [] {getResources().getString(R.string.pick_from_gallery)};
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<> (getActivity(), android.R.layout.select_dialog_item,items);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.pick_photo));
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) { //pick from camera
                switch (item) {
                    case 0:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_FILE);
                        break;
                    case 1:
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        mImageCaptureUri = Uri.fromFile(new File(rwTools.getEditedStorageDir(),
                                RecetasCookeoConstants.TEMP_CAMERA_NAME + String.valueOf(System.currentTimeMillis()) + ".jpg"));
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                        try {
                            takePictureIntent.putExtra("return-data", true);
                            if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) == null) {
                                Toast.makeText(getActivity(), getResources().getString(R.string.no_camera), Toast.LENGTH_LONG);
                            }
                            startActivityForResult(takePictureIntent, PICK_FROM_CAMERA);
                        } catch (ActivityNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), getResources().getString(R.string.no_camera), Toast.LENGTH_LONG).show();
                        }
                        break;
                }
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void doCrop(int mode) {
        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        //indicate image type and Uri
        cropIntent.setDataAndType(mImageCaptureUri, "image/*");
        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(
                cropIntent, 0);

        if (list.size() == 0) {

            try {
                photo = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), mImageCaptureUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // TODO: 5/3/17 descomentar
            //newPicName = rwTools.saveBitmap(getActivity().getApplicationContext(), photo, recipe.getPicture());

            rwTools.loadImageFromPath(getActivity().getApplicationContext(), mImageView, newPicName,
                    R.drawable.default_dish, System.currentTimeMillis());
            return;
        }
        //set crop properties
        cropIntent.putExtra("crop", "true");
        //indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 4);
        cropIntent.putExtra("aspectY", 3);
        //indicate output X and Y
        cropIntent.putExtra("outputX", 400);
        cropIntent.putExtra("outputY", 300);
        //retrieve data on return
        cropIntent.putExtra("return-data", true);
        //start the activity - we handle returning in onActivityResult
        startActivityForResult(cropIntent, mode);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
        switch (requestCode) {
            case PICK_FROM_CAMERA:
                doCrop(CROP_FROM_CAMERA);
                break;
            case PICK_FROM_FILE:
                Uri originalUri = data.getData();

                String id;
                try{
                    id = originalUri.getLastPathSegment().split(":")[1];
                }catch(Exception e){
                    id = originalUri.getLastPathSegment();
                }
                final String[] imageColumns = {MediaStore.Images.Media.DATA};

                Uri uri = getUri();
                String selectedImagePath = "path";

                Cursor cursor = getActivity().getContentResolver().query(uri, imageColumns,
                        MediaStore.Images.Media._ID + "=" + id, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    selectedImagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    cursor.close();
                }

                File file = new File(selectedImagePath);
                if (file.exists()) {
                    mImageCaptureUri = Uri.fromFile(new File(selectedImagePath));
                }

                doCrop(CROP_FROM_FILE);
                break;
            case CROP_FROM_CAMERA:
                Bundle extras = data.getExtras();
                if (extras != null) {
                    photo = extras.getParcelable("data");
                    updateNameOfNewImage(newPicName);
                    newPicName = rwTools.saveBitmap(getActivity().getApplicationContext(), photo, getPictureNameFromFileName());

                    rwTools.loadImageFromPath(getActivity().getApplicationContext(),
                            mImageView, newPicName,
                            R.drawable.default_dish, System.currentTimeMillis());
                }
                File f = new File(mImageCaptureUri.getPath());
                if (f.exists()) {
                    f.delete();
                }
                break;
            case CROP_FROM_FILE:
                Bundle extras2 = data.getExtras();
                if (extras2 != null) {
                    photo = extras2.getParcelable("data");
                    updateNameOfNewImage(newPicName);
                    newPicName = rwTools.saveBitmap(getActivity().getApplicationContext(), photo, getPictureNameFromFileName());
                    //if(recipe.getState().compareTo(RecetasCookeoConstants.STATE_OWN) != 0)
                    rwTools.loadImageFromPath(getActivity().getApplicationContext(),
                            mImageView, newPicName,
                            R.drawable.default_dish, System.currentTimeMillis());
                }
                break;
        }
    }

    private void updateNameOfNewImage(String name){
        if(!nameOfNewImage.isEmpty()){
            rwTools.deleteImageFromEditedPath(nameOfNewImage);
        }
        nameOfNewImage = name;
    }

    private Uri getUri() {
        String state = Environment.getExternalStorageState();
        if(!state.equalsIgnoreCase(Environment.MEDIA_MOUNTED))
            return MediaStore.Images.Media.INTERNAL_CONTENT_URI;

        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }



    public int getPortions(){
        try {
            return Integer.parseInt(portions.getText().toString());
        }catch(NumberFormatException e){
            return -1;
        }
    }

    public int getMinutes(){
        try {
            return Integer.parseInt(minutes.getText().toString());
        }catch(NumberFormatException e){
            return -1;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public Boolean checkInfoOk(){
        mTools.hideSoftKeyboard(getActivity());
        boolean ret = true;
        Integer min;
        try {
            min = Integer.valueOf(minutes.getText().toString());
        }catch (NumberFormatException e){
            min = 0;
            minutes.setText(min.toString());
        }
        Integer port;
        try {
            port = Integer.valueOf(portions.getText().toString());
        }catch (NumberFormatException e){
            port = 0;
            portions.setText(port.toString());
        }


        //create case
        if(createRecipeNameLayout != null) {
            createRecipeNameLayout.setError(null);
        }
        portionsLayout.setError(null);
        minutesLayout.setError(null);
        String sName = createRecipeName.getText().toString();
        if(sName.isEmpty()){
            createRecipeNameLayout.setError(getResources().getString(R.string.no_recipe_name));
            ret = false;
        }

        if(ret){
            RecipeComplete recipe = getRecipeFromParams();
            ((EditRecipeActivity)getActivity()).setRecipe(recipe);

        }
        return ret;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = CocinaConRollApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }

    @Override
    public void onPause(){
        if(createRecipeName != null) {
            String name = createRecipeName.getText().toString();
            //recipe.setName(name);
        }
        super.onPause();
    }

    private String getPictureNameFromFileName(){
        return mTools.getCurrentDate(getActivity()).concat(".jpg");
    }

    private String getKey(String uid){
        RecipeComplete recipe = ((EditRecipeActivity)getActivity()).getRecipe();
        if(recipe == null || recipe.getKey() == null || recipe.getKey().isEmpty()){
            DatabaseReference ref = FirebaseDatabase
                    .getInstance()
                    .getReference(RecetasCookeoConstants.PERSONAL_RECIPES_NODE);
            return ref.child(uid).push().getKey();
        }else{
            return recipe.getKey();
        }
    }

    private RecipeComplete getRecipeFromParams(){
        String texto = (java.lang.String) spinner.getSelectedItem();
        RecipeComplete recipe = ((EditRecipeActivity)getActivity()).getRecipe();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String author = "";
        if(user != null) {
            author = user.getDisplayName();
        }else{
            return null;
        }

        String key = getKey(user.getUid());
        return RecipeComplete.getRecipeFrom1Screen(recipe, key, createRecipeName.getText().toString(),
                newPicName, checkBox.isChecked(), getTypeFromSpinner(), Integer.valueOf(minutes.getText().toString()),
                Integer.valueOf(portions.getText().toString()), author);
    }

    private String getTypeFromSpinner(){
        int position = spinner.getSelectedItemPosition();
        String type;
        switch (position) {
            case 0:
                type = RecetasCookeoConstants.TYPE_STARTERS;
                break;
            case 1:
                type = RecetasCookeoConstants.TYPE_MAIN;
                break;
            case 2:
                type = RecetasCookeoConstants.TYPE_DESSERTS;
                break;
            default:
                type = RecetasCookeoConstants.TYPE_MAIN;
        }
        return type;

    }
}

