package com.axreng.backend.service;

import com.axreng.backend.enums.Status;
import com.axreng.backend.model.Search;
import com.axreng.backend.model.request.SearchRequest;
import com.axreng.backend.model.response.GetSearchResponse;
import com.axreng.backend.model.response.SearchResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchService {

    private final ConcurrentMap<String, Search> searches = new ConcurrentHashMap<>();
    private static final int THREAD_POOL_SIZE = 10;
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private static final String baseUrl = System.getenv("BASE_URL");

    private static final String REGEX = "<a\\s+[^>]*?href=\"([^\"]*)\"";

    public SearchResponse initiateSearch(SearchRequest searchRequest) {
        String keyword = searchRequest.getKeyword();
        String searchId = generateSearchId();
        searches.put(searchId, new Search(searchId));

        executorService.submit(() -> {
            try {
                startSearch(searchId, keyword);
            } catch (IOException e) {
                System.err.println("Error during search: " + e.getMessage());
            }
        });

        return new SearchResponse(searchId);
    }

    public GetSearchResponse getSearchResult(String searchId) {
        Search search = searches.get(searchId);
        if (search == null) {
            return null;
        }
        return new GetSearchResponse(searchId, search.getStatus(), search.getUrls());
    }

    private String generateSearchId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void startSearch(String searchId, String keyword) throws IOException {
        if (baseUrl == null || baseUrl.isEmpty()) {
            System.err.println("BASE_URL environment variable is not set.");
            return;
        }

        Set<String> foundUrls = new HashSet<>();
        Set<String> visitedUrls = new HashSet<>();
        crawlUrl(baseUrl, keyword, visitedUrls, foundUrls, searchId);

        Search search = searches.get(searchId);
        search.setStatus(Status.done.toString());
        search.setUrls(foundUrls);
        shutdown();
    }

    private void crawlUrl(String url, String keyword,Set<String> visitedUrls, Set<String> foundUrls, String searchId) throws IOException {
        if (!url.startsWith(baseUrl) || visitedUrls.contains(url)) {
            return;
        }

        visitedUrls.add(url);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            if (content.toString().toLowerCase().contains(keyword.toLowerCase())) {
                foundUrls.add(url);
                informPartialResult(searchId, foundUrls);
            }

            Pattern pattern = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content.toString());
            while (matcher.find()) {
                String link = matcher.group(1);
                if (!link.startsWith("http")) {
                    link = normalizeUrl(url, link);
                }
                crawlUrl(link, keyword, visitedUrls, foundUrls, searchId);
            }
        }

    }

    private static String normalizeUrl(String baseUrl, String link) throws IOException {
        URL base = new URL(baseUrl);
        URL normalizedUrl = new URL(base, link);
        return normalizedUrl.toString();
    }

    private void informPartialResult(String searchId, Set<String> foundUrls) {
        Search search = searches.get(searchId);
        if (search != null) {
            search.setUrls(foundUrls);
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
