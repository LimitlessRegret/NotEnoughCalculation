package nec.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ISchemaRecipe

@Serializable
data class JsonDumpSchema(
    val oreDict: List<SchemaOreDict>,
    val items: List<SchemaItem>,
    val sources: List<SchemaSource>
)

@Serializable
data class SchemaItem(
    val id: Int,
    @SerialName("d")
    val damage: Int,
    @SerialName("f")
    val isFluid: Boolean,
    @SerialName("uN")
    val internalName: String? = null,
    @SerialName("lN")
    val localizedName: String? = null
)

@Serializable
data class SchemaOreDict(
    val name: String,
    val ids: List<Int>
)

@Serializable
data class SchemaSource(
    val machines: List<SchemaMachine>? = null,
    val type: String? = null,
    @SerialName("n")
    val name: String? = null,
    @SerialName("recs")
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
    @SerialName("iI")
    val inputItems: List<SchemaItemAmount>,
    @SerialName("iO")
    val outputItems: List<SchemaItemAmount>,
    @SerialName("fI")
    val inputFluid: List<SchemaItemAmount>,
    @SerialName("fO")
    val outputFluid: List<SchemaItemAmount>,
    @SerialName("eut")
    val eut: Long? = null,
    @SerialName("rft")
    val rft: Long? = null
) : ISchemaRecipe

@Serializable
data class SchemaItemAmount(
    @SerialName("a")
    val amount: Int,
    val id: Int,
    val meta: String? = null,
    @SerialName("c")
    val chance: Int? = null
)

@Serializable
data class SchemaRecipe(
    @SerialName("o")
    val output: SchemaItemAmount,
    @SerialName("iI")
    val inputItems: List<SchemaOreDictItemAmount?>? = null,
    @SerialName("i")
    val inputItem: SchemaOreDictItemAmount? = null
) : ISchemaRecipe

@Serializable
data class SchemaOreDictItemAmount(
    @SerialName("a")
    val amount: Int? = null,
    val id: Int? = null,
    @SerialName("ods")
    val oreDictIds: List<Int>? = null,
    @SerialName("c")
    val chance: Int? = null
)
