package nec.persistence

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.WritableValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class SimplePropertySerializer<V : Any, K : WritableValue<*>>(
    val klass: Class<K>,
    val valueSerializer: KSerializer<V>
) : KSerializer<K> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun deserialize(decoder: Decoder): K {
        val instance = klass.newInstance()
        instance.value = decoder.decodeSerializableValue(valueSerializer)
        return instance
    }

    override fun serialize(encoder: Encoder, value: K) {
        encoder.encodeSerializableValue(valueSerializer, value.value as V)
    }
}

class SimpleDoublePropertySerializer : SimplePropertySerializer<Double, SimpleDoubleProperty>(
    SimpleDoubleProperty::class.java, Double.serializer()
)

class SimpleIntegerPropertySerializer : SimplePropertySerializer<Int, SimpleIntegerProperty>(
    SimpleIntegerProperty::class.java, Int.serializer()
)

class SimpleBooleanPropertySerializer : SimplePropertySerializer<Boolean, SimpleBooleanProperty>(
    SimpleBooleanProperty::class.java, Boolean.serializer()
)
