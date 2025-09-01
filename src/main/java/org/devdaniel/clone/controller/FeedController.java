package org.devdaniel.clone.controller;

import lombok.RequiredArgsConstructor;
import org.devdaniel.clone.entity.Feed;
import org.devdaniel.clone.service.FeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService service;

    @PostMapping
    public ResponseEntity save(@RequestBody Feed feed) {
        service.saveFeed(feed);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Feed> byId(@PathVariable String id) {
        Feed feed = service.byId(java.util.UUID.fromString(id));
        if (feed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(feed);
    }

    @GetMapping
    public ResponseEntity<List<Feed>> getAll() {
        List<Feed> feeds = service.getAllFeeds();
        return ResponseEntity.ok(feeds);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(@PathVariable String id) {
        service.deleteFeed(UUID.fromString(id));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Feed> update(@PathVariable String id, @RequestBody Feed feed
    ) {
        Feed updatedFeed = service.update(UUID.fromString(id), feed);
        if (updatedFeed == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedFeed);
    }

}


