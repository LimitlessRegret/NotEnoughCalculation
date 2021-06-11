package nec

import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.ceil

data class GTEutDuration(
    val euT: Int,
    val duration: Int
)

/**
 * Adapted from https://github.com/GregTechCE/GregTech/blob/master/src/main/java/gregtech/api/capability/impl/AbstractRecipeLogic.java
 */
object GTOverclock {
    val V = longArrayOf(8, 32, 128, 512, 2048, 8192, 32768, 131072, 524288, Int.MAX_VALUE.toLong())
    val TIER_NAMES = arrayOf("ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "MAX")

    const val ULV = 0
    const val LV = 1
    const val MV = 2
    const val HV = 3
    const val EV = 4
    const val IV = 5
    const val LuV = 6
    const val ZPM = 7
    const val UV = 8
    const val MAX = 9

    /**
     * @return lowest tier that can handle passed voltage
     */
    fun getTierByVoltage(voltage: Long): Int {
        var tier = 0
        while (++tier < V.size) {
            if (voltage == V[tier]) {
                return tier
            } else if (voltage < V[tier]) {
                return max(0, tier - 1)
            }
        }
        return min(V.size - 1, tier)
    }

    fun calculateOverclock(EUt: Int, voltage: Long, duration: Int): GTEutDuration {
        var EUt = EUt

        val negativeEU = EUt < 0
        val tier: Int = getTierByVoltage(voltage)
        if (V[tier] <= EUt || tier == 0) return GTEutDuration(EUt, duration)
        if (negativeEU) EUt = -EUt
        return if (EUt <= 16) {
            val multiplier = if (EUt <= 8) tier else tier - 1
            val resultEUt = EUt * (1 shl multiplier) * (1 shl multiplier)
            val resultDuration = duration / (1 shl multiplier)
            GTEutDuration(if (negativeEU) -resultEUt else resultEUt, resultDuration)
        } else {
            var resultEUt = EUt
            var resultDuration = duration.toDouble()
            //do not overclock further if duration is already too small
            while (resultDuration >= 3 && resultEUt <= V[tier - 1]) {
                resultEUt *= 4
                resultDuration /= 2.8
            }
            GTEutDuration(if (negativeEU) -resultEUt else resultEUt, ceil(resultDuration).toInt())
        }
    }
}
