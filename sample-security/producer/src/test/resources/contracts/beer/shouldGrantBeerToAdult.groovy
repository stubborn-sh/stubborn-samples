import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should grant beer to an adult"
    request {
        method POST()
        url '/check'
        body([age: 22])
        headers {
            contentType(applicationJson())
        }
    }
    response {
        status OK()
        body([status: 'OK'])
        headers {
            contentType(applicationJson())
        }
    }
}
