package com.example.recipeorganizer;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
// Add recipe page
public class RecipeActivity extends AppCompatActivity {

    private EditText recipeNameEditText, categoryEditText;
    private LinearLayout ingredientsLayout, instructionsLayout;
    private Button addIngredientButton, addInstructionButton, addRecipeButton, cancelButton;

    private DatabaseReference recipeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        recipeNameEditText = findViewById(R.id.recipeNameEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        ingredientsLayout = findViewById(R.id.ingredientsLayout);
        instructionsLayout = findViewById(R.id.instructionsLayout);
        addIngredientButton = findViewById(R.id.addIngredientButton);
        addInstructionButton = findViewById(R.id.addInstructionButton);
        addRecipeButton = findViewById(R.id.addRecipeButton);
        cancelButton = findViewById(R.id.cancelButton);

        recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

        addIngredientButton.setOnClickListener(v -> addIngredientField());
        addInstructionButton.setOnClickListener(v -> addInstructionField());
        addRecipeButton.setOnClickListener(v -> addRecipe());
        cancelButton.setOnClickListener(v -> finish()); // Close the activity on cancel
    }

    private void addIngredientField() {
        LinearLayout ingredientLayout = new LinearLayout(this);
        ingredientLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText ingredientEditText = new EditText(this);
        ingredientEditText.setHint("Ingredient");
        ingredientEditText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        ingredientEditText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button deleteButton = new Button(this);
        deleteButton.setText("-");
        deleteButton.setOnClickListener(v -> ingredientsLayout.removeView(ingredientLayout));

        ingredientLayout.addView(ingredientEditText);
        ingredientLayout.addView(deleteButton);
        ingredientsLayout.addView(ingredientLayout);
    }

    private void addInstructionField() {
        LinearLayout instructionLayout = new LinearLayout(this);
        instructionLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText instructionEditText = new EditText(this);
        instructionEditText.setHint("Instruction");
        instructionEditText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        instructionEditText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button deleteButton = new Button(this);
        deleteButton.setText("-");
        deleteButton.setOnClickListener(v -> instructionsLayout.removeView(instructionLayout));

        instructionLayout.addView(instructionEditText);
        instructionLayout.addView(deleteButton);
        instructionsLayout.addView(instructionLayout);
    }



    private void addRecipe() {
        String name = recipeNameEditText.getText().toString();
        String category = categoryEditText.getText().toString();
        List<String> ingredients = new ArrayList<>();
        List<String> instructions = new ArrayList<>();


        // Collect ingredients
        for (int i = 0; i < ingredientsLayout.getChildCount(); i++) {
            View view = ingredientsLayout.getChildAt(i);

            // Check if the child is a LinearLayout that contains the EditText and Button
            if (view instanceof LinearLayout) {
                LinearLayout ingredientLayout = (LinearLayout) view;

                // Loop through each child of ingredientLayout to find the EditText
                for (int j = 0; j < ingredientLayout.getChildCount(); j++) {
                    View innerView = ingredientLayout.getChildAt(j);

                    if (innerView instanceof EditText) {
                        String ingredient = ((EditText) innerView).getText().toString();
                        if (!ingredient.isEmpty()) {
                            ingredients.add(ingredient);
                        }
                        break; // Exit inner loop once we find the EditText
                    }
                }
            }
        }


        // Collect instructions
        for (int i = 0; i < instructionsLayout.getChildCount(); i++) {
            View view = instructionsLayout.getChildAt(i);

            // Check if the child is a LinearLayout that contains the EditText and Button
            if (view instanceof LinearLayout) {
                LinearLayout instructionLayout = (LinearLayout) view;

                // Loop through each child of instructionLayout to find the EditText
                for (int j = 0; j < instructionLayout.getChildCount(); j++) {
                    View innerView = instructionLayout.getChildAt(j);

                    if (innerView instanceof EditText) {
                        String instruction = ((EditText) innerView).getText().toString();
                        if (!instruction.isEmpty()) {
                            instructions.add(instruction);
                        }
                        break; // Exit inner loop once we find the EditText
                    }
                }
            }
        }




        if (name.isEmpty() || category.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(RecipeActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Recipe recipe = new Recipe(
                recipeRef.push().getKey(),
                name,
                String.join(", ", ingredients), // Store ingredients as comma-separated
                String.join(", ", instructions), // Store instructions as comma-separated
                category
        );

        // Store the recipe under the user's UID
        recipeRef.child(userId).child(recipe.getId()).setValue(recipe)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(RecipeActivity.this, "Recipe Added", Toast.LENGTH_SHORT).show();
                        finish(); // This will finish the current activity and return to the previous activity
                    } else {
                        Toast.makeText(RecipeActivity.this, "Failed to Add Recipe", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}