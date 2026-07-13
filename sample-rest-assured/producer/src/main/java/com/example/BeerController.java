package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BeerController {

    @PostMapping("/check")
    ResponseEntity<BeerResponse> check(@RequestBody PersonToCheck personToCheck) {
        if (personToCheck.age() >= 20) {
            return ResponseEntity.ok(new BeerResponse("OK"));
        }
        return ResponseEntity.ok(new BeerResponse("NOT_OK"));
    }
}
