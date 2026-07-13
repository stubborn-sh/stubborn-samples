package contracts.beer.rest

import sh.stubborn.contract.spec.Contract
import sh.stubborn.contract.verifier.http.ContractVerifierHttpMetaData

Contract.make {
    description("""
Represents a scenario of rejecting a beer request from a minor

```
given:
    client is too young
when:
    he applies for a beer
then:
    we'll reject the request
```

""")
    request {
        method 'POST'
        url '/beer.BeerService/Check'
        body(fileAsBytes("PersonToCheck_too_young.bin"))
        headers {
            contentType("application/grpc")
            header("te", "trailers")
        }
    }
    response {
        status 200
        body(fileAsBytes("Response_too_young.bin"))
        headers {
            contentType("application/grpc")
            header("grpc-encoding", "identity")
            header("grpc-accept-encoding", "gzip")
        }
    }
    metadata([
            "verifierHttp": [
                    "protocol": ContractVerifierHttpMetaData.Protocol.H2_PRIOR_KNOWLEDGE.toString()
            ]
    ])
}
