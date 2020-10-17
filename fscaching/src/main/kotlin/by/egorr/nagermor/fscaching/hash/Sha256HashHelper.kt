package by.egorr.nagermor.fscaching.hash

import org.apache.commons.codec.digest.DigestUtils
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Uses SHA-256 algorithm as hashing function.
 */
class Sha256HashHelper : HashHelper {
    private val messageDigest by lazy { MessageDigest.getInstance("SHA-256") }

    override fun hashString(input: String): ByteArray = DigestUtils.digest(messageDigest, input.encodeToByteArray())

    override fun hashFile(file: Path): ByteArray = DigestUtils.digest(messageDigest, file)
}
