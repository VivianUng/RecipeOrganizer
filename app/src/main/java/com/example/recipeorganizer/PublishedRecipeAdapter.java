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

public class PublishedRecipeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Recipe> publishedRecipeList;
    private Context context;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_SPACER = 2;

    public PublishedRecipeAdapter(Context context, List<Recipe> publishedRecipeList) {
        this.context = context;
        this.publishedRecipeList = publishedRecipeList;
    }

    @NonNull
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
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new PublishedRecipeViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            String category = publishedRecipeList.get(position).getCategory();
            headerHolder.categoryTextView.setText(category);
            headerHolder.categoryTextView.setTextSize(20); // Larger font size for headers
            headerHolder.categoryTextView.setTypeface(null, android.graphics.Typeface.BOLD); // Bold text
        } else if (holder instanceof PublishedRecipeViewHolder) {
            PublishedRecipeViewHolder recipeHolder = (PublishedRecipeViewHolder) holder;
            Recipe recipe = publishedRecipeList.get(position);
            recipeHolder.nameTextView.setText(recipe.getName());

            recipeHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, RecipeDetailActivity.class);
                intent.putExtra("recipe", recipe);
                intent.putExtra("isPublishedView", true); // Pass the flag
                context.startActivity(intent);
            });

        }
    }

    @Override
    public int getItemCount() {
        return publishedRecipeList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return publishedRecipeList.get(position).isHeader() ? TYPE_HEADER :
                publishedRecipeList.get(position).isSpacer() ? TYPE_SPACER : TYPE_ITEM;
    }

    public void filterList(List<Recipe> filteredList) {
        publishedRecipeList = filteredList;
        notifyDataSetChanged();
    }

    public void updateList(List<Recipe> newList) {
        this.publishedRecipeList = newList;
        notifyDataSetChanged();
    }

    static class PublishedRecipeViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;

        public PublishedRecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(android.R.id.text1);
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
