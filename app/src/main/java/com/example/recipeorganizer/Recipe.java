package com.example.recipeorganizer;

import java.io.Serializable;

public class Recipe implements Serializable {
    private String id;
    private String name;
    private String ingredients;
    private String instructions;
    private String category;

    public Recipe() {
    }

    public Recipe(String id, String name, String ingredients, String instructions, String category) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isHeader() {
        return name == null || name.isEmpty(); // Assuming that headers will have an empty or null name
    }

    public boolean isSpacer() {
        return name == null; // Treat a recipe with a null name as a spacer
    }
}