package com.saas.platform.core;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, Long> {
    Optional<Settings> findByKey(String key);
}
