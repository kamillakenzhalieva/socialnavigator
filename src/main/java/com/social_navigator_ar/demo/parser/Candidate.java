package com.social_navigator_ar.demo.parser;

public record Candidate(
        String source,
        String externalId,
        String title,
        String personName,
        String city,
        String text,
        String url,
        String section
) {
    public Candidate(String source, String externalId, String title, String personName,
                     String city, String text, String url) {
        this(source, externalId, title, personName, city, text, url, null);
    }
}
