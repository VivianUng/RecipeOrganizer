package com.example.recipeorganizer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RecipeListActivity extends AppCompatActivity {

    private RecyclerView recipeRecyclerView;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipeList;
    private DatabaseReference recipeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);

        recipeRecyclerView = findViewById(R.id.recipeRecyclerView);
        SearchView searchView = findViewById(R.id.searchView);
        Button profileButton = findViewById(R.id.profileButton);
        Button addRecipeButton = findViewById(R.id.addRecipeButton);
        Button viewPublishedRecipesButton = findViewById(R.id.viewPublishedRecipesButton);

        // Initialize Firebase Database reference
        recipeRef = FirebaseDatabase.getInstance().getReference("recipes");

        // Initialize the recipe list and adapter
        recipeList = new ArrayList<>();
        recipeAdapter = new RecipeAdapter(this, recipeList);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recipeRecyclerView.setAdapter(recipeAdapter);

        if (savedInstanceState != null) {
            // Restore the recipe list from savedInstanceState
            recipeList = (List<Recipe>) savedInstanceState.getSerializable("recipeList");
            recipeAdapter.updateList(recipeList); // Update the adapter with the restored list
        } else {
            // Fetch the recipes from the database if there's no saved state
            fetchRecipes();
        }

        // Set up search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterRecipes(newText);
                return true;
            }
        });

        // Set up profile button click listener
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecipeListActivity.this, ProfileActivity.class);
            startActivity(intent); // Navigates to the Profile screen
        });

        // Set up add recipe button click listener
        addRecipeButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecipeListActivity.this, RecipeActivity.class);
            startActivity(intent); // Navigates to the Add Recipe screen
        });

        // Set up view published recipes click listener
        viewPublishedRecipesButton.setOnClickListener(v -> {
                    Intent intent = new Intent(RecipeListActivity.this,
                            PublishedRecipesActivity.class);
                    startActivity(intent); // Navigates to the Published Recipes screen
        });
    }

    // Save the state of the recipe list when the activity is destroyed
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("recipeList", (Serializable) recipeList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recipeList = (List<Recipe>) savedInstanceState.getSerializable("recipeList");
        recipeAdapter.updateList(recipeList); // Update the adapter with the restored list
    }


    private void fetchRecipes() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        recipeRef.child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                recipeList.clear();
                HashMap<String, List<Recipe>> categorizedRecipes = new HashMap<>();

                // Fetch recipes and categorize them
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Recipe recipe = snapshot.getValue(Recipe.class);
                    if (recipe != null) {
                        String category = recipe.getCategory();
                        if (!categorizedRecipes.containsKey(category)) {
                            categorizedRecipes.put(category, new ArrayList<>());
                        }
                        categorizedRecipes.get(category).add(recipe);
                    }
                }

                // Sort categories and recipes
                List<String> sortedCategories = new ArrayList<>(categorizedRecipes.keySet());
                Collections.sort(sortedCategories); // Sort categories A-Z

                for (String category : sortedCategories) {
                    // Create a header for the category
                    Recipe header = new Recipe();
                    header.setCategory(category); // Set the category name
                    header.setName(null); // Set name to null to indicate it's a header
                    recipeList.add(header); // Add the header to the list

                    // Sort recipes in this category A-Z
                    List<Recipe> recipesInCategory = categorizedRecipes.get(category);
                    Collections.sort(recipesInCategory, (r1, r2) -> r1.getName()
                            .compareToIgnoreCase(r2.getName())); // Sort recipes A-Z

                    // Add recipes under this category
                    recipeList.addAll(recipesInCategory);

                    // Add a spacer after the last recipe of this category
                    recipeList.add(new Recipe()); // Add a spacer (null name)
                }

                // Check if the recipe list is empty
                if (recipeList.isEmpty()) {
                    findViewById(R.id.noRecipesTextView).setVisibility(View.VISIBLE); // Show message
                    recipeRecyclerView.setVisibility(View.GONE); // Hide the RecyclerView
                } else {
                    findViewById(R.id.noRecipesTextView).setVisibility(View.GONE); // Hide message
                    recipeRecyclerView.setVisibility(View.VISIBLE); // Show the RecyclerView
                }

                recipeAdapter.notifyDataSetChanged();
            }



            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle possible errors.
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchRecipes(); // Refresh the recipe list when returning to this activity
    }

    private void filterRecipes(String query) {
        List<Recipe> filteredList = new ArrayList<>();

        // Temporary map to categorize recipes
        HashMap<String, List<Recipe>> categorizedRecipes = new HashMap<>();

        // Group recipes by category
        for (Recipe recipe : recipeList) {
            String category = recipe.getCategory() != null ? recipe.getCategory() : "Uncategorized";
            if (!categorizedRecipes.containsKey(category)) {
                categorizedRecipes.put(category, new ArrayList<>());
            }
            categorizedRecipes.get(category).add(recipe);
        }

        // Process each category to see if it contains any matching recipes
        for (String category : categorizedRecipes.keySet()) {
            List<Recipe> categoryRecipes = categorizedRecipes.get(category);
            List<Recipe> matchingRecipes = new ArrayList<>();

            // Check for matches within this category
            for (Recipe recipe : categoryRecipes) {
                if (recipe.getName() == null) {
                    // If it's a header or spacer, ignore for matching purposes
                    continue;
                }

                boolean nameMatches = recipe.getName().toLowerCase().contains(query.toLowerCase());
                boolean categoryMatches = recipe.getCategory().toLowerCase().contains(query.toLowerCase());

                if (nameMatches || categoryMatches) {
                    matchingRecipes.add(recipe);
                }
            }

            // Only add category header and matching recipes if there's at least one match
            if (!matchingRecipes.isEmpty()) {
                // Add category header
                Recipe header = new Recipe();
                header.setCategory(category);  // Set category name
                header.setName(null);  // Set name as null to indicate it's a header
                filteredList.add(header);

                // Add all matching recipes under this header
                filteredList.addAll(matchingRecipes);

                // Add a spacer after each category
                Recipe spacer = new Recipe();
                spacer.setName(null);
                spacer.setCategory(null);
                filteredList.add(spacer);
            }
        }

        // Refresh the adapter with the new filtered list
        recipeAdapter.filterList(filteredList);
    }


}