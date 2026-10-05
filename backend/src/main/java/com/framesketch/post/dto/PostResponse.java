package com.framesketch.post.dto;

import com.framesketch.post.Post;
import java.util.List;

/** 글 상세 응답. 목차 전체를 포함한다. */
public record PostResponse(
        long id,
        String title,
        String url,
        String summary,
        List<SectionResponse> sections,
        List<String> tags,
        String createdAt,
        String updatedAt) {

    public record SectionResponse(String heading, int level) {}

    public static PostResponse from(Post post) {
        List<SectionResponse> sections = post.sections().stream()
                .map(section -> new SectionResponse(section.heading(), section.level()))
                .toList();

        return new PostResponse(
                post.id(),
                post.title(),
                post.url(),
                post.summary(),
                sections,
                post.tags(),
                post.createdAt(),
                post.updatedAt());
    }
}
