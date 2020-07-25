package nec.persistence

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.WritableValue
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer

open class SimplePropertySerializer<V : Any, K : WritableValue<*>>(
    val klass: Class<K>,
    val valueSerializer: KSerializer<V>
) : KSerializer<K> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun deserialize(decoder: Decoder): K {
        val instance = klass.newInstance()
        instance.value = decoder.decode(valueSerializer)
        return instance
    }

    override fun serialize(encoder: Encoder, value: K) {
        encoder.encode(valueSerializer, value.value as V)
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
