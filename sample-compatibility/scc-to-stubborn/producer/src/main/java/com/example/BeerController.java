package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BeerController {

    @PostMapping("/check")
    public ResponseEntity<String> check(@RequestBody Person person) {
        if (person.age() >= 18) {
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.badRequest().body("NOT_OK");
    }

    record Person(int age) {}
}
