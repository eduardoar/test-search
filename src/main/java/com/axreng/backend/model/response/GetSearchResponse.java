package com.axreng.backend.model.response;

import java.util.LinkedList;

public class GetSearchResponse {

    private String id;
    private String status;
    private LinkedList<String> urls;

    public GetSearchResponse(String id) {
        this.id = id;
    }

    public GetSearchResponse(String id, String status, LinkedList<String> urls) {
        this.id = id;
        this.status = status;
        this.urls = urls;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public LinkedList<String> getUrls() {
        return urls;
    }
}
