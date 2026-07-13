import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should reject a person under legal age"
    request {
        method POST()
        url '/check'
        headers { contentType applicationJson() }
        body(age: 15)
    }
    response {
        status OK()
        headers { contentType applicationJson() }
        body(status: "NOT_OK")
    }
}
