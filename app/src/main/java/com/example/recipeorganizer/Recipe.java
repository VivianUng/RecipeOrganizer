package com.example.recipeorganizer;

import java.io.Serializable;
import java.util.List;

public class Recipe implements Serializable {
    private String id;
    private String name;
    private List<String> ingredients;
    private List<String> instructions;
    private String category;
    private boolean isPublished;

    public Recipe() {
    }

    public Recipe(String id, String name, List<String> ingredients, List<String> instructions, String category, boolean isPublished) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.category = category;
        this.isPublished = isPublished;
    }

    // Getters and Setters
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

    public List<String> getIngredients() { // Updated
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {this.ingredients = ingredients;}

    public List<String> getInstructions() { // Updated
        return instructions;
    }

    public void setInstructions(List<String> instructions) { // Updated
        this.instructions = instructions;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPublished() {
        return isPublished;
    }

    public void setPublished(boolean published) {
        isPublished = published;
    }

    public boolean isHeader() {
        return name == null || name.isEmpty();
    }

    public boolean isSpacer() {
        return name == null;
    }
}