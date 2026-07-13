import sh.stubborn.contract.spec.Contract

Contract.make {
    description "should publish a BookReturned event when a book is returned"
    label 'book_returned'
    input {
        triggeredBy('bookReturned()')
    }
    outputMessage {
        sentTo('books-out-0')
        body([bookName: 'foo'])
        headers {
            header('contentType', 'application/json')
        }
    }
}
