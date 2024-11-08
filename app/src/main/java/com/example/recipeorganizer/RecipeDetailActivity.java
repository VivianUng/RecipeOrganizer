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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;

public class RecipeDetailActivity extends AppCompatActivity {

    private TextView nameTextView, ingredientsTextView, instructionsTextView, categoryTextView;
    private Button publishButton;

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
        Button backButton = findViewById(R.id.backButton);
        Button editButton = findViewById(R.id.editButton);
        publishButton = findViewById(R.id.publishButton);
        Button addToMyRecipesButton = findViewById(R.id.addToMyRecipesButton);

        // Get the isPublishedView flag from intent
        boolean isPublishedView = getIntent().getBooleanExtra("isPublishedView", false);
        boolean fromMyRecipes = getIntent().getBooleanExtra("fromMyRecipes", false); // Check if accessed from My Recipes

        // Hide the buttons if viewed from published recipes
        if (isPublishedView) {
            editButton.setVisibility(View.GONE);
            publishButton.setVisibility(View.GONE);
        }

        // Hide the "Add to My Recipes" button if accessed from My Recipes
        if (fromMyRecipes) {
            addToMyRecipesButton.setVisibility(View.GONE);
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
            if(recipe != null) {
                if (recipe.isPublished()) {
                    unpublishRecipe(recipe); // Unpublish the recipe
                } else {
                    publishRecipe(recipe); // Publish the recipe
                }
            }
        });

        // Set up the click listener for the "Add to My Recipes" button
        addToMyRecipesButton.setOnClickListener(v -> {
            assert recipe != null;
            addToMyRecipes(recipe);
        });
    }

    // Method to update button text
    private void updatePublishButton(boolean isPublished) {
        if (isPublished) {
            publishButton.setText(R.string.unpublish_recipe_button);
        } else {
            publishButton.setText(R.string.publish_recipe_button);
        }
    }

    // Method to publish a recipe
    private void publishRecipe(Recipe recipe) {
        recipe.setPublished(true);

        // Update the user's private recipe reference to set `published` to true
        DatabaseReference userRecipeRef = FirebaseDatabase.getInstance()
                .getReference("recipes")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
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
                        .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
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

    private void addToMyRecipes(Recipe recipe) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

        DatabaseReference userRecipeRef = FirebaseDatabase.getInstance()
                .getReference("recipes")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()))
                .child(recipe.getId());

        // Check if the recipe already exists
        userRecipeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Recipe already exists in the user's list
                    Toast.makeText(RecipeDetailActivity.this, "Recipe already exists in My Recipes", Toast.LENGTH_SHORT).show();
                } else {
                    // Create a new recipe object
                    Recipe newRecipe = new Recipe(
                            recipeRef.push().getKey(), // Generate a new ID
                            recipe.getName(),
                            recipe.getIngredients(),
                            recipe.getInstructions(),
                            recipe.getCategory(),
                            false // Set isPublished to false
                    );

                    recipeRef.child(userId).child(newRecipe.getId()).setValue(newRecipe).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RecipeDetailActivity.this, "Added to My Recipes", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RecipeDetailActivity.this, "Failed to add to My Recipes", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors here
                Toast.makeText(RecipeDetailActivity.this, "Error accessing database: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}