package com.influencerapp.domain.port.inbound;

public interface LoginUseCase {

    String execute(String email, String password);
}
