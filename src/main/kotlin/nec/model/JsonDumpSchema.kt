package nec.model

import kotlinx.serialization.*

@Serializable
data class JsonDumpSchema(
    val sources: List<SchemaSource>
)

@Serializable
data class SchemaSource(
    val machines: List<SchemaMachine>? = null,
    val type: String,
    val recipes: List<SchemaRecipe>? = null
)

@Serializable
data class SchemaMachine(
    @SerialName("n")
    val name: String,
    @SerialName("recs")
    val recipes: List<SchemaMachineRecipe>
)

@Serializable
data class SchemaMachineRecipe(
    @SerialName("en")
    val en: Boolean,
    @SerialName("dur")
    val duration: Long,
    @SerialName("eut")
    val eut: Long,
    @SerialName("iI")
    val inputItems: List<SchemaItem>,
    @SerialName("iO")
    val outputItems: List<SchemaItem>,
    @SerialName("fI")
    val inputFluid: List<SchemaItem>,
    @SerialName("fO")
    val outputFluid: List<SchemaItem>
)

@Serializable
data class SchemaItem(
    @SerialName("a")
    val amount: Long,
    @SerialName("uN")
    val internalName: String? = null,
    @SerialName("lN")
    val localizedName: String? = null,
    @SerialName("cfg")
    val cfg: Long? = null
)

@Serializable
data class SchemaRecipe(
    @SerialName("iI")
    val inputItems: List<SchemaCTItem?>,
    @SerialName("o")
    val output: SchemaItem
)

@Serializable
data class SchemaCTItem(
    @SerialName("a")
    val amount: Long? = null,
    @SerialName("uN")
    val internalName: String? = null,
    @SerialName("lN")
    val localizedName: String? = null,
    @SerialName("dns")
    val dictionaryNames: List<String>? = null,
    @SerialName("ims")
    val itemsMatching: List<SchemaItem>? = null
)
