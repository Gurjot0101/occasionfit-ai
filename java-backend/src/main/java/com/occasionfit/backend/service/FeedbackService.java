package com.occasionfit.backend.service;

import com.occasionfit.backend.dto.req.FeedbackRequest;
import com.occasionfit.backend.model.Feedback;
import com.occasionfit.backend.model.User;
import org.springframework.stereotype.Service;

@Service
public interface FeedbackService {
    Feedback submitFeedback(User user, FeedbackRequest request);
}
