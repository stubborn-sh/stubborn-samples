package com.example;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BeerController {

    @PostMapping("/check")
    ResponseEntity<BeerResponse> check(@RequestBody PersonToCheck person) {
        if (person.age() >= 18) {
            return ResponseEntity.ok(new BeerResponse("OK"));
        }
        return ResponseEntity.ok(new BeerResponse("NOT_OK"));
    }

    @GetMapping("/info")
    ResponseEntity<InfoResponse> info() {
        return ResponseEntity.ok(new InfoResponse("Beer API", "1.0"));
    }
}
