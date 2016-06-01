package nl.amc.ebioscience.rosemary.services

import javax.inject._
import org.keyczar.Crypter

trait CryptoService {
  def encrypt(plaintext: String): String
  def decrypt(cipher: String): String
}

@Singleton
class RosemaryCryptoService @Inject() (configService: ConfigService) extends CryptoService {
  private val crypter = new Crypter(configService.getConfig("crypto.key"))

  override def encrypt(plaintext: String) = crypter.encrypt(plaintext)
  override def decrypt(cipher: String) = crypter.decrypt(cipher)
}
