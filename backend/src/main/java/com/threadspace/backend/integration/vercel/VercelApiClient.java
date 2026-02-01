package com.threadspace.backend.integration.vercel;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Component
public class VercelApiClient {

    private static final String BASE_URL = "https://api.vercel.com";

    private final RestTemplate restTemplate;

    public VercelApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public <T> T get(String path, String token, Optional<String> teamId, Class<T> responseType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BASE_URL + path);
        teamId.ifPresent(id -> builder.queryParam("teamId", id));
        URI uri = builder.build(true).toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    responseType);

            return response.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            throw new VercelApiException(e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        }
    }
}
