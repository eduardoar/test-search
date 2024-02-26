package com.axreng.backend.model.response;

import java.util.Set;

public class GetSearchResponse {

    private String id;
    private String status;
    private Set<String> urls;

    public GetSearchResponse(String id) {
        this.id = id;
    }

    public GetSearchResponse(String id, String status, Set<String> urls) {
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

    public Set<String> getUrls() {
        return urls;
    }
}
