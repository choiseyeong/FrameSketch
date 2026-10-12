package com.framesketch.outline.dto;

import com.framesketch.outline.DetailLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 생성 결과를 저장하는 요청.
 *
 * <p>생성 응답과 거의 같은 모양이지만 "방금 받은 결과를 확인해 보고 저장을 택했다"는
 * 두 단계를 유지하기 위해 분리한다. 이렇게 하면 사용자가 재생성을 반복해도 DB 가 쌓이지
 * 않는다.
 */
public record SaveOutlineRequest(
        @NotBlank @Size(max = 500) String topic,
        @NotNull DetailLevel detailLevel,
        @Size(max = 2000) String overlapNote,

        @NotEmpty(message = "목차 항목이 비어 있습니다.")
        @Size(max = 200)
        @Valid
        List<ItemRequest> items,

        @Size(max = 50) List<ReferenceRequest> references) {

    public record ItemRequest(
            @NotBlank @Size(max = 300) String heading,
            int level,
            @Size(max = 500) String note) {}

    public record ReferenceRequest(
            long postId,
            @NotBlank @Size(max = 300) String postTitle,
            @Size(max = 500) String overlapMemo) {}

    public List<ReferenceRequest> referencesOrEmpty() {
        return references == null ? List.of() : references;
    }
}
