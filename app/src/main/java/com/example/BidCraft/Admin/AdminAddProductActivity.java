package com.example.BidCraft.Admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.BidCraft.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class AdminAddProductActivity extends AppCompatActivity {
    private String categoryName, Description, Price, Pname,saveCurrentDate, saveCurrentTime;
    private Button addNewProduct;
    private ImageView productImage;
    private EditText productName, productDescription, productPrice;
    private static final int GalleryPick = 1;
    private Uri ImageUri;
    private String productRandomKey, downloadImageUrl;
    private StorageReference productImageRef;
    private DatabaseReference productsRef;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_product);

        categoryName = getIntent().getExtras().get("category").toString();
        productImageRef = FirebaseStorage.getInstance().getReference().child("Product Images");
        productsRef = FirebaseDatabase.getInstance().getReference().child("Products");

        addNewProduct = findViewById(R.id.add_new_product);
        productImage = findViewById(R.id.select_product_image);
        productName = findViewById(R.id.product_name);
        productDescription = findViewById(R.id.product_description);
        productPrice = findViewById(R.id.product_price);
        loadingBar = new ProgressDialog(this);

        productImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenGallery();
            }
        });

        addNewProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ValidateProdutData();
            }
        });
    }

    private void OpenGallery(){
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/");
        startActivityForResult(galleryIntent, GalleryPick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GalleryPick && resultCode==RESULT_OK && data!=null){
            ImageUri = data.getData();
            productImage.setImageURI(ImageUri);
        }
    }

    private void ValidateProdutData(){
        Description = productDescription.getText().toString();
        Price = productPrice.getText().toString();
        Pname = productName.getText().toString();

        if (ImageUri == null){
            Toast.makeText(this,"Product image is mandatory", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Description)){
            Toast.makeText(this,"Please write product Description", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Price)){
            Toast.makeText(this,"Please write product Price", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(Pname)){
            Toast.makeText(this,"Please write product Name", Toast.LENGTH_SHORT).show();
        }
        else {
            StoreProductInformation();
        }
    }

    private void StoreProductInformation(){
        loadingBar.setTitle("Add New Product");
        loadingBar.setMessage("Please Wait");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MM dd,yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        productRandomKey = saveCurrentDate + saveCurrentTime;

           final StorageReference filePath = productImageRef.child(ImageUri.getLastPathSegment() + productRandomKey + ".jpg");

            final UploadTask uploadTask = filePath.putFile(ImageUri);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    String message = e.toString();
                    Toast.makeText(AdminAddProductActivity.this,"Error : " + message,Toast.LENGTH_SHORT).show();
                    loadingBar.dismiss();

                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(AdminAddProductActivity.this,"Image Uploaded Successfully...",Toast.LENGTH_SHORT).show();

                    Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()){
                                throw task.getException();
                            }

                            downloadImageUrl = filePath.getDownloadUrl().toString();
                            return filePath.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()){
                                downloadImageUrl = task.getResult().toString();

                                Toast.makeText(AdminAddProductActivity.this,"getting Product Image url Succesfully...",Toast.LENGTH_SHORT).show();

                                SaveProductInfotoDatabase();
                            }
                        }
                    });
                }
            });
    }

    private void SaveProductInfotoDatabase(){
        HashMap<String, Object> productMap = new HashMap<>();
        productMap.put("pid", productRandomKey);
        productMap.put("date", saveCurrentDate);
        productMap.put("time", saveCurrentTime);
        productMap.put("description", Description);
        productMap.put("image", downloadImageUrl);
        productMap.put("category", categoryName);
        productMap.put("price", Price);
        productMap.put("pname", Pname);

        productsRef.child(productRandomKey).updateChildren(productMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Intent intent = new Intent(AdminAddProductActivity.this, AdminCategoryActivity.class);
                            startActivity(intent);

                            loadingBar.dismiss();
                            Toast.makeText(AdminAddProductActivity.this, "Product is added succefully...", Toast.LENGTH_SHORT).show();
                        }else {
                            loadingBar.dismiss();
                            String message = task.getException().toString();
                            Toast.makeText(AdminAddProductActivity.this, "Error : " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}