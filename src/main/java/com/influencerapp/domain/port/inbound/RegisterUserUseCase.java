package com.influencerapp.domain.port.inbound;

public interface RegisterUserUseCase {

    String execute(String email, String password);
}
