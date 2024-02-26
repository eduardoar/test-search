package com.axreng.backend.model;

import com.axreng.backend.enums.Status;

import java.util.HashSet;
import java.util.Set;

public class Search {

    private String id;
    private String status;
    private Set<String> urls;

    public Search(String id) {
        this.id = id;
        this.status = Status.active.toString();
        this.urls = new HashSet<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<String> getUrls() {
        return urls;
    }

    public void setUrls(Set<String> urls) {
        this.urls = urls;
    }
}