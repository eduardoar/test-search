package com.axreng.backend;

import com.axreng.backend.controller.SearchController;
import com.axreng.backend.service.SearchService;
import com.google.gson.Gson;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {

        port(4567);

        SearchController searchController = new SearchController(new SearchService());

        post("/crawl", "application/json", (req, res) -> {
            res.type("application/json");
            return new Gson().toJson(searchController.search(req, res));
        });

        get("/crawl/:searchId", "application/json", (req, res) -> {
            res.type("application/json");
            return new Gson().toJson(searchController.getSearch(req, res));
        });

    }
}
