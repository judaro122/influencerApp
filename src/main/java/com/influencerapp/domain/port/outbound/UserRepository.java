package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.User;
import java.util.Optional;
/**
 * Outbound port defining the contract for user persistence operations.
 *
 * @author judaro122
 * @since 1.0.0
 */



public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(String userId);
}
