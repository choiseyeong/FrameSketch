package com.framesketch.outline;

/**
 * 목차 생성의 상세도 프리셋.
 *
 * <p>사용자가 depth 를 숫자로 직접 입력하지 않도록 3단계로 묶어 둔다. 각 단계는 생성될
 * 목차의 최대 깊이와 최상위 항목 개수 목표를 함께 가진다. 값은 프롬프트에 그대로 들어가
 * LLM 이 지침으로 쓰고, 응답을 서버에서 다듬을 때 느슨한 상한으로도 쓴다.
 */
public enum DetailLevel {

    /** 글 전체를 훑는 수준. 블로그 개요 글이나 발표 요약에 적합. */
    SIMPLE("간단", 1, 5),

    /** 보통 수준의 블로그 글. 기본값. */
    STANDARD("보통", 2, 8),

    /** 체계적인 학습 자료 수준. 세부 항목까지 포함. */
    DETAILED("상세", 3, 12);

    private final String label;
    private final int maxDepth;
    private final int targetTopItems;

    DetailLevel(String label, int maxDepth, int targetTopItems) {
        this.label = label;
        this.maxDepth = maxDepth;
        this.targetTopItems = targetTopItems;
    }

    public String label()           { return label; }
    public int maxDepth()           { return maxDepth; }
    public int targetTopItems()     { return targetTopItems; }
}
