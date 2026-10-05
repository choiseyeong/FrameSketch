package com.framesketch.post;

import java.util.List;

/**
 * 기존에 작성한 학습 글 하나.
 *
 * <p>본문은 보관하지 않고 제목과 목차만 저장한다. 중복 판단도 목차 레벨에서 이루어진다.
 */
public record Post(
        long id,
        String title,
        String url,
        String summary,
        List<PostSection> sections,
        List<String> tags,
        String createdAt,
        String updatedAt) {

    /** 전문 검색 색인의 headings 열에 넣을 문자열. 목차 제목을 줄바꿈으로 이어 붙인다. */
    public String headingsAsText() {
        return String.join("\n", sections.stream().map(PostSection::heading).toList());
    }

    /** 전문 검색 색인의 tags 열에 넣을 문자열. */
    public String tagsAsText() {
        return String.join(" ", tags);
    }
}
