package com.framesketch.post;

import com.framesketch.common.NotFoundException;
import com.framesketch.post.dto.PostRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글 등록·조회·수정·삭제와 검색.
 *
 * <p>쓰기 작업은 posts / post_sections / post_tags / posts_fts 네 테이블을 함께 건드리므로
 * 트랜잭션으로 묶는다. 중간에 실패하면 색인만 남거나 목차만 빠지는 상태가 되기 때문이다.
 */
@Service
public class PostService {

    /** 목록·검색 한 번에 돌려줄 최대 건수. */
    private static final int DEFAULT_LIMIT = 100;

    private final PostRepository repository;

    public PostService(PostRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Post create(PostRequest request) {
        long id = repository.insert(
                request.title().trim(),
                normalize(request.url()),
                normalize(request.summary()),
                toSections(request),
                normalizeTags(request.tagsOrEmpty()));

        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("방금 저장한 글을 다시 읽지 못했습니다. id=" + id));
    }

    @Transactional
    public Post update(long id, PostRequest request) {
        boolean updated = repository.update(
                id,
                request.title().trim(),
                normalize(request.url()),
                normalize(request.summary()),
                toSections(request),
                normalizeTags(request.tagsOrEmpty()));

        if (!updated) {
            throw new NotFoundException("해당 글을 찾을 수 없습니다. id=" + id);
        }

        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("방금 수정한 글을 다시 읽지 못했습니다. id=" + id));
    }

    @Transactional
    public void delete(long id) {
        if (!repository.delete(id)) {
            throw new NotFoundException("해당 글을 찾을 수 없습니다. id=" + id);
        }
    }

    @Transactional(readOnly = true)
    public Post findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("해당 글을 찾을 수 없습니다. id=" + id));
    }

    /** 검색어가 없으면 최근 글 목록을, 있으면 검색 결과를 돌려준다. */
    @Transactional(readOnly = true)
    public List<Post> findAll(String query) {
        if (query == null || query.isBlank()) {
            return repository.findAll(DEFAULT_LIMIT);
        }
        return repository.search(query, DEFAULT_LIMIT);
    }

    /** 요청의 목차 항목을 도메인 형태로 바꾼다. 표시 순서는 보내온 리스트 순서를 그대로 쓴다. */
    private List<PostSection> toSections(PostRequest request) {
        List<PostSection> sections = new ArrayList<>(request.sections().size());
        for (int i = 0; i < request.sections().size(); i++) {
            PostRequest.SectionRequest section = request.sections().get(i);
            sections.add(new PostSection(section.heading().trim(), section.levelOrDefault(), i));
        }
        return sections;
    }

    /** 앞뒤 공백을 떼고, 빈 문자열은 null 로 바꿔 저장한다. */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeTags(List<String> tags) {
        return tags.stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();
    }
}
