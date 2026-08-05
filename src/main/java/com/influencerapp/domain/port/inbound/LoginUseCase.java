package com.influencerapp.domain.port.inbound;
/**
 * Inbound port defining the contract for user authentication.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface LoginUseCase {

    String execute(String email, String password);
}
