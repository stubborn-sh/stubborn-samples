import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should publish a BookReturned JMS message when a book is returned"
    label 'book_returned'
    input {
        triggeredBy('bookReturned()')
    }
    outputMessage {
        sentTo('books')
        body([bookName: 'foo'])
        headers {
            header('contentType', 'application/json')
        }
    }
}
