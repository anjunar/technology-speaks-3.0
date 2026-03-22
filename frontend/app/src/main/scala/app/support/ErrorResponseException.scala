package app.support

class ErrorResponseException(val errors: Seq[ErrorResponse]) extends RuntimeException
