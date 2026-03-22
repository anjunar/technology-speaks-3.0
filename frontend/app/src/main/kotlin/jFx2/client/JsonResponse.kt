package jFx2.client

import kotlinx.serialization.Serializable

@Serializable
class JsonResponse(val status : String, val message : String? = null)