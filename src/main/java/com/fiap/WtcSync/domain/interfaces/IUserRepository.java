package com.fiap.WtcSync.domain.interfaces;

import com.fiap.WtcSync.domain.entities.User;

import java.util.List;
import java.util.Optional;

public interface IUserRepository {

    Optional<User> findByEmail(String email);

    User save(User user);

    boolean existsByEmail(String email);

    List<User> findAllBySegmentIdAndFcmTokenNotNull(String segmentId);
}
