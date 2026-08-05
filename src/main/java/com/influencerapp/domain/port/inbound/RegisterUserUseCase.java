package com.influencerapp.domain.port.inbound;
/**
 * Inbound port defining the contract for user registration.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface RegisterUserUseCase {

    String execute(String email, String password);
}
