package nl.amc.ebioscience.rosemary.services

import javax.inject._
import org.keyczar.Crypter

trait Crypto {
  def encrypt(plaintext: String): String
  def decrypt(cipher: String): String
}

@Singleton
class KeyCrypto @Inject() (rosemaryConfig: Config) extends Crypto {
  private val crypter = new Crypter(rosemaryConfig.getConfig("crypto.key"))

  override def encrypt(plaintext: String) = crypter.encrypt(plaintext)
  override def decrypt(cipher: String) = crypter.decrypt(cipher)
}
