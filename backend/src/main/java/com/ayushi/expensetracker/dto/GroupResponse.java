package com.ayushi.expensetracker.dto;

import java.util.List;

public class GroupResponse {
    private Long id;
    private String name;
    private String defaultCurrency;
    private List<String> memberEmails;

    public GroupResponse(Long id, String name, String defaultCurrency, List<String> memberEmails){
        this.id = id;
        this.name = name;
        this.defaultCurrency = defaultCurrency;
        this.memberEmails = memberEmails;
    }

    public Long getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getDefaultCurrency(){
        return defaultCurrency;
    }
    public List<String> getMemberEmails(){
        return memberEmails;
    }
}
