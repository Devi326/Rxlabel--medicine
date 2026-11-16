package com.example.andriod.data.api

import com.google.gson.annotations.SerializedName

data class OpenFdaResponse(
    @SerializedName("results") val results: List<DrugResult>? = null,
    @SerializedName("error") val error: OpenFdaError? = null
)

data class OpenFdaError(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null
)

data class DrugResult(
    @SerializedName("openfda") val openfda: OpenFdaDetails? = null,
    @SerializedName("purpose") val purpose: List<String>? = null,
    @SerializedName("indications_and_usage") val indicationsAndUsage: List<String>? = null,
    @SerializedName("dosage_and_administration") val dosageAndAdministration: List<String>? = null,
    @SerializedName("warnings") val warnings: List<String>? = null,
    @SerializedName("do_not_use") val doNotUse: List<String>? = null
)

data class OpenFdaDetails(
    @SerializedName("brand_name") val brandName: List<String>? = null,
    @SerializedName("generic_name") val genericName: List<String>? = null,
    @SerializedName("manufacturer_name") val manufacturerName: List<String>? = null
)
