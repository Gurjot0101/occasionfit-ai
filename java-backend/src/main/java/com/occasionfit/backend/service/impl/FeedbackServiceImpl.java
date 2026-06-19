package com.occasionfit.backend.service.impl;

import com.occasionfit.backend.dto.req.FeedbackRequest;
import com.occasionfit.backend.model.Feedback;
import com.occasionfit.backend.model.User;
import com.occasionfit.backend.repository.FeedbackRepository;
import com.occasionfit.backend.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Override
    public Feedback submitFeedback(User user, FeedbackRequest request) {
        Feedback feedback = Feedback.builder()
                .userId(user.getId())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .feedback(request.getFeedback())
                .rating(request.getRating())
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Feedback saved for userId={} rating={}",
                user.getId(), request.getRating());
        return saved;
    }
}
