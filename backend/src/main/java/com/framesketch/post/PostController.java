package com.framesketch.post;

import com.framesketch.post.dto.PostRequest;
import com.framesketch.post.dto.PostResponse;
import com.framesketch.post.dto.PostSummaryResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 기존 학습 글 관리 API. */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    /** 글 등록. */
    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostRequest request) {
        Post created = service.create(request);
        return ResponseEntity.created(URI.create("/api/posts/" + created.id()))
                .body(PostResponse.from(created));
    }

    /**
     * 글 목록. {@code q} 를 주면 제목·목차·태그에서 검색한다.
     *
     * <p>응답은 요약 형태다. 목차 전체가 필요하면 상세 조회를 쓴다.
     */
    @GetMapping
    public List<PostSummaryResponse> list(@RequestParam(required = false) String q) {
        return service.findAll(q).stream().map(PostSummaryResponse::from).toList();
    }

    /** 글 상세. 목차 전체를 포함한다. */
    @GetMapping("/{id}")
    public PostResponse get(@PathVariable long id) {
        return PostResponse.from(service.findById(id));
    }

    /** 글 수정. 목차와 태그는 보내온 내용으로 통째로 교체된다. */
    @PutMapping("/{id}")
    public PostResponse update(@PathVariable long id, @Valid @RequestBody PostRequest request) {
        return PostResponse.from(service.update(id, request));
    }

    /** 글 삭제. 목차·태그·검색 색인도 함께 지워진다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
