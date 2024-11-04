package com.example.recipeorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class RecipeDetailActivity extends AppCompatActivity {

    private TextView nameTextView, ingredientsTextView, instructionsTextView, categoryTextView;
    private Button backButton, editButton;

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
            ingredientsTextView.setText(formatList(recipe.getIngredients()));
            instructionsTextView.setText(formatList(recipe.getInstructions()));

        }

        // Set up the back button click listener
        backButton.setOnClickListener(v -> finish());

        // Set up the edit button click listener
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecipeDetailActivity.this, EditRecipeActivity.class);
            intent.putExtra("recipe", recipe);
            editRecipeLauncher.launch(intent); // Launch the edit activity
        });
    }

    private String formatList(String input) {
        String[] items = input.split(", ");
        StringBuilder formattedList = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            formattedList.append(i + 1).append(". ").append(items[i]).append("\n");
        }
        return formattedList.toString().trim();
    }
}