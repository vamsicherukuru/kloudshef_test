package com.kloudshef.backend.service;

import com.kloudshef.backend.dto.request.ReviewRequest;
import com.kloudshef.backend.dto.response.ReviewResponse;
import com.kloudshef.backend.entity.Cook;
import com.kloudshef.backend.entity.Review;
import com.kloudshef.backend.entity.User;
import com.kloudshef.backend.exception.BadRequestException;
import com.kloudshef.backend.exception.ResourceNotFoundException;
import com.kloudshef.backend.repository.CookRepository;
import com.kloudshef.backend.repository.ReviewRepository;
import com.kloudshef.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CookRepository cookRepository;
    private final UserRepository userRepository;
    private final HuggingFaceService huggingFaceService;
    private final FcmService fcmService;

    public Page<ReviewResponse> getReviewsByCookId(Long cookId, Pageable pageable) {
        return reviewRepository.findByCookId(cookId, pageable).map(this::toResponse);
    }

    public List<ReviewResponse> getMyReviews(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse addReview(Long cookId, Long userId, ReviewRequest request) {
        Cook cook = cookRepository.findById(cookId)
                .orElseThrow(() -> new ResourceNotFoundException("Cook not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (reviewRepository.findByCookIdAndUserId(cookId, userId).isPresent()) {
            throw new BadRequestException("You have already reviewed this cook");
        }
        Review review = Review.builder()
                .cook(cook)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        reviewRepository.save(review);
        updateCookRating(cook);
        huggingFaceService.refreshReviewSummary(cook.getId());

        // Notify the chef on ALL devices
        if (cook.getUser() != null) {
            String stars = "★".repeat(request.getRating()) + "☆".repeat(5 - request.getRating());
            fcmService.sendToUser(
                    cook.getUser().getId(),
                    "New Review! " + stars,
                    user.getFirstName() + " left a " + request.getRating() + "-star review"
                            + (request.getComment() != null ? ": \"" + request.getComment() + "\"" : ""),
                    "new_review"
            );
        }

        return toResponse(review);
    }

    @Transactional
    public ReviewResponse editReview(Long reviewId, Long userId, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only edit your own reviews");
        }
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        reviewRepository.save(review);
        Cook cook = review.getCook();
        updateCookRating(cook);
        huggingFaceService.refreshReviewSummary(cook.getId());
        return toResponse(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own reviews");
        }
        Cook cook = review.getCook();
        reviewRepository.delete(review);
        updateCookRating(cook);
        huggingFaceService.refreshReviewSummary(cook.getId());
    }

    private void updateCookRating(Cook cook) {
        Double avg = reviewRepository.getAverageRatingByCookId(cook.getId());
        long count = reviewRepository.countByCookId(cook.getId());
        cook.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        cook.setTotalReviews((int) count);
        cookRepository.save(cook);
    }

    private ReviewResponse toResponse(Review review) {
        User reviewer = review.getUser();
        return ReviewResponse.builder()
                .id(review.getId())
                .reviewerUserId(reviewer.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewerName(reviewer.getFullName())
                .reviewerImageUrl(reviewer.isDpPublic() ? reviewer.getProfileImageUrl() : null)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
