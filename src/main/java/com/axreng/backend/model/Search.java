package com.axreng.backend.model;

import com.axreng.backend.enums.Status;

import java.util.LinkedList;

public class Search {

    private String id;
    private String status;
    private LinkedList<String> urls;

    public Search(String id) {
        this.id = id;
        this.status = Status.active.toString();
        this.urls = new LinkedList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LinkedList<String> getUrls() {
        return urls;
    }

    public void setUrls(LinkedList<String> urls) {
        this.urls = urls;
    }
}