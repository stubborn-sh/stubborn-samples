import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should return beer API info (consumer-b)"
    request {
        method GET()
        url '/info'
    }
    response {
        status OK()
        body([
            name: 'Beer API',
            version: '1.0'
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
