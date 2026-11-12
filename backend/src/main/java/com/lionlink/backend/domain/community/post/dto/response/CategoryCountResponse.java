package com.lionlink.backend.domain.community.post.dto.response;

import com.lionlink.backend.domain.community.post.entity.PostCategory;
import java.util.Map;

public record CategoryCountResponse(long total, Map<PostCategory, Long> counts) {
}
