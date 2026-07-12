package com.example;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class BeerController {

    @PostMapping(value = "/check", consumes = "application/json", produces = "application/json")
    Response check(@RequestBody PersonToCheck personToCheck) {
        return personToCheck.age >= 18
                ? new Response(BeerCheckStatus.OK)
                : new Response(BeerCheckStatus.NOT_OK);
    }
}

record PersonToCheck(int age) {}
record Response(BeerCheckStatus status) {}

enum BeerCheckStatus { OK, NOT_OK }
