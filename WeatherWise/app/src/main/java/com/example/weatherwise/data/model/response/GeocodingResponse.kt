package com.example.weatherwise.data.model.response

data class GeocodingResponse(
    val name: String?,
    val local_names: Map<String, String>?,
    val lat: Double,
    val lon: Double,
    val country: String?
) {
    fun getAddressName(): String {
        return when {
            name != null && country != null -> "$name, $country"
            name != null -> name
            local_names?.get("en") != null -> local_names["en"]!!
            else -> "${"%.2f".format(lat)}, ${"%.2f".format(lon)}"
        }
    }
}