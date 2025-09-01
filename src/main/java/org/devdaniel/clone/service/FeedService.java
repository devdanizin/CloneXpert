package org.devdaniel.clone.service;

import lombok.AllArgsConstructor;
import org.devdaniel.clone.entity.Feed;
import org.devdaniel.clone.repository.FeedRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class FeedService {

    private final FeedRepository feedRepository;

    public void saveFeed(Feed feed) {
        feedRepository.save(feed);
    }

    public Feed byId(UUID id) {
        return feedRepository.findById(id).orElse(null);
    }

    public List<Feed> getAllFeeds() {
        return feedRepository.findAll();
    }

    public void deleteFeed(UUID id) {
        feedRepository.deleteById(id);
    }

    public Feed update(UUID id, Feed feedDetails) {
        Feed feed = byId(id);
        if (feed == null) return null;
        feed.setName(feedDetails.getName());
        feed.setMessage(feedDetails.getMessage());
        return feedRepository.save(feed);
    }
}