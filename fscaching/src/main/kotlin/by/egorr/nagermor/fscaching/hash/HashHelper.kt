package by.egorr.nagermor.fscaching.hash

import org.apache.commons.codec.binary.Hex
import java.nio.file.Path

/**
 * Provides content hashing.
 */
interface HashHelper {
    /**
     * Return hash of [input] string.
     */
    fun hashString(input: String): ByteArray

    /**
     * Return hash of [file] content.
     */
    fun hashFile(file: Path): ByteArray

    companion object {
        /**
         * Convert hash [ByteArray] into hex string representation.
         */
        fun ByteArray.toHex(): String = Hex.encodeHexString(this)
    }
}
