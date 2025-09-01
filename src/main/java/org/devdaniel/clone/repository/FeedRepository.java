package org.devdaniel.clone.repository;

import org.devdaniel.clone.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, UUID> {
}
