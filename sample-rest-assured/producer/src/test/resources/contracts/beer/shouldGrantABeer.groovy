import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should grant a beer to a person of legal age"
    request {
        method POST()
        url '/check'
        headers { contentType applicationJson() }
        body(age: 22)
    }
    response {
        status OK()
        headers { contentType applicationJson() }
        body(status: "OK")
    }
}
