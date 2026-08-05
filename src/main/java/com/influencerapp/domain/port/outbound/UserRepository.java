package com.influencerapp.domain.port.outbound;

import com.influencerapp.domain.model.User;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByEmail(String email);

    Optional<User> findById(String userId);
}
