import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should publish a verification event to SQS when a verification is triggered"
    label 'verification_published'
    input {
        triggeredBy('verificationTriggered()')
    }
    outputMessage {
        sentTo('verifications')
        body([bookName: 'foo'])
        headers {
            header('contentType', 'application/json')
        }
    }
}
