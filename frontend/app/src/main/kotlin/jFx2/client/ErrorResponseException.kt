package jFx2.client

import jFx2.forms.ErrorResponse

class ErrorResponseException(val errors : List<ErrorResponse>) : RuntimeException()