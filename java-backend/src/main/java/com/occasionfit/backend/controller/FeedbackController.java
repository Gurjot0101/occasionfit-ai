package com.occasionfit.backend.controller;

import com.occasionfit.backend.dto.req.FeedbackRequest;
import com.occasionfit.backend.model.Feedback;
import com.occasionfit.backend.model.User;
import com.occasionfit.backend.service.FeedbackService;
import com.occasionfit.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Log4j2
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    @PostMapping("/feedback")
    public ResponseEntity<Feedback> submitFeedback(@RequestHeader("Authorization") String authHeader,
                                                   @RequestBody @Valid FeedbackRequest request) {
        String accessToken = authHeader.replace("Bearer ", "");
        User user = userService.getUserFromToken(accessToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackService.submitFeedback(user, request));
    }
}