package com.framesketch.outline;

import java.util.List;

/**
 * 생성·저장된 목차 하나.
 *
 * @param overlapNote    기존 글과의 중복을 어떻게 다뤘는지 LLM 이 설명한 한국어 몇 문장
 * @param references     목차를 만들 때 참고한 기존 글의 요약 정보
 * @param items          목차 항목 리스트. 리스트 순서가 곧 표시 순서다.
 * @param id             저장 전에는 0.
 */
public record Outline(
        long id,
        String topic,
        DetailLevel detailLevel,
        String overlapNote,
        List<Reference> references,
        List<Item> items,
        String createdAt) {

    public record Item(String heading, int level, String note) {}

    /**
     * 참고한 기존 글.
     *
     * @param overlapMemo 이 글과 어떻게 겹치고 어떻게 처리했는지에 대한 짧은 메모. 없을 수 있다.
     */
    public record Reference(long postId, String postTitle, String overlapMemo) {}
}
