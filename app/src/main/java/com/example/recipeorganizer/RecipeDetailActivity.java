package com.example.recipeorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {

    private TextView nameTextView, ingredientsTextView, instructionsTextView, categoryTextView;
    private Button backButton, editButton, publishButton;

    // Declare ActivityResultLauncher
    private ActivityResultLauncher<Intent> editRecipeLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        // Initialize views
        nameTextView = findViewById(R.id.nameTextView);
        ingredientsTextView = findViewById(R.id.ingredientsTextView);
        instructionsTextView = findViewById(R.id.instructionsTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        backButton = findViewById(R.id.backButton);
        editButton = findViewById(R.id.editButton);
        publishButton = findViewById(R.id.publishButton);

        // Get the isPublishedView flag from intent
        boolean isPublishedView = getIntent().getBooleanExtra("isPublishedView", false);

        // Hide the buttons if viewed from published recipes
        if (isPublishedView) {
            editButton.setVisibility(View.GONE);
            publishButton.setVisibility(View.GONE);
        }

        // Initialize ActivityResultLauncher
        editRecipeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Recipe updatedRecipe = (Recipe) data.getSerializableExtra("updatedRecipe");
                            if (updatedRecipe != null) {
                                // Update the UI with the new recipe data
                                nameTextView.setText(updatedRecipe.getName());
                                categoryTextView.setText(updatedRecipe.getCategory());
                                ingredientsTextView.setText(formatList(updatedRecipe.getIngredients()));
                                instructionsTextView.setText(formatList(updatedRecipe.getInstructions()));
                            }
                        }
                    }
                });

        // Get the recipe data from the Intent
        Recipe recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        if (recipe != null) {
            nameTextView.setText(recipe.getName());
            categoryTextView.setText(recipe.getCategory());
            ingredientsTextView.setText(formatList(recipe.getIngredients())); // Updated to handle List<String>
            instructionsTextView.setText(formatList(recipe.getInstructions())); // Updated to handle List<String>
            updatePublishButton(recipe.isPublished()); // Update button text based on current status
        }

        // Set up the back button click listener
        backButton.setOnClickListener(v -> finish());

        // Set up the edit button click listener
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecipeDetailActivity.this, EditRecipeActivity.class);
            intent.putExtra("recipe", recipe);
            editRecipeLauncher.launch(intent); // Launch the edit activity
        });

        // Set up the publish button click listener
        publishButton.setOnClickListener(v -> {
            if (recipe.isPublished()) {
                unpublishRecipe(recipe); // Unpublish the recipe
            } else {
                publishRecipe(recipe); // Publish the recipe
            }
        });
    }

    // Method to update button text
    private void updatePublishButton(boolean isPublished) {
        if (isPublished) {
            publishButton.setText("Unpublish Recipe");
        } else {
            publishButton.setText("Publish Recipe");
        }
    }

    // Method to publish a recipe
    private void publishRecipe(Recipe recipe) {
        recipe.setPublished(true);

        // Update the user's private recipe reference to set `published` to true
        DatabaseReference userRecipeRef = FirebaseDatabase.getInstance()
                .getReference("recipes")
                .child(FirebaseAuth.getInstance().getUid()) // Replace with userId if you store it separately
                .child(recipe.getId());

        userRecipeRef.child("published").setValue(true).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                // Now add the recipe to the public recipes in the database
                DatabaseReference publicRecipeRef = FirebaseDatabase.getInstance().getReference("public_recipes");
                publicRecipeRef.child(recipe.getId()).setValue(recipe).addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        updatePublishButton(true); // Update button text to reflect the published status
                        Toast.makeText(RecipeDetailActivity.this, "Recipe Published Successfully", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // Handle error if updating the private recipe fails
                Toast.makeText(RecipeDetailActivity.this, "Failed to Publish Recipe", Toast.LENGTH_SHORT).show();
            }

        });
    }


    // Method to unpublish a recipe
    private void unpublishRecipe(Recipe recipe) {
        recipe.setPublished(false);

        // Remove the recipe from the public recipes list
        DatabaseReference publicRecipeRef = FirebaseDatabase.getInstance().getReference("public_recipes");
        publicRecipeRef.child(recipe.getId()).removeValue().addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                // If successful, update the user's private recipe to set `published` to false
                DatabaseReference userRecipeRef = FirebaseDatabase.getInstance()
                        .getReference("recipes")
                        .child(FirebaseAuth.getInstance().getUid())
                        .child(recipe.getId());

                userRecipeRef.child("published").setValue(false).addOnCompleteListener(task2 -> {
                    if (task2.isSuccessful()) {
                        updatePublishButton(false); // Update button text to reflect unpublished status
                    } else {
                        // Handle error if updating the private recipe fails
                        Log.e("UnpublishRecipe", "Failed to update published status in user's recipe list.");
                    }
                });
            } else {
                // Handle error if removing from public recipes fails
                Log.e("UnpublishRecipe", "Failed to remove recipe from public recipes list.");
            }
        });
    }

    private String formatList(List<String> items) {
        StringBuilder formattedList = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            formattedList.append(i + 1).append(". ").append(items.get(i)).append("\n");
        }
        return formattedList.toString().trim();
    }



}