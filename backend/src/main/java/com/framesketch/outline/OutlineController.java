package com.framesketch.outline;

import com.framesketch.outline.dto.GenerateOutlineRequest;
import com.framesketch.outline.dto.OutlineResponse;
import com.framesketch.outline.dto.OutlineSummaryResponse;
import com.framesketch.outline.dto.SaveOutlineRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 목차 생성·저장·조회·삭제 API.
 *
 * <p>생성과 저장을 두 엔드포인트로 나눈다. 재생성이 자유로워지고, 사용자가 결과를
 * 확인한 뒤에만 DB 에 적재된다.
 */
@RestController
@RequestMapping("/api/outlines")
public class OutlineController {

    private final OutlineService service;

    public OutlineController(OutlineService service) {
        this.service = service;
    }

    /** 목차 생성. 저장은 하지 않는다. 사용자가 결과를 보고 저장 여부를 결정한다. */
    @PostMapping("/generate")
    public OutlineResponse generate(@Valid @RequestBody GenerateOutlineRequest request) {
        Outline outline = service.generate(request.topic(), request.detailLevel());
        return OutlineResponse.from(outline);
    }

    /** 생성 결과(또는 사용자가 손본 결과)를 저장한다. */
    @PostMapping
    public ResponseEntity<OutlineResponse> save(@Valid @RequestBody SaveOutlineRequest request) {
        Outline saved = service.save(request);
        return ResponseEntity.created(URI.create("/api/outlines/" + saved.id()))
                .body(OutlineResponse.from(saved));
    }

    @GetMapping
    public List<OutlineSummaryResponse> list() {
        return service.findAll().stream().map(OutlineSummaryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OutlineResponse get(@PathVariable long id) {
        return OutlineResponse.from(service.findById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
