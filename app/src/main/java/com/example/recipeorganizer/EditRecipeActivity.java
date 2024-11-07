package com.example.recipeorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.Arrays;
import java.util.List;

public class EditRecipeActivity extends AppCompatActivity {

    private EditText recipeNameEditText, customCategoryEditText;
    private Spinner categorySpinner;
    private LinearLayout ingredientsLayout, instructionsLayout;
    private Button addIngredientButton, addInstructionButton, updateRecipeButton, deleteButton, cancelButton;

    private String recipeId;
    private DatabaseReference recipeRef;

    // Member variable to hold user categories
    private List<String> userCategoriesList;

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

        // Fetch user categories to populate the spinner
        fetchUserCategories();

        addIngredientButton.setOnClickListener(v -> addIngredientField(""));
        addInstructionButton.setOnClickListener(v -> addInstructionField(""));
        updateRecipeButton.setOnClickListener(v -> updateRecipe());
        deleteButton.setOnClickListener(v -> deleteRecipe());
        cancelButton.setOnClickListener(v -> finish());

        // Listener for category spinner selection
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position == categorySpinner.getAdapter().getCount() - 1) { // "Others" option
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

    private void fetchUserCategories() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference categoryRef = FirebaseDatabase.getInstance().getReference("category_preference").child(userId);

        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userCategoriesList = new ArrayList<>();
                // Add default categories
                userCategoriesList.addAll(Arrays.asList(getResources().getStringArray(R.array.category_array)));

                // Check if the user has custom preferences
                if (dataSnapshot.exists()) {
                    // Retrieve the categories as a List
                    List<String> userCategories = new ArrayList<>();

                    // Assuming categories are stored as a List in Firebase
                    for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                        // Get the value as a List<String>
                        List<String> categories = (List<String>) categorySnapshot.getValue();
                        if (categories != null) {
                            userCategories.addAll(categories); // Add all categories to the userCategories list
                        }
                    }

                    // Add the list of user categories to the main list
                    userCategoriesList.addAll(userCategories);
                }

                // Always add "Others" as the last option
                userCategoriesList.add("Others");

                // Set the adapter for the Spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditRecipeActivity.this, android.R.layout.simple_spinner_item, userCategoriesList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);

                // Now we can safely set the category selection
                setCategorySelection();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EditRecipeActivity.this, "Failed to fetch categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setCategorySelection() {
        // Get the recipe data from the Intent
        Recipe recipe = (Recipe) getIntent().getSerializableExtra("recipe");
        if (recipe != null) {
            recipeId = recipe.getId();
            recipeNameEditText.setText(recipe.getName());
            String category = recipe.getCategory();

            if (userCategoriesList.contains(category)) {
                int position = userCategoriesList.indexOf(category);
                categorySpinner.setSelection(position); // Select user category
                customCategoryEditText.setVisibility(View.GONE); // Hide custom category input
            } else {
                customCategoryEditText.setText(category);
                customCategoryEditText.setVisibility(View.VISIBLE); // Show custom category input
                categorySpinner.setSelection(userCategoriesList.indexOf("Others")); // Select "Others"
            }

            // Populate ingredients and instructions
            if (recipe.getIngredients() != null) {
                populateFields(recipe.getIngredients(), ingredientsLayout);
            }
            if (recipe.getInstructions() != null) {
                populateFields(recipe.getInstructions(), instructionsLayout);
            }
        }
    }

    private void populateFields(List<String> data, LinearLayout layout) {
        for (String item : data) {
            if (layout == ingredientsLayout) {
                addIngredientField(item); // Call to addIngredientField with item
            } else if (layout == instructionsLayout) {
                addInstructionField(item); // Call to addInstructionField with item
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


    // Method to save a new custom category
    private void saveCustomCategory(String customCategory) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference categoryRef = FirebaseDatabase.getInstance().getReference("category_preference").child(userId);

        categoryRef.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> currentCategories = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    currentCategories = (List<String>) dataSnapshot.getValue();
                }

                // Add the new custom category if it's not already in the list
                if (!currentCategories.contains(customCategory) && !customCategory.isEmpty()) {
                    currentCategories.add(customCategory);
                    categoryRef.child("categories").setValue(currentCategories).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(EditRecipeActivity.this, "Category saved!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EditRecipeActivity.this, "Failed to save category", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(EditRecipeActivity.this, "Failed to fetch current categories", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateRecipe() {
        String name = recipeNameEditText.getText().toString();
        String category = categorySpinner.getSelectedItem().toString();
        final String selectedCategory = category;

        if (category.equals("Others")) {
            String customCategory = customCategoryEditText.getText().toString();
            if (!customCategory.isEmpty()) {
                category = customCategory;
            } else {
                Toast.makeText(this, "Please enter a custom category", Toast.LENGTH_SHORT).show();
                return;
            }
        }

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
                    Recipe updatedRecipe = new Recipe(recipeId, name, ingredients, instructions, finalCategory, isPublished);

                    // Update the local recipe in the user's private list
                    recipeRef.child(userId).child(recipeId).setValue(updatedRecipe).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Save custom category only if it's new
                            if ("Others".equals(selectedCategory)) {
                                saveCustomCategory(finalCategory);
                            }
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("recipeName", recipeNameEditText.getText().toString());
        outState.putString("customCategory", customCategoryEditText.getText().toString());

        // Save dynamically added ingredients
        ArrayList<String> ingredients = new ArrayList<>();
        for (int i = 0; i < ingredientsLayout.getChildCount(); i++) {
            View view = ingredientsLayout.getChildAt(i);
            if (view instanceof LinearLayout) {
                EditText ingredientEditText = (EditText) ((LinearLayout) view).getChildAt(0);
                ingredients.add(ingredientEditText.getText().toString());
            }
        }
        outState.putStringArrayList("ingredients", ingredients);

        // Save dynamically added instructions
        ArrayList<String> instructions = new ArrayList<>();
        for (int i = 0; i < instructionsLayout.getChildCount(); i++) {
            View view = instructionsLayout.getChildAt(i);
            if (view instanceof LinearLayout) {
                EditText instructionEditText = (EditText) ((LinearLayout) view).getChildAt(0);
                instructions.add(instructionEditText.getText().toString());
            }
        }
        outState.putStringArrayList("instructions", instructions);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recipeNameEditText.setText(savedInstanceState.getString("recipeName"));
        customCategoryEditText.setText(savedInstanceState.getString("customCategory"));

        // Restore ingredients
        ArrayList<String> ingredients = savedInstanceState.getStringArrayList("ingredients");
        if (ingredients != null) {
            ingredientsLayout.removeAllViews();  // Clear existing views
            for (String ingredient : ingredients) {
                addIngredientField(ingredient);
            }
        }

        // Restore instructions
        ArrayList<String> instructions = savedInstanceState.getStringArrayList("instructions");
        if (instructions != null) {
            instructionsLayout.removeAllViews();  // Clear existing views
            for (String instruction : instructions) {
                addInstructionField(instruction);
            }
        }
    }
}