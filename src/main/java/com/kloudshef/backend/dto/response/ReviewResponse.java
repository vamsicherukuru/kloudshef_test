package com.kloudshef.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long reviewerUserId;
    private Integer rating;
    private String comment;
    private String reviewerName;
    private String reviewerImageUrl;
    private LocalDateTime createdAt;
}
