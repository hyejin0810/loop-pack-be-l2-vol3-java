package com.loopers.domain.user;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findById(Long id);
    User save(User user);
}
