package com.framesketch.post.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 글 등록과 수정에 공통으로 쓰는 요청 본문. 두 경우 필요한 필드가 같아 하나로 둔다.
 *
 * @param sections 목차 항목. 목차가 이 서비스의 핵심 데이터이므로 최소 한 개를 요구한다.
 */
public record PostRequest(
        @NotBlank(message = "제목은 비워 둘 수 없습니다.")
        @Size(max = 300, message = "제목은 300자를 넘을 수 없습니다.")
        String title,

        @Size(max = 1000, message = "URL 은 1000자를 넘을 수 없습니다.")
        String url,

        @Size(max = 2000, message = "요약은 2000자를 넘을 수 없습니다.")
        String summary,

        @NotEmpty(message = "목차 항목을 최소 한 개 입력해야 합니다.")
        @Size(max = 200, message = "목차 항목은 200개를 넘을 수 없습니다.")
        @Valid
        List<SectionRequest> sections,

        @Size(max = 20, message = "태그는 20개를 넘을 수 없습니다.")
        List<@NotBlank @Size(max = 50) String> tags) {

    /**
     * 목차 항목.
     *
     * @param level 1 이 최상위. 생략하면 1 로 본다. 표시 순서는 리스트 순서를 그대로 쓰므로
     *              요청에서 받지 않는다.
     */
    public record SectionRequest(
            @NotBlank(message = "목차 항목 제목은 비워 둘 수 없습니다.")
            @Size(max = 300, message = "목차 항목 제목은 300자를 넘을 수 없습니다.")
            String heading,

            @Min(value = 1, message = "목차 깊이는 1 이상이어야 합니다.")
            @Max(value = 6, message = "목차 깊이는 6 이하여야 합니다.")
            Integer level) {

        public int levelOrDefault() {
            return level == null ? 1 : level;
        }
    }

    /** tags 는 선택 항목이라 null 로 올 수 있다. 호출부에서 null 을 신경 쓰지 않게 감싼다. */
    public List<String> tagsOrEmpty() {
        return tags == null ? List.of() : tags;
    }
}
