import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should send a verification event when verification is triggered"
    label 'verification_received'
    input {
        triggeredBy('verificationEventTriggered()')
    }
    outputMessage {
        sentTo('verifications')
        body([bookName: 'foo'])
        headers {
            header('contentType', 'application/json')
        }
    }
}
