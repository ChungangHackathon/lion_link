package com.lionlink.backend.domain.community.post.dto.response;

public record PostLikeResponse(Long postId, boolean liked, long likeCount) {
}
