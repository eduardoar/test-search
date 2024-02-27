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
import java.net.URLConnection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    private final String BASE_URL = System.getenv("BASE_URL");

    private final String USER_AGENT = "Mozilla/5.0";
    private static final String REGEX = "<a\\s+[^>]*?href=\"([^\"]*)\"";

    public SearchResponse initiateSearch(SearchRequest searchRequest) {
        String keyword = searchRequest.getKeyword();
        String searchId = generateSearchId();
        searches.put(searchId, new Search(searchId));

        CompletableFuture.runAsync(() -> {
            try {
                startSearch(searchId, keyword);
            } catch (IOException e) {
                System.err.println("Error during search: " + e.getMessage());
            }
        }, executorService);

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
        if (BASE_URL == null || BASE_URL.isEmpty()) {
            System.err.println("BASE_URL environment variable is not set.");
            return;
        }

        LinkedList<String> foundUrls = new LinkedList<>();
        Set<String> visitedUrls = new HashSet<>();
        Map<String, Queue<String>> pageQueues = new ConcurrentHashMap<>();

        pageQueues.put(BASE_URL, new LinkedList<>());
        pageQueues.get(BASE_URL).add(BASE_URL);

        pageQueues.forEach((page, queue) -> {
            try {
                crawlUrls(keyword, visitedUrls, foundUrls, searchId, queue, page, pageQueues);
            } catch (IOException e) {
                System.err.println("Error during startSearch: " + e.getMessage());
            }
        });

        Search search = searches.get(searchId);
        search.setStatus(Status.done.toString());
        search.setUrls(foundUrls);
        shutdown();
    }

    private void crawlUrls(String keyword, Set<String> visitedUrls, LinkedList<String> foundUrls, String searchId, Queue<String> queue, String page, Map<String, Queue<String>> pageQueues) throws IOException {
        while (!queue.isEmpty()) {
            String currentUrl = queue.poll();

            if (!isValidUrl(currentUrl, visitedUrls)) {
                continue;
            }

            visitedUrls.add(currentUrl);

            processPageContent(getPageContent(currentUrl), currentUrl, keyword, foundUrls, searchId, page, pageQueues);
        }
    }

    private String getPageContent(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            return content.toString();
        } catch (IOException e) {
            System.err.println("URL: " + url + " Not Dound.");
        }
        return "";
    }

    private void processPageContent(String content, String currentUrl, String keyword, LinkedList<String> foundUrls, String searchId, String page, Map<String, Queue<String>> pageQueues) throws IOException {
        if (content.toLowerCase().contains(keyword.toLowerCase())) {
            foundUrls.add(currentUrl);
            informPartialResult(searchId, foundUrls);
        }

        Pattern pattern = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String link = matcher.group(1);
            if (!link.startsWith("http")) {
                link = normalizeUrl(currentUrl, link);
            }
            pageQueues.computeIfAbsent(page, k -> new LinkedList<>()).add(link);
        }
    }

    private boolean isValidUrl(String url, Set<String> visitedUrls) {
        return url.startsWith(BASE_URL) && !visitedUrls.contains(url);
    }

    private static String normalizeUrl(String baseUrl, String link) throws IOException {
        URL base = new URL(baseUrl);
        URL normalizedUrl = new URL(base, link);
        return normalizedUrl.toString();
    }

    private void informPartialResult(String searchId, LinkedList<String> foundUrls) {
        Search search = searches.get(searchId);
        if (search != null) {
            search.setUrls(foundUrls);
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
