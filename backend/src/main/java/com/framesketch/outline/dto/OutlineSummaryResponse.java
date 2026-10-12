package com.framesketch.outline.dto;

import com.framesketch.outline.DetailLevel;
import com.framesketch.outline.Outline;

/** 저장된 목차 목록용 요약. 항목까지는 싣지 않고 개수만 보낸다. */
public record OutlineSummaryResponse(
        long id,
        String topic,
        DetailLevel detailLevel,
        int itemCount,
        int referenceCount,
        String createdAt) {

    public static OutlineSummaryResponse from(Outline outline) {
        return new OutlineSummaryResponse(
                outline.id(),
                outline.topic(),
                outline.detailLevel(),
                outline.items().size(),
                outline.references().size(),
                outline.createdAt());
    }
}
