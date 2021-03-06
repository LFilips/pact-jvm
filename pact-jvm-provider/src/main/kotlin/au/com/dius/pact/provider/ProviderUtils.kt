package au.com.dius.pact.provider

import au.com.dius.pact.model.FileSource
import au.com.dius.pact.model.Interaction
import groovy.json.JsonSlurper
import org.fusesource.jansi.AnsiConsole
import java.io.File

/**
 * Common provider utils
 */
object ProviderUtils {

  @JvmStatic
  @JvmOverloads
  fun loadPactFiles(
    provider: IProviderInfo,
    pactFileDir: File,
    stateChange: Any? = null,
    stateChangeUsesBody: Boolean = true,
    verificationType: PactVerification = PactVerification.REQUEST_RESPONSE,
    packagesToScan: List<String> = emptyList(),
    pactFileAuthentication: List<String> = emptyList()
  ): List<IConsumerInfo> {
    if (!pactFileDir.exists()) {
      throw PactVerifierException("Pact file directory ($pactFileDir) does not exist")
    }

    if (!pactFileDir.isDirectory) {
      throw PactVerifierException("Pact file directory ($pactFileDir) is not a directory")
    }

    if (!pactFileDir.canRead()) {
      throw PactVerifierException("Pact file directory ($pactFileDir) is not readable")
    }

    AnsiConsole.out().println("Loading pact files for provider ${provider.name} from $pactFileDir")

    val consumers = mutableListOf<ConsumerInfo>()
    for (f in pactFileDir.listFiles { f, _ -> f.extension == "json" }) {
      val pactJson = JsonSlurper().parse(f) as Map<String, Any>
      val providerName = extractFromMap(pactJson, "provider", "name")
      if (providerName == provider.name) {
        consumers.add(ConsumerInfo(extractFromMap(pactJson, "consumer", "name").toString(),
          stateChange, stateChangeUsesBody, packagesToScan, verificationType,
          FileSource<Interaction>(f), pactFileAuthentication))
      } else {
        AnsiConsole.out().println("Skipping $f as the provider names don't match provider.name: " +
          "${provider.name} vs pactJson.provider.name: $providerName")
      }
    }
    AnsiConsole.out().println("Found ${consumers.size} pact files")
    return consumers
  }

  private fun extractFromMap(json: Map<String, Any>, vararg s: String): Any? {
    return if (s.size == 1) {
      json[s.first()]
    } else if (json.containsKey(s.first())) {
      val map = json[s.first()]
      if (map is Map<*, *>) {
        extractFromMap(map as Map<String, Any>, *s.drop(1).toTypedArray())
      } else {
        null
      }
    } else {
      null
    }
  }

  fun pactFileExists(pactFile: FileSource<Interaction>) = pactFile.file.exists()

  @JvmStatic
  fun verificationType(provider: IProviderInfo, consumer: IConsumerInfo): PactVerification {
    return consumer.verificationType ?: provider.verificationType ?: PactVerification.REQUEST_RESPONSE
  }

  @JvmStatic
  fun packagesToScan(providerInfo: IProviderInfo, consumer: IConsumerInfo): List<String> {
    return if (consumer.packagesToScan.isNotEmpty()) consumer.packagesToScan else providerInfo.packagesToScan
  }

  fun isS3Url(pactFile: Any?): Boolean {
    return pactFile is String && pactFile.toLowerCase().startsWith("s3://")
  }
}
