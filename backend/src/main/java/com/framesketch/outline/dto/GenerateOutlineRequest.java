package com.framesketch.outline.dto;

import com.framesketch.outline.DetailLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 목차 생성 요청. 생성만 하고 저장하지는 않는다. */
public record GenerateOutlineRequest(
        @NotBlank(message = "주제는 비워 둘 수 없습니다.")
        @Size(max = 500, message = "주제는 500자를 넘을 수 없습니다.")
        String topic,

        @NotNull(message = "상세도를 선택해야 합니다.")
        DetailLevel detailLevel) {}
