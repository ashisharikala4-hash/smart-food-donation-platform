package com.donation.service;

import com.donation.dto.request.LoginRequest;
import com.donation.dto.request.RegisterRequest;
import com.donation.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
