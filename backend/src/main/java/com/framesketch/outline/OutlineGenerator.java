package com.framesketch.outline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framesketch.post.Post;
import com.framesketch.post.PostSection;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Gemini 를 호출해 목차를 생성하는 어댑터.
 *
 * <p>프롬프트 조립과 응답 파싱의 두 가지 책임만 가진다. 관련 글 검색이나 저장은
 * {@link OutlineService} 가 담당한다. 이렇게 쪼개 두면 프롬프트를 바꿀 때
 * 서비스 계층을 건드리지 않아도 된다.
 *
 * <p>키가 없어서 {@link ChatModel} 빈이 등록되지 않은 경우 {@link ObjectProvider}
 * 가 {@code null}-safe 하게 그 사실을 알려 준다. {@link #isAvailable()} 로 미리 확인하거나
 * {@link #generate} 가 던지는 {@link GeminiUnavailableException} 을 받아 처리한다.
 */
@Component
public class OutlineGenerator {

    private static final Logger log = LoggerFactory.getLogger(OutlineGenerator.class);

    private final ObjectProvider<ChatModel> chatModelProvider;
    // 전용 매퍼를 직접 들고 쓴다. Spring 의 전역 ObjectMapper 를 공유해도 되지만
    // Boot 4 에서 그 빈이 조건부로 등록되는 데다, 여기서는 LLM 응답 전용 설정이면 충분해
    // 그냥 인스턴스 하나를 가지고 쓴다.
    private final ObjectMapper mapper = new ObjectMapper();

    public OutlineGenerator(ObjectProvider<ChatModel> chatModelProvider) {
        this.chatModelProvider = chatModelProvider;
    }

    public boolean isAvailable() {
        return chatModelProvider.getIfAvailable() != null;
    }

    /**
     * 주제와 관련 글을 받아 LLM 에게 목차를 요청한다.
     *
     * @param relatedPosts 사용자가 이미 작성한 글 중 주제와 관련된 것들. 중복 처리의 재료.
     * @return LLM 응답. 아직 저장되지 않은 상태(id=0)로 돌려준다.
     */
    public Outline generate(String topic, DetailLevel detailLevel, List<Post> relatedPosts) {
        ChatModel model = chatModelProvider.getIfAvailable();
        if (model == null) {
            throw new GeminiUnavailableException("Gemini API 키가 설정되지 않아 목차를 생성할 수 없습니다.");
        }

        String prompt = buildPrompt(topic, detailLevel, relatedPosts);
        log.debug("Gemini 프롬프트:\n{}", prompt);

        String rawResponse;
        try {
            rawResponse = model.chat(prompt);
        } catch (RuntimeException e) {
            log.error("Gemini 호출 실패", e);
            throw new GeminiUnavailableException("Gemini 호출이 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
        log.debug("Gemini 응답:\n{}", rawResponse);

        LlmResponse parsed = parseResponse(rawResponse);
        return assemble(topic, detailLevel, parsed, relatedPosts);
    }

    // -----------------------------------------------------------------------
    // 프롬프트
    // -----------------------------------------------------------------------

    private String buildPrompt(String topic, DetailLevel level, List<Post> relatedPosts) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("""
                당신은 개인의 학습 콘텐츠 설계를 돕는 한국어 전문 어시스턴트입니다.
                사용자가 작성하려는 새 글의 목차를 설계해야 합니다.

                **새 주제**
                """);
        sb.append(topic).append("\n\n");

        sb.append("**원하는 상세도**: ").append(level.label()).append('\n');
        sb.append("- 최상위 항목 목표 개수: 약 ").append(level.targetTopItems()).append("개\n");
        sb.append("- 목차 최대 깊이: ").append(level.maxDepth()).append("단계\n\n");

        if (relatedPosts.isEmpty()) {
            sb.append("**이미 작성한 관련 글**: 없음\n\n");
        } else {
            sb.append("**이미 작성한 관련 글** (중복 처리의 재료):\n\n");
            for (int i = 0; i < relatedPosts.size(); i++) {
                Post post = relatedPosts.get(i);
                sb.append("### [글 id=").append(post.id()).append("] ").append(post.title()).append('\n');
                if (post.summary() != null && !post.summary().isBlank()) {
                    sb.append("요약: ").append(post.summary()).append('\n');
                }
                sb.append("목차:\n");
                for (PostSection section : post.sections()) {
                    sb.append("  ".repeat(Math.max(0, section.level() - 1)))
                            .append("- ").append(section.heading()).append('\n');
                }
                if (i < relatedPosts.size() - 1) {
                    sb.append('\n');
                }
            }
            sb.append('\n');
        }

        sb.append("""
                **지침**
                1. 새 주제에 맞는 목차를 생성합니다.
                2. 위에 제시된 기존 글과 겹치는 내용은 가능하면 제외하거나, 꼭 필요하면
                   "이미 다룬 X 복습" 처럼 간략하게 처리합니다. 각 항목의 "note" 에
                   어떤 글의 어떤 부분과 겹치는지 짧게 적어 주세요.
                3. 학습 흐름이 자연스러워야 합니다. 선행 지식에서 심화로 순서를 잡습니다.
                4. "level" 은 1 부터 시작하는 들여쓰기 깊이이며 상한을 넘기지 않습니다.
                5. "overlapNote" 에 전체 중복 처리 전략을 한국어 2~3문장으로 요약합니다.
                   참고한 글이 없다면 짧게 그 사실만 적습니다.
                6. "referencedPostIds" 에는 위 목록에서 실제로 참고하거나 중복을 발견한
                   글의 id 만 포함합니다.

                **응답 형식 (반드시 아래 JSON 만, 다른 텍스트 금지)**
                {
                  "outline": [
                    { "heading": "...", "level": 1, "note": "..." }
                  ],
                  "overlapNote": "...",
                  "referencedPostIds": [1, 3]
                }
                """);
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // 파싱
    // -----------------------------------------------------------------------

    /** 모델이 간혹 코드블록으로 감싸거나 설명을 덧붙여도 중괄호 범위만 추려 파싱한다. */
    private LlmResponse parseResponse(String raw) {
        String json = extractJsonObject(raw);
        try {
            LlmResponse parsed = mapper.readValue(json, LlmResponse.class);
            if (parsed.outline == null || parsed.outline.isEmpty()) {
                throw new InvalidLlmResponseException("LLM 이 비어 있는 목차를 돌려주었습니다.");
            }
            return parsed;
        } catch (Exception e) {
            log.error("LLM 응답 파싱 실패. raw={}", raw, e);
            throw new InvalidLlmResponseException("LLM 응답 형식이 올바르지 않습니다.");
        }
    }

    /** raw 에서 첫 '{' 부터 마지막 '}' 까지를 잘라 돌려준다. 그 밖의 안전장치는 두지 않는다. */
    private String extractJsonObject(String raw) {
        if (raw == null) {
            throw new InvalidLlmResponseException("LLM 이 빈 응답을 돌려주었습니다.");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new InvalidLlmResponseException("LLM 응답에서 JSON 객체를 찾지 못했습니다.");
        }
        return raw.substring(start, end + 1);
    }

    // -----------------------------------------------------------------------
    // 조립
    // -----------------------------------------------------------------------

    private Outline assemble(String topic, DetailLevel level, LlmResponse parsed, List<Post> relatedPosts) {
        List<Outline.Item> items = new ArrayList<>(parsed.outline.size());
        for (LlmItem item : parsed.outline) {
            int clampedLevel = Math.min(Math.max(item.level, 1), level.maxDepth());
            items.add(new Outline.Item(nonBlank(item.heading, "(제목 없음)"), clampedLevel, nullableTrim(item.note)));
        }

        List<Outline.Reference> references = resolveReferences(parsed.referencedPostIds, relatedPosts);
        return new Outline(0L, topic, level, nullableTrim(parsed.overlapNote), references, items, null);
    }

    /** LLM 이 돌려준 id 가 실제 관련 글 목록에 있는 것만 추려 Reference 로 만든다. */
    private List<Outline.Reference> resolveReferences(List<Long> referencedIds, List<Post> relatedPosts) {
        if (referencedIds == null || referencedIds.isEmpty()) {
            return List.of();
        }
        List<Outline.Reference> out = new ArrayList<>(referencedIds.size());
        for (Long id : referencedIds) {
            if (id == null) continue;
            Optional<Post> hit = relatedPosts.stream().filter(p -> p.id() == id).findFirst();
            hit.ifPresent(post -> out.add(new Outline.Reference(post.id(), post.title(), null)));
        }
        return out;
    }

    private String nonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private String nullableTrim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // -----------------------------------------------------------------------
    // LLM 응답 매핑 전용 레코드
    // -----------------------------------------------------------------------

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class LlmResponse {
        public List<LlmItem> outline;
        public String overlapNote;
        public List<Long> referencedPostIds;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class LlmItem {
        public String heading;
        public int level = 1;
        public String note;
    }
}
