package com.example.recipeorganizer;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

// Add recipe page
public class RecipeActivity extends AppCompatActivity {

    private EditText recipeNameEditText, customCategoryEditText, recipeLinkEditText;
    private Spinner categorySpinner;
    private LinearLayout ingredientsLayout, instructionsLayout;
    private Button addIngredientButton, addInstructionButton, addRecipeButton, cancelButton, generateRecipeButton;

    private DatabaseReference recipeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        recipeNameEditText = findViewById(R.id.recipeNameEditText);
        categorySpinner = findViewById(R.id.categorySpinner);
        customCategoryEditText = findViewById(R.id.customCategoryEditText);
        ingredientsLayout = findViewById(R.id.ingredientsLayout);
        instructionsLayout = findViewById(R.id.instructionsLayout);
        addIngredientButton = findViewById(R.id.addIngredientButton);
        addInstructionButton = findViewById(R.id.addInstructionButton);
        addRecipeButton = findViewById(R.id.addRecipeButton);
        cancelButton = findViewById(R.id.cancelButton);

        recipeLinkEditText = findViewById(R.id.recipeLinkEditText);
        generateRecipeButton = findViewById(R.id.generateRecipeButton);

        recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

        // Set listener to show or hide custom category field based on selected item
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categorySpinner.getSelectedItem().toString();
                if ("Other".equals(selectedCategory)) {
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
        String category = "Other".equals(selectedCategory) ? customCategoryEditText.getText().toString() : selectedCategory;

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

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
                        Toast.makeText(RecipeActivity.this, "Recipe Added", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(RecipeActivity.this, "Failed to Add Recipe", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void generateRecipeFromLink() {
        String recipeLink = recipeLinkEditText.getText().toString();
        if (recipeLink.isEmpty()) {
            Toast.makeText(this, "Please paste a recipe link", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Document doc = Jsoup.connect(recipeLink).get();
                String recipeName = doc.title();

                // Target <ul> that contains <li> elements with attributes containing "ingredient" in any of the following: id, name, aria-label
                Element ingredientsElement = doc.select("ul:has(li[id*='ingredient'],li[class*='ingredient'], li[name*='ingredient'], li[aria-label*='ingredient'])").first();

                // Target <ol> that contains <li> elements with id containing "instruction"
                Element instructionsElement = doc.select("ol:has(li[id^='instruction'])").first();

                runOnUiThread(() -> {
                    if (ingredientsElement != null) {
                        List<String> ingredients = new ArrayList<>();
                        // Extracting ingredient names from <li> elements
                        for (Element ingredient : ingredientsElement.select("li")) {
                            // Extract the ingredient name from <strong> tags if they exist
                            Element ingredientName = ingredient.selectFirst("strong");
                            if (ingredientName != null) {
                                ingredients.add(ingredientName.text().trim());
                            }
                        }
                        // Set recipe name and add ingredients to the UI
                        recipeNameEditText.setText(recipeName);
                        for (String ingredient : ingredients) {
                            addIngredientField(ingredient);
                        }
                    } else {
                        Toast.makeText(this, "Unable to collect ingredients list", Toast.LENGTH_SHORT).show();
                    }

                    // Extract instructions
                    if (instructionsElement != null) {
                        List<String> instructions = new ArrayList<>();
                        for (Element instruction : instructionsElement.select("li[id^='instruction']")) {
                            // Use .ownText() to get only the main instruction text
                            String instructionText = instruction.ownText();
                            if (!instructionText.isEmpty()) {
                                instructions.add(instructionText);
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


}