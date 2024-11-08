package com.example.recipeorganizer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Recipe> recipeList;
    private final Context context;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_SPACER = 2;

    public RecipeAdapter(Context context, List<Recipe> recipeList) {
        this.context = context;
        this.recipeList = recipeList;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == TYPE_SPACER) {
            View view = new View(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 24)); // Set height for spacing
            return new SpacerViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_list_item, parent, false); // Updated this line
            return new RecipeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            String category = recipeList.get(position).getCategory();
            headerHolder.categoryTextView.setText(category);
            headerHolder.categoryTextView.setTextSize(20); // Larger font size for headers
            headerHolder.categoryTextView.setTypeface(null, android.graphics.Typeface.BOLD); // Bold text

        }
        else if (holder instanceof RecipeViewHolder) {
            RecipeViewHolder recipeHolder = (RecipeViewHolder) holder;
            Recipe recipe = recipeList.get(position);
            recipeHolder.nameTextView.setText(recipe.getName());

            // Set the published status text
            String publishedStatus = recipe.isPublished() ? "Published" : "Not Published";
            recipeHolder.publishedStatusTextView.setText(publishedStatus);

            recipeHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, RecipeDetailActivity.class);
                intent.putExtra("recipe", recipe);
                intent.putExtra("isPublishedView", false);
                intent.putExtra("fromMyRecipes", true);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return recipeList.get(position).isHeader() ? TYPE_HEADER :
                recipeList.get(position).isSpacer() ? TYPE_SPACER : TYPE_ITEM;
    }

    public void filterList(List<Recipe> filteredList) {
        recipeList = filteredList;
        notifyDataSetChanged();
    }

    public void updateList(List<Recipe> newList) {
        this.recipeList = newList;
        notifyDataSetChanged();
    }


    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView publishedStatusTextView;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            publishedStatusTextView = itemView.findViewById(R.id.publishedStatusTextView);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(android.R.id.text1);
        }
    }

    static class SpacerViewHolder extends RecyclerView.ViewHolder {
        public SpacerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}