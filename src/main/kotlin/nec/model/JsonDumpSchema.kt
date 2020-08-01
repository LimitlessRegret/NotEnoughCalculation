package nec.model

import com.dslplatform.json.CompiledJson
import com.dslplatform.json.JsonAttribute

interface ISchemaRecipe

@CompiledJson
data class JsonDumpSchema(
    val oreDict: List<SchemaOreDict>,
    val items: List<SchemaItem>,
    val sources: List<SchemaSource>
)

@CompiledJson
data class SchemaItem(
    val id: Int,
    @JsonAttribute(name = "d")
    val damage: Int,
    @JsonAttribute(name = "f")
    val isFluid: Boolean,
    @JsonAttribute(name = "uN")
    val internalName: String? = null,
    @JsonAttribute(name = "lN")
    val localizedName: String? = null
)

@CompiledJson
data class SchemaOreDict(
    val name: String,
    val ids: List<Int>
)

@CompiledJson
data class SchemaSource(
    val machines: List<SchemaMachine>? = null,
    val type: String? = null,
    @JsonAttribute(name = "n")
    val name: String? = null,
    @JsonAttribute(name = "recs")
    val recipes: List<SchemaRecipe>? = null
)

@CompiledJson
data class SchemaMachine(
    @JsonAttribute(name = "n")
    val name: String,
    @JsonAttribute(name = "recs")
    val recipes: List<SchemaMachineRecipe>
)

@CompiledJson
data class SchemaMachineRecipe(
    @JsonAttribute(name = "en")
    val en: Boolean,
    @JsonAttribute(name = "dur")
    val duration: Long,
    @JsonAttribute(name = "iI")
    val inputItems: List<SchemaItemAmount>,
    @JsonAttribute(name = "iO")
    val outputItems: List<SchemaItemAmount>,
    @JsonAttribute(name = "fI")
    val inputFluid: List<SchemaItemAmount>,
    @JsonAttribute(name = "fO")
    val outputFluid: List<SchemaItemAmount>,
    @JsonAttribute(name = "eut")
    val eut: Long? = null,
    @JsonAttribute(name = "rft")
    val rft: Long? = null
) : ISchemaRecipe

@CompiledJson
data class SchemaItemAmount(
    @JsonAttribute(name = "a")
    val amount: Int,
    val id: Int,
    val meta: String? = null,
    @JsonAttribute(name = "c")
    val chance: Int? = null
)

@CompiledJson
data class SchemaRecipe(
    @JsonAttribute(name = "o")
    val output: SchemaItemAmount,
    @JsonAttribute(name = "iI")
    val inputItems: List<SchemaOreDictItemAmount?>? = null,
    @JsonAttribute(name = "i")
    val inputItem: SchemaOreDictItemAmount? = null
) : ISchemaRecipe

@CompiledJson
data class SchemaOreDictItemAmount(
    @JsonAttribute(name = "a")
    val amount: Int? = null,
    val id: Int? = null,
    @JsonAttribute(name = "ods")
    val oreDictIds: List<Int>? = null,
    @JsonAttribute(name = "c")
    val chance: Int? = null
)
