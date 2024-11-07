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

public class PublishedRecipesActivity extends AppCompatActivity {

    private RecyclerView publishedRecipesRecyclerView;
    private PublishedRecipeAdapter publishedRecipeAdapter;
    private List<Recipe> publishedRecipeList;
    private DatabaseReference publishedRecipeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_published_recipes);

        publishedRecipesRecyclerView = findViewById(R.id.publishedRecipesRecyclerView);
        SearchView searchView = findViewById(R.id.searchView);
        Button profileButton = findViewById(R.id.profileButton);

        // Initialize Firebase Database reference for published recipes
        publishedRecipeRef = FirebaseDatabase.getInstance().getReference("public_recipes");

        // Initialize the published recipe list and adapter
        publishedRecipeList = new ArrayList<>();
        publishedRecipeAdapter = new PublishedRecipeAdapter(this, publishedRecipeList);
        publishedRecipesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        publishedRecipesRecyclerView.setAdapter(publishedRecipeAdapter);

        if (savedInstanceState != null) {
            // Restore the published recipe list from savedInstanceState
            publishedRecipeList = (List<Recipe>) savedInstanceState.getSerializable("publishedRecipeList");
            publishedRecipeAdapter.updateList(publishedRecipeList);
        } else {
            // Fetch published recipes from the database if there's no saved state
            fetchPublishedRecipes();
        }

        // Set up search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterPublishedRecipes(newText);
                return true;
            }
        });

        // Set up profile button click listener
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(PublishedRecipesActivity.this, ProfileActivity.class);
            startActivity(intent); // Navigates to the Profile screen
        });


        // Set up view published recipes click listener
        Button backToMyRecipesButton = findViewById(R.id.backToMyRecipesButton);
        backToMyRecipesButton.setOnClickListener(v -> {
            Intent intent = new Intent(PublishedRecipesActivity.this, RecipeListActivity.class);
            startActivity(intent); // Navigates to the My Recipes screen
        });

    }

    // Save the state of the published recipe list when the activity is destroyed
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("publishedRecipeList", (Serializable) publishedRecipeList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        publishedRecipeList = (List<Recipe>) savedInstanceState.getSerializable("publishedRecipeList");
        publishedRecipeAdapter.updateList(publishedRecipeList);
    }

    private void fetchPublishedRecipes() {
        publishedRecipeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                publishedRecipeList.clear();
                HashMap<String, List<Recipe>> categorizedPublishedRecipes = new HashMap<>();

                // Fetch published recipes directly from recipe_id nodes
                for (DataSnapshot recipeSnapshot : dataSnapshot.getChildren()) {
                    Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                    if (recipe != null) {
                        String category = recipe.getCategory();

                        // Ensure the category key exists in the HashMap
                        if (!categorizedPublishedRecipes.containsKey(category)) {
                            categorizedPublishedRecipes.put(category, new ArrayList<>());
                        }

                        // Add recipe to its category list
                        categorizedPublishedRecipes.get(category).add(recipe);
                    }
                }

                // Sort categories and recipes
                List<String> sortedCategories = new ArrayList<>(categorizedPublishedRecipes.keySet());
                Collections.sort(sortedCategories); // Sort categories alphabetically

                for (String category : sortedCategories) {
                    // Create a header for each category
                    Recipe header = new Recipe();
                    header.setCategory(category);
                    header.setName(null); // Set name to null to indicate it's a header
                    publishedRecipeList.add(header);

                    // Sort recipes within this category alphabetically by name
                    List<Recipe> recipesInCategory = categorizedPublishedRecipes.get(category);
                    Collections.sort(recipesInCategory, (r1, r2) -> r1.getName().compareToIgnoreCase(r2.getName()));

                    // Add sorted recipes under the category header
                    publishedRecipeList.addAll(recipesInCategory);

                    // Add a spacer after the last recipe of each category
                    publishedRecipeList.add(new Recipe()); // Spacer item
                }

                // Display a message if the list is empty
                if (publishedRecipeList.isEmpty()) {
                    findViewById(R.id.noRecipesTextView).setVisibility(View.VISIBLE);
                    publishedRecipesRecyclerView.setVisibility(View.GONE);
                } else {
                    findViewById(R.id.noRecipesTextView).setVisibility(View.GONE);
                    publishedRecipesRecyclerView.setVisibility(View.VISIBLE);
                }

                publishedRecipeAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle potential errors
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        fetchPublishedRecipes(); // Refresh the published recipes list when returning to this activity
    }

    private void filterPublishedRecipes(String query) {
        List<Recipe> filteredList = new ArrayList<>();

        // Temporary map to categorize recipes
        HashMap<String, List<Recipe>> categorizedRecipes = new HashMap<>();

        // Group recipes by category
        for (Recipe recipe : publishedRecipeList) {
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
        publishedRecipeAdapter.filterList(filteredList);
    }


}
