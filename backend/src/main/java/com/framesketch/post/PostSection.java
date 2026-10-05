package com.framesketch.post;

/**
 * 글의 목차 항목 하나.
 *
 * @param heading  항목 제목
 * @param level    들여쓰기 깊이. 1 이 최상위다.
 * @param orderIdx 같은 글 안에서의 표시 순서. 0 부터 시작한다.
 */
public record PostSection(String heading, int level, int orderIdx) {}
