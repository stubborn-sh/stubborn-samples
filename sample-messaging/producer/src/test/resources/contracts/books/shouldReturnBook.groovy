import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should produce a book-returned event when a book is returned"
    label 'book_returned'
    input {
        triggeredBy('bookReturnedTriggered()')
    }
    outputMessage {
        sentTo('output')
        body([bookName: 'foo'])
        headers {
            header('BOOK-NAME', 'foo')
        }
    }
}
