package com.occasionfit.backend.service;

import org.springframework.stereotype.Service;

@Service
public interface ContextService {
    void updateContext(String threadId);
}
