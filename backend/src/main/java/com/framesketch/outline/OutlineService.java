package com.framesketch.outline;

import com.framesketch.common.NotFoundException;
import com.framesketch.outline.dto.SaveOutlineRequest;
import com.framesketch.post.Post;
import com.framesketch.post.PostService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 목차 생성·저장을 조립한다. 세 가지 책임을 가진다.
 * <ol>
 *   <li>주제로 관련 글을 검색한다 ({@link PostService#findAll(String)}).</li>
 *   <li>검색 결과를 {@link OutlineGenerator} 에 넘겨 Gemini 호출을 맡긴다.</li>
 *   <li>사용자가 저장을 선택하면 {@link OutlineRepository} 로 영속화한다.</li>
 * </ol>
 */
@Service
public class OutlineService {

    /** 관련 글을 몇 개까지 LLM 프롬프트에 넣을지. 너무 많으면 토큰이 터지고 노이즈만 늘어난다. */
    private static final int MAX_RELATED_POSTS = 10;

    /** 저장된 목차 목록 조회의 기본 상한. */
    private static final int DEFAULT_OUTLINE_LIMIT = 100;

    private final PostService postService;
    private final OutlineGenerator generator;
    private final OutlineRepository repository;

    public OutlineService(PostService postService, OutlineGenerator generator, OutlineRepository repository) {
        this.postService = postService;
        this.generator = generator;
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Outline generate(String topic, DetailLevel detailLevel) {
        List<Post> candidates = postService.findAll(topic);
        List<Post> related = candidates.size() <= MAX_RELATED_POSTS
                ? candidates
                : candidates.subList(0, MAX_RELATED_POSTS);
        return generator.generate(topic.trim(), detailLevel, related);
    }

    @Transactional
    public Outline save(SaveOutlineRequest request) {
        List<Outline.Item> items = new ArrayList<>(request.items().size());
        for (SaveOutlineRequest.ItemRequest item : request.items()) {
            items.add(new Outline.Item(
                    item.heading().trim(),
                    Math.max(1, item.level()),
                    normalize(item.note())));
        }

        List<Outline.Reference> refs = new ArrayList<>();
        for (SaveOutlineRequest.ReferenceRequest ref : request.referencesOrEmpty()) {
            refs.add(new Outline.Reference(
                    ref.postId(),
                    ref.postTitle().trim(),
                    normalize(ref.overlapMemo())));
        }

        long id = repository.insert(
                request.topic().trim(),
                request.detailLevel(),
                normalize(request.overlapNote()),
                items,
                refs);

        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("방금 저장한 목차를 다시 읽지 못했습니다. id=" + id));
    }

    @Transactional(readOnly = true)
    public Outline findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 목차를 찾을 수 없습니다. id=" + id));
    }

    @Transactional(readOnly = true)
    public List<Outline> findAll() {
        return repository.findAll(DEFAULT_OUTLINE_LIMIT);
    }

    @Transactional
    public void delete(long id) {
        if (!repository.delete(id)) {
            throw new NotFoundException("해당 목차를 찾을 수 없습니다. id=" + id);
        }
    }

    public boolean isGeneratorAvailable() {
        return generator.isAvailable();
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
