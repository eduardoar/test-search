package com.axreng.backend.controller;

import com.axreng.backend.model.response.ErrorResponse;
import com.axreng.backend.model.response.GetSearchResponse;
import com.axreng.backend.model.request.SearchRequest;
import com.axreng.backend.model.response.SearchResponse;
import com.axreng.backend.service.SearchService;
import com.google.gson.Gson;
import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

public class SearchController {

    private final SearchService searchService;
    private static final int MIN_KEYWORD_LENGTH = 4;
    private static final int MAX_KEYWORD_LENGTH = 32;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    public Object search(Request req, Response res) {
        String requestBody = req.body();
        SearchRequest searchRequest = new Gson().fromJson(requestBody, SearchRequest.class);

        if (!isValidKeyword(searchRequest.getKeyword())) {
            res.status(HttpStatus.BAD_REQUEST_400);
            return new ErrorResponse(HttpStatus.BAD_REQUEST_400,"field 'keyword' is required (from 4 up to 32 chars)");
        }

        SearchResponse searchResponse = searchService.initiateSearch(searchRequest);

        res.status(HttpStatus.OK_200);
        return searchResponse;
    }

    public Object getSearch(Request req, Response res) {
        String searchId = req.params("searchId");
        GetSearchResponse getSearchResponse = searchService.getSearchResult(searchId);

        if (getSearchResponse == null) {
            res.status(HttpStatus.NOT_FOUND_404);
            return new ErrorResponse(HttpStatus.NOT_FOUND_404,"Search not found.");
        }

        res.status(HttpStatus.OK_200);
        return getSearchResponse;
    }

    private boolean isValidKeyword(String keyword) {
        return keyword != null && keyword.length() >= MIN_KEYWORD_LENGTH && keyword.length() <= MAX_KEYWORD_LENGTH;
    }

}
