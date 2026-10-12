package com.framesketch.outline.dto;

import com.framesketch.outline.DetailLevel;
import com.framesketch.outline.Outline;
import java.util.List;

/** 목차 상세 응답. 생성 결과와 저장된 결과에 공통으로 쓴다. */
public record OutlineResponse(
        long id,
        String topic,
        DetailLevel detailLevel,
        String overlapNote,
        List<ItemResponse> items,
        List<ReferenceResponse> references,
        String createdAt) {

    public record ItemResponse(String heading, int level, String note) {}
    public record ReferenceResponse(long postId, String postTitle, String overlapMemo) {}

    public static OutlineResponse from(Outline outline) {
        List<ItemResponse> items = outline.items().stream()
                .map(i -> new ItemResponse(i.heading(), i.level(), i.note()))
                .toList();
        List<ReferenceResponse> refs = outline.references().stream()
                .map(r -> new ReferenceResponse(r.postId(), r.postTitle(), r.overlapMemo()))
                .toList();
        return new OutlineResponse(
                outline.id(),
                outline.topic(),
                outline.detailLevel(),
                outline.overlapNote(),
                items,
                refs,
                outline.createdAt());
    }
}
