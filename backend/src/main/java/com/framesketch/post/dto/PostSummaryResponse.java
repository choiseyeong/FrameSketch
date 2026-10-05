package com.framesketch.post.dto;

import com.framesketch.post.Post;
import java.util.List;

/**
 * 목록·검색 결과에 쓰는 요약 응답.
 *
 * <p>목차 전체 대신 항목 수만 싣는다. 글이 늘어나도 목록 응답이 비대해지지 않게 하려는 것으로,
 * 목차가 필요하면 상세 조회를 따로 호출한다.
 */
public record PostSummaryResponse(
        long id,
        String title,
        String url,
        String summary,
        List<String> tags,
        int sectionCount,
        String createdAt) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.id(),
                post.title(),
                post.url(),
                post.summary(),
                post.tags(),
                post.sections().size(),
                post.createdAt());
    }
}
