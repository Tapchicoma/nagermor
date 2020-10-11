package by.egorr.nagermor.fscaching.hash

import by.egorr.nagermor.fscaching.hash.HashHelper.Companion.toHex
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.assertEquals

internal class Sha256HashHelperTest {
    private val hashHelper = Sha256HashHelper()

    @ParameterizedTest
    @MethodSource("provideTestStringHashes")
    internal fun `Should hash correctly string`(
        inputString: String,
        expectedHash: String
    ) {
        assertEquals(
            expectedHash,
            hashHelper.hashString(inputString).toHex()
        )
    }

    @ParameterizedTest
    @MethodSource("provideTestFileHashes")
    internal fun `Should has correctly files`(
        file: Path,
        expectedHash: String
    ) {
        assertEquals(
            expectedHash,
            hashHelper.hashFile(file).toHex()
        )
    }

    companion object {
        @JvmStatic
        fun provideTestStringHashes(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
            ),
            Arguments.of(
                "test string\n",
                "37d2046a395cbfcb2712ff5c96a727b1966876080047c56717009dbbc235f566"
            ),
            Arguments.of(
                "/path/to/some/file\n",
                "9b089b4e7ef81e1c141c4c35760217997cac10061f66fe5e7ac77e2115d20e01"
            )
        )

        @JvmStatic
        fun provideTestFileHashes(): Stream<Arguments> {
            val classloader = this::class.java.classLoader

            fun getResource(resourcePath: String): Path = Path.of(classloader.getResource(resourcePath)?.file)

            return Stream.of(
                Arguments.of(
                    getResource("hash/test_file1.txt"),
                    "13d60d11ceccc5808b1f58b523d07a9bd998d5bf4538de9c535595155cdb62fc"
                ),
                Arguments.of(
                    getResource("hash/commons-codec-1.14.jar"),
                    "a128e4f93fabe5381ded64cf2873019e06030b718eb43ceeae0b0e5d17ad33e9"
                ),
            )
        }
    }
}
