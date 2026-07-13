package com.example;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PersonController {

    @QueryMapping
    public Person personToCheck(@Argument String name) {
        if ("Old Enough".equals(name)) {
            return new Person("Old Enough", "40");
        }
        return new Person(name, "10");
    }
}
