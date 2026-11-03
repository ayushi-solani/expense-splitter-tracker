package com.ayushi.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateGroupRequest {
    @NotBlank
    private String name;

    private String defaultCurrency = "INR";

    public String getName(){ return name;}

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }
}
