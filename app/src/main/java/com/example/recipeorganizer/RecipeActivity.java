package com.example.recipeorganizer;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

// Add recipe page
public class RecipeActivity extends AppCompatActivity {

    private EditText recipeNameEditText, customCategoryEditText, recipeLinkEditText;
    private Spinner categorySpinner;
    private LinearLayout ingredientsLayout, instructionsLayout;

    private DatabaseReference recipeRef;

    // Member variable to hold user categories
    private List<String> userCategoriesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        recipeNameEditText = findViewById(R.id.recipeNameEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        customCategoryEditText = findViewById(R.id.customCategoryEditText);
        ingredientsLayout = findViewById(R.id.ingredientsLayout);
        instructionsLayout = findViewById(R.id.instructionsLayout);
        Button addIngredientButton = findViewById(R.id.addIngredientButton);
        Button addInstructionButton = findViewById(R.id.addInstructionButton);
        Button addRecipeButton = findViewById(R.id.addRecipeButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        recipeLinkEditText = findViewById(R.id.recipeLinkEditText);
        Button generateRecipeButton = findViewById(R.id.generateRecipeButton);

        recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

        // Fetch user categories to populate the spinner
        fetchUserCategories();

        // Set listener to show or hide custom category field based on selected item
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categorySpinner.getSelectedItem().toString();
                if ("Others".equals(selectedCategory)) {
                    customCategoryEditText.setVisibility(View.VISIBLE);
                } else {
                    customCategoryEditText.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        addIngredientButton.setOnClickListener(v -> addIngredientField(""));
        addInstructionButton.setOnClickListener(v -> addInstructionField(""));
        addRecipeButton.setOnClickListener(v -> addRecipe());
        cancelButton.setOnClickListener(v -> finish()); // Close the activity on cancel
        generateRecipeButton.setOnClickListener(v -> generateRecipeFromLink());
    }


    private void addRecipe() {
        String name = recipeNameEditText.getText().toString();
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        String category = "Others".equals(selectedCategory) ? customCategoryEditText.getText().toString() : selectedCategory;

        List<String> ingredients = new ArrayList<>();
        List<String> instructions = new ArrayList<>();
        boolean isPublished = false;

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
            Toast.makeText(RecipeActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        Recipe recipe = new Recipe(
                recipeRef.push().getKey(),
                name,
                ingredients, // Pass list directly
                instructions, // Pass list directly
                category,
                isPublished
        );

        recipeRef.child(userId).child(recipe.getId()).setValue(recipe)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Save custom category only if it's new
                        if ("Others".equals(selectedCategory)) {
                            saveCustomCategory(category);
                        }
                        Toast.makeText(RecipeActivity.this, "Recipe Added", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RecipeActivity.this, "Failed to Add Recipe", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static boolean isValidURL(String url) {
        String regex = "^(https?://)?(www\\.)?([\\w-]+)\\.+[\\w]{2,}(/\\S*)?$";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    private void generateRecipeFromLink() {
        String recipeLink = recipeLinkEditText.getText().toString();
        if (recipeLink.isEmpty()) {
            Toast.makeText(this, "Please paste a recipe link", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!isValidURL(recipeLink)){
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(recipeLink).get();
                String recipeName = doc.title();

                // Target <ul> or <ol> that contains <li> elements with attributes containing
                // "ingredient" in any of the following: id, name, aria-label
                Element ingredientsElement = doc.select("ul:has(li[id*='ingredient']," +
                        "li[class*='ingredient'], li[name*='ingredient'], " +
                        "li[aria-label*='ingredient']), ol:has(li[id*='ingredient']," +
                        "li[class*='ingredient'], li[name*='ingredient'], " +
                        "li[aria-label*='ingredient'])").first();

                // Target <ul> or <ol> that contains <li> elements with attributes containing
                // "instruction" in any of the following: id, name, aria-label
                Element instructionsElement = doc.select("ul:has(li[id*='instruction']," +
                        "li[class*='instruction'], li[name*='instruction'], " +
                        "li[aria-label*='instruction']), ol:has(li[id*='instruction']," +
                        "li[class*='instruction'], li[name*='instruction'], " +
                        "li[aria-label*='instruction'])").first();

                runOnUiThread(() -> {
                    // Set recipe name to the UI
                    recipeNameEditText.setText(recipeName);

                    if (ingredientsElement != null) {
                        List<String> ingredients = new ArrayList<>();
                        // Extracting ingredient names from <li> elements
                        for (Element ingredient : ingredientsElement.select("li")) {
                            StringBuilder ingredientText = new StringBuilder();
                            // Concatenate all text within the <li>
                            ingredientText.append(ingredient.text().trim());

                            if (!ingredientText.toString().isEmpty()) {
                                ingredients.add(ingredientText.toString());
                            }
                        }

                        for (String ingredient : ingredients) {
                            addIngredientField(ingredient);
                        }
                    } else {
                        Toast.makeText(this, "Unable to collect ingredients list", Toast.LENGTH_SHORT).show();
                    }

                    // Extract instructions
                    if (instructionsElement != null) {
                        List<String> instructions = new ArrayList<>();
                        for (Element instruction : instructionsElement.select("li")) {

                            StringBuilder instructionText = new StringBuilder();
                            // Concatenate all text within the <li>
                            instructionText.append(instruction.text().trim());

                            if (!instructionText.toString().isEmpty()) {
                                instructions.add(instructionText.toString());
                            }
                        }
                        for (String instruction : instructions) {
                            addInstructionField(instruction);
                        }
                    } else {
                        Toast.makeText(this, "Unable to collect instruction list", Toast.LENGTH_SHORT).show();
                    }

                    Toast.makeText(this, "Recipe generated!", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to fetch recipe", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // Method to fetch user categories from the database
    private void fetchUserCategories() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
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
                ArrayAdapter<String> adapter = new ArrayAdapter<>(RecipeActivity.this, R.layout.spinner_item, userCategoriesList);
                categorySpinner.setAdapter(adapter);

                // Now we can safely set the category selection
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RecipeActivity.this, "Failed to fetch categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to save a new custom category
    private void saveCustomCategory(String customCategory) {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference categoryRef = FirebaseDatabase.getInstance().getReference("category_preference").child(userId);

        categoryRef.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> currentCategories = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    currentCategories = (List<String>) dataSnapshot.getValue();
                }

                // Add the new custom category if it's not already in the list
                assert currentCategories != null;
                if (!currentCategories.contains(customCategory) && !customCategory.isEmpty()) {
                    currentCategories.add(customCategory);
                    categoryRef.child("categories").setValue(currentCategories).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RecipeActivity.this, "Category saved!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RecipeActivity.this, "Failed to save category", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(RecipeActivity.this, "Failed to fetch current categories", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void addIngredientField(String ingredient) {
        // Method to create and add an ingredient field with pre-filled text
        LinearLayout ingredientLayout = new LinearLayout(this);
        ingredientLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText ingredientEditText = new EditText(this);
        ingredientEditText.setText(ingredient);
        ingredientEditText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        ingredientEditText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button deleteButton = new Button(this);
        deleteButton.setText("-");
        deleteButton.setOnClickListener(v -> ingredientsLayout.removeView(ingredientLayout));

        ingredientLayout.addView(ingredientEditText);
        ingredientLayout.addView(deleteButton);
        ingredientsLayout.addView(ingredientLayout);
    }

    private void addInstructionField(String instruction) {
        // Method to create and add an instruction field with pre-filled text
        LinearLayout instructionLayout = new LinearLayout(this);
        instructionLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText instructionEditText = new EditText(this);
        instructionEditText.setText(instruction);
        instructionEditText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        instructionEditText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button deleteButton = new Button(this);
        deleteButton.setText("-");
        deleteButton.setOnClickListener(v -> instructionsLayout.removeView(instructionLayout));

        instructionLayout.addView(instructionEditText);
        instructionLayout.addView(deleteButton);
        instructionsLayout.addView(instructionLayout);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("recipeName", recipeNameEditText.getText().toString());
        outState.putString("customCategory", customCategoryEditText.getText().toString());
        outState.putString("recipeLink", recipeLinkEditText.getText().toString());

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
        recipeLinkEditText.setText(savedInstanceState.getString("recipeLink"));

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