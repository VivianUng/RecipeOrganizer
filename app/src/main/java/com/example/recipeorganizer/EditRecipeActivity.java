package com.example.recipeorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EditRecipeActivity extends AppCompatActivity {

    private EditText recipeNameEditText, customCategoryEditText;
    private Spinner categorySpinner;
    private LinearLayout ingredientsLayout, instructionsLayout;
    private Button addIngredientButton, addInstructionButton, updateRecipeButton, deleteButton, cancelButton;

    private String recipeId;
    private DatabaseReference recipeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        recipeNameEditText = findViewById(R.id.recipeNameEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        customCategoryEditText = findViewById(R.id.customCategoryEditText);
        ingredientsLayout = findViewById(R.id.ingredientsLayout);
        instructionsLayout = findViewById(R.id.instructionsLayout);
        addIngredientButton = findViewById(R.id.addIngredientButton);
        addInstructionButton = findViewById(R.id.addInstructionButton);
        updateRecipeButton = findViewById(R.id.updateRecipeButton);
        deleteButton = findViewById(R.id.deleteButton);
        cancelButton = findViewById(R.id.cancelButton);

        recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

        // Get the recipe data from the Intent
        Recipe recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        if (recipe != null) {
            recipeId = recipe.getId();
            recipeNameEditText.setText(recipe.getName());
            String category = recipe.getCategory();

            // Check if the saved category is within the preset categories
            if (isCategoryPreset(category)) {
                categorySpinner.setSelection(getCategoryPosition(category));
                customCategoryEditText.setVisibility(View.GONE);
            } else {
                // If not, set it as a custom category
                customCategoryEditText.setText(category);
                customCategoryEditText.setVisibility(View.VISIBLE);
                // Set the spinner to the last position (assuming "Other" is the last item)
                categorySpinner.setSelection(categorySpinner.getAdapter().getCount() - 1);
            }

            populateFields(recipe.getIngredients(), ingredientsLayout);
            populateFields(recipe.getInstructions(), instructionsLayout);
        }

        addIngredientButton.setOnClickListener(v -> addIngredientField(""));
        addInstructionButton.setOnClickListener(v -> addInstructionField(""));
        updateRecipeButton.setOnClickListener(v -> updateRecipe());
        deleteButton.setOnClickListener(v -> deleteRecipe());
        cancelButton.setOnClickListener(v -> finish());

        // Listener for category spinner selection
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position == categorySpinner.getAdapter().getCount() - 1) { // custom option as last option
                    customCategoryEditText.setVisibility(View.VISIBLE);
                } else {
                    customCategoryEditText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });
    }

    private boolean isCategoryPreset(String category) {
        String[] categories = getResources().getStringArray(R.array.category_array);
        for (String presetCategory : categories) {
            if (presetCategory.equals(category)) {
                return true;
            }
        }
        return false;
    }

    private int getCategoryPosition(String category) {
        String[] categories = getResources().getStringArray(R.array.category_array);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                return i;
            }
        }
        return 0; // Default to first category if not found
    }

    private void populateFields(String data, LinearLayout layout) {
        String[] items = data.split(", ");
        for (String item : items) {
            if (layout == ingredientsLayout) {
                addIngredientField(item);
            } else if (layout == instructionsLayout) {
                addInstructionField(item);
            }
        }
    }

    private void addIngredientField(String text) {
        LinearLayout ingredientLayout = new LinearLayout(this);
        ingredientLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText ingredientEditText = new EditText(this);
        ingredientEditText.setHint("Ingredient");
        ingredientEditText.setText(text);
        ingredientEditText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        ingredientEditText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button deleteButton = new Button(this);
        deleteButton.setText("-");
        deleteButton.setOnClickListener(v -> ingredientsLayout.removeView(ingredientLayout));

        ingredientLayout.addView(ingredientEditText);
        ingredientLayout.addView(deleteButton);
        ingredientsLayout.addView(ingredientLayout);
    }

    private void addInstructionField(String text) {
        LinearLayout instructionLayout = new LinearLayout(this);
        instructionLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText instructionEditText = new EditText(this);
        instructionEditText.setHint("Instruction");
        instructionEditText.setText(text);
        instructionEditText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        instructionEditText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button deleteButton = new Button(this);
        deleteButton.setText("-");
        deleteButton.setOnClickListener(v -> instructionsLayout.removeView(instructionLayout));

        instructionLayout.addView(instructionEditText);
        instructionLayout.addView(deleteButton);
        instructionsLayout.addView(instructionLayout);
    }

    private void updateRecipe() {
        String name = recipeNameEditText.getText().toString();
        String category = categorySpinner.getSelectedItem().toString();

        // Check for custom category input if "Other" is selected
        if (category.equals("Other")) {
            String customCategory = customCategoryEditText.getText().toString();
            if (!customCategory.isEmpty()) {
                category = customCategory;
            } else {
                Toast.makeText(this, "Please enter a custom category", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Create a new variable for the category to use in the inner class
        final String finalCategory = category;

        List<String> ingredients = new ArrayList<>();
        List<String> instructions = new ArrayList<>();

        // Collect ingredients
        for (int i = 0; i < ingredientsLayout.getChildCount(); i++) {
            View view = ingredientsLayout.getChildAt(i);
            if (view instanceof LinearLayout) {
                LinearLayout ingredientLayout = (LinearLayout) view;
                for (int j = 0; j < ingredientLayout.getChildCount(); j++) {
                    View innerView = ingredientLayout.getChildAt(j);
                    if (innerView instanceof EditText) {
                        String ingredient = ((EditText) innerView).getText().toString();
                        if (!ingredient.isEmpty()) {
                            ingredients.add(ingredient);
                        }
                        break;
                    }
                }
            }
        }

        // Collect instructions
        for (int i = 0; i < instructionsLayout.getChildCount(); i++) {
            View view = instructionsLayout.getChildAt(i);
            if (view instanceof LinearLayout) {
                LinearLayout instructionLayout = (LinearLayout) view;
                for (int j = 0; j < instructionLayout.getChildCount(); j++) {
                    View innerView = instructionLayout.getChildAt(j);
                    if (innerView instanceof EditText) {
                        String instruction = ((EditText) innerView).getText().toString();
                        if (!instruction.isEmpty()) {
                            instructions.add(instruction);
                        }
                        break;
                    }
                }
            }
        }

        if (name.isEmpty() || category.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(EditRecipeActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch the current recipe to retain the published status
        recipeRef.child(userId).child(recipeId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Recipe existingRecipe = dataSnapshot.getValue(Recipe.class);
                    boolean isPublished = existingRecipe != null && existingRecipe.isPublished();

                    // Create new Recipe object with existing published status
                    Recipe updatedRecipe = new Recipe(recipeId, name, String.join(", ", ingredients), String.join(", ", instructions), finalCategory, isPublished);

                    // Update the local recipe in the user's private list
                    recipeRef.child(userId).child(recipeId).setValue(updatedRecipe).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Update the public recipes database if the recipe is published
                            if (isPublished) {
                                DatabaseReference publicRecipeRef = FirebaseDatabase.getInstance().getReference("public_recipes");
                                publicRecipeRef.child(recipeId).setValue(updatedRecipe).addOnCompleteListener(publicTask -> {
                                    if (publicTask.isSuccessful()) {
                                        Toast.makeText(EditRecipeActivity.this, "Recipe Updated Successfully", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(EditRecipeActivity.this, "Failed to Update Public Recipe", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(EditRecipeActivity.this, "Recipe Updated Successfully", Toast.LENGTH_SHORT).show();
                            }

                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("updatedRecipe", updatedRecipe);
                            setResult(RESULT_OK, returnIntent);
                            finish();
                        } else {
                            Toast.makeText(EditRecipeActivity.this, "Failed to Update Recipe", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(EditRecipeActivity.this, "Recipe not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EditRecipeActivity.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteRecipe() {
        if (recipeId != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Remove the recipe from the user's private list
            recipeRef.child(userId).child(recipeId).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Check and remove the recipe from public recipes if it is published
                    DatabaseReference publicRecipeRef = FirebaseDatabase.getInstance().getReference("public_recipes");
                    publicRecipeRef.child(recipeId).removeValue().addOnCompleteListener(publicTask -> {
                        if (publicTask.isSuccessful()) {
                            Toast.makeText(EditRecipeActivity.this, "Recipe Deleted", Toast.LENGTH_SHORT).show();
                            // Navigate back to RecipeListActivity
                            Intent intent = new Intent(EditRecipeActivity.this, RecipeListActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent); // Start RecipeListActivity
                            finish(); // Close current activity
                        } else {
                            Toast.makeText(EditRecipeActivity.this, "Failed to Delete from Public Recipes", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(EditRecipeActivity.this, "Failed to Delete Recipe", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}