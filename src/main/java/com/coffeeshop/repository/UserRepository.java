package com.coffeeshop.repository;

import com.coffeeshop.model.RoleInfo;
import com.coffeeshop.security.UserCredentials;

import java.util.Optional;
import java.util.Set;

/** Persistence operations required by authentication. */
public interface UserRepository {
    Optional<UserCredentials> findActiveByUsername(String username);

    Set<RoleInfo> findActiveRoles(long userId);
}
